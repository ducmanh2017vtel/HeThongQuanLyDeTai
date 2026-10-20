package blog.hethong_quanlydetai.config;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * GlobalModelAttributes — @ControllerAdvice
 * Gắn biến quyền cho mọi view để sidebar hiển thị đúng theo vai trò.
 * Không thay đổi logic nghiệp vụ, chỉ cấp dữ liệu menu.
 */
@ControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalModelAttributes {

    @ModelAttribute
    public void addGlobalAttributes(Authentication authentication, Model model, HttpSession session) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return;
        }

        Set<String> perms = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> !a.startsWith("ROLE_"))
                .collect(Collectors.toSet());

        Map<String, Boolean> flags = new LinkedHashMap<>();
        flags.put("canManagePeriods",        perms.contains("PERIOD_MANAGE"));
        flags.put("canViewTopics",           perms.contains("TOPIC_VIEW"));
        flags.put("canCreateTopics",         perms.contains("TOPIC_CREATE"));
        flags.put("canApproveTopics",        perms.contains("TOPIC_APPROVE"));
        flags.put("canManageGroups",         perms.contains("REGISTRATION_CREATE"));
        flags.put("canManageRegistrations", perms.contains("REGISTRATION_CONFIRM"));
        flags.put("canViewReports",         perms.contains("REPORT_VIEW"));
        flags.put("canSubmitReports",        perms.contains("SUBMISSION_CREATE"));
        flags.put("canManageCouncils",       perms.contains("COUNCIL_MANAGE"));
        flags.put("canManageAssignments",    perms.contains("ASSIGNMENT_MANAGE"));
        flags.put("canEvaluate",             perms.contains("EVALUATION_CREATE"));
        flags.put("canViewResults",          perms.contains("RESULT_VIEW"));
        flags.put("canManageResults",        perms.contains("RESULT_CALCULATE") || perms.contains("RESULT_PUBLISH"));
        flags.put("canViewNotices",          perms.contains("NOTICE_VIEW"));
        flags.put("canManageNotices",        perms.contains("NOTICE_MANAGE"));
        flags.put("canManageUsers",          perms.contains("USER_MANAGE"));
        flags.put("canManageRoles",          perms.contains("ROLE_MANAGE"));
        flags.put("canManagePermissions",    perms.contains("PERMISSION_MANAGE"));

        flags.forEach(model::addAttribute);
        flags.forEach((k, v) -> session.setAttribute(k, v));
    }
}
