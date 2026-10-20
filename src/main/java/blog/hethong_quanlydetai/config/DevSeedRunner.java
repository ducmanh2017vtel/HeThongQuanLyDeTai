package blog.hethong_quanlydetai.config;

import blog.hethong_quanlydetai.domain.Permission;
import blog.hethong_quanlydetai.domain.Role;
import blog.hethong_quanlydetai.domain.User;
import blog.hethong_quanlydetai.domain.UserStatus;
import blog.hethong_quanlydetai.repository.PermissionRepository;
import blog.hethong_quanlydetai.repository.RoleRepository;
import blog.hethong_quanlydetai.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Seed tối thiểu cho profile "dev" (H2) — tạo 5 vai trò hệ thống, 19 quyền,
 * 1 user admin để có thể đăng nhập xem giao diện mà không cần MySQL server
 * hay chạy HT_QLDT.sql.
 *
 * KHÔNG chạy ở production (chỉ @Profile("dev")).
 */
@Configuration
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class DevSeedRunner {

    private final PermissionRepository permissionRepo;
    private final RoleRepository roleRepo;
    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;

    @Bean
    @Transactional
    public ApplicationRunner seedDevData() {
        return args -> {
            // 1. Permissions
            Map<String, String> perms = new LinkedHashMap<>();
            perms.put("USER_MANAGE",          "Quản lý người dùng");
            perms.put("ROLE_MANAGE",          "Quản lý vai trò");
            perms.put("PERMISSION_MANAGE",   "Phân quyền");
            perms.put("PERIOD_MANAGE",        "Quản lý đợt đăng ký");
            perms.put("TOPIC_VIEW",           "Xem đề tài");
            perms.put("TOPIC_CREATE",         "Đề xuất đề tài");
            perms.put("TOPIC_APPROVE",        "Duyệt đề tài");
            perms.put("REGISTRATION_CREATE", "Lập nhóm và đăng ký đề tài");
            perms.put("REGISTRATION_CONFIRM","Chấp thuận nhóm đăng ký");
            perms.put("SUBMISSION_CREATE",   "Nộp báo cáo");
            perms.put("REPORT_VIEW",         "Xem báo cáo");
            perms.put("COUNCIL_MANAGE",      "Lập hội đồng phản biện");
            perms.put("ASSIGNMENT_MANAGE",    "Phân công chấm");
            perms.put("EVALUATION_CREATE",   "Nhập điểm");
            perms.put("RESULT_CALCULATE",    "Tổng hợp điểm");
            perms.put("RESULT_PUBLISH",      "Công bố kết quả");
            perms.put("RESULT_VIEW",         "Xem kết quả của tôi");
            perms.put("NOTICE_VIEW",         "Xem thông báo");
            perms.put("NOTICE_MANAGE",       "Quản lý thông báo");

            Map<String, Permission> permByCode = new HashMap<>();
            for (var e : perms.entrySet()) {
                Permission p = permissionRepo.findByCode(e.getKey())
                        .orElseGet(() -> permissionRepo.save(Permission.builder()
                                .code(e.getKey()).name(e.getValue()).build()));
                permByCode.put(e.getKey(), p);
            }

            // 2. Roles
            Map<String, List<String>> rolePerms = new LinkedHashMap<>();
            rolePerms.put("ADMIN", List.of(
                "USER_MANAGE","ROLE_MANAGE","PERMISSION_MANAGE","PERIOD_MANAGE",
                "TOPIC_VIEW","TOPIC_APPROVE","REGISTRATION_CONFIRM","SUBMISSION_CREATE",
                "REPORT_VIEW","COUNCIL_MANAGE","ASSIGNMENT_MANAGE","EVALUATION_CREATE",
                "RESULT_CALCULATE","RESULT_PUBLISH","RESULT_VIEW","NOTICE_VIEW","NOTICE_MANAGE"));
            rolePerms.put("TRUONG_KHOA", List.of(
                "PERIOD_MANAGE","TOPIC_VIEW","TOPIC_APPROVE","REPORT_VIEW",
                "COUNCIL_MANAGE","RESULT_PUBLISH","NOTICE_VIEW","NOTICE_MANAGE"));
            rolePerms.put("CHU_TICH_HOI_DONG", List.of(
                "TOPIC_VIEW","REPORT_VIEW","ASSIGNMENT_MANAGE","EVALUATION_CREATE",
                "RESULT_CALCULATE","NOTICE_VIEW"));
            rolePerms.put("GIANG_VIEN", List.of(
                "TOPIC_VIEW","TOPIC_CREATE","REGISTRATION_CONFIRM","REPORT_VIEW",
                "EVALUATION_CREATE","NOTICE_VIEW"));
            rolePerms.put("SINH_VIEN", List.of(
                "TOPIC_VIEW","REGISTRATION_CREATE","SUBMISSION_CREATE","REPORT_VIEW",
                "RESULT_VIEW","NOTICE_VIEW"));

            Map<String, Role> roleByCode = new HashMap<>();
            for (var e : rolePerms.entrySet()) {
                Role r = roleRepo.findByCode(e.getKey()).orElseGet(() ->
                    roleRepo.save(Role.builder()
                        .code(e.getKey())
                        .name(displayName(e.getKey()))
                        .description("Seed dev")
                        .systemFlag(true)
                        .build()));
                Set<Permission> have = r.getPermissions();
                if (have == null) have = new HashSet<>();
                for (String code : e.getValue()) {
                    Permission p = permByCode.get(code);
                    if (p != null) have.add(p);
                }
                r.setPermissions(have);
                roleRepo.save(r);
                roleByCode.put(e.getKey(), r);
            }

            // 3. Users demo (1 cho mỗi vai trò) - mật khẩu chung: admin123
            Map<String, String> demoUsers = new LinkedHashMap<>();
            demoUsers.put("admin",      "Quản trị hệ thống");
            demoUsers.put("truongkhoa", "Trần Trưởng Khoa");
            demoUsers.put("cthd1",      "Nguyễn Chủ Tịch");
            demoUsers.put("gv_demo",    "Lê Hoàng Giảng");
            demoUsers.put("sv_demo",    "Sinh viên Demo");

            for (var e : demoUsers.entrySet()) {
                String uname = e.getKey();
                if (userRepo.findByUsername(uname).isPresent()) continue;

                String roleCode = switch (uname) {
                    case "admin"      -> "ADMIN";
                    case "truongkhoa" -> "TRUONG_KHOA";
                    case "cthd1"      -> "CHU_TICH_HOI_DONG";
                    case "gv_demo"    -> "GIANG_VIEN";
                    case "sv_demo"    -> "SINH_VIEN";
                    default -> "ADMIN";
                };
                Role role = roleByCode.get(roleCode);
                User u = User.builder()
                    .username(uname)
                    .password(passwordEncoder.encode("admin123"))
                    .fullName(e.getValue())
                    .email(uname + "@local")
                    .status(UserStatus.ACTIVE)
                    .enabled(true)
                    .roles(new HashSet<>(java.util.Set.of(role)))
                    .build();
                userRepo.save(u);
                log.info("[dev] Seeded demo user: {} / admin123 ({})", uname, roleCode);
            }
        };
    }

    private static String displayName(String code) {
        return switch (code) {
            case "ADMIN" -> "Quản trị hệ thống";
            case "TRUONG_KHOA" -> "Trưởng khoa";
            case "CHU_TICH_HOI_DONG" -> "Chủ tịch hội đồng";
            case "GIANG_VIEN" -> "Giảng viên";
            case "SINH_VIEN" -> "Sinh viên";
            default -> code;
        };
    }
}
