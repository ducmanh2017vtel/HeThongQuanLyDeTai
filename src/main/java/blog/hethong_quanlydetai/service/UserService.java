package blog.hethong_quanlydetai.service;

import blog.hethong_quanlydetai.audit.AuditLogger;
import blog.hethong_quanlydetai.domain.*;
import blog.hethong_quanlydetai.dto.UserForm;
import blog.hethong_quanlydetai.exception.BusinessException;
import blog.hethong_quanlydetai.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Quản lý tài khoản (USER_MANAGE). */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final StudentRepository studentRepository;
    private final LecturerRepository lecturerRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogger auditLogger;

    @Value("${app.demo.default-password:Demo@123}")
    private String defaultPassword;

    /* ============ Queries ============ */

    public Page<User> search(String q, Long roleId, String status, Pageable pageable) {
        return userRepository.search(q, roleId, status, pageable);
    }

    public User getById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Tài khoản không tồn tại"));
    }

    public List<Role> allRoles() { return roleRepository.findAll(); }

    /* ============ Create / update ============ */

    @Transactional
    public User create(UserForm form) {
        validateUsernameEmail(form.getUsername(), form.getEmail(), null);

        User u = User.builder()
                .username(form.getUsername())
                .password(passwordEncoder.encode(
                        StringUtils.hasText(defaultPassword) ? defaultPassword : "ChangeMe@123"))
                .fullName(form.getFullName())
                .email(form.getEmail())
                .phone(form.getPhone())
                .status(UserStatus.ACTIVE)
                .enabled(true)
                .failedAttempts(0)
                .roles(new HashSet<>())
                .build();
        u = userRepository.save(u);
        assignRoles(u, form.getRoleCodes());
        ensureProfile(u, form);
        auditLogger.log("USER_CREATE", "User", u.getId(), "Tạo tài khoản " + u.getUsername());
        return u;
    }

    @Transactional
    public User update(Long id, UserForm form) {
        User u = getById(id);
        validateUsernameEmail(form.getUsername(), form.getEmail(), u.getId());
        u.setFullName(form.getFullName());
        u.setEmail(form.getEmail());
        u.setPhone(form.getPhone());
        assignRoles(u, form.getRoleCodes());
        updateProfile(u, form);
        userRepository.save(u);
        auditLogger.log("USER_UPDATE", "User", u.getId(),
                "Cập nhật tài khoản " + u.getUsername());
        return u;
    }

    /* ============ Lock / unlock / reset password ============ */

    @Transactional
    public void setLocked(Long id, boolean locked, Long currentUserId) {
        if (id.equals(currentUserId) && locked) {
            throw new BusinessException("Bạn không thể tự khóa tài khoản của chính mình");
        }
        User u = getById(id);
        u.setStatus(locked ? UserStatus.LOCKED : UserStatus.ACTIVE);
        u.setEnabled(!locked);
        if (locked) {
            u.setLockedUntil(null);
            u.setFailedAttempts(0);
        }
        userRepository.save(u);
        auditLogger.log(locked ? "USER_LOCK" : "USER_UNLOCK", "User", u.getId(),
                (locked ? "Khóa " : "Mở khóa ") + u.getUsername());
    }

    @Transactional
    public void resetPassword(Long id) {
        User u = getById(id);
        if (!StringUtils.hasText(defaultPassword)) {
            throw new BusinessException("Chưa cấu hình mật khẩu mặc định");
        }
        u.setPassword(passwordEncoder.encode(defaultPassword));
        userRepository.save(u);
        auditLogger.log("USER_RESET_PASSWORD", "User", u.getId(),
                "Đặt lại mật khẩu " + u.getUsername());
    }

    /* ============ Role/Profile helpers ============ */

    private void assignRoles(User u, String roleCodesCsv) {
        Set<Role> roles = new HashSet<>();
        if (StringUtils.hasText(roleCodesCsv)) {
            for (String code : roleCodesCsv.split(",")) {
                String trimmed = code.trim();
                if (trimmed.isEmpty()) continue;
                Role r = roleRepository.findByCode(trimmed.toUpperCase())
                        .orElseThrow(() -> new BusinessException(
                                "Vai trò không tồn tại: " + trimmed));
                roles.add(r);
            }
        }
        u.setRoles(roles);
    }

    private void ensureProfile(User u, UserForm form) {
        if (u.getRoles().stream().anyMatch(r -> "SINH_VIEN".equals(r.getCode()))) {
            requireField(form.getStudentCode(), "Mã sinh viên");
            requireField(form.getClassName(),   "Lớp");
            requireField(form.getMajor(),       "Ngành");
            if (studentRepository.findByStudentCode(form.getStudentCode()).isPresent()) {
                throw new BusinessException("Mã sinh viên đã tồn tại");
            }
            Student s = Student.builder()
                    .user(u)
                    .studentCode(form.getStudentCode())
                    .className(form.getClassName())
                    .major(form.getMajor())
                    .build();
            studentRepository.save(s);
        }
        if (u.getRoles().stream().anyMatch(r -> "GIANG_VIEN".equals(r.getCode()))) {
            requireField(form.getLecturerCode(), "Mã giảng viên");
            if (form.getDepartmentId() == null) {
                throw new BusinessException("Vui lòng chọn bộ môn cho giảng viên");
            }
            if (lecturerRepository.findByLecturerCode(form.getLecturerCode()).isPresent()) {
                throw new BusinessException("Mã giảng viên đã tồn tại");
            }
            Department d = departmentRepository.findById(form.getDepartmentId())
                    .orElseThrow(() -> new BusinessException("Bộ môn không tồn tại"));
            Lecturer l = Lecturer.builder()
                    .user(u)
                    .lecturerCode(form.getLecturerCode())
                    .academicDegree(form.getAcademicDegree())
                    .department(d)
                    .build();
            lecturerRepository.save(l);
        }
    }

    private void updateProfile(User u, UserForm form) {
        // Đơn giản: nếu thêm vai trò SV/GV mà chưa có profile, tạo;
        // nếu gỡ vai trò, KHÔNG xóa profile (an toàn).
        if (studentRepository.findByUserId(u.getId()).isEmpty()
                && u.getRoles().stream().anyMatch(r -> "SINH_VIEN".equals(r.getCode()))) {
            ensureProfile(u, form);
        }
        if (lecturerRepository.findByUserId(u.getId()).isEmpty()
                && u.getRoles().stream().anyMatch(r -> "GIANG_VIEN".equals(r.getCode()))) {
            ensureProfile(u, form);
        }
    }

    private void requireField(String value, String label) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(label + " không được để trống");
        }
    }

    private void validateUsernameEmail(String username, String email, Long excludeId) {
        if (StringUtils.hasText(username)) {
            if (userRepository.existsByUsername(username)
                    && (excludeId == null
                        || userRepository.findByUsername(username).map(u -> !u.getId().equals(excludeId)).orElse(false))) {
                throw new BusinessException("Tên đăng nhập đã tồn tại");
            }
        }
        if (StringUtils.hasText(email)) {
            if (userRepository.existsByEmail(email)
                    && (excludeId == null
                        || userRepository.findByEmail(email).map(u -> !u.getId().equals(excludeId)).orElse(false))) {
                throw new BusinessException("Email đã tồn tại");
            }
        }
    }
}