package blog.hethong_quanlydetai.controller;

import blog.hethong_quanlydetai.domain.Notification;
import blog.hethong_quanlydetai.domain.NotificationStatus;
import blog.hethong_quanlydetai.domain.NotificationType;
import blog.hethong_quanlydetai.domain.Role;
import blog.hethong_quanlydetai.domain.User;
import blog.hethong_quanlydetai.dto.NotificationForm;
import blog.hethong_quanlydetai.repository.RoleRepository;
import blog.hethong_quanlydetai.security.AppUserPrincipal;
import blog.hethong_quanlydetai.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * GĐ10 — Thông báo.
 * - /notices          : danh sách thông báo (mọi role có NOTICE_VIEW)
 * - /notices/manage   : quản lý (NOTICE_MANAGE) — tạo/sửa/publish/hide
 * - /notices/{id}     : chi tiết
 */
@Controller
@RequestMapping("/notices")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final RoleRepository roleRepository;

    // ---- Danh sách + xem (mọi user có NOTICE_VIEW) ----

    @GetMapping
    @PreAuthorize("hasAuthority('NOTICE_VIEW')")
    public String list(Authentication authentication, Model model) {
        model.addAttribute("pageTitle", "Thông báo");
        model.addAttribute("activeMenu", "notices");
        Set<String> roles = currentRoles(authentication);
        model.addAttribute("notices", notificationService.findPublishedForRoles(roles));
        return "notices/list";
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('NOTICE_VIEW')")
    public String detail(@PathVariable Long id,
                         Authentication authentication,
                         Model model) {
        Notification n = notificationService.getById(id);
        Set<String> roles = currentRoles(authentication);
        boolean visible = n.getStatus() == NotificationStatus.PUBLISHED
                && (n.getTargetRoles() == null || n.getTargetRoles().isEmpty()
                    || n.getTargetRoles().stream().anyMatch(r -> roles.contains(r.getCode())));
        if (!visible) {
            return "redirect:/notices";
        }
        model.addAttribute("pageTitle", n.getTitle());
        model.addAttribute("activeMenu", "notices");
        model.addAttribute("notice", n);
        return "notices/detail";
    }

    // ---- Quản lý (NOTICE_MANAGE) ----

    @GetMapping("/manage")
    @PreAuthorize("hasAuthority('NOTICE_MANAGE')")
    public String manage(Model model) {
        model.addAttribute("pageTitle", "Quản lý thông báo");
        model.addAttribute("activeMenu", "notices-manage");
        model.addAttribute("notices", notificationService.findAll());
        return "notices/manage";
    }

    @GetMapping("/manage/new")
    @PreAuthorize("hasAuthority('NOTICE_MANAGE')")
    public String newForm(Model model) {
        model.addAttribute("pageTitle", "Tạo thông báo");
        model.addAttribute("activeMenu", "notices-manage");
        model.addAttribute("form", NotificationForm.builder()
                .type(NotificationType.FACULTY)
                .targetRoleCodes(new HashSet<>())
                .build());
        model.addAttribute("allRoles", roleRepository.findAll());
        model.addAttribute("types", NotificationType.values());
        return "notices/form";
    }

    @PostMapping("/manage/save")
    @PreAuthorize("hasAuthority('NOTICE_MANAGE')")
    public String save(@Valid @ModelAttribute("form") NotificationForm form,
                       BindingResult br,
                       @AuthenticationPrincipal AppUserPrincipal principal,
                       Model model,
                       RedirectAttributes ra) {
        if (br.hasErrors()) {
            model.addAttribute("pageTitle", "Tạo thông báo");
            model.addAttribute("activeMenu", "notices-manage");
            model.addAttribute("allRoles", roleRepository.findAll());
            model.addAttribute("types", NotificationType.values());
            return "notices/form";
        }
        try {
            User u = new User();
            u.setId(principal.getUserId());
            Notification n = notificationService.create(form, u);
            ra.addFlashAttribute("flashSuccess", "Đã tạo thông báo");
            return "redirect:/notices/manage";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("pageTitle", "Tạo thông báo");
            model.addAttribute("activeMenu", "notices-manage");
            model.addAttribute("allRoles", roleRepository.findAll());
            model.addAttribute("types", NotificationType.values());
            model.addAttribute("flashError", ex.getMessage());
            return "notices/form";
        }
    }

    @GetMapping("/manage/{id}/edit")
    @PreAuthorize("hasAuthority('NOTICE_MANAGE')")
    public String editForm(@PathVariable Long id, Model model) {
        Notification n = notificationService.getById(id);
        if (n.getStatus() == NotificationStatus.PUBLISHED) {
            return "redirect:/notices/manage";
        }
        NotificationForm form = NotificationForm.builder()
                .title(n.getTitle())
                .content(n.getContent())
                .type(n.getType())
                .targetRoleCodes(n.getTargetRoles() == null ? new HashSet<>()
                        : n.getTargetRoles().stream().map(Role::getCode).collect(Collectors.toSet()))
                .build();
        model.addAttribute("pageTitle", "Sửa thông báo");
        model.addAttribute("activeMenu", "notices-manage");
        model.addAttribute("form", form);
        model.addAttribute("noticeId", n.getId());
        model.addAttribute("allRoles", roleRepository.findAll());
        model.addAttribute("types", NotificationType.values());
        return "notices/form";
    }

    @PostMapping("/manage/{id}/update")
    @PreAuthorize("hasAuthority('NOTICE_MANAGE')")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("form") NotificationForm form,
                         BindingResult br,
                         @AuthenticationPrincipal AppUserPrincipal principal,
                         Model model,
                         RedirectAttributes ra) {
        if (br.hasErrors()) {
            model.addAttribute("pageTitle", "Sửa thông báo");
            model.addAttribute("activeMenu", "notices-manage");
            model.addAttribute("noticeId", id);
            model.addAttribute("allRoles", roleRepository.findAll());
            model.addAttribute("types", NotificationType.values());
            return "notices/form";
        }
        try {
            User u = new User();
            u.setId(principal.getUserId());
            notificationService.update(id, form, u);
            ra.addFlashAttribute("flashSuccess", "Đã cập nhật thông báo");
            return "redirect:/notices/manage";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("pageTitle", "Sửa thông báo");
            model.addAttribute("activeMenu", "notices-manage");
            model.addAttribute("noticeId", id);
            model.addAttribute("allRoles", roleRepository.findAll());
            model.addAttribute("types", NotificationType.values());
            model.addAttribute("flashError", ex.getMessage());
            return "notices/form";
        }
    }

    @PostMapping("/manage/{id}/publish")
    @PreAuthorize("hasAuthority('NOTICE_MANAGE')")
    public String publish(@PathVariable Long id,
                          @AuthenticationPrincipal AppUserPrincipal principal,
                          RedirectAttributes ra) {
        try {
            User u = new User();
            u.setId(principal.getUserId());
            notificationService.publish(id, u);
            ra.addFlashAttribute("flashSuccess", "Đã công bố thông báo");
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/notices/manage";
    }

    @PostMapping("/manage/{id}/hide")
    @PreAuthorize("hasAuthority('NOTICE_MANAGE')")
    public String hide(@PathVariable Long id,
                       @AuthenticationPrincipal AppUserPrincipal principal,
                       RedirectAttributes ra) {
        try {
            User u = new User();
            u.setId(principal.getUserId());
            notificationService.hide(id, u);
            ra.addFlashAttribute("flashSuccess", "Đã ẩn thông báo");
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/notices/manage";
    }

    @PostMapping("/manage/{id}/delete")
    @PreAuthorize("hasAuthority('NOTICE_MANAGE')")
    public String delete(@PathVariable Long id,
                         @AuthenticationPrincipal AppUserPrincipal principal,
                         RedirectAttributes ra) {
        try {
            User u = new User();
            u.setId(principal.getUserId());
            notificationService.delete(id, u);
            ra.addFlashAttribute("flashSuccess", "Đã xoá thông báo");
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/notices/manage";
    }

    // ---- helpers ----

    private Set<String> currentRoles(Authentication auth) {
        if (auth == null) return Set.of();
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring(5))
                .collect(Collectors.toSet());
    }
}
