package com.hwz.personalized.controller;

import com.hwz.admin.service.AdminAccessService;
import com.hwz.common.entity.User;
import com.hwz.personalized.service.KnowledgeMasteryQueryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/**
 * 学生端知识点掌握度接口。
 *
 * 学生只能查询当前登录账号自己的掌握度，
 * 不能通过请求参数指定其他学生 ID。
 */
@RestController
@RequestMapping("/personalized/student/mastery")
public class KnowledgeMasteryStudentController {

    private final KnowledgeMasteryQueryService queryService;

    private final AdminAccessService accessService;

    public KnowledgeMasteryStudentController(
            KnowledgeMasteryQueryService queryService,
            AdminAccessService accessService) {
        this.queryService = queryService;
        this.accessService = accessService;
    }

    /**
     * 查询当前登录学生自己的知识点掌握度。
     *
     * 请求示例：
     * GET /personalized/student/mastery/me
     */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>>
    getMyMastery() {
        User currentUser =
                accessService.requireUser();

        if (currentUser.getRole()
                != User.Role.USER) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "仅普通学生可以访问个人掌握度"
            );
        }

        Map<String, Object> response =
                queryService.getStudentMastery(
                        currentUser.getId()
                );

        return ResponseEntity.ok(response);
    }
}