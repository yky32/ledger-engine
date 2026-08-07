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
        Pagination mate = new Pagination();
        mate.setFrom(pageable.getOffset() + 1);
        mate.setTo(pageable.getOffset() + page.getNumberOfElements());
        mate.setTotal(page.getTotalElements());
        mate.setCurrentPage(pageable.getPageNumber() + 1L);
        mate.setMaxPage((long) page.getTotalPages());
        mate.setPageSize((long) pageable.getPageSize());
        return mate;
    }

}