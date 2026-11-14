# University Management System

A simple **CSE 3230, Lab 04** project using **Java 21, Spring Boot, Spring Security, Spring Data JPA, Thymeleaf, and PostgreSQL**. The frontend uses plain CSS and JavaScript; no Node.js setup is needed.

## What it does

- Email/password login returning a signed JWT, with server-enforced token revocation on logout.
- Separate Student, Teacher, and Admin directories.
- List, search, create, edit, and delete records from the browser.
- REST CRUD endpoints with JSON responses and HTTP status codes.
- BCrypt password hashing, input validation, Bearer-token authentication, and role/ownership checks.
- PostgreSQL storage that survives container restarts.

| Signed-in role | Student section | Teacher section | Admin section |
| --- | --- | --- | --- |
| Admin | Full CRUD | Full CRUD | Full CRUD |
| Teacher | No access | View all; edit/delete own account | No access |
| Student | View all; edit/delete own account | No access | No access |

Only Admin can create accounts or edit/delete other people's records. Students and Teachers can see the full list in their own section and can edit or delete only their own account. Spring Security restricts section access; the service checks record ownership using the signed-in account. These checks also apply to direct API calls.

## Run with Docker (recommended)

Install and start Docker Desktop with Docker Compose, open a terminal in this project folder, and run:

```powershell
.\setup.ps1
docker compose up -d --build
```

The setup script generates a random JWT signing key in the ignored `.env` file. It preserves an existing key. Do not commit or share that file. For Linux/macOS, set `JWT_SECRET` to a Base64-encoded random key of at least 32 bytes before starting Compose.

When the application has started, open **http://localhost:8085**.

The Dockerfile builds and tests the application. Compose starts PostgreSQL first and waits for its health check before starting the app. You do not need Java, Maven, or PostgreSQL installed separately for this option.

Run `docker compose stop` to stop the services, or `docker compose down` to remove the containers. The named PostgreSQL volume keeps your records. These are local lab credentials; the optional `.env.example` file shows how to change the database password before the first run.

For later runs, start Docker Desktop and run `docker compose up -d`. Use `docker compose stop` to stop the app and database, and `docker compose ps` to check their status.

### Demo login accounts

| Role | Login email | Password |
| --- | --- | --- |
| Admin | `admin@university.edu` | `Admin@123` |
| Teacher | `farhana@cse.kuet.ac.bd` | `Teacher@123` |
| Student | `arif@stud.kuet.ac.bd` | `Student@123` |

Enter these credentials in the login form. Six sample accounts are created **only when the database is empty**. All roles log in with their email address and password. The username remains a profile field. New accounts use the email address and password entered by Admin. Demo data is intended for this local lab project.

## Run from IntelliJ or a terminal

1. Install JDK 21 and PostgreSQL, or start just the database with `docker compose up -d db`.
2. For a locally installed PostgreSQL server, use pgAdmin or `psql` to run the following as a PostgreSQL administrator:

   ```sql
   CREATE USER university WITH PASSWORD 'university123';
   CREATE DATABASE university_db OWNER university;
   ```

3. Open `pom.xml` as a Maven project in IntelliJ, select JDK 21, and run `UniversityApplication`. Alternatively, use the included Maven Wrapper:

   ```powershell
   # Windows PowerShell
   .\mvnw.cmd spring-boot:run
   ```

   ```sh
   # macOS / Linux
   sh mvnw spring-boot:run
   ```

4. Open **http://localhost:8085** and sign in.

The Maven Wrapper downloads Maven automatically on first use. It needs internet access to download Maven and dependencies. Set `JAVA_HOME` to your JDK 21 folder if your terminal uses an older Java installation.

A direct Java/IDE run also needs the `JWT_SECRET` environment variable. Run `setup.ps1` once, then set that environment variable to the `JWT_SECRET` value from `.env` in your IDE run configuration.

Default database settings are in `src/main/resources/application.properties`. Override them with environment variables when needed:

```powershell
$env:DB_URL = "jdbc:postgresql://localhost:5432/university_db"
$env:DB_USERNAME = "university"
$env:DB_PASSWORD = "university123"
$env:JWT_SECRET = (Get-Content .env | Where-Object { $_ -match "^JWT_SECRET=" }) -replace "^JWT_SECRET=", ""
.\mvnw.cmd spring-boot:run
```

If port 8085 is busy, set `PORT` for a local Java run, or set `APP_PORT` in `.env` for Docker Compose.

## REST API

All resource operations require login. The same methods work on `/api/students`, `/api/teachers`, and `/api/admins`, subject to the permissions above.

| Method | Example endpoint | Result |
| --- | --- | --- |
| GET | `/api/students` | `200 OK`: JSON list |
| GET | `/api/students/{id}` | `200 OK`: one JSON record |
| POST | `/api/students` | `201 Created`: JSON record and `Location` header |
| PUT | `/api/students/{id}` | `200 OK`: updated JSON record |
| DELETE | `/api/students/{id}` | `204 No Content` |

Example create request body:

```json
{
  "name": "Ayesha Khan",
  "username": "ayesha",
  "email": "ayesha@stud.kuet.ac.bd",
  "department": "Computer Science",
  "password": "Ayesha@123"
}
```

For updates, send all four profile fields. Omit `password`, send `null`, or send an empty string to keep the current password. A new password must have at least 8 characters and at most 72 UTF-8 bytes. Usernames and emails are stored in lowercase. Login matches email addresses without case sensitivity. The endpoint fixes the record's role. Students and Teachers cannot change their role by adding `role: ADMIN` to an update request.

Student emails must use `@stud.kuet.ac.bd`; teacher emails must use `@cse.kuet.ac.bd`. These rules apply to both create and update requests. Admin emails can use any valid domain.

Passwords and password hashes are never returned. IDs are shared across the three directories. A teacher ID requested through `/api/students/{id}` returns `404`.

Other responses: `400` invalid input, `401` login required, `403` forbidden section/record access, `404` record not found, and `409` duplicate username/email or attempted Admin self-deletion. Changing an account's email/password revokes all its outstanding JWTs; deleting it also makes its tokens invalid. Students and Teachers may delete their own accounts. Admin self-deletion is blocked to preserve administrative access.

### JWT authentication

`POST /api/auth/login` accepts JSON with `email` and `password`. It returns:

```json
{
  "accessToken": "<signed-jwt>",
  "tokenType": "Bearer",
  "expiresIn": 1800,
  "user": {"id": 1, "name": "University Admin", "username": "admin", "email": "admin@university.edu", "department": "Administration", "role": "ADMIN"}
}
```

Send the token as `Authorization: Bearer <accessToken>` on protected requests. `GET /api/me` returns the authenticated profile. `POST /api/auth/logout` returns 204 and revokes **all tokens for that account**, including tokens in other tabs/devices.

The app issues its own JWTs and uses Spring Security Resource Server to validate them; no external identity provider is required. JWTs use HS256 signatures and include issuer, audience, account ID (`sub`), email, role, expiry, issued-at time, token ID, and token version. Passwords and password hashes are never included. The server verifies the signature, timestamps, issuer, audience, current account details, and stored token version. Expired, altered, revoked, or deleted-account tokens receive 401.

HTTP security uses `SessionCreationPolicy.STATELESS`. There are no login sessions, authentication cookies, or CSRF-token endpoints. Authentication is accepted only from the explicit Bearer header, so browsers do not automatically attach authentication to cross-site requests. CSRF is disabled for this header-only API design; adding cookie-based authentication would require reconsidering CSRF protection.

The browser stores the access token in **sessionStorage** for the current tab and clears it on logout or 401. It loads account information through `/api/me`; the public dashboard HTML contains no private account data. There is no refresh token: after the default 30-minute expiry, sign in again. A production system should also address XSS, HTTPS, login rate limiting, and its token-storage requirements.

The signing key persists in `.env`, so restarting the app does not invalidate unexpired tokens when the key and account version remain unchanged. Token versions are stored in PostgreSQL; revocation therefore survives restarts. Normal profile updates cannot reset the version counter.

### Test the API in PowerShell

```powershell
$base = "http://localhost:8085"
$loginBody = @{email="admin@university.edu"; password="Admin@123"} | ConvertTo-Json
$login = Invoke-RestMethod "$base/api/auth/login" -Method Post -ContentType "application/json" -Body $loginBody
$headers = @{Authorization="Bearer " + $login.accessToken}

Invoke-RestMethod "$base/api/me" -Headers $headers
Invoke-RestMethod "$base/api/students" -Headers $headers

$body = @{
    name = "Ayesha Khan"
    username = "ayesha"
    email = "ayesha@stud.kuet.ac.bd"
    department = "Computer Science"
    password = "Ayesha@123"
} | ConvertTo-Json

$created = Invoke-RestMethod "$base/api/students" -Method Post -Headers $headers -ContentType "application/json" -Body $body
$created

Invoke-WebRequest "$base/api/auth/logout" -Method Post -Headers $headers | Out-Null
# The previous access token is now revoked.
```

In Postman, POST JSON credentials to `/api/auth/login`, copy `accessToken`, and select **Authorization / Bearer Token** for subsequent requests. Login returns JSON, with no redirect or session cookie.

## Tests

```powershell
.\mvnw.cmd test
```

The automated tests cover the role-access matrix, permitted CRUD, forbidden writes, login/logout, JSON errors, validation, hashing, JWT signatures/claims/expiry, role-escalation attempts, record ownership, self-deletion, email login, and token revocation. Tests use a temporary H2 database in PostgreSQL compatibility mode, so running them does not touch your application data. The application itself uses PostgreSQL.

For code locations, likely teacher questions, and demonstration commands, read [VIVA_GUIDE.md](VIVA_GUIDE.md).

## Project guide for the lab

```text
src/main/java/com/example/university/
  UniversityApplication.java       Application entry point
  model/                           UserAccount entity and Role enum
  repository/                      Database queries using Spring Data JPA
  dto/                             Request validation and safe JSON responses
  service/                         CRUD rules, email login, hashing, token revocation
  security/                        JWT signing, validation, and account checks
  controller/                      REST endpoints, pages, error handling
  config/                          Security rules and sample data
src/main/resources/
  application.properties           PostgreSQL configuration
  templates/                       Login and dashboard HTML
  static/                          CSS and JavaScript
src/test/                          API and security tests
setup.ps1                          Generates the private JWT signing key in .env
Dockerfile                         Builds and runs the Java app
compose.yaml                       App + PostgreSQL services and data volume
```

To keep the code small, all three resources share one `user_accounts` table with a `role` column, one service, and one REST controller. They still have separate REST URLs and access rules. The role-specific screens use the same dashboard template.

Request flow: **Browser → Spring Security → Controller → Service → Repository → PostgreSQL**.

For a lab demonstration, sign in as Admin, create a student, edit the record, then sign in as Teacher and Student to show their separate menus. Trying another role's API URL returns `403`. Students and Teachers can see their peers but have Edit/Delete buttons only on their own row. Direct attempts to edit/delete another person's record return `403`.

Reference: [Spring Security JWT support](https://docs.spring.io/spring-security/reference/6.5/servlet/oauth2/resource-server/jwt.html).
