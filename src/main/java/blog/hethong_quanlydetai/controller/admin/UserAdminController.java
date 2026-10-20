package blog.hethong_quanlydetai.controller.admin;

import blog.hethong_quanlydetai.domain.Role;
import blog.hethong_quanlydetai.domain.User;
import blog.hethong_quanlydetai.dto.UserForm;
import blog.hethong_quanlydetai.repository.RoleRepository;
import blog.hethong_quanlydetai.security.AppUserPrincipal;
import blog.hethong_quanlydetai.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/users")
@PreAuthorize("hasAuthority('USER_MANAGE')")
@RequiredArgsConstructor
public class UserAdminController {

    private final UserService userService;
    private final RoleRepository roleRepository;

    @GetMapping
    public String list(@RequestParam(required = false) String q,
                       @RequestParam(required = false) Long roleId,
                       @RequestParam(required = false) String status,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "20") int size,
                       Model model) {
        Page<User> p = userService.search(q, roleId, status,
                PageRequest.of(page, size, Sort.by("id").descending()));
        model.addAttribute("page", p);
        model.addAttribute("q", q);
        model.addAttribute("roleId", roleId);
        model.addAttribute("status", status);
        model.addAttribute("allRoles", roleRepository.findAll());
        return "admin/users/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("form", new UserForm());
        model.addAttribute("allRoles", roleRepository.findAll());
        model.addAttribute("isNew", true);
        return "admin/users/form";
    }

    @PostMapping("/new")
    public String create(@Valid @ModelAttribute("form") UserForm form,
                         BindingResult br,
                         RedirectAttributes ra,
                         Model model) {
        if (br.hasErrors()) {
            model.addAttribute("allRoles", roleRepository.findAll());
            model.addAttribute("isNew", true);
            return "admin/users/form";
        }
        try {
            userService.create(form);
            ra.addFlashAttribute("flashSuccess", "Tạo tài khoản thành công");
            return "redirect:/admin/users";
        } catch (Exception ex) {
            ra.addFlashAttribute("flashError", ex.getMessage());
            return "redirect:/admin/users/new";
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model, RedirectAttributes ra) {
        try {
            User u = userService.getById(id);
            UserForm form = toForm(u);
            model.addAttribute("form", form);
            model.addAttribute("allRoles", roleRepository.findAll());
            model.addAttribute("isNew", false);
            return "admin/users/form";
        } catch (Exception ex) {
            ra.addFlashAttribute("flashError", ex.getMessage());
            return "redirect:/admin/users";
        }
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("form") UserForm form,
                         BindingResult br,
                         RedirectAttributes ra,
                         Model model) {
        if (br.hasErrors()) {
            model.addAttribute("allRoles", roleRepository.findAll());
            model.addAttribute("isNew", false);
            return "admin/users/form";
        }
        try {
            userService.update(id, form);
            ra.addFlashAttribute("flashSuccess", "Cập nhật tài khoản thành công");
            return "redirect:/admin/users";
        } catch (Exception ex) {
            ra.addFlashAttribute("flashError", ex.getMessage());
            return "redirect:/admin/users/" + id + "/edit";
        }
    }

    @PostMapping("/{id}/lock")
    public String lock(@PathVariable Long id,
                       @AuthenticationPrincipal AppUserPrincipal me,
                       RedirectAttributes ra) {
        try {
            userService.setLocked(id, true, me.getUserId());
            ra.addFlashAttribute("flashSuccess", "Đã khóa tài khoản");
        } catch (Exception ex) {
            ra.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/unlock")
    public String unlock(@PathVariable Long id, RedirectAttributes ra) {
        try {
            userService.setLocked(id, false, null);
            ra.addFlashAttribute("flashSuccess", "Đã mở khóa tài khoản");
        } catch (Exception ex) {
            ra.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/reset-password")
    public String resetPassword(@PathVariable Long id, RedirectAttributes ra) {
        try {
            userService.resetPassword(id);
            ra.addFlashAttribute("flashSuccess", "Đã đặt lại mật khẩu (mặc định trong application.properties)");
        } catch (Exception ex) {
            ra.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/admin/users";
    }

    private UserForm toForm(User u) {
        UserForm f = new UserForm();
        f.setId(u.getId());
        f.setUsername(u.getUsername());
        f.setFullName(u.getFullName());
        f.setEmail(u.getEmail());
        f.setPhone(u.getPhone());
        f.setRoleCodes(String.join(",", u.getRoles().stream().map(Role::getCode).toList()));
        return f;
    }
}