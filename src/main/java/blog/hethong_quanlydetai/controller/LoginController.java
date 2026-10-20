package blog.hethong_quanlydetai.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LoginController {

    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "error", required = false) String error,
                            @RequestParam(value = "logout", required = false) String logout,
                            Model model) {
        // Thông báo chung chung, không lộ username có tồn tại hay không (§5.1)
        if (error != null) {
            model.addAttribute("loginError", "Sai tài khoản hoặc mật khẩu hoặc tài khoản bị khóa");
        }
        if (logout != null) {
            model.addAttribute("loginInfo", "Bạn đã đăng xuất thành công.");
        }
        return "login";
    }
}