package com.dataflow.ai.domain.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 分页响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {

    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;

    /**
     * 根据列表与分页元信息构建分页响应。
     *
     * @param content       当前页数据
     * @param page          页码（从 0 开始）
     * @param size          每页大小
     * @param totalElements 总记录数
     * @return 分页响应
     */
    public static <T> PageResponse<T> of(List<T> content, int page, int size, long totalElements) {
        // 按总记录数与每页大小计算总页数
        int totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 0;
        return PageResponse.<T>builder()
                .content(content)
                .page(page)
                .size(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .build();
    }
}
