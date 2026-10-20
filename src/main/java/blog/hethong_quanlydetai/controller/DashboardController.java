package blog.hethong_quanlydetai.controller;

import blog.hethong_quanlydetai.domain.RoleCode;
import blog.hethong_quanlydetai.domain.User;
import blog.hethong_quanlydetai.security.AppUserPrincipal;
import blog.hethong_quanlydetai.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping({"/", "/dashboard"})
    @PreAuthorize("isAuthenticated()")
    public String dashboard(Authentication authentication, Model model) {
        AppUserPrincipal p = (AppUserPrincipal) authentication.getPrincipal();
        Set<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring(5))
                .collect(Collectors.toSet());

        model.addAttribute("principal", p);
        model.addAttribute("roles", roles);
        model.addAttribute("activeMenu", "dashboard");
        // Các biến quyền (canXxx) đã được GlobalModelAttributes gắn tự động
        // Các biến dashboard data do DashboardService cung cấp
        model.addAttribute("todos", dashboardService.todoFor(p.getUserId(), roles));
        model.addAttribute("summary", dashboardService.summary());
        model.addAttribute("openPeriods", dashboardService.openPeriods());
        model.addAttribute("latestNotices", dashboardService.latestNotices(roles));
        return "dashboard";
    }
}