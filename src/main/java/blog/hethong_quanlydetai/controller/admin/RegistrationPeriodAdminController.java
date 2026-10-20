package blog.hethong_quanlydetai.controller.admin;

import blog.hethong_quanlydetai.domain.PeriodStatus;
import blog.hethong_quanlydetai.domain.PeriodType;
import blog.hethong_quanlydetai.domain.RegistrationPeriod;
import blog.hethong_quanlydetai.dto.RegistrationPeriodForm;
import blog.hethong_quanlydetai.service.RegistrationPeriodService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;

@Controller
@RequestMapping("/admin/periods")
@PreAuthorize("hasAuthority('PERIOD_MANAGE')")
@RequiredArgsConstructor
public class RegistrationPeriodAdminController {

    private final RegistrationPeriodService periodService;

    @GetMapping
    public String list(@RequestParam(required = false) String q,
                       @RequestParam(required = false) PeriodType type,
                       @RequestParam(required = false) PeriodStatus status,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "20") int size,
                       Model model) {
        model.addAttribute("pageTitle", "Đợt đăng ký");
        model.addAttribute("activeMenu", "periods");
        model.addAttribute("q", q);
        model.addAttribute("type", type);
        model.addAttribute("status", status);
        model.addAttribute("types", PeriodType.values());
        model.addAttribute("statuses", PeriodStatus.values());
        model.addAttribute("page", periodService.search(q, type, status,
            PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"))));
        return "admin/periods/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("pageTitle", "Tạo đợt đăng ký");
        model.addAttribute("activeMenu", "periods");
        model.addAttribute("form", new RegistrationPeriodForm());
        model.addAttribute("types", PeriodType.values());
        model.addAttribute("isEdit", false);
        return "admin/periods/form";
    }

    @PostMapping("/new")
    public String create(@Valid @ModelAttribute("form") RegistrationPeriodForm form,
                         BindingResult br,
                         RedirectAttributes ra) {
        if (br.hasErrors()) {
            return "admin/periods/form";
        }
        try {
            RegistrationPeriod p = periodService.create(form, currentUserOrNull());
            ra.addFlashAttribute("flashSuccess", "Đã tạo đợt đăng ký #" + p.getId());
            return "redirect:/admin/periods";
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("flashError", ex.getMessage());
            return "redirect:/admin/periods/new";
        }
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        RegistrationPeriod p = periodService.getById(id);
        model.addAttribute("pageTitle", "Đợt đăng ký #" + p.getId());
        model.addAttribute("activeMenu", "periods");
        model.addAttribute("period", p);
        model.addAttribute("currentStage", currentStage(p));
        return "admin/periods/detail";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        RegistrationPeriod p = periodService.getById(id);
        RegistrationPeriodForm form = new RegistrationPeriodForm();
        form.setName(p.getName());
        form.setPeriodType(p.getPeriodType());
        form.setLecturerStartAt(p.getLecturerStartAt());
        form.setLecturerEndAt(p.getLecturerEndAt());
        form.setStudentStartAt(p.getStudentStartAt());
        form.setStudentEndAt(p.getStudentEndAt());
        form.setReviewDeadline(p.getReviewDeadline());
        form.setCouncilReportDate(p.getCouncilReportDate());
        model.addAttribute("pageTitle", "Sửa đợt đăng ký");
        model.addAttribute("activeMenu", "periods");
        model.addAttribute("form", form);
        model.addAttribute("period", p);
        model.addAttribute("types", PeriodType.values());
        model.addAttribute("isEdit", true);
        return "admin/periods/form";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("form") RegistrationPeriodForm form,
                         BindingResult br,
                         RedirectAttributes ra) {
        if (br.hasErrors()) {
            return "admin/periods/form";
        }
        try {
            periodService.update(id, form, currentUserOrNull());
            ra.addFlashAttribute("flashSuccess", "Đã cập nhật đợt đăng ký");
            return "redirect:/admin/periods/" + id;
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("flashError", ex.getMessage());
            return "redirect:/admin/periods/" + id + "/edit";
        }
    }

    @PostMapping("/{id}/publish")
    public String publish(@PathVariable Long id, RedirectAttributes ra) {
        try {
            periodService.publish(id, currentUserOrNull());
            ra.addFlashAttribute("flashSuccess", "Đã công bố đợt đăng ký");
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/admin/periods/" + id;
    }

    @PostMapping("/{id}/close")
    public String close(@PathVariable Long id, RedirectAttributes ra) {
        try {
            periodService.close(id, currentUserOrNull());
            ra.addFlashAttribute("flashSuccess", "Đã đóng đợt đăng ký");
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/admin/periods/" + id;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        try {
            periodService.delete(id, currentUserOrNull());
            ra.addFlashAttribute("flashSuccess", "Đã xoá đợt đăng ký");
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/admin/periods";
    }

    private static String currentStage(RegistrationPeriod p) {
        if (p.getStatus() == PeriodStatus.DRAFT) return "Chưa mở";
        if (p.getStatus() == PeriodStatus.CLOSED) return "Đã đóng";
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(p.getLecturerStartAt())) return "Chưa mở";
        if (!now.isAfter(p.getLecturerEndAt())) return "GV đăng ký";
        if (now.isBefore(p.getStudentStartAt())) return "Chờ SV";
        if (!now.isAfter(p.getStudentEndAt())) return "SV đăng ký";
        return "Đã đóng";
    }

    private static blog.hethong_quanlydetai.domain.User currentUserOrNull() {
        var auth = org.springframework.security.core.context.SecurityContextHolder
            .getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        if (auth.getPrincipal() instanceof blog.hethong_quanlydetai.security.AppUserPrincipal p) {
            blog.hethong_quanlydetai.domain.User u = new blog.hethong_quanlydetai.domain.User();
            u.setId(p.getUserId());
            return u;
        }
        return null;
    }
}
