package com.fenglin.springboottest.dto;

/**
 * 归档条目：某年某月发表了几篇。
 * 由 PostMapper 的分组 SQL 直接映射，故用普通类。
 */
public class ArchiveItem {

    private String year;
    private String month;
    private Long count;

    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }

    public String getMonth() {
        return month;
    }

    public void setMonth(String month) {
        this.month = month;
    }

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }
}
