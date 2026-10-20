package blog.hethong_quanlydetai.controller;

import blog.hethong_quanlydetai.domain.*;
import blog.hethong_quanlydetai.dto.TopicForm;
import blog.hethong_quanlydetai.repository.*;
import blog.hethong_quanlydetai.service.RegistrationPeriodService;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controller đề xuất & quản lý đề tài của giảng viên.
 * - GV đề xuất đề tài mới.
 * - GV xem và sửa đề tài của mình.
 */
@Controller
@RequestMapping("/topics")
@RequiredArgsConstructor
public class TopicLecturerController {

    private final TopicService topicService;
    private final RegistrationPeriodService periodService;
    private final RegistrationPeriodRepository periodRepository;
    private final DepartmentRepository departmentRepository;
    private final LecturerRepository lecturerRepository;

    // ------------------------------------------------------------------
    // Danh sách đề tài của tôi
    // ------------------------------------------------------------------

    @GetMapping
    @PreAuthorize("hasAuthority('TOPIC_CREATE')")
    public String myTopics(Model model) {
        model.addAttribute("pageTitle", "Đề tài của tôi");
        model.addAttribute("activeMenu", "topics");

        // Lấy các đợt đang mở cho GV
        List<RegistrationPeriod> openPeriods = periodRepository.findOpenForLecturers(LocalDateTime.now());
        model.addAttribute("openPeriods", openPeriods);

        return "topics/list";
    }

    // ------------------------------------------------------------------
    // Tạo mới
    // ------------------------------------------------------------------

    @GetMapping("/new")
    @PreAuthorize("hasAuthority('TOPIC_CREATE')")
    public String createForm(@RequestParam(required = false) Long periodId, Model model) {
        model.addAttribute("pageTitle", "Đề xuất đề tài mới");
        model.addAttribute("activeMenu", "topics");

        // Lấy thông tin bộ môn của GV đang đăng nhập
        Long currentUserId = getCurrentUserId();
        Lecturer lecturer = lecturerRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new IllegalArgumentException("Tài khoản không phải giảng viên"));

        // Các đợt đang trong khung thời gian GV
        List<RegistrationPeriod> openPeriods = periodRepository.findOpenForLecturers(LocalDateTime.now());
        if (openPeriods.isEmpty()) {
            model.addAttribute("flashError", "Hiện không có đợt nào mở cho giảng viên đăng ký đề tài");
            return "redirect:/topics";
        }

        TopicForm form = new TopicForm();
        // Mặc định: bộ môn của GV, đợt đầu tiên đang mở
        form.setDepartmentId(lecturer.getDepartment().getId());

        model.addAttribute("form", form);
        model.addAttribute("department", lecturer.getDepartment());
        model.addAttribute("departments", departmentRepository.findAll());
        model.addAttribute("periods", openPeriods);
        model.addAttribute("selectedPeriodId", periodId != null ? periodId : (openPeriods.isEmpty() ? null : openPeriods.get(0).getId()));

        // GVHD khả dụng (cùng bộ môn)
        List<Lecturer> availableSupervisors = lecturerRepository.findActiveByDepartmentId(lecturer.getDepartment().getId());
        model.addAttribute("availableSupervisors", availableSupervisors);

        return "topics/form";
    }

    @PostMapping("/new")
    @PreAuthorize("hasAuthority('TOPIC_CREATE')")
    public String create(@Valid @ModelAttribute("form") TopicForm form,
                        BindingResult br,
                        RedirectAttributes ra) {
        if (br.hasErrors()) {
            return "topics/form";
        }
        try {
            Topic topic = topicService.create(form, getCurrentUserEntity());
            ra.addFlashAttribute("flashSuccess", "Đã gửi đề tài '" + topic.getTopicName() + "' để duyệt");
            return "redirect:/topics";
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("flashError", ex.getMessage());
            return "redirect:/topics/new?periodId=" + form.getPeriodId();
        }
    }

    // ------------------------------------------------------------------
    // Sửa
    // ------------------------------------------------------------------

    @GetMapping("/{id}/edit")
    @PreAuthorize("hasAuthority('TOPIC_CREATE')")
    public String editForm(@PathVariable Long id, Model model) {
        Topic topic = topicService.getById(id);

        // Kiểm tra quyền sở hữu
        Long currentUserId = getCurrentUserId();
        Lecturer lecturer = lecturerRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new IllegalArgumentException("Tài khoản không phải giảng viên"));

        if (!topic.getCreatedByLecturer().getUser().getId().equals(currentUserId)) {
            throw new IllegalArgumentException("Bạn không có quyền sửa đề tài này");
        }

        // Chỉ sửa PENDING hoặc REJECTED
        if (topic.getStatus() == TopicStatus.APPROVED) {
            model.addAttribute("flashError", "Đề tài đã được duyệt, không thể sửa");
            return "redirect:/topics/" + id;
        }

        model.addAttribute("pageTitle", "Sửa đề tài");
        model.addAttribute("activeMenu", "topics");

        // Map entity -> form
        TopicForm form = new TopicForm();
        form.setTopicCode(topic.getTopicCode());
        form.setTopicName(topic.getTopicName());
        form.setDescription(topic.getDescription());
        form.setDepartmentId(topic.getDepartment().getId());
        form.setPeriodId(topic.getPeriod().getId());

        List<TopicSupervisor> supervisors = topic.getSupervisors().stream()
                .sorted((a, b) -> Integer.compare(a.getSupervisorOrder(), b.getSupervisorOrder()))
                .toList();
        if (!supervisors.isEmpty()) {
            form.setSupervisor1Id(supervisors.get(0).getLecturer().getId());
        }
        if (supervisors.size() > 1) {
            form.setSupervisor2Id(supervisors.get(1).getLecturer().getId());
        }

        model.addAttribute("form", form);
        model.addAttribute("topic", topic);
        model.addAttribute("department", topic.getDepartment());
        model.addAttribute("period", topic.getPeriod());
        model.addAttribute("departments", departmentRepository.findAll());

        // GVHD khả dụng (cùng bộ môn)
        List<Lecturer> availableSupervisors = lecturerRepository.findActiveByDepartmentId(topic.getDepartment().getId());
        model.addAttribute("availableSupervisors", availableSupervisors);

        return "topics/form";
    }

    @PostMapping("/{id}/edit")
    @PreAuthorize("hasAuthority('TOPIC_CREATE')")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("form") TopicForm form,
                         BindingResult br,
                         RedirectAttributes ra) {
        if (br.hasErrors()) {
            return "topics/form";
        }
        try {
            topicService.update(id, form, getCurrentUserEntity());
            ra.addFlashAttribute("flashSuccess", "Đã cập nhật đề tài");
            return "redirect:/topics";
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("flashError", ex.getMessage());
            return "redirect:/topics/" + id + "/edit";
        }
    }

    // ------------------------------------------------------------------
    // Chi tiết
    // ------------------------------------------------------------------

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('TOPIC_VIEW')")
    public String detail(@PathVariable Long id, Model model) {
        Topic topic = topicService.getWithDetails(id);
        model.addAttribute("pageTitle", topic.getTopicName());
        model.addAttribute("activeMenu", "topics");
        model.addAttribute("topic", topic);
        model.addAttribute("supervisors", topic.getSupervisors());
        return "topics/detail";
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private Long getCurrentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof blog.hethong_quanlydetai.security.AppUserPrincipal p) {
            return p.getUserId();
        }
        throw new IllegalStateException("Không xác định được người dùng");
    }

    private User getCurrentUserEntity() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof blog.hethong_quanlydetai.security.AppUserPrincipal p) {
            User u = new User();
            u.setId(p.getUserId());
            return u;
        }
        throw new IllegalStateException("Không xác định được người dùng");
    }
}
