package com.nexora.bank.nexora_backend.config;

import com.nexora.bank.nexora_backend.entity.Role;
import com.nexora.bank.nexora_backend.entity.User;
import com.nexora.bank.nexora_backend.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.security.crypto.password.PasswordEncoder;


//@Component
public class DataInitializer {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostConstruct
    public void init() {

        if (userRepository.findByEmail("admin@nexora.com").isEmpty()) {

            User admin = new User();
            admin.setName("Admin");
            admin.setEmail("admin@nexora.com");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setPhoneNumber("00000000000");
            admin.setRole(Role.ADMIN);

            userRepository.save(admin);

            System.out.println("✅ Admin created: admin@nexora.com / admin123");
        }
    }
}
