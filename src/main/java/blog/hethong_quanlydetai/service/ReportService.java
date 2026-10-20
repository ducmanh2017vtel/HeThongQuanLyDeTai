package blog.hethong_quanlydetai.service;

import blog.hethong_quanlydetai.audit.AuditLogger;
import blog.hethong_quanlydetai.domain.*;
import blog.hethong_quanlydetai.dto.UploadReportForm;
import blog.hethong_quanlydetai.repository.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Nghiệp vụ nộp báo cáo (GĐ6 — mục 5.5, A7).
 *
 * Quy tắc:
 * - Chỉ nhóm trưởng mới được nộp.
 * - Nhóm phải ở trạng thái APPROVED (đã có GVHD chấp thuận).
 * - File: PDF/DOC/DOCX, ≤ maxFileSize (mặc định 20MB).
 * - Version tăng dần trong cùng nhóm.
 * - Tất cả bản cũ trong cùng nhóm sẽ chuyển sang SUPERSEDED.
 * - Lưu file ra thư mục app.storage.dir (ngoài static/).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private static final Set<String> ALLOWED_EXT = Set.of("pdf", "doc", "docx");
    private static final Set<String> ALLOWED_MIME = Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document");

    private final ReportRepository reportRepository;
    private final StudentGroupRepository groupRepository;
    private final StudentRepository studentRepository;
    private final LecturerRepository lecturerRepository;
    private final UserRepository userRepository;
    private final AuditLogger auditLogger;

    @Value("${app.storage.dir}")
    private String storageDir;

    @Value("${spring.servlet.multipart.max-file-size:20MB}")
    private String maxFileSizeRaw;

    private long maxFileSizeBytes = 20L * 1024 * 1024;

    @PostConstruct
    void init() {
        try {
            Files.createDirectories(Paths.get(storageDir));
            log.info("[report] storage dir = {}", storageDir);
        } catch (IOException e) {
            log.warn("[report] cannot create storage dir: {}", e.getMessage());
        }
        try {
            String s = maxFileSizeRaw.trim().toUpperCase();
            long mult = 1;
            if (s.endsWith("MB")) { mult = 1024L * 1024; s = s.substring(0, s.length() - 2); }
            else if (s.endsWith("KB")) { mult = 1024L; s = s.substring(0, s.length() - 2); }
            else if (s.endsWith("B")) { s = s.substring(0, s.length() - 1); }
            maxFileSizeBytes = Long.parseLong(s.trim()) * mult;
        } catch (Exception e) {
            log.warn("[report] cannot parse max-file-size '{}', dùng mặc định 20MB", maxFileSizeRaw);
        }
    }

    // ------------------------------------------------------------------
    // Query
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Report getById(Long id) {
        return reportRepository.findWithDetailsById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy báo cáo #" + id));
    }

    @Transactional(readOnly = true)
    public List<Report> findByGroup(Long groupId) {
        return reportRepository.findActiveByGroupId(groupId);
    }

    @Transactional(readOnly = true)
    public Page<Report> findByGroupPaged(Long groupId, Pageable pageable) {
        return reportRepository.findByGroupIdOrderByVersionDesc(groupId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Report> findByTopic(Long topicId, Pageable pageable) {
        return reportRepository.findByTopicId(topicId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Report> search(String q, Pageable pageable) {
        return reportRepository.search(emptyToNull(q), pageable);
    }

    // ------------------------------------------------------------------
    // Submit
    // ------------------------------------------------------------------

    /**
     * Nộp báo cáo mới. Lưu file vật lý, tăng version, đánh dấu version cũ SUPERSEDED.
     */
    @Transactional
    public Report submit(Long groupId, UploadReportForm form, MultipartFile file, User actor) {
        // 1. Validate nhóm
        StudentGroup group = groupRepository.findWithDetailsById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhóm #" + groupId));

        // 2. Chỉ nhóm trưởng
        Student actorStudent = studentRepository.findByUserId(actor.getId())
                .orElseThrow(() -> new IllegalArgumentException("Tài khoản không phải sinh viên"));
        boolean isLeader = group.getMembers() != null
                && group.getMembers().stream().anyMatch(m ->
                    m.getStudent().getId().equals(actorStudent.getId())
                    && Boolean.TRUE.equals(m.getIsLeader()));
        if (!isLeader) {
            throw new IllegalArgumentException("Chỉ nhóm trưởng mới được nộp báo cáo");
        }

        // 3. Nhóm phải APPROVED hoặc SUBMITTED (cho phép nộp lại version mới)
        if (group.getStatus() != GroupStatus.APPROVED && group.getStatus() != GroupStatus.SUBMITTED) {
            throw new IllegalArgumentException("Nhóm chưa được GVHD chấp thuận đề tài, không thể nộp báo cáo");
        }

        // 4. Validate file
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn file báo cáo");
        }
        if (file.getSize() > maxFileSizeBytes) {
            throw new IllegalArgumentException("File vượt quá kích thước tối đa "
                    + (maxFileSizeBytes / 1024 / 1024) + "MB");
        }
        String original = file.getOriginalFilename();
        if (original == null || original.isBlank()) {
            throw new IllegalArgumentException("Tên file không hợp lệ");
        }
        String ext = "";
        int dot = original.lastIndexOf('.');
        if (dot >= 0) ext = original.substring(dot + 1).toLowerCase();
        if (!ALLOWED_EXT.contains(ext)) {
            throw new IllegalArgumentException("Chỉ chấp nhận file .pdf, .doc, .docx");
        }
        String contentType = file.getContentType();
        if (contentType != null && !contentType.isBlank()
                && !ALLOWED_MIME.contains(contentType)
                && !contentType.startsWith("application/octet-stream")) {
            log.info("[report] content-type={} (không chuẩn nhưng cho phép theo ext)", contentType);
        }

        // 5. Đánh version
        int newVersion = reportRepository.findMaxVersionByGroupId(groupId) + 1;

        // 6. Lưu file
        String storedName = "report_g" + groupId + "_v" + newVersion + "_"
                + UUID.randomUUID().toString().replace("-", "").substring(0, 8)
                + "." + ext;
        Path target = Paths.get(storageDir, storedName);
        try {
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalArgumentException("Không lưu được file: " + e.getMessage());
        }

        // 7. Cập nhật bản cũ: SUBMITTED -> SUPERSEDED
        List<Report> olds = reportRepository.findActiveByGroupId(groupId);
        for (Report r : olds) {
            r.setStatus("SUPERSEDED");
            reportRepository.save(r);
        }

        // 8. Lưu bản ghi
        User realUser = userRepository.findById(actor.getId()).orElse(actor);
        Report report = Report.builder()
                .group(group)
                .submittedBy(realUser)
                .reportTitle(form.getReportTitle().trim())
                .fileName(original)
                .storedPath(target.toString())
                .fileSize(file.getSize())
                .version(newVersion)
                .description(form.getDescription() == null ? null : form.getDescription().trim())
                .status("SUBMITTED")
                .build();
        Report saved = reportRepository.save(report);

        // 9. Chuyển nhóm sang SUBMITTED nếu là lần đầu nộp
        if (group.getStatus() == GroupStatus.APPROVED) {
            group.setStatus(GroupStatus.SUBMITTED);
            groupRepository.save(group);
        }

        auditLogger.log("REPORT_SUBMIT", "Report", saved.getId(),
                "Nhóm " + group.getGroupCode() + " nộp báo cáo v" + newVersion + ": " + original);
        return saved;
    }

    // ------------------------------------------------------------------
    // Permission helper
    // ------------------------------------------------------------------

    /**
     * Kiểm tra user có quyền xem báo cáo của nhóm này không.
     */
    @Transactional(readOnly = true)
    public boolean canView(Report report, User actor, Set<String> permissions) {
        if (permissions.contains("REPORT_VIEW") || permissions.contains("USER_MANAGE")) {
            return true;
        }
        StudentGroup g = report.getGroup();
        if (g == null) return false;
        // GVHD của đề tài
        if (g.getTopic() != null && g.getTopic().getSupervisors() != null) {
            Optional<Lecturer> lecOpt = lecturerRepository.findByUserId(actor.getId());
            if (lecOpt.isPresent()) {
                Long lecId = lecOpt.get().getId();
                boolean isSupervisor = g.getTopic().getSupervisors().stream()
                        .anyMatch(ts -> ts.getLecturer().getId().equals(lecId));
                if (isSupervisor) return true;
            }
        }
        // SV là thành viên nhóm
        Optional<Student> stOpt = studentRepository.findByUserId(actor.getId());
        if (stOpt.isPresent() && g.getMembers() != null) {
            Long sid = stOpt.get().getId();
            return g.getMembers().stream().anyMatch(m -> m.getStudent().getId().equals(sid));
        }
        return false;
    }

    public long getMaxFileSizeBytes() { return maxFileSizeBytes; }

    private static String emptyToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
