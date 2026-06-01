package com.example.noteshare.common;

import java.util.List;

/**
 * 分页响应包装
 */
public class PageResponse<T> {

    private List<T> items;
    private int page;
    private int pageSize;
    private long total;
    private boolean hasMore;

    public static <T> PageResponse<T> of(org.springframework.data.domain.Page<T> pageData) {
        PageResponse<T> resp = new PageResponse<>();
        resp.items = pageData.getContent();
        resp.page = pageData.getNumber() + 1;       // Spring 从 0 开始，前端从 1 开始
        resp.pageSize = pageData.getSize();
        resp.total = pageData.getTotalElements();
        resp.hasMore = pageData.hasNext();
        return resp;
    }

    // getters & setters

    public List<T> getItems() { return items; }
    public void setItems(List<T> items) { this.items = items; }

    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }

    public int getPageSize() { return pageSize; }
    public void setPageSize(int pageSize) { this.pageSize = pageSize; }

    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }

    public boolean isHasMore() { return hasMore; }
    public void setHasMore(boolean hasMore) { this.hasMore = hasMore; }
}
