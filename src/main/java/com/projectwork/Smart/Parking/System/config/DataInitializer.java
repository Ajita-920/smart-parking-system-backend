package com.projectwork.Smart.Parking.System.config;

import com.projectwork.Smart.Parking.System.entity.User;
import com.projectwork.Smart.Parking.System.entity.UserRole;
import com.projectwork.Smart.Parking.System.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class DataInitializer implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        log.info("Seed initialization started");

        seedUserIfMissing("System Admin", "admin@parking.com", "Admin@123", "9800000000", UserRole.ADMIN);
        seedUserIfMissing("Vendor One", "vendor@parking.com", "Vendor@123", "9800000001", UserRole.VENDOR);
        seedUserIfMissing("Driver One", "driver@parking.com", "Driver@123", "9800000002", UserRole.DRIVER);

        log.info("Seed initialization completed");
    }

    private void seedUserIfMissing(String name, String email, String rawPassword, String phone, UserRole role) {
        if (userRepository.findByEmail(email).isPresent()) {
            log.info("Seed skipped: {} already exists", email);
            return;
        }

        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setPhone(phone);
        user.setRole(role);
        userRepository.save(user);

        log.info("Seed inserted: {} ({})", email, role);
    }
}
