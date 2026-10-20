package blog.hethong_quanlydetai.controller;

import blog.hethong_quanlydetai.domain.User;
import blog.hethong_quanlydetai.dto.ChangePasswordForm;
import blog.hethong_quanlydetai.security.AppUserPrincipal;
import blog.hethong_quanlydetai.service.PasswordService;
import blog.hethong_quanlydetai.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/me")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class MeController {

    private final UserService userService;
    private final PasswordService passwordService;

    @GetMapping
    public String profile(@AuthenticationPrincipal AppUserPrincipal me, Model model) {
        User u = userService.getById(me.getUserId());
        model.addAttribute("user", u);
        model.addAttribute("pageTitle", "Hồ sơ của tôi");
        model.addAttribute("activeMenu", "profile");
        return "me/profile";
    }

    @GetMapping("/change-password")
    public String changePasswordForm(Model model) {
        model.addAttribute("form", new ChangePasswordForm());
        model.addAttribute("pageTitle", "Đổi mật khẩu");
        model.addAttribute("activeMenu", "profile");
        return "me/change-password";
    }

    @PostMapping("/change-password")
    public String changePassword(@AuthenticationPrincipal AppUserPrincipal me,
                                 @Valid @ModelAttribute("form") ChangePasswordForm form,
                                 BindingResult br,
                                 RedirectAttributes ra,
                                 Model model) {
        if (br.hasErrors()) {
            model.addAttribute("pageTitle", "Đổi mật khẩu");
            model.addAttribute("activeMenu", "profile");
            return "me/change-password";
        }
        try {
            passwordService.changePassword(me.getUserId(), form);
            ra.addFlashAttribute("flashSuccess", "Đã đổi mật khẩu thành công");
            return "redirect:/me";
        } catch (Exception ex) {
            ra.addFlashAttribute("flashError", ex.getMessage());
            return "redirect:/me/change-password";
        }
    }
}