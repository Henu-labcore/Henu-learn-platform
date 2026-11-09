package com.hwz.admin.util;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hwz.common.entity.User;
import com.hwz.common.mapper.UserMapper;
import com.hwz.common.util.PasswordSupport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Slf4j
@Component
public class AdminUserInitializer implements CommandLineRunner {

    private static final String DEFAULT_ADMIN_USERNAME = "admin";

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final String defaultAdminPassword;

    public AdminUserInitializer(UserMapper userMapper,
                                PasswordEncoder passwordEncoder,
                                @Value("${labcore.admin.default-password:}") String defaultAdminPassword) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.defaultAdminPassword = defaultAdminPassword;
    }

    @Override
    public void run(String... args) {
        try {
            User admin = userMapper.selectOne(
                    new LambdaQueryWrapper<User>()
                            .eq(User::getUsername, DEFAULT_ADMIN_USERNAME)
                            .last("LIMIT 1")
            );

            if (admin == null) {
                if (!StringUtils.hasText(defaultAdminPassword)) {
                    log.warn("Default admin account is not created because labcore.admin.default-password is not configured");
                    return;
                }
                LocalDateTime now = LocalDateTime.now();
                admin = User.builder()
                        .username(DEFAULT_ADMIN_USERNAME)
                        .password(passwordEncoder.encode(defaultAdminPassword))
                        .displayName("管理员")
                        .role(User.Role.ADMIN)
                        .status(User.Status.ACTIVE)
                        .createdTime(now)
                        .updatedTime(now)
                        .build();
                userMapper.insert(admin);
                log.info("Created default admin account: {}", DEFAULT_ADMIN_USERNAME);
                return;
            }

            boolean updated = false;
            if (admin.getRole() != User.Role.ADMIN) {
                admin.setRole(User.Role.ADMIN);
                updated = true;
            }
            if (admin.getStatus() != User.Status.ACTIVE) {
                admin.setStatus(User.Status.ACTIVE);
                updated = true;
            }
            if (!PasswordSupport.isEncoded(admin.getPassword())
                    && StringUtils.hasText(admin.getPassword())) {
                admin.setPassword(passwordEncoder.encode(admin.getPassword()));
                updated = true;
            }
            if (admin.getPassword() == null || admin.getPassword().trim().isEmpty()) {
                if (StringUtils.hasText(defaultAdminPassword)) {
                    admin.setPassword(passwordEncoder.encode(defaultAdminPassword));
                    updated = true;
                } else {
                    log.warn("Admin password is empty and labcore.admin.default-password is not configured");
                }
            }
            if (updated) {
                admin.setUpdatedTime(LocalDateTime.now());
                userMapper.updateById(admin);
                log.info("Updated admin defaults");
            }
        } catch (Exception e) {
            log.error("Failed to initialize admin account", e);
        }
    }
}
