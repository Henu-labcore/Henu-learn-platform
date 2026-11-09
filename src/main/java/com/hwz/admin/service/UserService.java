package com.hwz.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hwz.common.PageResponse;
import com.hwz.common.entity.User;
import com.hwz.common.mapper.UserMapper;
import com.hwz.common.util.PasswordSupport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.sql.SQLIntegrityConstraintViolationException;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    public User findByUsername(String username) {
        return userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getUsername, username)
                        .last("LIMIT 1")
        );
    }

    public User findByUserId(Long userId) {
        return userMapper.selectById(userId);
    }

    public User createUser(String username, String password, String displayName,
                           String email, String realName, String grade) {
        if (findByUsername(username) != null) {
            throw new RuntimeException("Username already exists");
        }

        LocalDateTime now = LocalDateTime.now();
        User user = User.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .displayName(displayName != null && !displayName.trim().isEmpty() ? displayName : username)
                .email(email)
                .realName(realName)
                .grade(grade)
                .role(User.Role.USER)
                .status(User.Status.ACTIVE)
                .createdTime(now)
                .updatedTime(now)
                .build();

        try {
            userMapper.insert(user);
            log.info("Created user: {}", username);
            return user;
        } catch (DataIntegrityViolationException e) {
            Throwable rootCause = e.getRootCause();
            if (rootCause instanceof SQLIntegrityConstraintViolationException) {
                SQLIntegrityConstraintViolationException sqlEx = (SQLIntegrityConstraintViolationException) rootCause;
                String errorMessage = sqlEx.getMessage();
                if (errorMessage != null && errorMessage.contains("username")) {
                    log.warn("Username unique constraint violated: {}", username);
                    throw new RuntimeException("Username already exists");
                }
            }
            log.error("Failed to create user due to constraint violation: {}", username, e);
            throw new RuntimeException("Failed to create user because of a data constraint", e);
        } catch (Exception e) {
            Throwable cause = e.getCause();
            if (cause instanceof SQLIntegrityConstraintViolationException) {
                SQLIntegrityConstraintViolationException sqlEx = (SQLIntegrityConstraintViolationException) cause;
                String errorMessage = sqlEx.getMessage();
                if (errorMessage != null && errorMessage.contains("username")) {
                    log.warn("Username unique constraint violated: {}", username);
                    throw new RuntimeException("Username already exists");
                }
            }
            log.error("Failed to create user: {}", username, e);
            throw new RuntimeException("Failed to create user: " + e.getMessage(), e);
        }
    }

    public void updateLastLoginTime(String username) {
        User user = findByUsername(username);
        if (user != null) {
            user.setLastLoginTime(LocalDateTime.now());
            userMapper.updateById(user);
        }
    }

    public boolean validatePassword(String rawPassword, String storedPassword) {
        return PasswordSupport.matches(passwordEncoder, rawPassword, storedPassword);
    }

    public List<User> getAllUsers() {
        return userMapper.selectList(null);
    }

    public PageResponse<User> pageUsers(long page, long pageSize, String keyword) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.trim().isEmpty()) {
            String trimmed = keyword.trim();
            wrapper.and(w -> w.like(User::getUsername, trimmed)
                    .or()
                    .like(User::getDisplayName, trimmed)
                    .or()
                    .like(User::getEmail, trimmed)
                    .or()
                    .like(User::getRealName, trimmed));
        }
        long total = userMapper.selectCount(wrapper);
        wrapper.orderByDesc(User::getCreatedTime).orderByDesc(User::getId);
        wrapper.last("LIMIT " + offset(page, pageSize) + ", " + pageSize);
        List<User> records = userMapper.selectList(wrapper);
        return PageResponse.of(records, total, page, pageSize);
    }

    private long offset(long page, long pageSize) {
        return (page - 1) * pageSize;
    }

    public void updateUser(User user) {
        user.setUpdatedTime(LocalDateTime.now());
        userMapper.updateById(user);
        log.info("Updated user: userId={}, username={}", user.getId(), user.getUsername());
    }

    public void updatePassword(Long userId, String newPassword) {
        User user = findByUserId(userId);
        if (user != null) {
            user.setPassword(passwordEncoder.encode(newPassword));
            user.setUpdatedTime(LocalDateTime.now());
            userMapper.updateById(user);
            log.info("Updated user password: userId={}, username={}", userId, user.getUsername());
        }
    }

    public void deleteUser(Long userId) {
        userMapper.deleteById(userId);
        log.info("Deleted user: userId={}", userId);
    }
}
