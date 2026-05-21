package com.msp.scheduler.service;

import com.msp.common.core.Role;
import com.msp.scheduler.repository.RoleRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 角色服务
 */
@Service
public class RoleService {

    private final RoleRepository roleRepository;
    private final PermissionService permissionService;

    public RoleService(RoleRepository roleRepository, PermissionService permissionService) {
        this.roleRepository = roleRepository;
        this.permissionService = permissionService;
    }

    public String create(Role role) {
        if (roleRepository.findByCode(role.getRoleCode()).isPresent()) {
            throw new RuntimeException("角色编码已存在: " + role.getRoleCode());
        }

        role.setRoleId(UUID.randomUUID().toString());
        long now = System.currentTimeMillis();
        role.setCreateTime(now);
        role.setUpdateTime(now);

        roleRepository.save(role);

        // 分配权限
        if (role.getPermissions() != null && !role.getPermissions().isEmpty()) {
            assignPermissions(role.getRoleId(), role.getPermissions());
        }

        return role.getRoleId();
    }

    public void update(Role role) {
        if (roleRepository.findById(role.getRoleId()).isEmpty()) {
            throw new RuntimeException("角色不存在: " + role.getRoleId());
        }

        role.setUpdateTime(System.currentTimeMillis());
        roleRepository.update(role);

        // 更新权限
        if (role.getPermissions() != null) {
            assignPermissions(role.getRoleId(), role.getPermissions());
        }
    }

    public Optional<Role> getById(String roleId) {
        return roleRepository.findById(roleId);
    }

    public List<Role> list(int page, int size) {
        return roleRepository.findAll(page, size);
    }

    public long count() {
        return roleRepository.count();
    }

    public void delete(String roleId) {
        roleRepository.delete(roleId);
    }

    public void assignPermissions(String roleId, List<String> permissionCodes) {
        // 将权限编码转换为ID
        List<String> permissionIds = permissionCodes.stream()
            .map(code -> permissionService.getByCode(code))
            .filter(opt -> opt.isPresent())
            .map(opt -> opt.get().getPermissionId())
            .toList();

        permissionService.assignToRole(roleId, permissionIds);
    }

    public List<String> getPermissions(String roleId) {
        return permissionService.getPermissionCodesByRoleId(roleId);
    }

    /**
     * 获取用户角色编码列表
     */
    public List<String> getRoleCodesByUserId(String userId) {
        return roleRepository.findRoleCodesByUserId(userId);
    }
}