package com.msp.common.core;

import java.util.List;

/**
 * 权限实体
 */
public class Permission {

    private String permissionId;
    private String permissionCode;
    private String permissionName;
    private ResourceType resourceType;
    private String parentId; // 父权限ID，用于构建权限树
    private String path; // 路径或标识
    private String icon; // 图标
    private Integer sortOrder;
    private List<Permission> children; // 子权限

    public enum ResourceType {
        MENU,    // 菜单权限
        BUTTON,  // 按钮权限
        DATA     // 数据权限
    }

    public String getPermissionId() { return permissionId; }
    public void setPermissionId(String permissionId) { this.permissionId = permissionId; }

    public String getPermissionCode() { return permissionCode; }
    public void setPermissionCode(String permissionCode) { this.permissionCode = permissionCode; }

    public String getPermissionName() { return permissionName; }
    public void setPermissionName(String permissionName) { this.permissionName = permissionName; }

    public ResourceType getResourceType() { return resourceType; }
    public void setResourceType(ResourceType resourceType) { this.resourceType = resourceType; }

    public String getParentId() { return parentId; }
    public void setParentId(String parentId) { this.parentId = parentId; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }

    public List<Permission> getChildren() { return children; }
    public void setChildren(List<Permission> children) { this.children = children; }
}