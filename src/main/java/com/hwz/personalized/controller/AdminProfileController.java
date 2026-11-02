package com.hwz.personalized.controller;

import com.hwz.admin.service.AdminAccessService;
import com.hwz.personalized.service.AdminProfileService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * 管理端学生画像接口：列表、概览、详情、CSV 导出。
 */
@RestController
@RequestMapping("/personalized/admin/profiles")
public class AdminProfileController {

    private final AdminProfileService adminProfileService;

    private final AdminAccessService adminAccessService;

    public AdminProfileController(
            AdminProfileService adminProfileService,
            AdminAccessService adminAccessService) {
        this.adminProfileService = adminProfileService;
        this.adminAccessService = adminAccessService;
    }

    /**
     * 学生画像列表（每页最多 8 人，支持姓名/学号搜索与类型筛选）。
     *
     * GET /personalized/admin/profiles?keyword=&persona=&page=1&pageSize=8
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>>
    getProfiles(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String persona,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize) {

        adminAccessService.requireAdmin();

        return ResponseEntity.ok(
                adminProfileService.getProfiles(
                        keyword,
                        persona,
                        page,
                        pageSize
                )
        );
    }

    /**
     * 画像概览：类型分布、维度均值、置信度分布。
     */
    @GetMapping("/overview")
    public ResponseEntity<Map<String, Object>>
    getOverview() {
        adminAccessService.requireAdmin();

        return ResponseEntity.ok(
                adminProfileService.getOverview()
        );
    }

    /**
     * 导出学生画像 CSV（教师端完整标签）。
     */
    @GetMapping(value = "/export", produces = "text/csv;charset=UTF-8")
    public ResponseEntity<byte[]> exportProfiles(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String persona) {

        adminAccessService.requireAdmin();

        List<Map<String, Object>> rows =
                adminProfileService.getFilteredProfiles(
                        keyword,
                        persona
                );

        StringBuilder csv = new StringBuilder();

        // UTF-8 BOM，避免 Excel 乱码
        csv.append('\uFEFF');

        csv.append("学号,姓名,画像类型,置信度,知识掌握,考核表现,学习投入,学习稳定,任务执行,平均掌握度,高风险数,标签\n");

        for (Map<String, Object> row : rows) {
            Map<?, ?> personaView =
                    (Map<?, ?>) row.get("persona");
            Map<?, ?> confidence =
                    (Map<?, ?>) row.get("confidence");

            csv.append(csvCell(row.get("username"))).append(',');
            csv.append(csvCell(row.get("displayName"))).append(',');
            csv.append(csvCell(
                    personaView == null ? "" : personaView.get("label")
            )).append(',');
            csv.append(csvCell(
                    confidence == null ? "" : confidence.get("label")
            )).append(',');

            Map<String, Object> scores =
                    new java.util.LinkedHashMap<>();

            Object dimensions = row.get("dimensions");

            if (dimensions instanceof List) {
                for (Object item : (List<?>) dimensions) {
                    if (item instanceof Map) {
                        Map<?, ?> dimension = (Map<?, ?>) item;
                        scores.put(
                                String.valueOf(dimension.get("key")),
                                dimension.get("score")
                        );
                    }
                }
            }

            csv.append(csvCell(scores.get("knowledge"))).append(',');
            csv.append(csvCell(scores.get("assessment"))).append(',');
            csv.append(csvCell(scores.get("engagement"))).append(',');
            csv.append(csvCell(scores.get("stability"))).append(',');
            csv.append(csvCell(scores.get("execution"))).append(',');
            csv.append(csvCell(row.get("averageMasteryScore"))).append(',');
            csv.append(csvCell(row.get("highRiskCount"))).append(',');

            StringBuilder tags = new StringBuilder();

            Object tagList = row.get("tags");

            if (tagList instanceof List) {
                for (Object item : (List<?>) tagList) {
                    if (item instanceof Map) {
                        if (tags.length() > 0) {
                            tags.append(';');
                        }
                        tags.append(
                                String.valueOf(
                                        ((Map<?, ?>) item).get("label")
                                )
                        );
                    }
                }
            }

            csv.append(csvCell(tags.toString())).append('\n');
        }

        String filename = "学生画像-"
                + LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
        ) + ".csv";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''"
                                + UriUtils.encode(filename, StandardCharsets.UTF_8)
                )
                .body(csv.toString().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 单个学生完整画像（教师端可见完整标签文案与证据）。
     */
    @GetMapping("/{studentId}")
    public ResponseEntity<Map<String, Object>>
    getStudentProfile(
            @PathVariable Long studentId) {

        adminAccessService.requireAdmin();

        return ResponseEntity.ok(
                adminProfileService.getStudentProfile(studentId)
        );
    }

    /**
     * 单个学生画像历史（最近 30 天）。
     */
    @GetMapping("/{studentId}/history")
    public ResponseEntity<Map<String, Object>>
    getStudentHistory(
            @PathVariable Long studentId,
            @RequestParam(required = false) Integer days) {

        adminAccessService.requireAdmin();

        return ResponseEntity.ok(
                adminProfileService.getStudentHistory(
                        studentId,
                        days
                )
        );
    }

    private String csvCell(Object value) {
        if (value == null) {
            return "";
        }

        String text = String.valueOf(value);

        if (text.contains(",")
                || text.contains("\"")
                || text.contains("\n")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }

        return text;
    }
}
