package blog.hethong_quanlydetai.controller.admin;

import blog.hethong_quanlydetai.domain.*;
import blog.hethong_quanlydetai.dto.TopicForm;
import blog.hethong_quanlydetai.repository.DepartmentRepository;
import blog.hethong_quanlydetai.repository.LecturerRepository;
import blog.hethong_quanlydetai.repository.RegistrationPeriodRepository;
import blog.hethong_quanlydetai.service.TopicService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;
import java.util.List;

/**
 * Controller quản lý đề tài dành cho Trưởng khoa.
 * - Xem danh sách đề tài (lọc theo bộ môn, đợt, trạng thái).
 * - Duyệt/từ chối đề tài.
 * - Công bố danh sách đề tài.
 */
@Controller
@RequestMapping("/admin/topics")
@RequiredArgsConstructor
public class TopicAdminController {

    private final TopicService topicService;
    private final DepartmentRepository departmentRepository;
    private final RegistrationPeriodRepository periodRepository;
    private final LecturerRepository lecturerRepository;

    // ------------------------------------------------------------------
    // Danh sách & tìm kiếm
    // ------------------------------------------------------------------

    @GetMapping
    @PreAuthorize("hasAnyAuthority('TOPIC_APPROVE', 'TOPIC_VIEW')")
    public String list(@RequestParam(required = false) String q,
                       @RequestParam(required = false) TopicStatus status,
                       @RequestParam(required = false) Long periodId,
                       @RequestParam(required = false) Long departmentId,
                       @RequestParam(required = false) Boolean published,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "20") int size,
                       Model model) {
        model.addAttribute("pageTitle", "Quản lý đề tài");
        model.addAttribute("activeMenu", "topics");

        model.addAttribute("q", q);
        model.addAttribute("status", status);
        model.addAttribute("periodId", periodId);
        model.addAttribute("departmentId", departmentId);
        model.addAttribute("published", published);

        model.addAttribute("statuses", TopicStatus.values());
        model.addAttribute("periods", periodRepository.findAll(Sort.by(Sort.Direction.DESC, "id")));
        model.addAttribute("departments", departmentRepository.findAll());

        model.addAttribute("page", topicService.search(q, status, periodId, departmentId, published,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"))));

        return "admin/topics/list";
    }

    // ------------------------------------------------------------------
    // Chi tiết
    // ------------------------------------------------------------------

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('TOPIC_APPROVE', 'TOPIC_VIEW')")
    public String detail(@PathVariable Long id, Model model) {
        Topic topic = topicService.getWithDetails(id);
        model.addAttribute("pageTitle", "Chi tiết đề tài");
        model.addAttribute("activeMenu", "topics");
        model.addAttribute("topic", topic);
        model.addAttribute("supervisors", topic.getSupervisors());
        return "admin/topics/detail";
    }

    // ------------------------------------------------------------------
    // Duyệt / Từ chối
    // ------------------------------------------------------------------

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('TOPIC_APPROVE')")
    public String approve(@PathVariable Long id, RedirectAttributes ra) {
        try {
            topicService.approve(id, currentUser());
            ra.addFlashAttribute("flashSuccess", "Đã duyệt đề tài");
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/admin/topics/" + id;
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('TOPIC_APPROVE')")
    public String reject(@PathVariable Long id,
                        @RequestParam String rejectReason,
                        RedirectAttributes ra) {
        try {
            topicService.reject(id, rejectReason, currentUser());
            ra.addFlashAttribute("flashSuccess", "Đã từ chối đề tài");
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/admin/topics/" + id;
    }

    // ------------------------------------------------------------------
    // Công bố
    // ------------------------------------------------------------------

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAuthority('TOPIC_APPROVE')")
    public String publish(@PathVariable Long id, RedirectAttributes ra) {
        try {
            topicService.publish(id, currentUser());
            ra.addFlashAttribute("flashSuccess", "Đã công bố đề tài");
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/admin/topics/" + id;
    }

    @PostMapping("/publish-by-period")
    @PreAuthorize("hasAuthority('TOPIC_APPROVE')")
    public String publishByPeriod(@RequestParam Long periodId, RedirectAttributes ra) {
        try {
            int count = topicService.publishByPeriod(periodId, currentUser());
            ra.addFlashAttribute("flashSuccess", "Đã công bố " + count + " đề tài");
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/admin/topics?periodId=" + periodId;
    }

    // ------------------------------------------------------------------
    // Xóa
    // ------------------------------------------------------------------

    @PostMapping("/{id}/delete")
    @PreAuthorize("hasAuthority('TOPIC_APPROVE')")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        try {
            topicService.delete(id, currentUser());
            ra.addFlashAttribute("flashSuccess", "Đã xóa đề tài");
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/admin/topics";
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private User currentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof blog.hethong_quanlydetai.security.AppUserPrincipal p) {
            User u = new User();
            u.setId(p.getUserId());
            return u;
        }
        return null;
    }
}
