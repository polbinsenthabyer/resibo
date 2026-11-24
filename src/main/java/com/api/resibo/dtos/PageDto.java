package com.api.resibo.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Pagination envelope carried in {@link ResponseDto#getData()} by any listing
 * endpoint.
 *
 * <p>Deliberately not Spring's {@code Page}: that serialises its internal
 * structure, which is unstable across Spring versions and awkward for a
 * frontend to consume. This exposes only what a pager actually needs.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageDto<T> {

    private List<T> items;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean hasNext;
    private boolean hasPrevious;

    public static <T> PageDto<T> of(Page<T> page) {
        return new PageDto<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext(),
                page.hasPrevious());
    }
}
