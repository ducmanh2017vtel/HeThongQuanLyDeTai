package blog.hethong_quanlydetai.service;

import blog.hethong_quanlydetai.audit.AuditLogger;
import blog.hethong_quanlydetai.domain.*;
import blog.hethong_quanlydetai.dto.EvaluationForm;
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
 * Nghiệp vụ Phân công chấm & Nhập điểm (GĐ8 — mục 5.7, A4).
 *
 * Quy tắc:
 * - ASSIGN (CTHD/Chủ tịch HĐ): tạo assignment (council, topic, lecturer).
 * - EVALUATE (GV được phân công): nhập điểm 0–10 (HALF_UP 2 chữ số), comment ≥ 10 ký tự.
 * - 1 giảng viên chỉ chấm 1 lần / 1 đề tài trong 1 hội đồng.
 * - Chỉ GV trong assignment mới được nhập điểm.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EvaluationService {

    private final ReviewAssignmentRepository assignmentRepository;
    private final EvaluationRepository evaluationRepository;
    private final CouncilRepository councilRepository;
    private final CouncilTopicRepository councilTopicRepository;
    private final LecturerRepository lecturerRepository;
    private final UserRepository userRepository;
    private final AuditLogger auditLogger;

    @Value("${app.score-decimals:2}")
    private int scoreDecimals;

    // ------------------------------------------------------------------
    // Phân công
    // ------------------------------------------------------------------

    @Transactional
    public ReviewAssignment assign(Long councilId, Long topicId, Long lecturerId, User actor) {
        Council c = councilRepository.findById(councilId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hội đồng #" + councilId));
        if (c.getStatus() != CouncilStatus.READY && c.getStatus() != CouncilStatus.DRAFT) {
            throw new IllegalArgumentException("Hội đồng đã hoàn tất, không thể phân công thêm");
        }
        // Topic phải thuộc hội đồng
        boolean inCouncil = councilTopicRepository.findByCouncilId(councilId).stream()
                .anyMatch(ct -> ct.getTopic().getId().equals(topicId));
        if (!inCouncil) {
            throw new IllegalArgumentException("Đề tài chưa được gán vào hội đồng này");
        }
        if (assignmentRepository.existsByCouncilIdAndTopicIdAndLecturerId(councilId, topicId, lecturerId)) {
            throw new IllegalArgumentException("Giảng viên đã được phân công chấm đề tài này trong hội đồng");
        }
        Lecturer lec = lecturerRepository.findById(lecturerId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy giảng viên #" + lecturerId));

        ReviewAssignment a = ReviewAssignment.builder()
                .council(c)
                .topic(councilTopicRepository.findByCouncilId(councilId).stream()
                        .filter(ct -> ct.getTopic().getId().equals(topicId))
                        .findFirst().orElseThrow().getTopic())
                .lecturer(lec)
                .assignedBy(actor)
                .status("ASSIGNED")
                .build();
        ReviewAssignment saved = assignmentRepository.save(a);
        auditLogger.log("ASSIGNMENT_CREATE", "ReviewAssignment", saved.getId(),
                "Phân công GV " + lec.getUser().getFullName() + " chấm đề tài #" + topicId);
        return saved;
    }

    @Transactional
    public void removeAssignment(Long assignmentId, User actor) {
        ReviewAssignment a = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phân công #" + assignmentId));
        if (evaluationRepository.findByAssignmentId(assignmentId).isPresent()) {
            throw new IllegalArgumentException("Đã có điểm, không thể xoá phân công");
        }
        assignmentRepository.delete(a);
        auditLogger.log("ASSIGNMENT_DELETE", "ReviewAssignment", assignmentId, "Xoá phân công");
    }

    @Transactional(readOnly = true)
    public List<ReviewAssignment> findByCouncil(Long councilId) {
        return assignmentRepository.findByCouncilId(councilId);
    }

    @Transactional(readOnly = true)
    public List<ReviewAssignment> findByLecturer(Long lecturerId) {
        return assignmentRepository.findDetailedByLecturerId(lecturerId);
    }

    // ------------------------------------------------------------------
    // Nhập điểm
    // ------------------------------------------------------------------

    @Transactional
    public Evaluation submitEvaluation(Long assignmentId, EvaluationForm form, User actor) {
        ReviewAssignment a = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phân công #" + assignmentId));
        // Phải là GV được phân công
        Optional<Lecturer> lecOpt = lecturerRepository.findByUserId(actor.getId());
        if (lecOpt.isEmpty() || !a.getLecturer().getId().equals(lecOpt.get().getId())) {
            throw new IllegalArgumentException("Bạn không được phân công chấm đề tài này");
        }
        if (a.getStatus().equals("COMPLETED")) {
            throw new IllegalArgumentException("Điểm đã được nộp, không thể sửa");
        }
        // Validate nhận xét
        if (form.getComment() == null || form.getComment().trim().length() < 10) {
            throw new IllegalArgumentException("Nhận xét phải có ít nhất 10 ký tự");
        }

        BigDecimal score = form.getScore().setScale(scoreDecimals, RoundingMode.HALF_UP);
        if (score.compareTo(BigDecimal.ZERO) < 0 || score.compareTo(BigDecimal.TEN) > 0) {
            throw new IllegalArgumentException("Điểm phải nằm trong khoảng 0–10");
        }

        Optional<Evaluation> existing = evaluationRepository.findByAssignmentId(assignmentId);
        Evaluation e;
        if (existing.isPresent()) {
            e = existing.get();
            e.setScore(score);
            e.setComment(form.getComment().trim());
            e.setStatus("SUBMITTED");
        } else {
            e = Evaluation.builder()
                    .assignment(a)
                    .score(score)
                    .comment(form.getComment().trim())
                    .status("SUBMITTED")
                    .build();
        }
        Evaluation saved = evaluationRepository.save(e);

        a.setStatus("COMPLETED");
        assignmentRepository.save(a);

        auditLogger.log("EVALUATION_SUBMIT", "Evaluation", saved.getId(),
                "GV " + actor.getFullName() + " chấm đề tài " + a.getTopic().getTopicName() + ": " + score);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Evaluation> findByTopic(Long topicId) {
        return evaluationRepository.findByTopicId(topicId);
    }

    @Transactional(readOnly = true)
    public List<Evaluation> findEvaluationsByLecturer(Long lecturerId) {
        return evaluationRepository.findByLecturerId(lecturerId);
    }

    @Transactional(readOnly = true)
    public BigDecimal averageScoreForTopic(Long topicId) {
        List<Evaluation> evals = evaluationRepository.findByTopicId(topicId);
        if (evals.isEmpty()) return null;
        BigDecimal sum = BigDecimal.ZERO;
        for (Evaluation e : evals) {
            sum = sum.add(e.getScore());
        }
        return sum.divide(BigDecimal.valueOf(evals.size()), scoreDecimals, RoundingMode.HALF_UP);
    }
}
