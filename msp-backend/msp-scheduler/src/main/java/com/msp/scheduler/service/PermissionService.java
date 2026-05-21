package com.msp.scheduler.service;

import com.msp.common.core.Permission;
import com.msp.scheduler.repository.PermissionRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 权限服务
 */
@Service
public class PermissionService {

    private final PermissionRepository permissionRepository;

    public PermissionService(PermissionRepository permissionRepository) {
        this.permissionRepository = permissionRepository;
    }

    public String create(Permission permission) {
        if (permissionRepository.findByCode(permission.getPermissionCode()).isPresent()) {
            throw new RuntimeException("权限编码已存在: " + permission.getPermissionCode());
        }

        permission.setPermissionId(UUID.randomUUID().toString());
        permissionRepository.save(permission);
        return permission.getPermissionId();
    }

    public void update(Permission permission) {
        if (permissionRepository.findById(permission.getPermissionId()).isEmpty()) {
            throw new RuntimeException("权限不存在: " + permission.getPermissionId());
        }
        permissionRepository.update(permission);
    }

    public Optional<Permission> getById(String permissionId) {
        return permissionRepository.findById(permissionId);
    }

    public Optional<Permission> getByCode(String permissionCode) {
        return permissionRepository.findByCode(permissionCode);
    }

    public List<Permission> getAll() {
        return permissionRepository.findAll();
    }

    public List<Permission> getTree() {
        List<Permission> all = permissionRepository.findAll();
        return buildTree(all, null);
    }

    public List<Permission> getByParentId(String parentId) {
        return permissionRepository.findByParentId(parentId);
    }

    public void delete(String permissionId) {
        permissionRepository.delete(permissionId);
    }

    /**
     * 分配权限给角色
     */
    public void assignToRole(String roleId, List<String> permissionIds) {
        // 先删除角色现有权限
        permissionRepository.deleteRolePermissions(roleId);
        // 再添加新权限
        for (String permissionId : permissionIds) {
            permissionRepository.saveRolePermission(roleId, permissionId);
        }
    }

    /**
     * 获取角色拥有的权限编码列表
     */
    public List<String> getPermissionCodesByRoleId(String roleId) {
        return permissionRepository.findPermissionCodesByRoleId(roleId);
    }

    /**
     * 构建权限树
     */
    private List<Permission> buildTree(List<Permission> all, String parentId) {
        return all.stream()
            .filter(p -> {
                if (parentId == null) {
                    return p.getParentId() == null;
                }
                return parentId.equals(p.getParentId());
            })
            .peek(p -> p.setChildren(buildTree(all, p.getPermissionId())))
            .collect(Collectors.toList());
    }
}