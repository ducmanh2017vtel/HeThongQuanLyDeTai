package blog.hethong_quanlydetai.service;

import blog.hethong_quanlydetai.audit.AuditLogger;
import blog.hethong_quanlydetai.domain.Role;
import blog.hethong_quanlydetai.dto.RoleForm;
import blog.hethong_quanlydetai.exception.BusinessException;
import blog.hethong_quanlydetai.repository.RoleRepository;
import blog.hethong_quanlydetai.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Quản lý vai trò (ROLE_MANAGE). Vai trò hệ thống không xóa được. */
@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final AuditLogger auditLogger;

    public List<Role> findAll() { return roleRepository.findAll(); }

    public Role getById(Long id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Vai trò không tồn tại"));
    }

    @Transactional
    public Role create(RoleForm form) {
        if (roleRepository.existsByCode(form.getCode().toUpperCase())) {
            throw new BusinessException("Mã vai trò đã tồn tại");
        }
        Role r = Role.builder()
                .code(form.getCode().toUpperCase())
                .name(form.getName())
                .description(form.getDescription())
                .systemFlag(false)
                .build();
        r = roleRepository.save(r);
        auditLogger.log("ROLE_CREATE", "Role", r.getId(), "Tạo vai trò " + r.getCode());
        return r;
    }

    @Transactional
    public Role update(Long id, RoleForm form) {
        Role r = getById(id);
        if (Boolean.TRUE.equals(r.getSystemFlag())) {
            throw new BusinessException("Không thể sửa vai trò hệ thống");
        }
        r.setName(form.getName());
        r.setDescription(form.getDescription());
        r = roleRepository.save(r);
        auditLogger.log("ROLE_UPDATE", "Role", r.getId(), "Cập nhật vai trò " + r.getCode());
        return r;
    }

    @Transactional
    public void delete(Long id) {
        Role r = getById(id);
        if (Boolean.TRUE.equals(r.getSystemFlag())) {
            throw new BusinessException("Không thể xóa vai trò hệ thống");
        }
        // Không xóa nếu còn user giữ
        if (!userRepository.search(null, id, null,
                org.springframework.data.domain.PageRequest.of(0, 1)).isEmpty()) {
            throw new BusinessException("Vai trò còn được gán cho tài khoản, không thể xóa");
        }
        try {
            roleRepository.delete(r);
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessException("Không thể xóa do ràng buộc dữ liệu");
        }
        auditLogger.log("ROLE_DELETE", "Role", r.getId(), "Xóa vai trò " + r.getCode());
    }
}