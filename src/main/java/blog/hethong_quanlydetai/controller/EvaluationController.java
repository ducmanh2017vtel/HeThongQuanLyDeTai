package blog.hethong_quanlydetai.controller;

import blog.hethong_quanlydetai.domain.Council;
import blog.hethong_quanlydetai.domain.CouncilTopic;
import blog.hethong_quanlydetai.domain.Lecturer;
import blog.hethong_quanlydetai.domain.ReviewAssignment;
import blog.hethong_quanlydetai.domain.User;
import blog.hethong_quanlydetai.dto.EvaluationForm;
import blog.hethong_quanlydetai.repository.CouncilTopicRepository;
import blog.hethong_quanlydetai.repository.LecturerRepository;
import blog.hethong_quanlydetai.service.CouncilService;
import blog.hethong_quanlydetai.service.EvaluationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Phân công chấm (GĐ8).
 * - /assignments: ASSIGNMENT_MANAGE (CTHD/Chủ tịch HĐ)
 * - /evaluations: EVALUATION_CREATE (GV được phân công)
 */
@Controller
@RequiredArgsConstructor
public class EvaluationController {

    private final EvaluationService evaluationService;
    private final CouncilService councilService;
    private final CouncilTopicRepository councilTopicRepository;
    private final LecturerRepository lecturerRepository;

    // ---- Phân công (CTHD/Chủ tịch HĐ) ----

    @GetMapping("/assignments")
    @PreAuthorize("hasAuthority('ASSIGNMENT_MANAGE')")
    public String listAssignments(@RequestParam(required = false) Long councilId,
                                  Model model) {
        model.addAttribute("pageTitle", "Phân công chấm");
        model.addAttribute("activeMenu", "assignments");
        model.addAttribute("councils", councilService.search(null, null, null,
                org.springframework.data.domain.PageRequest.of(0, 200)).getContent());
        if (councilId != null) {
            Council c = councilService.getById(councilId);
            model.addAttribute("selectedCouncil", c);
            model.addAttribute("assignments", evaluationService.findByCouncil(councilId));
            model.addAttribute("councilTopics", councilTopicRepository.findByCouncilId(councilId));
            model.addAttribute("lecturers", lecturerRepository.findAll());
        }
        return "assignments/list";
    }

    @PostMapping("/assignments/create")
    @PreAuthorize("hasAuthority('ASSIGNMENT_MANAGE')")
    public String createAssignment(@RequestParam Long councilId,
                                   @RequestParam Long topicId,
                                   @RequestParam Long lecturerId,
                                   @AuthenticationPrincipal blog.hethong_quanlydetai.security.AppUserPrincipal principal,
                                   RedirectAttributes ra) {
        try {
            evaluationService.assign(councilId, topicId, lecturerId, currentUser(principal));
            ra.addFlashAttribute("flashSuccess", "Đã phân công");
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/assignments?councilId=" + councilId;
    }

    @PostMapping("/assignments/{id}/delete")
    @PreAuthorize("hasAuthority('ASSIGNMENT_MANAGE')")
    public String deleteAssignment(@PathVariable Long id,
                                   @AuthenticationPrincipal blog.hethong_quanlydetai.security.AppUserPrincipal principal,
                                   RedirectAttributes ra) {
        try {
            evaluationService.removeAssignment(id, currentUser(principal));
            ra.addFlashAttribute("flashSuccess", "Đã xoá phân công");
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/assignments";
    }

    // ---- Nhập điểm (GV được phân công) ----

    @GetMapping("/evaluations")
    @PreAuthorize("hasAuthority('EVALUATION_CREATE')")
    public String myAssignments(@AuthenticationPrincipal blog.hethong_quanlydetai.security.AppUserPrincipal principal,
                                Model model) {
        Lecturer lec = lecturerRepository.findByUserId(principal.getUserId()).orElse(null);
        model.addAttribute("pageTitle", "Nhập điểm phản biện");
        model.addAttribute("activeMenu", "evaluations");
        if (lec == null) {
            model.addAttribute("assignments", java.util.List.of());
        } else {
            model.addAttribute("assignments", evaluationService.findByLecturer(lec.getId()));
        }
        return "evaluations/list";
    }

    @GetMapping("/evaluations/{assignmentId}")
    @PreAuthorize("hasAuthority('EVALUATION_CREATE')")
    public String evalForm(@PathVariable Long assignmentId, Model model) {
        model.addAttribute("pageTitle", "Nhập điểm");
        model.addAttribute("activeMenu", "evaluations");
        model.addAttribute("assignmentId", assignmentId);
        model.addAttribute("form", new EvaluationForm());
        return "evaluations/form";
    }

    @PostMapping("/evaluations/{assignmentId}")
    @PreAuthorize("hasAuthority('EVALUATION_CREATE')")
    public String submit(@PathVariable Long assignmentId,
                         @Valid @ModelAttribute("form") EvaluationForm form,
                         BindingResult br,
                         @AuthenticationPrincipal blog.hethong_quanlydetai.security.AppUserPrincipal principal,
                         RedirectAttributes ra) {
        if (br.hasErrors()) {
            ra.addFlashAttribute("flashError", "Vui lòng kiểm tra điểm và nhận xét (>= 10 ký tự)");
            return "redirect:/evaluations/" + assignmentId;
        }
        try {
            evaluationService.submitEvaluation(assignmentId, form, currentUser(principal));
            ra.addFlashAttribute("flashSuccess", "Đã nộp điểm");
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/evaluations";
    }

    private User currentUser(blog.hethong_quanlydetai.security.AppUserPrincipal p) {
        User u = new User();
        u.setId(p.getUserId());
        return u;
    }
}
