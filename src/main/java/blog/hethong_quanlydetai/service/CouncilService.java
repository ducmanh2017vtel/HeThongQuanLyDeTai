package blog.hethong_quanlydetai.service;

import blog.hethong_quanlydetai.audit.AuditLogger;
import blog.hethong_quanlydetai.domain.*;
import blog.hethong_quanlydetai.dto.CouncilForm;
import blog.hethong_quanlydetai.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Nghiệp vụ Hội đồng phản biện (GĐ7 — mục 5.6, A6).
 *
 * Quy tắc:
 * - Trưởng khoa (COUNCIL_MANAGE) tạo/sửa/xoá.
 * - Trạng thái: DRAFT → READY → FINISHED.
 * - Thành viên: 1 CHAIRMAN + 1 SECRETARY + 1..3 MEMBER (tổng 3..5 người, mỗi người 1 vai trò).
 * - Không trùng giảng viên trong hội đồng.
 * - Cùng bộ môn: tuỳ chọn, không bắt buộc (A5).
 * - Mỗi đề tài chỉ gán vào 1 hội đồng.
 * - reportDate bắt buộc với KLTN.
 * - Chỉ DRAFT mới sửa/xoá được thành viên và đề tài.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CouncilService {

    private final CouncilRepository councilRepository;
    private final CouncilMemberRepository memberRepository;
    private final CouncilTopicRepository topicRepository;
    private final RegistrationPeriodRepository periodRepository;
    private final LecturerRepository lecturerRepository;
    private final TopicRepository topicRepositoryForTopic;
    private final AuditLogger auditLogger;

    // ------------------------------------------------------------------
    // Query
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Page<Council> search(String q, Long periodId, CouncilStatus status, Pageable pageable) {
        return councilRepository.search(emptyToNull(q), periodId, status, pageable);
    }

    @Transactional(readOnly = true)
    public Council getById(Long id) {
        return councilRepository.findWithDetailsById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hội đồng #" + id));
    }

    @Transactional(readOnly = true)
    public List<Council> findByPeriod(Long periodId) {
        return councilRepository.findByPeriodId(periodId);
    }

    // ------------------------------------------------------------------
    // Create
    // ------------------------------------------------------------------

    @Transactional
    public Council create(CouncilForm form, User creator) {
        validateForm(form);
        RegistrationPeriod period = periodRepository.findById(form.getPeriodId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đợt #" + form.getPeriodId()));
        if (councilRepository.existsByCouncilCode(form.getCouncilCode().trim())) {
            throw new IllegalArgumentException("Mã hội đồng đã tồn tại");
        }
        if (period.getPeriodType() == PeriodType.KLTN && form.getReportDate() == null) {
            throw new IllegalArgumentException("Đợt KLTN phải có ngày báo cáo hội đồng");
        }

        Council c = Council.builder()
                .councilCode(form.getCouncilCode().trim())
                .councilName(form.getCouncilName().trim())
                .period(period)
                .reportDate(form.getReportDate())
                .createdBy(creator)
                .status(CouncilStatus.DRAFT)
                .build();
        Council saved = councilRepository.save(c);

        // Thành viên
        saveMembers(saved, form);

        // Đề tài
        saveTopics(saved, form);

        auditLogger.log("COUNCIL_CREATE", "Council", saved.getId(),
                "Tạo hội đồng: " + saved.getCouncilName());
        return getById(saved.getId());
    }

    // ------------------------------------------------------------------
    // Update
    // ------------------------------------------------------------------

    @Transactional
    public Council update(Long id, CouncilForm form, User actor) {
        validateForm(form);
        Council c = getById(id);
        if (c.getStatus() != CouncilStatus.DRAFT) {
            throw new IllegalArgumentException("Chỉ sửa được hội đồng ở trạng thái NHÁP");
        }
        if (!c.getCouncilCode().equalsIgnoreCase(form.getCouncilCode().trim())
                && councilRepository.existsByCouncilCode(form.getCouncilCode().trim())) {
            throw new IllegalArgumentException("Mã hội đồng đã tồn tại");
        }
        c.setCouncilCode(form.getCouncilCode().trim());
        c.setCouncilName(form.getCouncilName().trim());
        c.setReportDate(form.getReportDate());

        // Xoá thành viên cũ, thêm mới
        memberRepository.deleteByCouncilId(id);
        c.getMembers().clear();
        saveMembers(c, form);

        // Xoá topic cũ, thêm mới
        topicRepository.deleteByCouncilId(id);
        saveTopics(c, form);

        Council saved = councilRepository.save(c);
        auditLogger.log("COUNCIL_UPDATE", "Council", saved.getId(),
                "Cập nhật hội đồng: " + saved.getCouncilName());
        return getById(saved.getId());
    }

    @Transactional
    public Council markReady(Long id, User actor) {
        Council c = getById(id);
        if (c.getStatus() != CouncilStatus.DRAFT) {
            throw new IllegalArgumentException("Chỉ chuyển trạng thái từ NHÁP");
        }
        if (c.getMembers() == null || c.getMembers().size() < 3) {
            throw new IllegalArgumentException("Hội đồng cần ít nhất 3 thành viên (CTHD, thư ký, ủy viên)");
        }
        if (topicRepository.findByCouncilId(id).isEmpty()) {
            throw new IllegalArgumentException("Hội đồng cần có ít nhất 1 đề tài");
        }
        c.setStatus(CouncilStatus.READY);
        Council saved = councilRepository.save(c);
        auditLogger.log("COUNCIL_READY", "Council", saved.getId(),
                "Chuyển hội đồng sang SẴN SÀNG");
        return saved;
    }

    @Transactional
    public Council markFinished(Long id, User actor) {
        Council c = getById(id);
        if (c.getStatus() != CouncilStatus.READY) {
            throw new IllegalArgumentException("Chỉ hoàn tất hội đồng đang ở trạng thái SẴN SÀNG");
        }
        c.setStatus(CouncilStatus.FINISHED);
        Council saved = councilRepository.save(c);
        auditLogger.log("COUNCIL_FINISHED", "Council", saved.getId(),
                "Hoàn tất hội đồng");
        return saved;
    }

    @Transactional
    public void delete(Long id, User actor) {
        Council c = getById(id);
        if (c.getStatus() != CouncilStatus.DRAFT) {
            throw new IllegalArgumentException("Chỉ xoá được hội đồng ở trạng thái NHÁP");
        }
        memberRepository.deleteByCouncilId(id);
        topicRepository.deleteByCouncilId(id);
        Long deletedId = c.getId();
        String name = c.getCouncilName();
        councilRepository.delete(c);
        auditLogger.log("COUNCIL_DELETE", "Council", deletedId, "Xoá hội đồng: " + name);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private void validateForm(CouncilForm form) {
        if (form.getLecturerIds() == null || form.getRoles() == null
                || form.getLecturerIds().size() != form.getRoles().size()) {
            throw new IllegalArgumentException("Danh sách thành viên và vai trò phải khớp nhau");
        }
        Set<Long> uniq = new HashSet<>(form.getLecturerIds());
        if (uniq.size() != form.getLecturerIds().size()) {
            throw new IllegalArgumentException("Không được trùng giảng viên trong hội đồng");
        }
        int total = form.getLecturerIds().size();
        if (total < 3 || total > 5) {
            throw new IllegalArgumentException("Hội đồng phải có 3..5 thành viên (hiện " + total + ")");
        }
        long chair = form.getRoles().stream().filter(r -> "CHAIRMAN".equals(r)).count();
        long sec = form.getRoles().stream().filter(r -> "SECRETARY".equals(r)).count();
        if (chair != 1) throw new IllegalArgumentException("Phải có đúng 1 Chủ tịch hội đồng");
        if (sec != 1) throw new IllegalArgumentException("Phải có đúng 1 Thư ký");
    }

    private void saveMembers(Council c, CouncilForm form) {
        for (int i = 0; i < form.getLecturerIds().size(); i++) {
            Long lecId = form.getLecturerIds().get(i);
            String role = form.getRoles().get(i);
            Lecturer lec = lecturerRepository.findById(lecId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy giảng viên #" + lecId));
            CouncilMember m = CouncilMember.builder()
                    .council(c)
                    .lecturer(lec)
                    .role(CouncilMemberRole.valueOf(role))
                    .build();
            CouncilMember savedM = memberRepository.save(m);
            c.getMembers().add(savedM);
        }
    }

    private void saveTopics(Council c, CouncilForm form) {
        if (form.getTopicIds() == null) return;
        for (Long topicId : form.getTopicIds()) {
            if (councilRepository.isTopicAlreadyAssigned(topicId)) {
                throw new IllegalArgumentException("Đề tài #" + topicId + " đã được gán cho hội đồng khác");
            }
            Topic t = topicRepositoryForTopic.findById(topicId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đề tài #" + topicId));
            if (!t.getPeriod().getId().equals(c.getPeriod().getId())) {
                throw new IllegalArgumentException("Đề tài phải cùng đợt với hội đồng");
            }
            if (t.getStatus() != TopicStatus.APPROVED) {
                throw new IllegalArgumentException("Đề tài phải ở trạng thái ĐÃ DUYỆT");
            }
            CouncilTopic ct = CouncilTopic.builder().council(c).topic(t).build();
            topicRepository.save(ct);
        }
    }

    private static String emptyToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
