package com.msp.scheduler.controller;

import com.msp.common.core.ApiResponse;
import com.msp.common.core.Permission;
import com.msp.scheduler.service.PermissionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 权限管理REST接口
 */
@RestController
@RequestMapping("/api/v1/msp/permissions")
public class PermissionController {

    private final PermissionService permissionService;

    public PermissionController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @PostMapping
    public ApiResponse<String> create(@RequestBody PermissionRequest request) {
        Permission permission = new Permission();
        permission.setPermissionCode(request.getPermissionCode());
        permission.setPermissionName(request.getPermissionName());
        permission.setResourceType(request.getResourceType());
        permission.setParentId(request.getParentId());
        permission.setPath(request.getPath());
        permission.setIcon(request.getIcon());
        permission.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);

        String permissionId = permissionService.create(permission);
        return ApiResponse.success(permissionId);
    }

    @PutMapping("/{permissionId}")
    public ApiResponse<Boolean> update(@PathVariable(name = "permissionId") String permissionId, @RequestBody PermissionRequest request) {
        Permission permission = new Permission();
        permission.setPermissionId(permissionId);
        permission.setPermissionName(request.getPermissionName());
        permission.setResourceType(request.getResourceType());
        permission.setParentId(request.getParentId());
        permission.setPath(request.getPath());
        permission.setIcon(request.getIcon());
        permission.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);

        permissionService.update(permission);
        return ApiResponse.success(true);
    }

    @GetMapping("/{permissionId}")
    public ApiResponse<Permission> getById(@PathVariable(name = "permissionId") String permissionId) {
        return permissionService.getById(permissionId)
            .map(ApiResponse::success)
            .orElse(ApiResponse.error("PERMISSION_NOT_FOUND", "权限不存在"));
    }

    @GetMapping
    public ApiResponse<List<Permission>> getAll() {
        List<Permission> permissions = permissionService.getAll();
        return ApiResponse.success(permissions);
    }

    @GetMapping("/tree")
    public ApiResponse<List<Permission>> getTree() {
        List<Permission> tree = permissionService.getTree();
        return ApiResponse.success(tree);
    }

    @GetMapping("/children")
    public ApiResponse<List<Permission>> getByParentId(@RequestParam(required = false) String parentId) {
        List<Permission> children = permissionService.getByParentId(parentId);
        return ApiResponse.success(children);
    }

    @DeleteMapping("/{permissionId}")
    public ApiResponse<Boolean> delete(@PathVariable(name = "permissionId") String permissionId) {
        permissionService.delete(permissionId);
        return ApiResponse.success(true);
    }

    public static class PermissionRequest {
        private String permissionCode;
        private String permissionName;
        private Permission.ResourceType resourceType;
        private String parentId;
        private String path;
        private String icon;
        private Integer sortOrder;

        public String getPermissionCode() { return permissionCode; }
        public void setPermissionCode(String permissionCode) { this.permissionCode = permissionCode; }

        public String getPermissionName() { return permissionName; }
        public void setPermissionName(String permissionName) { this.permissionName = permissionName; }

        public Permission.ResourceType getResourceType() { return resourceType; }
        public void setResourceType(Permission.ResourceType resourceType) { this.resourceType = resourceType; }

        public String getParentId() { return parentId; }
        public void setParentId(String parentId) { this.parentId = parentId; }

        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }

        public String getIcon() { return icon; }
        public void setIcon(String icon) { this.icon = icon; }

        public Integer getSortOrder() { return sortOrder; }
        public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    }
}