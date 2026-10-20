package blog.hethong_quanlydetai.controller;

import blog.hethong_quanlydetai.domain.GroupStatus;
import blog.hethong_quanlydetai.domain.StudentGroup;
import blog.hethong_quanlydetai.domain.TopicRegistration;
import blog.hethong_quanlydetai.dto.RejectRegistrationForm;
import blog.hethong_quanlydetai.repository.LecturerRepository;
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

/**
 * Màn hình GVHD duyệt/từ chối đăng ký đề tài (mục 5.4).
 * URL: /registrations-approval  (giải phóng /registrations cho sidebar spec)
 */
@Controller
@RequestMapping("/registrations-approval")
@PreAuthorize("hasAuthority('REGISTRATION_CONFIRM')")
@RequiredArgsConstructor
public class RegistrationApprovalController {

    private final StudentGroupService groupService;
    private final RegistrationService registrationService;
    private final LecturerRepository lecturerRepository;

    @GetMapping
    public String list(@RequestParam(required = false) GroupStatus status,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "20") int size,
                       @AuthenticationPrincipal blog.hethong_quanlydetai.security.AppUserPrincipal principal,
                       Model model) {
        var lecturer = lecturerRepository.findByUserId(principal.getUserId()).orElse(null);
        model.addAttribute("pageTitle", "Duyệt nhóm đăng ký đề tài");
        model.addAttribute("activeMenu", "groups");
        model.addAttribute("status", status);
        model.addAttribute("statuses", GroupStatus.values());
        if (lecturer == null) {
            model.addAttribute("page", PageRequest.of(0, size).getPageNumber() == 0
                    ? org.springframework.data.domain.Page.empty()
                    : org.springframework.data.domain.Page.empty());
        } else {
            model.addAttribute("page", groupService.findGroupsForSupervisor(
                    lecturer.getId(), status,
                    PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"))));
        }
        return "registrations/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        StudentGroup g = groupService.getById(id);
        TopicRegistration reg = registrationService.findByGroupId(id);
        model.addAttribute("pageTitle", "Duyệt đăng ký - Nhóm " + g.getGroupCode());
        model.addAttribute("activeMenu", "groups");
        model.addAttribute("group", g);
        model.addAttribute("registration", reg);
        model.addAttribute("rejectForm", new RejectRegistrationForm());
        return "registrations/detail";
    }

    @PostMapping("/{id}/approve")
    public String approve(@PathVariable Long id,
                          @AuthenticationPrincipal blog.hethong_quanlydetai.security.AppUserPrincipal principal,
                          RedirectAttributes ra) {
        try {
            TopicRegistration reg = registrationService.findByGroupId(id);
            if (reg == null) {
                ra.addFlashAttribute("flashError", "Nhóm chưa đăng ký đề tài");
            } else {
                registrationService.approve(reg.getId(), currentUser(principal));
                ra.addFlashAttribute("flashSuccess", "Đã duyệt đăng ký");
            }
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/registrations-approval";
    }

    @PostMapping("/{id}/reject")
    public String reject(@PathVariable Long id,
                         @Valid @ModelAttribute("rejectForm") RejectRegistrationForm form,
                         BindingResult br,
                         @AuthenticationPrincipal blog.hethong_quanlydetai.security.AppUserPrincipal principal,
                         RedirectAttributes ra) {
        if (br.hasErrors()) {
            ra.addFlashAttribute("flashError", "Lý do từ chối từ 10 đến 500 ký tự");
            return "redirect:/registrations-approval/" + id;
        }
        try {
            TopicRegistration reg = registrationService.findByGroupId(id);
            if (reg == null) {
                ra.addFlashAttribute("flashError", "Nhóm chưa đăng ký đề tài");
            } else {
                registrationService.reject(reg.getId(), form, currentUser(principal));
                ra.addFlashAttribute("flashSuccess", "Đã từ chối đăng ký");
            }
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/registrations-approval";
    }

    private blog.hethong_quanlydetai.domain.User currentUser(blog.hethong_quanlydetai.security.AppUserPrincipal principal) {
        blog.hethong_quanlydetai.domain.User u = new blog.hethong_quanlydetai.domain.User();
        u.setId(principal.getUserId());
        return u;
    }
}
