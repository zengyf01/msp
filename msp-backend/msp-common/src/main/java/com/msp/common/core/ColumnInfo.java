package com.msp.common.core;

/**
 * 数据库字段信息：字段名 + 注释（来自 INFORMATION_SCHEMA.COLUMNS.COLUMN_COMMENT / CREATE TABLE 的 COMMENT 子句）
 */
public class ColumnInfo {
    private String name;
    private String comment;

    public ColumnInfo() {}

    public ColumnInfo(String name, String comment) {
        this.name = name;
        this.comment = comment;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}
