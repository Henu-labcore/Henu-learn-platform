package com.hwz.favorite.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/** 收藏独立请求/响应；不复用并修改原学习内容分类字段。 */
public final class FavoriteDtos {
    private FavoriteDtos() {}
    @Data public static class SaveRequest {
        private Long itemPk;
        private Long categoryId;
        private List<String> tags;
        private String note;
        private Integer version;
    }
    @Data public static class NameRequest { private String name; }
    @Data public static class ImportRequest { private Boolean confirmed; private List<Long> itemIds; }
    @Data public static class Label { private Long id; private String name; }
    @Data public static class FavoriteView {
        private Long id, itemPk, categoryId;
        private String title, summary, categoryName, difficulty, duration, note;
        private boolean available;
        private int version;
        private List<String> tags;
        private LocalDateTime favoritedAt;
    }
}
