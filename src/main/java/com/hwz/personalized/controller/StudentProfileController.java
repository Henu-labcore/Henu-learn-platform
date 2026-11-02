package com.hwz.personalized.controller;

import com.hwz.admin.service.AdminAccessService;
import com.hwz.common.entity.User;
import com.hwz.personalized.service.StudentProfileService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/**
 * 学生端画像接口。
 *
 * <p>只允许当前登录学生查看自己的画像，
 * 不接受请求参数指定其他学生 ID。</p>
 */
@RestController
@RequestMapping("/personalized/student/profile")
public class StudentProfileController {

    private final StudentProfileService studentProfileService;

    private final AdminAccessService accessService;

    public StudentProfileController(
            StudentProfileService studentProfileService,
            AdminAccessService accessService) {
        this.studentProfileService = studentProfileService;
        this.accessService = accessService;
    }

    /**
     * 查询并刷新当前学生画像（按天快照，只保留最近 30 天）。
     *
     * 请求示例：
     * GET /personalized/student/profile?days=30
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>>
    getMyProfile(
            @RequestParam(required = false) Integer days) {

        User currentUser = requireStudent();

        return ResponseEntity.ok(
                studentProfileService.getProfile(
                        currentUser.getId(),
                        days
                )
        );
    }

    /**
     * 查询当前学生的画像历史（最近 30 天）。
     *
     * 请求示例：
     * GET /personalized/student/profile/history?days=30
     */
    @GetMapping("/history")
    public ResponseEntity<Map<String, Object>>
    getMyProfileHistory(
            @RequestParam(required = false) Integer days) {

        User currentUser = requireStudent();

        return ResponseEntity.ok(
                studentProfileService.getProfileHistory(
                        currentUser.getId(),
                        days
                )
        );
    }

    /**
     * 主动重算当前学生画像。
     *
     * 页面刷新本身也会重算，此接口用于“重新生成画像”按钮。
     */
    @PostMapping("/recalculate")
    public ResponseEntity<Map<String, Object>>
    recalculateMyProfile(
            @RequestParam(required = false) Integer days) {

        User currentUser = requireStudent();

        return ResponseEntity.ok(
                studentProfileService.getProfile(
                        currentUser.getId(),
                        days
                )
        );
    }

    private User requireStudent() {

        User currentUser =
                accessService.requireUser();

        if (currentUser.getRole()
                != User.Role.USER) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "仅普通学生可以访问个人画像"
            );
        }

        return currentUser;
    }
}
