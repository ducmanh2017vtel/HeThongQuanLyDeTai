package blog.hethong_quanlydetai.controller;

import blog.hethong_quanlydetai.domain.Report;
import blog.hethong_quanlydetai.domain.StudentGroup;
import blog.hethong_quanlydetai.domain.User;
import blog.hethong_quanlydetai.dto.UploadReportForm;
import blog.hethong_quanlydetai.repository.StudentGroupRepository;
import blog.hethong_quanlydetai.repository.StudentRepository;
import blog.hethong_quanlydetai.service.ReportService;
import blog.hethong_quanlydetai.service.StudentGroupService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Báo cáo (GĐ6).
 * - SUBMISSION_CREATE: SV (nhóm trưởng) nộp.
 * - REPORT_VIEW: xem + tải.
 */
@Controller
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
    private final StudentGroupService groupService;
    private final StudentGroupRepository groupRepository;
    private final StudentRepository studentRepository;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('SUBMISSION_CREATE','REPORT_VIEW')")
    public String index(@AuthenticationPrincipal blog.hethong_quanlydetai.security.AppUserPrincipal principal,
                        Model model) {
        Set<String> permissions = principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
        model.addAttribute("pageTitle", "Báo cáo");
        model.addAttribute("activeMenu", "reports");
        model.addAttribute("permissions", permissions);
        return "reports/index";
    }

    @GetMapping("/group/{groupId}")
    @PreAuthorize("hasAnyAuthority('SUBMISSION_CREATE','REPORT_VIEW')")
    public String listByGroup(@PathVariable Long groupId,
                              @RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "10") int size,
                              Model model) {
        StudentGroup g = groupService.getById(groupId);
        model.addAttribute("pageTitle", "Báo cáo — Nhóm " + g.getGroupCode());
        model.addAttribute("activeMenu", "reports");
        model.addAttribute("group", g);
        model.addAttribute("reports",
                reportService.findByGroupPaged(groupId, PageRequest.of(page, size)));
        return "reports/list";
    }

    @GetMapping("/upload/{groupId}")
    @PreAuthorize("hasAuthority('SUBMISSION_CREATE')")
    public String uploadForm(@PathVariable Long groupId, Model model) {
        StudentGroup g = groupService.getById(groupId);
        model.addAttribute("pageTitle", "Nộp báo cáo");
        model.addAttribute("activeMenu", "reports");
        model.addAttribute("group", g);
        model.addAttribute("form", new UploadReportForm());
        return "reports/upload";
    }

    @PostMapping("/upload/{groupId}")
    @PreAuthorize("hasAuthority('SUBMISSION_CREATE')")
    public String upload(@PathVariable Long groupId,
                         @Valid @ModelAttribute("form") UploadReportForm form,
                         BindingResult br,
                         @RequestParam("file") MultipartFile file,
                         @AuthenticationPrincipal blog.hethong_quanlydetai.security.AppUserPrincipal principal,
                         RedirectAttributes ra) {
        if (br.hasErrors()) {
            ra.addFlashAttribute("flashError", "Vui lòng kiểm tra lại các trường");
            return "redirect:/reports/upload/" + groupId;
        }
        try {
            Report r = reportService.submit(groupId, form, file, currentUser(principal));
            ra.addFlashAttribute("flashSuccess", "Đã nộp báo cáo v" + r.getVersion());
            return "redirect:/reports/group/" + groupId;
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("flashError", ex.getMessage());
            return "redirect:/reports/upload/" + groupId;
        }
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("hasAuthority('REPORT_VIEW')")
    public void download(@PathVariable Long id,
                         @AuthenticationPrincipal blog.hethong_quanlydetai.security.AppUserPrincipal principal,
                         HttpServletResponse response) throws IOException {
        Report r = reportService.getById(id);
        Set<String> permissions = principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).collect(Collectors.toSet());
        if (!reportService.canView(r, currentUser(principal), permissions)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        Path file = Paths.get(r.getStoredPath());
        if (!Files.exists(file)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "File không còn tồn tại trên server");
            return;
        }
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"" + r.getFileName().replace("\"", "") + "\"");
        response.setContentLengthLong(r.getFileSize());
        Files.copy(file, response.getOutputStream());
        response.getOutputStream().flush();
    }

    private User currentUser(blog.hethong_quanlydetai.security.AppUserPrincipal p) {
        User u = new User();
        u.setId(p.getUserId());
        return u;
    }
}
