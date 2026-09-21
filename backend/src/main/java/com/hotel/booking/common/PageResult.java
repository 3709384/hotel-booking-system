package com.hotel.booking.common;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 分页响应结构：{@code {records, total, page, size}}。
 */
@Data
public class PageResult<T> implements Serializable {

    private List<T> records;
    private long total;
    private long page;
    private long size;

    public PageResult(List<T> records, long total, long page, long size) {
        this.records = records;
        this.total = total;
        this.page = page;
        this.size = size;
    }

    public static <T> PageResult<T> of(List<T> records, long total, long page, long size) {
        return new PageResult<>(records, total, page, size);
    }
}
