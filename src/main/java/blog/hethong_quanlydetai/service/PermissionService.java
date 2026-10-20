package blog.hethong_quanlydetai.service;

import blog.hethong_quanlydetai.audit.AuditLogger;
import blog.hethong_quanlydetai.domain.Permission;
import blog.hethong_quanlydetai.domain.Role;
import blog.hethong_quanlydetai.exception.BusinessException;
import blog.hethong_quanlydetai.repository.PermissionRepository;
import blog.hethong_quanlydetai.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Phân quyền cho vai trò (PERMISSION_MANAGE). */
@Service
@RequiredArgsConstructor
public class PermissionService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final AuditLogger auditLogger;

    public List<Permission> allPermissions() { return permissionRepository.findAll(); }

    public List<Role> allRoles() { return roleRepository.findAll(); }

    public Role getRoleWithPermissions(Long roleId) {
        return roleRepository.findById(roleId)
                .orElseThrow(() -> new BusinessException("Vai trò không tồn tại"));
    }

    @Transactional
    public void assignPermissions(Long roleId, Set<Long> permissionIds) {
        Role r = getRoleWithPermissions(roleId);
        Set<Permission> perms = new HashSet<>();
        if (permissionIds != null) {
            for (Long pid : permissionIds) {
                Permission p = permissionRepository.findById(pid)
                        .orElseThrow(() -> new BusinessException("Quyền không tồn tại: " + pid));
                perms.add(p);
            }
        }
        r.setPermissions(perms);
        roleRepository.save(r);
        auditLogger.log("PERMISSION_ASSIGN", "Role", r.getId(),
                "Gán " + perms.size() + " quyền cho " + r.getCode());
    }
}