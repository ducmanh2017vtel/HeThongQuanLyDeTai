package blog.hethong_quanlydetai.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * URL alias forwarding — khớp sidebar spec với backend thực.
 * Không thay đổi @PreAuthorize hay template nào.
 */
@Controller
public class UrlAliasController {

    /** /registration-periods → /admin/periods */
    @GetMapping("/registration-periods")
    public String periods() {
        return "forward:/admin/periods";
    }

    /** /registration-periods/new → /admin/periods/new */
    @GetMapping("/registration-periods/new")
    public String periodsNew() {
        return "forward:/admin/periods/new";
    }

    /** /registration-periods/{id}/edit → /admin/periods/{id}/edit */
    @GetMapping("/registration-periods/{id}/edit")
    public String periodsEdit(Long id) {
        return "forward:/admin/periods/" + id + "/edit";
    }

    /** /registration-periods/{id} → /admin/periods/{id} */
    @GetMapping("/registration-periods/{id}")
    public String periodsDetail(Long id) {
        return "forward:/admin/periods/" + id;
    }

    /** /registrations → /groups */
    @GetMapping("/registrations")
    public String registrations() {
        return "forward:/groups";
    }

    /** /registrations/{id} → /groups/{id} */
    @GetMapping("/registrations/{id}")
    public String registrationsDetail(Long id) {
        return "forward:/groups/" + id;
    }

    /** /review-assignments → /assignments */
    @GetMapping("/review-assignments")
    public String reviewAssignments() {
        return "forward:/assignments";
    }

    /** /results → /results/me  (cho SV xem kết quả) */
    @GetMapping("/results")
    public String results() {
        return "forward:/results/me";
    }

    /** /notifications → /notices/manage */
    @GetMapping("/notifications")
    public String notifications() {
        return "forward:/notices/manage";
    }

    /** /notifications/new → /notices/manage/new */
    @GetMapping("/notifications/new")
    public String notificationsNew() {
        return "forward:/notices/manage/new";
    }

    /** /notifications/{id}/edit → /notices/manage/{id}/edit */
    @GetMapping("/notifications/{id}/edit")
    public String notificationsEdit(Long id) {
        return "forward:/notices/manage/" + id + "/edit";
    }

    /** /notifications/{id} → /notices/manage/{id} */
    @GetMapping("/notifications/{id}")
    public String notificationsDetail(Long id) {
        return "forward:/notices/manage/" + id;
    }
}
