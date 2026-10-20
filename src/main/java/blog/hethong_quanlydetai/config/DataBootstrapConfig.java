package blog.hethong_quanlydetai.config;

import blog.hethong_quanlydetai.domain.Permission;
import blog.hethong_quanlydetai.domain.Role;
import blog.hethong_quanlydetai.repository.PermissionRepository;
import blog.hethong_quanlydetai.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Đảm bảo danh mục permission và ánh xạ role-permission khớp bảng ở mục 3 của đề bài.
 * Chạy sau khi ứng dụng đã Hibernate-validate thành công (cần quyền đọc bảng).
 * Chỉ chèn nếu thiếu; KHÔNG ghi đè ánh xạ đã có.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataBootstrapConfig {

    private final PermissionRepository permissionRepo;
    private final RoleRepository roleRepo;

    @Bean
    @Transactional
    public ApplicationRunner initRolesAndPermissions() {
        return args -> {
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

            for (var e : rolePerms.entrySet()) {
                Role r = roleRepo.findByCode(e.getKey()).orElse(null);
                if (r == null) {
                    log.warn("Role {} chưa có trong DB. Hãy chạy docs/sql/HT_QLDT.sql trước.", e.getKey());
                    continue;
                }
                Set<Permission> have = r.getPermissions();
                for (String code : e.getValue()) {
                    Permission p = permByCode.get(code);
                    if (p != null && !have.contains(p)) {
                        have.add(p);
                    }
                }
                roleRepo.save(r);
            }
            log.info("Đồng bộ danh mục quyền & ánh xạ vai trò-quyền xong.");
        };
    }
}