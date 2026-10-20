package blog.hethong_quanlydetai.controller.admin;

import blog.hethong_quanlydetai.domain.Role;
import blog.hethong_quanlydetai.dto.RoleForm;
import blog.hethong_quanlydetai.service.RoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/roles")
@PreAuthorize("hasAuthority('ROLE_MANAGE')")
@RequiredArgsConstructor
public class RoleAdminController {

    private final RoleService roleService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("roles", roleService.findAll());
        return "admin/roles/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("form", new RoleForm());
        model.addAttribute("isNew", true);
        return "admin/roles/form";
    }

    @PostMapping("/new")
    public String create(@Valid @ModelAttribute("form") RoleForm form,
                         BindingResult br, RedirectAttributes ra, Model model) {
        if (br.hasErrors()) { model.addAttribute("isNew", true); return "admin/roles/form"; }
        try {
            roleService.create(form);
            ra.addFlashAttribute("flashSuccess", "Tạo vai trò thành công");
            return "redirect:/admin/roles";
        } catch (Exception ex) {
            ra.addFlashAttribute("flashError", ex.getMessage());
            return "redirect:/admin/roles/new";
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model, RedirectAttributes ra) {
        try {
            Role r = roleService.getById(id);
            RoleForm f = new RoleForm();
            f.setId(r.getId());
            f.setCode(r.getCode());
            f.setName(r.getName());
            f.setDescription(r.getDescription());
            model.addAttribute("form", f);
            model.addAttribute("isNew", false);
            model.addAttribute("systemFlag", r.getSystemFlag());
            return "admin/roles/form";
        } catch (Exception ex) {
            ra.addFlashAttribute("flashError", ex.getMessage());
            return "redirect:/admin/roles";
        }
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("form") RoleForm form,
                         BindingResult br, RedirectAttributes ra, Model model) {
        if (br.hasErrors()) { model.addAttribute("isNew", false); return "admin/roles/form"; }
        try {
            roleService.update(id, form);
            ra.addFlashAttribute("flashSuccess", "Cập nhật vai trò thành công");
            return "redirect:/admin/roles";
        } catch (Exception ex) {
            ra.addFlashAttribute("flashError", ex.getMessage());
            return "redirect:/admin/roles/" + id + "/edit";
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        try {
            roleService.delete(id);
            ra.addFlashAttribute("flashSuccess", "Đã xóa vai trò");
        } catch (Exception ex) {
            ra.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/admin/roles";
    }
}