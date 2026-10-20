package blog.hethong_quanlydetai.controller;

import blog.hethong_quanlydetai.domain.GroupStatus;
import blog.hethong_quanlydetai.domain.StudentGroup;
import blog.hethong_quanlydetai.domain.Student;
import blog.hethong_quanlydetai.domain.Topic;
import blog.hethong_quanlydetai.domain.TopicRegistration;
import blog.hethong_quanlydetai.dto.AddMemberForm;
import blog.hethong_quanlydetai.dto.StudentGroupForm;
import blog.hethong_quanlydetai.dto.TopicRegistrationForm;
import blog.hethong_quanlydetai.repository.RegistrationPeriodRepository;
import blog.hethong_quanlydetai.repository.StudentRepository;
import blog.hethong_quanlydetai.repository.TopicRegistrationRepository;
import blog.hethong_quanlydetai.repository.TopicRepository;
import blog.hethong_quanlydetai.service.RegistrationPeriodService;
import blog.hethong_quanlydetai.service.RegistrationService;
import blog.hethong_quanlydetai.service.StudentGroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller cho SV lập nhóm, quản lý thành viên, đăng ký đề tài (mục 5.4).
 *
 * Phân quyền:
 * - SV (REGISTRATION_CREATE): lập nhóm, thêm/xoá thành viên, đăng ký/huỷ đề tài.
 * - GVHD (REGISTRATION_CONFIRM): duyệt/từ chối đăng ký.
 */
@Controller
@RequestMapping("/groups")
@RequiredArgsConstructor
public class GroupController {

    private final StudentGroupService groupService;
    private final RegistrationService registrationService;
    private final RegistrationPeriodService periodService;
    private final RegistrationPeriodRepository periodRepository;
    private final TopicRegistrationRepository registrationRepository;
    private final TopicRepository topicRepository;
    private final StudentRepository studentRepository;

    @GetMapping
    @PreAuthorize("hasAuthority('REGISTRATION_CREATE')")
    public String list(@AuthenticationPrincipal blog.hethong_quanlydetai.security.AppUserPrincipal principal,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "20") int size,
                       Model model) {
        Long studentId = currentStudentId(principal);
        model.addAttribute("pageTitle", "Nhóm & đăng ký");
        model.addAttribute("activeMenu", "groups");
        if (studentId == null) {
            model.addAttribute("myGroups", List.of());
        } else {
            // Lấy các nhóm mà SV này là thành viên
            List<StudentGroup> myGroups = new ArrayList<>();
            for (var p : periodRepository.findAll()) {
                myGroups.addAll(groupService.findMyGroups(studentId, p.getId()));
            }
            model.addAttribute("myGroups", myGroups);
        }
        model.addAttribute("openPeriods", periodRepository.findOpenForStudents(LocalDateTime.now()));
        return "groups/list";
    }

    @GetMapping("/new")
    @PreAuthorize("hasAuthority('REGISTRATION_CREATE')")
    public String createForm(Model model) {
        model.addAttribute("pageTitle", "Tạo nhóm SV");
        model.addAttribute("activeMenu", "groups");
        model.addAttribute("form", new StudentGroupForm());
        model.addAttribute("openPeriods", periodRepository.findOpenForStudents(LocalDateTime.now()));
        return "groups/form";
    }

    @PostMapping("/new")
    @PreAuthorize("hasAuthority('REGISTRATION_CREATE')")
    public String create(@Valid @ModelAttribute("form") StudentGroupForm form,
                         BindingResult br,
                         @AuthenticationPrincipal blog.hethong_quanlydetai.security.AppUserPrincipal principal,
                         RedirectAttributes ra) {
        if (br.hasErrors()) {
            return "groups/form";
        }
        try {
            StudentGroup g = groupService.createDraft(form, currentUser(principal));
            ra.addFlashAttribute("flashSuccess", "Đã tạo nhóm " + g.getGroupCode());
            return "redirect:/groups/" + g.getId();
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("flashError", ex.getMessage());
            return "redirect:/groups/new";
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('REGISTRATION_CREATE')")
    public String detail(@PathVariable Long id, Model model,
                         @AuthenticationPrincipal blog.hethong_quanlydetai.security.AppUserPrincipal principal) {
        StudentGroup g = groupService.getById(id);
        TopicRegistration reg = registrationService.findByGroupId(id);

        // Lấy danh sách đề tài đã công bố trong đợt của nhóm
        List<Topic> publishedTopics = topicRepository.findByPeriodIdAndPublishedTrue(g.getPeriod().getId());

        model.addAttribute("pageTitle", "Nhóm " + g.getGroupCode());
        model.addAttribute("activeMenu", "groups");
        model.addAttribute("group", g);
        model.addAttribute("registration", reg);
        model.addAttribute("publishedTopics", publishedTopics);
        model.addAttribute("addMemberForm", new AddMemberForm());
        model.addAttribute("regForm", new TopicRegistrationForm());
        model.addAttribute("currentStudentId", currentStudentId(principal));
        return "groups/detail";
    }

    @PostMapping("/{id}/add-member")
    @PreAuthorize("hasAuthority('REGISTRATION_CREATE')")
    public String addMember(@PathVariable Long id,
                            @Valid @ModelAttribute("addMemberForm") AddMemberForm form,
                            BindingResult br,
                            @AuthenticationPrincipal blog.hethong_quanlydetai.security.AppUserPrincipal principal,
                            RedirectAttributes ra) {
        if (br.hasErrors()) {
            ra.addFlashAttribute("flashError", "Vui lòng nhập mã sinh viên");
            return "redirect:/groups/" + id;
        }
        try {
            groupService.addMember(id, form, currentUser(principal));
            ra.addFlashAttribute("flashSuccess", "Đã thêm thành viên");
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/groups/" + id;
    }

    @PostMapping("/{id}/remove-member/{studentId}")
    @PreAuthorize("hasAuthority('REGISTRATION_CREATE')")
    public String removeMember(@PathVariable Long id,
                               @PathVariable Long studentId,
                               @AuthenticationPrincipal blog.hethong_quanlydetai.security.AppUserPrincipal principal,
                               RedirectAttributes ra) {
        try {
            groupService.removeMember(id, studentId, currentUser(principal));
            ra.addFlashAttribute("flashSuccess", "Đã xoá thành viên");
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/groups/" + id;
    }

    @PostMapping("/{id}/change-leader/{studentId}")
    @PreAuthorize("hasAuthority('REGISTRATION_CREATE')")
    public String changeLeader(@PathVariable Long id,
                               @PathVariable Long studentId,
                               @AuthenticationPrincipal blog.hethong_quanlydetai.security.AppUserPrincipal principal,
                               RedirectAttributes ra) {
        try {
            groupService.changeLeader(id, studentId, currentUser(principal));
            ra.addFlashAttribute("flashSuccess", "Đã đổi trưởng nhóm");
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/groups/" + id;
    }

    @PostMapping("/{id}/leave")
    @PreAuthorize("hasAuthority('REGISTRATION_CREATE')")
    public String leave(@PathVariable Long id,
                        @AuthenticationPrincipal blog.hethong_quanlydetai.security.AppUserPrincipal principal,
                        RedirectAttributes ra) {
        try {
            groupService.leaveGroup(id, currentUser(principal));
            ra.addFlashAttribute("flashSuccess", "Đã rời nhóm");
            return "redirect:/groups";
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("flashError", ex.getMessage());
            return "redirect:/groups/" + id;
        }
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize("hasAuthority('REGISTRATION_CREATE')")
    public String delete(@PathVariable Long id,
                         @AuthenticationPrincipal blog.hethong_quanlydetai.security.AppUserPrincipal principal,
                         RedirectAttributes ra) {
        try {
            groupService.deleteGroup(id, currentUser(principal));
            ra.addFlashAttribute("flashSuccess", "Đã xoá nhóm");
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/groups";
    }

    // ------------------------------------------------------------------
    // Đăng ký đề tài (SV)
    // ------------------------------------------------------------------

    @PostMapping("/{id}/register")
    @PreAuthorize("hasAuthority('REGISTRATION_CREATE')")
    public String register(@PathVariable Long id,
                           @RequestParam Long topicId,
                           @AuthenticationPrincipal blog.hethong_quanlydetai.security.AppUserPrincipal principal,
                           RedirectAttributes ra) {
        try {
            registrationService.register(id, topicId, currentUser(principal));
            ra.addFlashAttribute("flashSuccess", "Đã đăng ký đề tài. Đang chờ GVHD chấp thuận");
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/groups/" + id;
    }

    @PostMapping("/{id}/cancel-registration")
    @PreAuthorize("hasAuthority('REGISTRATION_CREATE')")
    public String cancelRegistration(@PathVariable Long id,
                                     @AuthenticationPrincipal blog.hethong_quanlydetai.security.AppUserPrincipal principal,
                                     RedirectAttributes ra) {
        try {
            registrationService.cancel(id, currentUser(principal));
            ra.addFlashAttribute("flashSuccess", "Đã huỷ đăng ký");
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/groups/" + id;
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private Long currentStudentId(blog.hethong_quanlydetai.security.AppUserPrincipal principal) {
        if (principal == null) return null;
        return studentRepository.findByUserId(principal.getUserId()).map(Student::getId).orElse(null);
    }

    private blog.hethong_quanlydetai.domain.User currentUser(blog.hethong_quanlydetai.security.AppUserPrincipal principal) {
        blog.hethong_quanlydetai.domain.User u = new blog.hethong_quanlydetai.domain.User();
        u.setId(principal.getUserId());
        return u;
    }
}
