package com.example.university.config;

import com.example.university.model.Role;
import com.example.university.model.UserAccount;
import com.example.university.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(name = "app.seed-demo-data", havingValue = "true")
public class DemoData implements CommandLineRunner {
    private final UserRepository users;
    private final PasswordEncoder passwords;

    public DemoData(UserRepository users, PasswordEncoder passwords) {
        this.users = users;
        this.passwords = passwords;
    }

    @Override
    @Transactional
    public void run(String... args) {
        // Seed only an empty database so restarts never recreate deleted demo users.
        if (users.count() != 0) return;
        add("admin", "Admin@123", "University Admin", "admin@university.edu", "Administration", Role.ADMIN);
        add("teacher", "Teacher@123", "Dr. Farhana Ahmed", "farhana@cse.kuet.ac.bd", "Computer Science", Role.TEACHER);
        add("student", "Student@123", "Arif Rahman", "arif@stud.kuet.ac.bd", "Computer Science", Role.STUDENT);
        add("nusrat", "Student@123", "Nusrat Jahan", "nusrat@stud.kuet.ac.bd", "Electrical Engineering", Role.STUDENT);
        add("sadia", "Student@123", "Sadia Islam", "sadia@stud.kuet.ac.bd", "Business Administration", Role.STUDENT);
        add("hasan", "Teacher@123", "Dr. Hasan Mahmud", "hasan@cse.kuet.ac.bd", "Electrical Engineering", Role.TEACHER);
    }

    private void add(String username, String password, String name, String email, String department, Role role) {
        UserAccount user = new UserAccount();
        user.setUsername(username);
        user.setPassword(passwords.encode(password));
        user.setName(name);
        user.setEmail(email);
        user.setDepartment(department);
        user.setRole(role);
        users.save(user);
    }
}
