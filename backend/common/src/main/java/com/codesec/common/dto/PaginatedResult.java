package com.codesec.common.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class PaginatedResult<T> {
    private List<T> items;
    private long total;
    private int page;
    private int size;

    public static <T> PaginatedResult<T> of(List<T> items, long total, int page, int size) {
        return PaginatedResult.<T>builder()
            .items(items).total(total).page(page).size(size)
            .build();
    }
}
