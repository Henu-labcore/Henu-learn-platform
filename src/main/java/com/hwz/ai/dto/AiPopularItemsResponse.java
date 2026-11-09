package com.hwz.ai.dto;

import lombok.Data;

import java.util.List;

/** 管理端高频求助课程 TOP 5 响应，不包含学生身份和具体对话正文。 */
@Data
public class AiPopularItemsResponse {
    private List<PopularItem> items;

    @Data
    public static class PopularItem {
        private Long itemPk;
        private String title;
        private long questionCount;
    }
}
