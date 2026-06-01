package com.example.noteshare.dto.response;

/**
 * 图片信息
 */
public class ImageInfo {

    private Long id;
    private String url;
    private Integer sort;

    public ImageInfo(Long id, String url, Integer sort) {
        this.id = id;
        this.url = url;
        this.sort = sort;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }
}
