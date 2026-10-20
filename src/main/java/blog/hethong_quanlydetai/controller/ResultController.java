package blog.hethong_quanlydetai.controller;

import blog.hethong_quanlydetai.domain.*;
import blog.hethong_quanlydetai.dto.ReviewResultForm;
import blog.hethong_quanlydetai.repository.RegistrationPeriodRepository;
import blog.hethong_quanlydetai.repository.ReviewAssignmentRepository;
import blog.hethong_quanlydetai.repository.StudentGroupRepository;
import blog.hethong_quanlydetai.repository.StudentRepository;
import blog.hethong_quanlydetai.service.ResultService;
import blog.hethong_quanlydetai.service.StudentGroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tổng hợp & công bố kết quả (GĐ9).
 * - /results/manage: Trưởng khoa (RESULT_CALCULATE/RESULT_PUBLISH)
 * - /results/me: Sinh viên xem kết quả nhóm (RESULT_VIEW)
 */
@Controller
@RequiredArgsConstructor
public class ResultController {

    private final ResultService resultService;
    private final RegistrationPeriodRepository periodRepository;
    private final StudentGroupRepository groupRepository;
    private final StudentRepository studentRepository;
    private final ReviewAssignmentRepository assignmentRepository;

    // ---- Quản lý (Trưởng khoa) ----

    @GetMapping("/results/manage")
    @PreAuthorize("hasAnyAuthority('RESULT_CALCULATE','RESULT_PUBLISH')")
    public String manage(@RequestParam(required = false) Long periodId, Model model) {
        model.addAttribute("pageTitle", "Tổng hợp & công bố kết quả");
        model.addAttribute("activeMenu", "results");
        model.addAttribute("periods", periodRepository.findAll());
        model.addAttribute("periodId", periodId);
        if (periodId != null) {
            model.addAttribute("results", resultService.findByPeriod(periodId));
            // Lấy tất cả topic đã có phân công chấm trong đợt này
            List<Topic> allTopics = new ArrayList<>();
            Map<Long, Boolean> calculatedMap = new HashMap<>();
            for (ReviewAssignment a : assignmentRepository.findAll()) {
                if (a.getTopic() != null
                        && a.getTopic().getPeriod() != null
                        && a.getTopic().getPeriod().getId().equals(periodId)
                        && !allTopics.contains(a.getTopic())) {
                    allTopics.add(a.getTopic());
                    calculatedMap.put(a.getTopic().getId(),
                            resultService.findByGroup(
                                groupRepository.findAll().stream()
                                    .filter(g -> g.getTopic() != null && g.getTopic().getId().equals(a.getTopic().getId()))
                                    .findFirst().map(StudentGroup::getId).orElse(0L)
                            ).isPresent());
                }
            }
            model.addAttribute("topics", allTopics);
            model.addAttribute("calculatedMap", calculatedMap);
        } else {
            model.addAttribute("results", List.of());
            model.addAttribute("topics", List.of());
        }
        return "results/manage";
    }

    @PostMapping("/results/calculate")
    @PreAuthorize("hasAuthority('RESULT_CALCULATE')")
    public String calculate(@RequestParam Long topicId,
                            @AuthenticationPrincipal blog.hethong_quanlydetai.security.AppUserPrincipal principal,
                            @RequestParam(required = false) Long periodId,
                            RedirectAttributes ra) {
        try {
            User u = new User();
            u.setId(principal.getUserId());
            ReviewResult r = resultService.calculate(topicId, u);
            ra.addFlashAttribute("flashSuccess", "Đã tổng hợp điểm: "
                    + r.getFinalScore() + " (" + r.getResult() + ")");
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/results/manage" + (periodId != null ? "?periodId=" + periodId : "");
    }

    @PostMapping("/results/publish")
    @PreAuthorize("hasAuthority('RESULT_PUBLISH')")
    public String publish(@RequestParam Long resultId,
                          @AuthenticationPrincipal blog.hethong_quanlydetai.security.AppUserPrincipal principal,
                          @RequestParam(required = false) Long periodId,
                          RedirectAttributes ra) {
        try {
            User u = new User();
            u.setId(principal.getUserId());
            resultService.publish(resultId, u);
            ra.addFlashAttribute("flashSuccess", "Đã công bố kết quả");
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/results/manage" + (periodId != null ? "?periodId=" + periodId : "");
    }

    @PostMapping("/results/unpublish")
    @PreAuthorize("hasAuthority('RESULT_PUBLISH')")
    public String unpublish(@RequestParam Long resultId,
                            @RequestParam(required = false) Long periodId,
                            RedirectAttributes ra) {
        try {
            resultService.unpublish(resultId, null);
            ra.addFlashAttribute("flashSuccess", "Đã thu hồi công bố");
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/results/manage" + (periodId != null ? "?periodId=" + periodId : "");
    }

    @PostMapping("/results/{id}/summary")
    @PreAuthorize("hasAuthority('RESULT_CALCULATE')")
    public String updateSummary(@PathVariable Long id,
                                @Valid @ModelAttribute("form") ReviewResultForm form,
                                BindingResult br,
                                @RequestParam(required = false) Long periodId,
                                RedirectAttributes ra) {
        try {
            resultService.updateSummary(id, form.getSummaryComment(), null);
            ra.addFlashAttribute("flashSuccess", "Đã cập nhật nhận xét");
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/results/manage" + (periodId != null ? "?periodId=" + periodId : "");
    }

    // ---- Sinh viên xem ----

    @GetMapping("/results/me")
    @PreAuthorize("hasAuthority('RESULT_VIEW')")
    public String myResults(@AuthenticationPrincipal blog.hethong_quanlydetai.security.AppUserPrincipal principal,
                            Model model) {
        model.addAttribute("pageTitle", "Kết quả của tôi");
        model.addAttribute("activeMenu", "results-me");
        Student st = studentRepository.findByUserId(principal.getUserId()).orElse(null);
        if (st == null) {
            model.addAttribute("results", List.of());
            return "results/me";
        }
        java.util.List<ReviewResult> myResults = new java.util.ArrayList<>();
        for (var p : periodRepository.findAll()) {
            var groups = groupRepository.findByMemberStudentIdAndPeriodId(st.getId(), p.getId());
            for (var g : groups) {
                resultService.findByGroup(g.getId()).ifPresent(r -> {
                    if (r.getPublished()) myResults.add(r);
                });
            }
        }
        model.addAttribute("results", myResults);
        return "results/me";
    }
}
