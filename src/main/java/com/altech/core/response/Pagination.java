package com.altech.core.response;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Getter
@Setter
public class Pagination {

    private Long from;
    private Long to;
    private Long total;
    private Long currentPage;
    private Long maxPage;
    private Long pageSize;

    public static Pagination create(Page<?> page) {
        Pageable pageable = page.getPageable();
        Pagination meta = new Pagination();
        if (pageable.isPaged()) {
            meta.setFrom(pageable.getOffset() + 1);
            meta.setTo(pageable.getOffset() + page.getNumberOfElements());
            meta.setCurrentPage(pageable.getPageNumber() + 1L);
            meta.setPageSize((long) pageable.getPageSize());
        } else {
            meta.setFrom(page.getNumberOfElements() == 0 ? 0L : 1L);
            meta.setTo((long) page.getNumberOfElements());
            meta.setCurrentPage(1L);
            meta.setPageSize((long) page.getNumberOfElements());
        }
        meta.setTotal(page.getTotalElements());
        meta.setMaxPage((long) Math.max(page.getTotalPages(), 1));
        return meta;
    }
}
