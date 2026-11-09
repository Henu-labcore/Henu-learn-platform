package com.hwz.admin.controller;

import com.hwz.admin.service.AdminAccessService;
import com.hwz.admin.service.UserService;
import com.hwz.common.PageResponse;
import com.hwz.common.Result;
import com.hwz.common.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 管理端用户管理接口
 */
@RestController
@RequestMapping("/users")
public class UserController {

    private static final Logger AUDIT_LOG = LoggerFactory.getLogger("admin-audit");

    private final UserService userService;
    private final AdminAccessService accessService;

    public UserController(UserService userService,
                          AdminAccessService accessService) {
        this.userService = userService;
        this.accessService = accessService;
    }

    @GetMapping
    public Result<PageResponse<Map<String, Object>>> getAllUsers(@RequestParam(defaultValue = "1") long page,
                                                                 @RequestParam(defaultValue = "10") long pageSize,
                                                                 @RequestParam(required = false) String keyword) {
        accessService.requireAdmin();
        PageResponse<User> users = userService.pageUsers(normalizePage(page), normalizePageSize(pageSize), keyword);
        List<Map<String, Object>> userList = users.getRecords().stream().map(this::toUserMap).collect(Collectors.toList());
        return Result.ok(PageResponse.of(userList, users.getTotal(), users.getPage(), users.getPageSize()));
    }

    @GetMapping("/{userId}")
    public Result<Map<String, Object>> getUser(@PathVariable Long userId) {
        accessService.requireAdmin();
        User user = userService.findByUserId(userId);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
        return Result.ok(toUserMap(user));
    }

    @PostMapping
    public Result<Map<String, Object>> createUser(@RequestBody CreateUserRequest request) {
        User operator = accessService.requireAdmin();
        if (request == null || !StringUtils.hasText(request.getUsername())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username is required");
        }
        if (!StringUtils.hasText(request.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password is required");
        }
        validateNewPassword(request.getPassword());
        if (StringUtils.hasText(request.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Role changes require a dedicated approval flow");
        }

        try {
            User user = userService.createUser(
                    request.getUsername().trim(),
                    request.getPassword(),
                    request.getDisplayName(),
                    request.getEmail(),
                    request.getRealName(),
                    request.getGrade()
            );
            AUDIT_LOG.info("operator={} action=create_user targetUserId={} targetUsername={}",
                    operator.getUsername(), user.getId(), user.getUsername());
            return Result.ok(toUserMap(user));
        } catch (ResponseStatusException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
    }

    @PutMapping("/{userId}")
    public Result<Map<String, Object>> updateUser(@PathVariable Long userId, @RequestBody UpdateUserRequest request) {
        User operator = accessService.requireAdmin();
        User user = userService.findByUserId(userId);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required");
        }
        if (StringUtils.hasText(request.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Role changes require a dedicated approval flow");
        }
        if (StringUtils.hasText(request.getStatus())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Status changes require a dedicated approval flow");
        }
        if (StringUtils.hasText(request.getPassword())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Password reset requires a dedicated audited flow");
        }
        if (StringUtils.hasText(request.getDisplayName())) {
            user.setDisplayName(request.getDisplayName().trim());
        }
        if (StringUtils.hasText(request.getEmail())) {
            user.setEmail(request.getEmail().trim());
        }
        if (StringUtils.hasText(request.getRealName())) {
            user.setRealName(request.getRealName().trim());
        }
        if (StringUtils.hasText(request.getGrade())) {
            user.setGrade(request.getGrade().trim());
        }
        userService.updateUser(user);
        AUDIT_LOG.info("operator={} action=update_user_profile targetUserId={} targetUsername={}",
                operator.getUsername(), user.getId(), user.getUsername());
        return Result.ok(toUserMap(user));
    }

    /**
     * 当前登录用户更新自己的信息（不能修改角色和状态）
     */
    @PutMapping("/me")
    public Result<Map<String, Object>> updateMe(@RequestBody UpdateUserRequest request) {
        User currentUser = accessService.requireUser();
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required");
        }
        User user = userService.findByUserId(currentUser.getId());
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }

        if (StringUtils.hasText(request.getDisplayName())) {
            user.setDisplayName(request.getDisplayName().trim());
        }
        if (StringUtils.hasText(request.getEmail())) {
            user.setEmail(request.getEmail().trim());
        }
        if (StringUtils.hasText(request.getRealName())) {
            user.setRealName(request.getRealName().trim());
        }
        if (StringUtils.hasText(request.getGrade())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Grade cannot be changed by this endpoint");
        }
        if (StringUtils.hasText(request.getPassword())) {
            if (!StringUtils.hasText(request.getOldPassword())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Old password is required");
            }
            if (!userService.validatePassword(request.getOldPassword(), user.getPassword())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Old password is incorrect");
            }
            validateNewPassword(request.getPassword());
        }
        userService.updateUser(user);
        // role and status cannot be changed by normal user
        if (StringUtils.hasText(request.getPassword())) {
            userService.updatePassword(user.getId(), request.getPassword());
            AUDIT_LOG.info("operator={} action=change_own_password targetUserId={} targetUsername={}",
                    user.getUsername(), user.getId(), user.getUsername());
        }
        return Result.ok(toUserMap(user));
    }

    @DeleteMapping("/{userId}")
    public Result<Void> deleteUser(@PathVariable Long userId) {
        accessService.requireAdmin();
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User deletion requires a dedicated audited flow");
    }

    private Map<String, Object> toUserMap(User user) {
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("userId", user.getId());
        userMap.put("username", user.getUsername());
        userMap.put("displayName", user.getDisplayName());
        userMap.put("email", user.getEmail());
        userMap.put("realName", user.getRealName());
        userMap.put("grade", user.getGrade());
        userMap.put("role", user.getRole());
        userMap.put("status", user.getStatus());
        userMap.put("createdTime", user.getCreatedTime());
        userMap.put("lastLoginTime", user.getLastLoginTime());
        return userMap;
    }

    private long normalizePage(long page) {
        return page < 1 ? 1 : page;
    }

    private long normalizePageSize(long pageSize) {
        if (pageSize < 1) {
            return 10;
        }
        return Math.min(pageSize, 100);
    }

    private User.Role parseRole(String role) {
        try {
            return User.Role.valueOf(role.trim().toUpperCase());
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid role");
        }
    }

    private User.Status parseStatus(String status) {
        try {
            return User.Status.valueOf(status.trim().toUpperCase());
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status");
        }
    }

    private void validateNewPassword(String password) {
        if (password == null || password.length() < 12) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be at least 12 characters");
        }
        boolean hasLower = false;
        boolean hasUpper = false;
        boolean hasDigit = false;
        boolean hasSpecial = false;
        for (int i = 0; i < password.length(); i++) {
            char ch = password.charAt(i);
            if (Character.isLowerCase(ch)) {
                hasLower = true;
            } else if (Character.isUpperCase(ch)) {
                hasUpper = true;
            }
            if (Character.isDigit(ch)) {
                hasDigit = true;
            } else if (!Character.isLetterOrDigit(ch)) {
                hasSpecial = true;
            }
        }
        if (!hasLower || !hasUpper || !hasDigit || !hasSpecial) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must contain uppercase letters, lowercase letters, numbers, and special characters");
        }
    }

    public static class CreateUserRequest {
        private String username;
        private String password;
        private String displayName;
        private String email;
        private String realName;
        private String grade;
        private String role;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getRealName() { return realName; }
        public void setRealName(String realName) { this.realName = realName; }
        public String getGrade() { return grade; }
        public void setGrade(String grade) { this.grade = grade; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
    }

    public static class UpdateUserRequest {
        private String displayName;
        private String email;
        private String realName;
        private String grade;
        private String role;
        private String status;
        private String password;
        private String oldPassword;

        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getRealName() { return realName; }
        public void setRealName(String realName) { this.realName = realName; }
        public String getGrade() { return grade; }
        public void setGrade(String grade) { this.grade = grade; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getOldPassword() { return oldPassword; }
        public void setOldPassword(String oldPassword) { this.oldPassword = oldPassword; }
    }
}
