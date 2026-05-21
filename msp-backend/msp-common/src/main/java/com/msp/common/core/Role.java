package com.msp.common.core;

import java.util.List;

/**
 * 角色实体
 */
public class Role {

    private String roleId;
    private String roleCode;
    private String roleName;
    private String description;
    private RoleStatus status;
    private List<String> permissions; // 权限编码列表
    private Long createTime;
    private Long updateTime;

    public enum RoleStatus {
        ACTIVE,
        DISABLED
    }

    public String getRoleId() { return roleId; }
    public void setRoleId(String roleId) { this.roleId = roleId; }

    public String getRoleCode() { return roleCode; }
    public void setRoleCode(String roleCode) { this.roleCode = roleCode; }

    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public RoleStatus getStatus() { return status; }
    public void setStatus(RoleStatus status) { this.status = status; }

    public List<String> getPermissions() { return permissions; }
    public void setPermissions(List<String> permissions) { this.permissions = permissions; }

    public Long getCreateTime() { return createTime; }
    public void setCreateTime(Long createTime) { this.createTime = createTime; }

    public Long getUpdateTime() { return updateTime; }
    public void setUpdateTime(Long updateTime) { this.updateTime = updateTime; }
}