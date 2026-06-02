package com.rankwise.config;

import com.rankwise.admin.AdminUser;
import com.rankwise.admin.AdminUserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seeds the admin account from env-backed properties if it does not exist.
 * Keeps the admin password out of SQL migrations and in the .env source of truth.
 */
@Component
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties props;

    public DataSeeder(AdminUserRepository adminUserRepository,
                      PasswordEncoder passwordEncoder,
                      AppProperties props) {
        this.adminUserRepository = adminUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.props = props;
    }

    @Override
    public void run(String... args) {
        String username = props.getAdmin().getUsername();
        if (adminUserRepository.existsByUsername(username)) {
            return;
        }
        AdminUser admin = AdminUser.builder()
                .username(username)
                .email(props.getAdmin().getEmail())
                .password(passwordEncoder.encode(props.getAdmin().getPassword()))
                .role("ADMIN")
                .build();
        adminUserRepository.save(admin);
        log.info("Seeded admin user '{}'", username);
    }
}
