package com.hwz.personalized.controller;

import com.hwz.admin.service.AdminAccessService;
import com.hwz.common.entity.StudentKnowledgeMastery;
import com.hwz.personalized.service.KnowledgeMasteryOverviewService;
import com.hwz.personalized.service.KnowledgeMasteryQueryService;
import com.hwz.personalized.service.KnowledgeMasteryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 知识点掌握度管理员接口。
 */
@RestController
@RequestMapping("/personalized/admin/mastery")
public class KnowledgeMasteryAdminController {

    private final KnowledgeMasteryService masteryService;

    private final KnowledgeMasteryQueryService queryService;

    private final KnowledgeMasteryOverviewService overviewService;

    private final AdminAccessService adminAccessService;

    public KnowledgeMasteryAdminController(
            KnowledgeMasteryService masteryService,
            KnowledgeMasteryQueryService queryService,
            KnowledgeMasteryOverviewService overviewService,
            AdminAccessService adminAccessService) {
        this.masteryService = masteryService;
        this.queryService = queryService;
        this.overviewService = overviewService;
        this.adminAccessService = adminAccessService;
    }

    /**
     * 查询全部启用学生的掌握度风险总览。
     *
     * 请求示例：
     * GET /personalized/admin/mastery/students
     */
    @GetMapping("/students")
    public ResponseEntity<Map<String, Object>>
    getStudentOverview() {
        adminAccessService.requireAdmin();

        Map<String, Object> response =
                overviewService.getStudentOverview();

        return ResponseEntity.ok(response);
    }

    /**
     * 查询指定学生的知识点掌握度。
     *
     * 请求示例：
     * GET /personalized/admin/mastery/students/2
     */
    @GetMapping("/students/{studentId}")
    public ResponseEntity<Map<String, Object>>
    getStudentMastery(
            @PathVariable Long studentId) {
        adminAccessService.requireAdmin();

        Map<String, Object> response =
                queryService.getStudentMastery(
                        studentId
                );

        return ResponseEntity.ok(response);
    }

    /**
     * 重新计算指定学生的全部启用知识点掌握度。
     *
     * 请求示例：
     * POST /personalized/admin/mastery/students/2/recalculate
     */
    @PostMapping(
            "/students/{studentId}/recalculate"
    )
    public ResponseEntity<Map<String, Object>>
    recalculateStudentMastery(
            @PathVariable Long studentId) {
        adminAccessService.requireAdmin();

        List<StudentKnowledgeMastery> results =
                masteryService.recalculateStudent(
                        studentId
                );

        Map<String, Object> response =
                new LinkedHashMap<>();

        response.put(
                "message",
                "Knowledge mastery recalculated successfully"
        );

        response.put(
                "studentId",
                studentId
        );

        response.put(
                "count",
                results.size()
        );

        response.put(
                "data",
                results
        );

        return ResponseEntity.ok(response);
    }
}