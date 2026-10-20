package blog.hethong_quanlydetai.controller.admin;

import blog.hethong_quanlydetai.domain.Permission;
import blog.hethong_quanlydetai.domain.Role;
import blog.hethong_quanlydetai.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Controller
@RequestMapping("/admin/permissions")
@PreAuthorize("hasAuthority('PERMISSION_MANAGE')")
@RequiredArgsConstructor
public class PermissionAdminController {

    private final PermissionService permissionService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("roles", permissionService.allRoles());
        return "admin/permissions/list";
    }

    @GetMapping("/assign/{roleId}")
    public String assignForm(@PathVariable Long roleId, Model model, RedirectAttributes ra) {
        try {
            Role r = permissionService.getRoleWithPermissions(roleId);
            List<Permission> all = permissionService.allPermissions();
            model.addAttribute("role", r);
            model.addAttribute("allPermissions", all);
            model.addAttribute("selectedIds",
                    r.getPermissions().stream().map(Permission::getId).toList());
            return "admin/permissions/assign";
        } catch (Exception ex) {
            ra.addFlashAttribute("flashError", ex.getMessage());
            return "redirect:/admin/permissions";
        }
    }

    @PostMapping("/assign/{roleId}")
    public String assign(@PathVariable Long roleId,
                         @RequestParam(required = false) Set<Long> permissionIds,
                         RedirectAttributes ra) {
        try {
            permissionService.assignPermissions(roleId, permissionIds == null
                    ? new HashSet<>() : permissionIds);
            ra.addFlashAttribute("flashSuccess", "Đã cập nhật quyền cho vai trò");
        } catch (Exception ex) {
            ra.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/admin/permissions/assign/" + roleId;
    }
}