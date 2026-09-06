package com.example.shadowvibe.Configurations;

import com.example.shadowvibe.Models.User;
import com.example.shadowvibe.Repositories.UserRepository;
import com.example.shadowvibe.enums.UserRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.username:admin}")
    private String adminUsername;

    @Value("${app.admin.password:}")
    private String adminPassword;

    @Value("${app.admin.email:admin@example.com}")
    private String adminEmail;

    public AdminInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (adminUsername == null || adminUsername.isBlank()) {
            return;
        }
        User admin = userRepository.findByUsername(adminUsername).orElse(null);
        if (admin == null) {
            if (adminPassword == null || adminPassword.isBlank()) {
                adminPassword = generatePassword();
                log.warn("ADMIN_PASSWORD не задан, сгенерирован случайный пароль для {}: {}", adminUsername, adminPassword);
            }
            admin = new User(adminUsername, adminEmail, passwordEncoder.encode(adminPassword), UserRole.ADMIN);
            userRepository.save(admin);
            log.info("Создан администратор: {}", adminUsername);
        } else if (admin.getEmail() == null || admin.getEmail().isBlank()) {
            admin.setEmail(adminEmail);
            userRepository.save(admin);
            log.info("Администратору {} задан email: {}", adminUsername, adminEmail);
        }
    }

    private String generatePassword() {
        byte[] bytes = new byte[16];
        new java.security.SecureRandom().nextBytes(bytes);
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
