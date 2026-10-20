package blog.hethong_quanlydetai;

import blog.hethong_quanlydetai.domain.Role;
import blog.hethong_quanlydetai.domain.User;
import blog.hethong_quanlydetai.domain.UserStatus;
import blog.hethong_quanlydetai.dto.ChangePasswordForm;
import blog.hethong_quanlydetai.dto.RoleForm;
import blog.hethong_quanlydetai.dto.UserForm;
import blog.hethong_quanlydetai.exception.BusinessException;
import blog.hethong_quanlydetai.repository.RoleRepository;
import blog.hethong_quanlydetai.repository.UserRepository;
import blog.hethong_quanlydetai.service.PasswordService;
import blog.hethong_quanlydetai.service.RoleService;
import blog.hethong_quanlydetai.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test nghiệp vụ cho UserService / RoleService / PasswordService (GĐ2).
 * Mỗi quy tắc: ≥ 1 ca thành công + 1 ca bị từ chối.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserManagementTest {

    @Autowired UserService userService;
    @Autowired RoleService roleService;
    @Autowired PasswordService passwordService;
    @Autowired UserRepository userRepository;
    @Autowired RoleRepository roleRepository;

    @BeforeEach
    void seedBaseRoles() {
        if (roleRepository.findByCode("ADMIN").isEmpty()) {
            roleRepository.save(Role.builder().code("ADMIN").name("Admin").systemFlag(true).build());
        }
        if (roleRepository.findByCode("SINH_VIEN").isEmpty()) {
            roleRepository.save(Role.builder().code("SINH_VIEN").name("Sinh viên").systemFlag(true).build());
        }
        if (roleRepository.findByCode("GIANG_VIEN").isEmpty()) {
            roleRepository.save(Role.builder().code("GIANG_VIEN").name("Giảng viên").systemFlag(true).build());
        }
    }

    /* ----- UserService ----- */

    @Test
    void createUser_success() {
        UserForm form = new UserForm();
        form.setUsername("tester01");
        form.setFullName("Người Thử");
        form.setEmail("tester01@cntt.edu.vn");
        form.setRoleCodes("SINH_VIEN");
        form.setStudentCode("SVTEST01");
        form.setClassName("CNTT-K15");
        form.setMajor("CNTT");

        User u = userService.create(form);
        assertNotNull(u.getId());
        assertEquals(UserStatus.ACTIVE, u.getStatus());
    }

    @Test
    void createUser_duplicateUsername_rejected() {
        UserForm form = new UserForm();
        form.setUsername("dup_user");
        form.setFullName("A");
        form.setEmail("a@x.vn");
        form.setRoleCodes("SINH_VIEN");
        form.setStudentCode("SVA");
        form.setClassName("K15");
        form.setMajor("CNTT");
        userService.create(form);

        UserForm form2 = new UserForm();
        form2.setUsername("dup_user");
        form2.setFullName("B");
        form2.setEmail("b@x.vn");
        form2.setRoleCodes("SINH_VIEN");
        form2.setStudentCode("SVB");
        form2.setClassName("K15");
        form2.setMajor("CNTT");
        assertThrows(BusinessException.class, () -> userService.create(form2));
    }

    @Test
    void lockOwnAccount_rejected() {
        // Tạo admin để bị cấm tự khóa
        UserForm form = new UserForm();
        form.setUsername("admin01");
        form.setFullName("Admin");
        form.setEmail("admin01@x.vn");
        form.setRoleCodes("ADMIN");
        User u = userService.create(form);

        assertThrows(BusinessException.class,
                () -> userService.setLocked(u.getId(), true, u.getId()));
    }

    @Test
    void lockOtherAccount_succeeds() {
        UserForm a = new UserForm();
        a.setUsername("admin01");
        a.setFullName("Admin");
        a.setEmail("admin01@x.vn");
        a.setRoleCodes("ADMIN");
        User me = userService.create(a);

        UserForm b = new UserForm();
        b.setUsername("user01");
        b.setFullName("U");
        b.setEmail("u1@x.vn");
        b.setRoleCodes("SINH_VIEN");
        b.setStudentCode("SVU1");
        b.setClassName("K15");
        b.setMajor("CNTT");
        User other = userService.create(b);

        userService.setLocked(other.getId(), true, me.getId());
        assertEquals(UserStatus.LOCKED,
                userRepository.findById(other.getId()).orElseThrow().getStatus());
    }

    @Test
    void createSinhVien_missingProfile_rejected() {
        UserForm form = new UserForm();
        form.setUsername("noprofile");
        form.setFullName("NP");
        form.setEmail("np@x.vn");
        form.setRoleCodes("SINH_VIEN");
        // thiếu studentCode/className/major
        assertThrows(BusinessException.class, () -> userService.create(form));
    }

    /* ----- RoleService ----- */

    @Test
    void createRole_success() {
        RoleForm f = new RoleForm();
        f.setCode("CVHT");
        f.setName("Cố vấn học tập");
        f.setDescription("Vai trò cố vấn học tập");
        Role r = roleService.create(f);
        assertNotNull(r.getId());
        assertEquals(Boolean.FALSE, r.getSystemFlag());
    }

    @Test
    void updateSystemRole_rejected() {
        Role admin = roleRepository.findByCode("ADMIN").orElseThrow();
        RoleForm f = new RoleForm();
        f.setId(admin.getId());
        f.setCode(admin.getCode());
        f.setName("Đổi tên");
        assertThrows(BusinessException.class, () -> roleService.update(admin.getId(), f));
    }

    @Test
    void deleteSystemRole_rejected() {
        Role admin = roleRepository.findByCode("ADMIN").orElseThrow();
        assertThrows(BusinessException.class, () -> roleService.delete(admin.getId()));
    }

    /* ----- PasswordService ----- */

    @Test
    void changePassword_success() {
        UserForm form = new UserForm();
        form.setUsername("pwd01");
        form.setFullName("PWD");
        form.setEmail("pwd01@x.vn");
        form.setRoleCodes("ADMIN");
        User u = userService.create(form);

        ChangePasswordForm cp = new ChangePasswordForm();
        cp.setOldPassword("Demo@123");   // mật khẩu mặc định
        cp.setNewPassword("NewPass123");
        cp.setConfirmPassword("NewPass123");
        passwordService.changePassword(u.getId(), cp);
    }

    @Test
    void changePassword_mismatch_rejected() {
        UserForm form = new UserForm();
        form.setUsername("pwd02");
        form.setFullName("PWD");
        form.setEmail("pwd02@x.vn");
        form.setRoleCodes("ADMIN");
        User u = userService.create(form);

        ChangePasswordForm cp = new ChangePasswordForm();
        cp.setOldPassword("Demo@123");
        cp.setNewPassword("NewPass123");
        cp.setConfirmPassword("Different123");
        assertThrows(BusinessException.class, () -> passwordService.changePassword(u.getId(), cp));
    }

    @Test
    void changePassword_tooShort_rejected() {
        UserForm form = new UserForm();
        form.setUsername("pwd03");
        form.setFullName("PWD");
        form.setEmail("pwd03@x.vn");
        form.setRoleCodes("ADMIN");
        User u = userService.create(form);

        ChangePasswordForm cp = new ChangePasswordForm();
        cp.setOldPassword("Demo@123");
        cp.setNewPassword("abc");
        cp.setConfirmPassword("abc");
        assertThrows(BusinessException.class, () -> passwordService.changePassword(u.getId(), cp));
    }

    @Test
    void changePassword_wrongOld_rejected() {
        UserForm form = new UserForm();
        form.setUsername("pwd04");
        form.setFullName("PWD");
        form.setEmail("pwd04@x.vn");
        form.setRoleCodes("ADMIN");
        User u = userService.create(form);

        ChangePasswordForm cp = new ChangePasswordForm();
        cp.setOldPassword("Wrong-Old");
        cp.setNewPassword("NewPass123");
        cp.setConfirmPassword("NewPass123");
        assertThrows(BusinessException.class, () -> passwordService.changePassword(u.getId(), cp));
    }
}