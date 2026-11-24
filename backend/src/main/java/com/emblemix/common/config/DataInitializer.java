package com.emblemix.common.config;

import com.emblemix.organization.Organization;
import com.emblemix.organization.OrganizationRepository;
import com.emblemix.user.Role;
import com.emblemix.user.User;
import com.emblemix.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(OrganizationRepository organizationRepository,
                           UserRepository userRepository,
                           PasswordEncoder passwordEncoder) {
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // Ensure default demo organization
        Organization org = organizationRepository.findBySlug("acme-global")
                .orElseGet(() -> {
                    Organization newOrg = new Organization(
                            UUID.fromString("a0000000-0000-0000-0000-000000000001"),
                            "Acme Global Enterprises",
                            "acme-global"
                    );
                    return organizationRepository.save(newOrg);
                });

        // Initialize or synchronize default dev accounts with documented password: "Password123!"
        initOrUpdateUser("admin@emblemix.com", "Alex", "Admin", Role.ORG_ADMIN, org);
        initOrUpdateUser("engineer@emblemix.com", "Elena", "Engineer", Role.ENGINEER, org);
        initOrUpdateUser("viewer@emblemix.com", "Victor", "Viewer", Role.VIEWER, org);

        log.info("Emblemix seed accounts initialized successfully: admin@emblemix.com / Password123!");
    }

    private void initOrUpdateUser(String email, String firstName, String lastName, Role role, Organization org) {
        userRepository.findByEmail(email).ifPresentOrElse(
                user -> {
                    // Refresh seed password hash to guarantee match with active BCrypt encoder
                    user.setPasswordHash(passwordEncoder.encode("Password123!"));
                    userRepository.save(user);
                },
                () -> {
                    User newUser = new User(
                            UUID.randomUUID(),
                            org,
                            email,
                            passwordEncoder.encode("Password123!"),
                            firstName,
                            lastName,
                            role
                    );
                    userRepository.save(newUser);
                }
        );
    }
}
