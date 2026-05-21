package com.msp.scheduler.controller;

import com.msp.common.core.ApiResponse;
import com.msp.common.core.Page;
import com.msp.common.core.Role;
import com.msp.scheduler.service.RoleService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色管理REST接口
 */
@RestController
@RequestMapping("/api/v1/msp/roles")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @PostMapping
    public ApiResponse<String> create(@RequestBody RoleRequest request) {
        Role role = new Role();
        role.setRoleCode(request.getRoleCode());
        role.setRoleName(request.getRoleName());
        role.setDescription(request.getDescription());
        role.setStatus(request.getStatus() != null ? request.getStatus() : Role.RoleStatus.ACTIVE);
        role.setPermissions(request.getPermissions());

        String roleId = roleService.create(role);
        return ApiResponse.success(roleId);
    }

    @PutMapping("/{roleId}")
    public ApiResponse<Boolean> update(@PathVariable(name = "roleId") String roleId, @RequestBody RoleRequest request) {
        Role role = new Role();
        role.setRoleId(roleId);
        role.setRoleName(request.getRoleName());
        role.setDescription(request.getDescription());
        role.setStatus(request.getStatus());
        role.setPermissions(request.getPermissions());

        roleService.update(role);
        return ApiResponse.success(true);
    }

    @GetMapping("/{roleId}")
    public ApiResponse<Role> getById(@PathVariable(name = "roleId") String roleId) {
        return roleService.getById(roleId)
            .map(ApiResponse::success)
            .orElse(ApiResponse.error("ROLE_NOT_FOUND", "角色不存在"));
    }

    @GetMapping
    public ApiResponse<Page<Role>> list(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        List<Role> content = roleService.list(page, size);
        long total = roleService.count();
        return ApiResponse.success(new Page<>(content, total, page, size));
    }

    @DeleteMapping("/{roleId}")
    public ApiResponse<Boolean> delete(@PathVariable(name = "roleId") String roleId) {
        roleService.delete(roleId);
        return ApiResponse.success(true);
    }

    @GetMapping("/{roleId}/permissions")
    public ApiResponse<List<String>> getPermissions(@PathVariable(name = "roleId") String roleId) {
        List<String> permissions = roleService.getPermissions(roleId);
        return ApiResponse.success(permissions);
    }

    @PutMapping("/{roleId}/permissions")
    public ApiResponse<Boolean> assignPermissions(
            @PathVariable(name = "roleId") String roleId,
            @RequestBody List<String> permissionCodes) {
        roleService.assignPermissions(roleId, permissionCodes);
        return ApiResponse.success(true);
    }

    public static class RoleRequest {
        private String roleCode;
        private String roleName;
        private String description;
        private Role.RoleStatus status;
        private List<String> permissions;

        public String getRoleCode() { return roleCode; }
        public void setRoleCode(String roleCode) { this.roleCode = roleCode; }

        public String getRoleName() { return roleName; }
        public void setRoleName(String roleName) { this.roleName = roleName; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public Role.RoleStatus getStatus() { return status; }
        public void setStatus(Role.RoleStatus status) { this.status = status; }

        public List<String> getPermissions() { return permissions; }
        public void setPermissions(List<String> permissions) { this.permissions = permissions; }
    }
}