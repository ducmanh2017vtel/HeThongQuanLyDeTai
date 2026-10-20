package blog.hethong_quanlydetai.controller;

import blog.hethong_quanlydetai.domain.Council;
import blog.hethong_quanlydetai.domain.CouncilStatus;
import blog.hethong_quanlydetai.domain.PeriodType;
import blog.hethong_quanlydetai.domain.RegistrationPeriod;
import blog.hethong_quanlydetai.domain.TopicStatus;
import blog.hethong_quanlydetai.domain.User;
import blog.hethong_quanlydetai.dto.CouncilForm;
import blog.hethong_quanlydetai.repository.LecturerRepository;
import blog.hethong_quanlydetai.repository.RegistrationPeriodRepository;
import blog.hethong_quanlydetai.repository.TopicRepository;
import blog.hethong_quanlydetai.service.CouncilService;
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

import java.util.ArrayList;
import java.util.List;

/**
 * Hội đồng phản biện (GĐ7).
 */
@Controller
@RequestMapping("/councils")
@PreAuthorize("hasAuthority('COUNCIL_MANAGE')")
@RequiredArgsConstructor
public class CouncilAdminController {

    private final CouncilService councilService;
    private final RegistrationPeriodRepository periodRepository;
    private final TopicRepository topicRepository;
    private final LecturerRepository lecturerRepository;

    @GetMapping
    public String list(@RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "20") int size,
                       @RequestParam(required = false) Long periodId,
                       @RequestParam(required = false) CouncilStatus status,
                       @RequestParam(required = false) String q,
                       Model model) {
        model.addAttribute("pageTitle", "Hội đồng phản biện");
        model.addAttribute("activeMenu", "councils");
        model.addAttribute("periods", periodRepository.findAll());
        model.addAttribute("statuses", CouncilStatus.values());
        model.addAttribute("periodId", periodId);
        model.addAttribute("status", status);
        model.addAttribute("q", q);
        model.addAttribute("page", councilService.search(q, periodId, status,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"))));
        return "councils/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("pageTitle", "Tạo hội đồng");
        model.addAttribute("activeMenu", "councils");
        model.addAttribute("form", emptyForm());
        model.addAttribute("periods", periodRepository.findAll());
        model.addAttribute("lecturers", lecturerRepository.findAll());
        return "councils/form";
    }

    @PostMapping("/new")
    public String create(@Valid @ModelAttribute("form") CouncilForm form,
                         BindingResult br,
                         @AuthenticationPrincipal blog.hethong_quanlydetai.security.AppUserPrincipal principal,
                         RedirectAttributes ra) {
        if (br.hasErrors()) {
            ra.addFlashAttribute("flashError", "Vui lòng kiểm tra lại các trường");
            return "redirect:/councils/new";
        }
        try {
            Council c = councilService.create(form, currentUser(principal));
            ra.addFlashAttribute("flashSuccess", "Đã tạo hội đồng");
            return "redirect:/councils/" + c.getId();
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("flashError", ex.getMessage());
            return "redirect:/councils/new";
        }
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Council c = councilService.getById(id);
        List<RegistrationPeriod> periods = periodRepository.findAll();
        model.addAttribute("pageTitle", "Hội đồng " + c.getCouncilCode());
        model.addAttribute("activeMenu", "councils");
        model.addAttribute("council", c);
        model.addAttribute("periods", periods);
        return "councils/detail";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Council c = councilService.getById(id);
        if (c.getStatus() != CouncilStatus.DRAFT) {
            return "redirect:/councils/" + id;
        }
        CouncilForm form = CouncilForm.builder()
                .councilCode(c.getCouncilCode())
                .councilName(c.getCouncilName())
                .periodId(c.getPeriod().getId())
                .reportDate(c.getReportDate())
                .lecturerIds(c.getMembers().stream().map(m -> m.getLecturer().getId()).toList())
                .roles(c.getMembers().stream().map(m -> m.getRole().name()).toList())
                .build();
        model.addAttribute("pageTitle", "Sửa hội đồng");
        model.addAttribute("activeMenu", "councils");
        model.addAttribute("form", form);
        model.addAttribute("council", c);
        model.addAttribute("periods", periodRepository.findAll());
        model.addAttribute("lecturers", lecturerRepository.findAll());
        return "councils/form";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("form") CouncilForm form,
                         BindingResult br,
                         @AuthenticationPrincipal blog.hethong_quanlydetai.security.AppUserPrincipal principal,
                         RedirectAttributes ra) {
        if (br.hasErrors()) {
            ra.addFlashAttribute("flashError", "Vui lòng kiểm tra lại các trường");
            return "redirect:/councils/" + id + "/edit";
        }
        try {
            councilService.update(id, form, currentUser(principal));
            ra.addFlashAttribute("flashSuccess", "Đã cập nhật hội đồng");
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/councils/" + id;
    }

    @PostMapping("/{id}/ready")
    public String ready(@PathVariable Long id,
                        @AuthenticationPrincipal blog.hethong_quanlydetai.security.AppUserPrincipal principal,
                        RedirectAttributes ra) {
        try {
            councilService.markReady(id, currentUser(principal));
            ra.addFlashAttribute("flashSuccess", "Đã chuyển sang SẴN SÀNG");
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/councils/" + id;
    }

    @PostMapping("/{id}/finish")
    public String finish(@PathVariable Long id,
                         @AuthenticationPrincipal blog.hethong_quanlydetai.security.AppUserPrincipal principal,
                         RedirectAttributes ra) {
        try {
            councilService.markFinished(id, currentUser(principal));
            ra.addFlashAttribute("flashSuccess", "Đã hoàn tất hội đồng");
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/councils/" + id;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id,
                         @AuthenticationPrincipal blog.hethong_quanlydetai.security.AppUserPrincipal principal,
                         RedirectAttributes ra) {
        try {
            councilService.delete(id, currentUser(principal));
            ra.addFlashAttribute("flashSuccess", "Đã xoá hội đồng");
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/councils";
    }

    // Lấy đề tài theo đợt (cho form)
    @GetMapping("/topics-by-period/{periodId}")
    @ResponseBody
    public List<?> topicsByPeriod(@PathVariable Long periodId) {
        return topicRepository.search(null, null, periodId, null, true,
                org.springframework.data.domain.PageRequest.of(0, 500)).getContent().stream()
                .filter(t -> t.getStatus() == TopicStatus.APPROVED)
                .map(t -> java.util.Map.of(
                        "id", t.getId(),
                        "code", t.getTopicCode(),
                        "name", t.getTopicName()))
                .toList();
    }

    private CouncilForm emptyForm() {
        CouncilForm f = new CouncilForm();
        f.setLecturerIds(new ArrayList<>());
        f.setRoles(new ArrayList<>());
        return f;
    }

    private User currentUser(blog.hethong_quanlydetai.security.AppUserPrincipal p) {
        User u = new User();
        u.setId(p.getUserId());
        return u;
    }
}
