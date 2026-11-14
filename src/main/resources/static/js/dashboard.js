"use strict";
const $ = id => document.getElementById(id);
const tokenKey = "campus.accessToken";
let role;
let currentUserId;
let isAdmin;
let allowedSections = [];
const labels = {students: "Student", teachers: "Teacher", admins: "Admin"};
const cache = {};
let section;
let editingId = null;
let deletingId = null;
let toastTimer;

async function api(path, options = {}) {
    const response = await fetch(path, {
        ...options,
        headers: {"Content-Type": "application/json",
            "Authorization": "Bearer " + sessionStorage.getItem(tokenKey), ...options.headers}
    });
    if (response.status === 401) {
        sessionStorage.removeItem(tokenKey);
        window.location.replace("/login?expired");
        throw new Error("Please sign in again.");
    }
    if (response.status === 204) return null;
    const data = await response.json();
    if (!response.ok) {
        const details = Object.entries(data.errors || {}).map(([field, message]) => `${field}: ${message}`).join(". ");
        throw new Error(details || data.message || "Something went wrong. Please try again.");
    }
    return data;
}

function notice(id, message = "") {
    $(id).textContent = message;
    $(id).hidden = !message;
}

function toast(message) {
    clearTimeout(toastTimer);
    $("toast").textContent = message;
    $("toast").hidden = false;
    toastTimer = setTimeout(() => $("toast").hidden = true, 3500);
}

function element(tag, className, text) {
    const node = document.createElement(tag);
    if (className) node.className = className;
    if (text !== undefined) node.textContent = text;
    return node;
}

function renderStats() {
    $("stats-grid").replaceChildren();
    allowedSections.forEach(key => {
        const card = element("button", "stat-card" + (key === section ? " selected" : ""));
        card.type = "button";
        card.addEventListener("click", () => switchSection(key));
        const top = element("div", "stat-top");
        top.append(element("span", "", "Total " + key));
        card.append(top, element("strong", "stat-number", cache[key] ? String(cache[key].length) : "..."));
        $("stats-grid").append(card);
        $("nav-" + key).textContent = cache[key] ? cache[key].length : "...";
    });
}

function renderTable() {
    const query = $("search").value.trim().toLowerCase();
    const all = cache[section] || [];
    const records = all.filter(user => [user.name, user.email, user.username, user.department, user.id].join(" ").toLowerCase().includes(query));
    const body = $("records");
    body.replaceChildren();
    records.forEach(user => {
        const row = element("tr");
        const nameCell = element("td");
        const person = element("div", "person");
        const copy = element("div", "person-copy");
        copy.append(element("strong", "", user.name), element("small", "", user.email));
        person.append(copy);
        nameCell.append(person);
        const roleCell = element("td");
        roleCell.append(element("span", "record-role " + user.role.toLowerCase(), labels[section]));
        const actions = element("td", "row-actions");
        if (isAdmin || user.id === currentUserId) {
            const edit = element("button", "button secondary", "Edit");
            edit.type = "button";
            edit.title = "Edit " + user.name;
            edit.setAttribute("aria-label", edit.title);
            edit.addEventListener("click", () => openForm(user));
            const remove = element("button", "button danger", "Delete");
            remove.type = "button";
            remove.title = isAdmin && user.id === currentUserId ? "Admins cannot delete their own account" : "Delete " + user.name;
            remove.setAttribute("aria-label", remove.title);
            remove.disabled = isAdmin && user.id === currentUserId;
            remove.addEventListener("click", () => openDelete(user));
            actions.append(edit, remove);
        } else {
            actions.textContent = "View only";
        }
        row.append(nameCell, element("td", "username-cell", user.username), element("td", "department-cell", user.department), roleCell, actions);
        body.append(row);
    });
    $("record-count").textContent = all.length;
    $("table-summary").textContent = cache[section] ? `Showing ${records.length} of ${all.length} ${section}` : "Loading records...";
    $("empty-state").hidden = !cache[section] || records.length !== 0;
    $("empty-title").textContent = query ? "No matches found" : "No " + section + " yet";
    $("empty-description").textContent = query ? "Try a different name, username, or department." : "Add your first " + labels[section].toLowerCase() + " to get started.";
}

function switchSection(key) {
    if (!allowedSections.includes(key)) return;
    section = key;
    const title = labels[key];
    document.title = title + "s - University Management System";
    $("page-title").textContent = title + "s";
    $("page-description").textContent = isAdmin ? `Manage ${title.toLowerCase()} records.`
        : `View all ${key}. You can edit or delete only your own account.`;
    if (isAdmin) $("add-label").textContent = "Add " + title.toLowerCase();
    $("directory-title").textContent = "All " + key;
    $("search").placeholder = "Search " + key + "...";
    $("search").value = "";
    document.querySelectorAll("[data-section]").forEach(button => {
        const selected = button.dataset.section === key;
        button.classList.toggle("active", selected);
        if (selected) button.setAttribute("aria-current", "page");
        else button.removeAttribute("aria-current");
    });
    renderStats();
    renderTable();
}

async function loadRecords() {
    notice("page-error");
    try {
        const results = await Promise.all(allowedSections.map(async key => [key, await api("/api/" + key)]));
        results.forEach(([key, records]) => cache[key] = records);
        renderStats();
        renderTable();
    } catch (error) {
        notice("page-error", error.message);
        $("table-summary").textContent = "Could not load records. Refresh the page to try again.";
        $("empty-state").hidden = true;
    }
}

function openForm(user = null) {
    if ((!user && !isAdmin) || (user && !isAdmin && user.id !== currentUserId)) return;
    editingId = user ? user.id : null;
    $("user-form").reset();
    notice("form-error");
    const singular = labels[section].toLowerCase();
    const emailDomain = {students: "stud.kuet.ac.bd", teachers: "cse.kuet.ac.bd"}[section];
    const emailField = $("field-email");
    emailField.placeholder = "name@" + (emailDomain || "university.edu");
    $("email-help").textContent = emailDomain ? "Use an @" + emailDomain + " email address." : "";
    if (emailDomain) {
        emailField.pattern = "[^@\\s]+@" + emailDomain.replaceAll(".", "\\.");
        emailField.title = "Use an @" + emailDomain + " email address.";
    } else {
        emailField.removeAttribute("pattern");
        emailField.removeAttribute("title");
    }
    $("dialog-title").textContent = (user ? "Edit " : "Add ") + singular;
    $("save-button").textContent = user ? "Save changes" : "Create record";
    $("field-password").required = !user;
    $("password-label").textContent = user ? "New password (optional)" : "Password";
    $("password-help").textContent = user ? "Leave blank to keep the current password. Changing the email or password signs this user out." : "The new user will sign in with their email address and this password.";
    if (user) ["name", "username", "email", "department"].forEach(key => $("field-" + key).value = user[key]);
    $("user-dialog").showModal();
    $("field-name").focus();
}

function openDelete(user) {
    if (!isAdmin && user.id !== currentUserId) return;
    deletingId = user.id;
    notice("delete-error");
    $("delete-description").textContent = `Delete ${user.name} from the ${section} directory? This removes their account and cannot be undone.`;
    $("delete-dialog").showModal();
}

$("user-form").addEventListener("submit", async event => {
    event.preventDefault();
    const payload = Object.fromEntries(new FormData(event.currentTarget));
    ["name", "username", "email", "department"].forEach(key => payload[key] = payload[key].trim());
    const wasEditing = editingId !== null;
    $("save-button").disabled = true;
    $("save-button").textContent = "Saving...";
    notice("form-error");
    try {
        await api("/api/" + section + (wasEditing ? "/" + editingId : ""), {
            method: wasEditing ? "PUT" : "POST", body: JSON.stringify(payload)
        });
        $("user-dialog").close();
        toast(labels[section] + (wasEditing ? " updated successfully" : " created successfully"));
        await loadRecords();
    } catch (error) {
        notice("form-error", error.message);
    } finally {
        $("save-button").disabled = false;
        $("save-button").textContent = wasEditing ? "Save changes" : "Create record";
    }
});

$("confirm-delete").addEventListener("click", async () => {
    $("confirm-delete").disabled = true;
    try {
        await api("/api/" + section + "/" + deletingId, {method: "DELETE"});
        if (deletingId === currentUserId) {
            sessionStorage.removeItem(tokenKey);
            window.location.assign("/login?deleted");
            return;
        }
        $("delete-dialog").close();
        toast(labels[section] + " deleted successfully");
        await loadRecords();
    } catch (error) {
        notice("delete-error", error.message);
    } finally {
        $("confirm-delete").disabled = false;
    }
});

document.querySelectorAll("[data-close]").forEach(button => button.addEventListener("click", () => $(button.dataset.close).close()));
document.querySelectorAll("[data-section]").forEach(button => button.addEventListener("click", () => switchSection(button.dataset.section)));
$("add-button").addEventListener("click", () => openForm());
$("search").addEventListener("input", renderTable);
$("logout-button").addEventListener("click", async () => {
    $("logout-button").disabled = true;
    try {
        await api("/api/auth/logout", {method: "POST"});
        sessionStorage.removeItem(tokenKey);
        window.location.replace("/login?logout");
    } catch (error) {
        notice("page-error", "Could not log out. " + error.message);
        $("logout-button").disabled = false;
    }
});

async function initialize() {
    if (!sessionStorage.getItem(tokenKey)) {
        window.location.replace("/login");
        return;
    }
    try {
        const user = await api("/api/me");
        role = user.role;
        currentUserId = user.id;
        isAdmin = role === "ADMIN";
        allowedSections = isAdmin ? ["students", "teachers", "admins"] : [role.toLowerCase() + "s"];
        section = allowedSections[0];
        $("current-user-name").textContent = user.name;
        $("current-user-role").textContent = role.charAt(0) + role.slice(1).toLowerCase();
        document.querySelectorAll("[data-section]").forEach(button => {
            button.hidden = !allowedSections.includes(button.dataset.section);
        });
        $("add-button").hidden = !isAdmin;
        switchSection(section);
        document.body.hidden = false;
        await loadRecords();
    } catch (error) {
        document.body.hidden = false;
        notice("page-error", error.message);
    }
}
initialize();

