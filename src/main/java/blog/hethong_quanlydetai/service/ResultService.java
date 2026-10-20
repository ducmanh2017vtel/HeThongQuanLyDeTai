package blog.hethong_quanlydetai.service;

import blog.hethong_quanlydetai.audit.AuditLogger;
import blog.hethong_quanlydetai.domain.*;
import blog.hethong_quanlydetai.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

/**
 * Nghiệp vụ Tổng hợp & Công bố kết quả (GĐ9 — mục 5.8, A4, A2).
 *
 * Quy tắc:
 * - Chỉ tổng hợp khi: HĐ FINISHED, có đầy đủ điểm từ các GV được phân công.
 * - Điểm tổng hợp = trung bình cộng điểm từ ReviewAssignment → Evaluation.
 * - ĐẠT nếu finalScore >= passing-score (mặc định 5.00, cấu hình qua app.passing-score).
 * - Công bố: chỉ Trưởng khoa, set published=true, published_by, published_at.
 * - Sau khi công bố: SV có RESULT_VIEW mới xem được điểm.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ResultService {

    private final ReviewResultRepository resultRepository;
    private final ReviewAssignmentRepository assignmentRepository;
    private final EvaluationRepository evaluationRepository;
    private final StudentGroupRepository groupRepository;
    private final CouncilRepository councilRepository;
    private final CouncilTopicRepository councilTopicRepository;
    private final UserRepository userRepository;
    private final AuditLogger auditLogger;

    @Value("${app.passing-score:5.00}")
    private BigDecimal passingScore;

    @Value("${app.score-decimals:2}")
    private int scoreDecimals;

    // ------------------------------------------------------------------
    // Calculate
    // ------------------------------------------------------------------

    @Transactional
    public ReviewResult calculate(Long topicId, User actor) {
        Topic topic = councilTopicRepository.findByTopicId(topicId)
                .map(CouncilTopic::getTopic)
                .orElseThrow(() -> new IllegalArgumentException("Đề tài chưa được gán cho hội đồng"));
        Council council = councilRepository.findById(
                councilTopicRepository.findByTopicId(topicId).orElseThrow().getCouncil().getId()
        ).orElseThrow();

        if (council.getStatus() != CouncilStatus.FINISHED) {
            throw new IllegalArgumentException("Hội đồng chưa hoàn tất, không thể tổng hợp");
        }
        // Tất cả phân công phải có điểm
        List<ReviewAssignment> assignments = assignmentRepository.findByTopicId(topicId);
        if (assignments.isEmpty()) {
            throw new IllegalArgumentException("Đề tài chưa được phân công chấm");
        }
        for (ReviewAssignment a : assignments) {
            if (!"COMPLETED".equals(a.getStatus())) {
                throw new IllegalArgumentException("Còn phân công chưa chấm xong (GV " + a.getLecturer().getUser().getFullName() + ")");
            }
        }
        // Tính trung bình
        List<Evaluation> evals = evaluationRepository.findByTopicId(topicId);
        BigDecimal sum = BigDecimal.ZERO;
        for (Evaluation e : evals) {
            sum = sum.add(e.getScore());
        }
        BigDecimal avg = sum.divide(BigDecimal.valueOf(evals.size()), scoreDecimals, RoundingMode.HALF_UP);

        // Nhóm thực hiện
        Optional<StudentGroup> groupOpt = groupRepository.findAll().stream()
                .filter(g -> g.getTopic() != null && g.getTopic().getId().equals(topicId))
                .findFirst();
        if (groupOpt.isEmpty()) {
            throw new IllegalArgumentException("Đề tài chưa có nhóm thực hiện");
        }
        StudentGroup group = groupOpt.get();

        ReviewResult result = resultRepository.findByTopicId(topicId).orElseGet(() ->
                ReviewResult.builder()
                        .group(group)
                        .topic(topic)
                        .council(council)
                        .published(false)
                        .build());
        result.setFinalScore(avg);
        result.setResult(avg.compareTo(passingScore) >= 0 ? ReviewResultStatus.PASSED : ReviewResultStatus.FAILED);
        result.setCouncil(council);
        result.setCalculatedBy(actor);
        result.setCalculatedAt(java.time.LocalDateTime.now());
        ReviewResult saved = resultRepository.save(result);

        auditLogger.log("RESULT_CALCULATE", "ReviewResult", saved.getId(),
                "Tổng hợp điểm đề tài " + topic.getTopicName() + ": " + avg
                        + (saved.getResult() == ReviewResultStatus.PASSED ? " (ĐẠT)" : " (KHÔNG ĐẠT)"));
        return saved;
    }

    // ------------------------------------------------------------------
    // Publish
    // ------------------------------------------------------------------

    @Transactional
    public ReviewResult publish(Long resultId, User actor) {
        ReviewResult r = resultRepository.findById(resultId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy kết quả #" + resultId));
        if (r.getPublished()) {
            throw new IllegalArgumentException("Kết quả đã được công bố trước đó");
        }
        r.setPublished(true);
        r.setPublishedBy(actor);
        r.setPublishedAt(java.time.LocalDateTime.now());
        ReviewResult saved = resultRepository.save(r);

        // Cập nhật trạng thái nhóm → COMPLETED
        if (r.getGroup() != null) {
            StudentGroup g = r.getGroup();
            g.setStatus(GroupStatus.COMPLETED);
            groupRepository.save(g);
        }
        auditLogger.log("RESULT_PUBLISH", "ReviewResult", saved.getId(),
                "Công bố kết quả đề tài " + (r.getTopic() != null ? r.getTopic().getTopicName() : ""));
        return saved;
    }

    @Transactional
    public ReviewResult unpublish(Long resultId, User actor) {
        ReviewResult r = resultRepository.findById(resultId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy kết quả #" + resultId));
        if (!r.getPublished()) {
            throw new IllegalArgumentException("Kết quả chưa được công bố");
        }
        r.setPublished(false);
        r.setPublishedBy(null);
        r.setPublishedAt(null);
        return resultRepository.save(r);
    }

    @Transactional
    public ReviewResult updateSummary(Long resultId, String summary, User actor) {
        ReviewResult r = resultRepository.findById(resultId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy kết quả #" + resultId));
        r.setSummaryComment(summary);
        return resultRepository.save(r);
    }

    // ------------------------------------------------------------------
    // Query
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public ReviewResult getById(Long id) {
        return resultRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy kết quả #" + id));
    }

    @Transactional(readOnly = true)
    public List<ReviewResult> findByPeriod(Long periodId) {
        return resultRepository.findByPeriodId(periodId);
    }

    @Transactional(readOnly = true)
    public Optional<ReviewResult> findByGroup(Long groupId) {
        return resultRepository.findByGroupId(groupId);
    }

    @Transactional(readOnly = true)
    public List<ReviewResult> findPublished() {
        return resultRepository.findPublished();
    }
}
