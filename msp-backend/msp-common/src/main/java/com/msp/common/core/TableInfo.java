package com.msp.common.core;

/**
 * 数据库表信息：表名 + 注释（来自 INFORMATION_SCHEMA.TABLES.TABLE_COMMENT / CREATE TABLE 的 COMMENT 子句）
 */
public class TableInfo {
    private String name;
    private String comment;

    public TableInfo() {}

    public TableInfo(String name, String comment) {
        this.name = name;
        this.comment = comment;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}
