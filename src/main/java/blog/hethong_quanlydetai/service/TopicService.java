package blog.hethong_quanlydetai.service;

import blog.hethong_quanlydetai.audit.AuditLogger;
import blog.hethong_quanlydetai.domain.*;
import blog.hethong_quanlydetai.dto.TopicForm;
import blog.hethong_quanlydetai.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Nghiệp vụ quản lý đề tài (mục 5.3).
 * - GV đề xuất trong khung thời gian GV của đợt PUBLISHED.
 * - Trưởng khoa duyệt/từ chối.
 * - Trưởng khoa công bố danh sách đề tài.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TopicService {

    private final TopicRepository topicRepository;
    private final TopicSupervisorRepository supervisorRepository;
    private final RegistrationPeriodRepository periodRepository;
    private final DepartmentRepository departmentRepository;
    private final LecturerRepository lecturerRepository;
    private final AuditLogger auditLogger;

    // ------------------------------------------------------------------
    // Query
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Page<Topic> search(String q, TopicStatus status, Long periodId,
                              Long departmentId, Boolean published, Pageable pageable) {
        return topicRepository.search(emptyToNull(q), status, periodId, departmentId, published, pageable);
    }

    @Transactional(readOnly = true)
    public Topic getById(Long id) {
        return topicRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đề tài #" + id));
    }

    @Transactional(readOnly = true)
    public Topic getWithDetails(Long id) {
        return topicRepository.findWithDetailsById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đề tài #" + id));
    }

    /** Tìm đề tài đã công bố theo đợt (cho SV đăng ký). */
    @Transactional(readOnly = true)
    public List<Topic> findPublishedByPeriod(Long periodId) {
        return topicRepository.findByPeriodIdAndPublishedTrue(periodId);
    }

    /** Đếm đề tài chờ duyệt (cho dashboard Trưởng khoa). */
    @Transactional(readOnly = true)
    public long countPending() {
        return topicRepository.countByStatus(TopicStatus.PENDING);
    }

    // ------------------------------------------------------------------
    // Create
    // ------------------------------------------------------------------

    /**
     * GV đề xuất đề tài mới.
     *
     * Quy tắc:
     * - GV phải đang trong khung thời gian GV của đợt PUBLISHED.
     * - Bộ môn của đề tài phải khớp với bộ môn của GV tạo.
     * - 1 ≤ số GVHD ≤ 2, không trùng.
     * - Mã đề tài duy nhất.
     * - Mô tả tối thiểu 30 ký tự.
     * - Tên đề tài tối thiểu 10 ký tự.
     */
    @Transactional
    public Topic create(TopicForm form, User creator) {
        // 1. Tìm lecturer profile của người tạo
        Lecturer creatorLecturer = lecturerRepository.findByUserId(creator.getId())
                .orElseThrow(() -> new IllegalArgumentException("Tài khoản không phải giảng viên"));

        // 2. Lấy đợt và kiểm tra khung thời gian GV
        RegistrationPeriod period = periodRepository.findById(form.getPeriodId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đợt đăng ký #" + form.getPeriodId()));
        LocalDateTime now = LocalDateTime.now();
        if (!isWithinLecturerWindow(period, now)) {
            throw new IllegalArgumentException("Hiện không trong khung thời gian giảng viên đăng ký đề tài của đợt này");
        }

        // 2.5. Kiểm tra độ dài mô tả và tên ở service (phòng khi bypass Bean Validation)
        if (form.getDescription() == null || form.getDescription().trim().length() < 30) {
            throw new IllegalArgumentException("Mô tả đề tài phải có ít nhất 30 ký tự");
        }
        if (form.getTopicName() == null || form.getTopicName().trim().length() < 10) {
            throw new IllegalArgumentException("Tên đề tài phải có ít nhất 10 ký tự");
        }

        // 3. Kiểm tra mã đề tài duy nhất
        if (topicRepository.existsByTopicCode(form.getTopicCode().trim())) {
            throw new IllegalArgumentException("Mã đề tài '" + form.getTopicCode() + "' đã tồn tại");
        }

        // 4. Kiểm tra bộ môn
        Department department = departmentRepository.findById(form.getDepartmentId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bộ môn #" + form.getDepartmentId()));
        if (!department.getId().equals(creatorLecturer.getDepartment().getId())) {
            throw new IllegalArgumentException("Bộ môn của đề tài phải trùng với bộ môn của giảng viên tạo");
        }

        // 5. Lấy danh sách GVHD
        List<Lecturer> supervisors = resolveSupervisors(form.getSupervisor1Id(), form.getSupervisor2Id());
        if (supervisors.isEmpty()) {
            throw new IllegalArgumentException("Đề tài phải có ít nhất 1 giảng viên hướng dẫn");
        }
        if (supervisors.size() > 2) {
            throw new IllegalArgumentException("Đề tài có tối đa 2 giảng viên hướng dẫn");
        }

        // 6. Tạo đề tài
        Topic topic = Topic.builder()
                .period(period)
                .department(department)
                .createdByLecturer(creatorLecturer)
                .topicCode(form.getTopicCode().trim())
                .topicName(form.getTopicName().trim())
                .description(form.getDescription().trim())
                .status(TopicStatus.PENDING)
                .published(false)
                .build();

        Topic saved = topicRepository.save(topic);

        // 7. Lưu GVHD
        for (int i = 0; i < supervisors.size(); i++) {
            TopicSupervisor ts = TopicSupervisor.builder()
                    .topic(saved)
                    .lecturer(supervisors.get(i))
                    .supervisorOrder(i + 1) // 1 = chính, 2 = phụ
                    .build();
            supervisorRepository.save(ts);
        }

        auditLogger.log("TOPIC_CREATE", "Topic", saved.getId(),
                "GV " + creator.getFullName() + " đề xuất đề tài: " + saved.getTopicName());

        return saved;
    }

    // ------------------------------------------------------------------
    // Update (GV sửa đề tài PENDING/REJECTED)
    // ------------------------------------------------------------------

    /**
     * GV sửa đề tài của mình (chỉ PENDING hoặc REJECTED).
     * Không sửa được nếu đề tài đã APPROVED và có đăng ký được chấp thuận.
     */
    @Transactional
    public Topic update(Long id, TopicForm form, User actor) {
        Topic topic = getById(id);

        // Kiểm tra quyền sở hữu
        Lecturer actorLecturer = lecturerRepository.findByUserId(actor.getId())
                .orElseThrow(() -> new IllegalArgumentException("Tài khoản không phải giảng viên"));
        if (!isOwner(topic, actorLecturer)) {
            throw new IllegalArgumentException("Bạn không có quyền sửa đề tài này");
        }

        // Kiểm tra trạng thái cho phép sửa
        if (topic.getStatus() == TopicStatus.APPROVED) {
            // Kiểm tra đã có đăng ký được chấp thuận chưa
            if (topicRepository.hasApprovedRegistration(id)) {
                throw new IllegalArgumentException("Đề tài đã được duyệt và có nhóm thực hiện, không thể sửa");
            }
        }

        // Kiểm tra mã đề tài duy nhất (nếu đổi mã)
        String newCode = form.getTopicCode().trim();
        if (!newCode.equalsIgnoreCase(topic.getTopicCode())
                && topicRepository.existsByTopicCode(newCode)) {
            throw new IllegalArgumentException("Mã đề tài '" + newCode + "' đã tồn tại");
        }

        // Lấy danh sách GVHD mới
        List<Lecturer> supervisors = resolveSupervisors(form.getSupervisor1Id(), form.getSupervisor2Id());
        if (supervisors.isEmpty()) {
            throw new IllegalArgumentException("Đề tài phải có ít nhất 1 giảng viên hướng dẫn");
        }
        if (supervisors.size() > 2) {
            throw new IllegalArgumentException("Đề tài có tối đa 2 giảng viên hướng dẫn");
        }

        // Cập nhật thông tin đề tài
        topic.setTopicCode(newCode);
        topic.setTopicName(form.getTopicName().trim());
        topic.setDescription(form.getDescription().trim());

        // Cập nhật GVHD: xóa cũ, tạo mới
        supervisorRepository.deleteByTopicId(id);
        for (int i = 0; i < supervisors.size(); i++) {
            TopicSupervisor ts = TopicSupervisor.builder()
                    .topic(topic)
                    .lecturer(supervisors.get(i))
                    .supervisorOrder(i + 1)
                    .build();
            supervisorRepository.save(ts);
        }

        // Nếu đang REJECTED, chuyển về PENDING để gửi duyệt lại
        if (topic.getStatus() == TopicStatus.REJECTED) {
            topic.setStatus(TopicStatus.PENDING);
            topic.setRejectReason(null);
        }

        Topic saved = topicRepository.save(topic);
        auditLogger.log("TOPIC_UPDATE", "Topic", saved.getId(),
                "GV " + actor.getFullName() + " cập nhật đề tài: " + saved.getTopicName());

        return saved;
    }

    // ------------------------------------------------------------------
    // Approval (Trưởng khoa duyệt/từ chối)
    // ------------------------------------------------------------------

    /**
     * Trưởng khoa duyệt đề tài: PENDING → APPROVED.
     */
    @Transactional
    public Topic approve(Long id, User approver) {
        Topic topic = getById(id);
        if (topic.getStatus() != TopicStatus.PENDING) {
            throw new IllegalArgumentException("Chỉ duyệt được đề tài đang ở trạng thái CHỜ DUYỆT");
        }
        topic.setStatus(TopicStatus.APPROVED);
        topic.setApprovedBy(approver);
        topic.setApprovedAt(LocalDateTime.now());
        Topic saved = topicRepository.save(topic);
        auditLogger.log("TOPIC_APPROVE", "Topic", saved.getId(),
                "Trưởng khoa duyệt đề tài: " + saved.getTopicName());
        return saved;
    }

    /**
     * Trưởng khoa từ chối đề tài: PENDING → REJECTED.
     * @param rejectReason Lý do từ chối (bắt buộc, tối thiểu 10 ký tự).
     */
    @Transactional
    public Topic reject(Long id, String rejectReason, User approver) {
        if (rejectReason == null || rejectReason.trim().length() < 10) {
            throw new IllegalArgumentException("Lý do từ chối phải có ít nhất 10 ký tự");
        }
        Topic topic = getById(id);
        if (topic.getStatus() != TopicStatus.PENDING) {
            throw new IllegalArgumentException("Chỉ từ chối được đề tài đang ở trạng thái CHỜ DUYỆT");
        }
        topic.setStatus(TopicStatus.REJECTED);
        topic.setRejectReason(rejectReason.trim());
        topic.setApprovedBy(approver);
        topic.setApprovedAt(LocalDateTime.now());
        Topic saved = topicRepository.save(topic);
        auditLogger.log("TOPIC_REJECT", "Topic", saved.getId(),
                "Trưởng khoa từ chối đề tài: " + saved.getTopicName() + ". Lý do: " + rejectReason);
        return saved;
    }

    // ------------------------------------------------------------------
    // Publish (Công bố đề tài)
    // ------------------------------------------------------------------

    /**
     * Công bố đề tài: APPROVED → published = true.
     * Đề tài APPROVED và published=true mới hiển thị cho SV đăng ký.
     */
    @Transactional
    public Topic publish(Long id, User actor) {
        Topic topic = getById(id);
        if (topic.getStatus() != TopicStatus.APPROVED) {
            throw new IllegalArgumentException("Chỉ công bố được đề tài đã DUYỆT");
        }
        if (topic.getPublished()) {
            throw new IllegalArgumentException("Đề tài đã được công bố trước đó");
        }
        topic.setPublished(true);
        Topic saved = topicRepository.save(topic);
        auditLogger.log("TOPIC_PUBLISH", "Topic", saved.getId(),
                "Công bố đề tài: " + saved.getTopicName());
        return saved;
    }

    /**
     * Công bố tất cả đề tài APPROVED theo đợt.
     */
    @Transactional
    public int publishByPeriod(Long periodId, User actor) {
        List<Topic> approved = topicRepository.search(null, TopicStatus.APPROVED, periodId, null, false, Pageable.unpaged())
                .getContent();
        int count = 0;
        for (Topic topic : approved) {
            topic.setPublished(true);
            topicRepository.save(topic);
            count++;
        }
        if (count > 0) {
            auditLogger.log("TOPIC_PUBLISH_BATCH", "RegistrationPeriod", periodId,
                    "Công bố " + count + " đề tài của đợt #" + periodId);
        }
        return count;
    }

    // ------------------------------------------------------------------
    // Delete
    // ------------------------------------------------------------------

    /**
     * Xóa đề tài: chỉ PENDING và chưa có đăng ký.
     */
    @Transactional
    public void delete(Long id, User actor) {
        Topic topic = getById(id);
        if (topic.getStatus() != TopicStatus.PENDING) {
            throw new IllegalArgumentException("Chỉ xóa được đề tài đang ở trạng thái CHỜ DUYỆT");
        }
        if (topicRepository.hasApprovedRegistration(id)) {
            throw new IllegalArgumentException("Đề tài đã có nhóm thực hiện, không thể xóa");
        }
        supervisorRepository.deleteByTopicId(id);
        Long deletedId = topic.getId();
        String name = topic.getTopicName();
        topicRepository.delete(topic);
        auditLogger.log("TOPIC_DELETE", "Topic", deletedId, "Xóa đề tài: " + name);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /** Kiểm tra GV có phải người tạo hoặc GVHD của đề tài không. */
    @Transactional(readOnly = true)
    public boolean isOwnerOrSupervisor(Topic topic, Lecturer lecturer) {
        if (topic.getCreatedByLecturer().getId().equals(lecturer.getId())) {
            return true;
        }
        return supervisorRepository.existsByTopicIdAndLecturerId(topic.getId(), lecturer.getId());
    }

    /** Kiểm tra GV có phải người tạo đề tài không. */
    private boolean isOwner(Topic topic, Lecturer lecturer) {
        return topic.getCreatedByLecturer().getId().equals(lecturer.getId());
    }

    /** Lấy danh sách GVHD từ 1-2 ID, loại bỏ trùng, kiểm tra tồn tại và đang hoạt động. */
    private List<Lecturer> resolveSupervisors(Long supervisor1Id, Long supervisor2Id) {
        List<Lecturer> result = new ArrayList<>();

        if (supervisor1Id == null) {
            throw new IllegalArgumentException("Đề tài phải có giảng viên hướng dẫn chính");
        }

        Lecturer s1 = lecturerRepository.findById(supervisor1Id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy giảng viên hướng dẫn #" + supervisor1Id));
        if (!Boolean.TRUE.equals(s1.getUser().getEnabled()) || s1.getUser().getStatus() != UserStatus.ACTIVE) {
            throw new IllegalArgumentException("Giảng viên hướng dẫn chính không còn hoạt động");
        }
        result.add(s1);

        if (supervisor2Id != null) {
            if (supervisor2Id.equals(supervisor1Id)) {
                throw new IllegalArgumentException("Giảng viên hướng dẫn thứ hai không được trùng với GVHD chính");
            }
            Lecturer s2 = lecturerRepository.findById(supervisor2Id)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy giảng viên hướng dẫn #" + supervisor2Id));
            if (!Boolean.TRUE.equals(s2.getUser().getEnabled()) || s2.getUser().getStatus() != UserStatus.ACTIVE) {
                throw new IllegalArgumentException("Giảng viên hướng dẫn thứ hai không còn hoạt động");
            }
            result.add(s2);
        }

        return result;
    }

    /** Kiểm tra đợt có đang trong khung thời gian GV không. */
    @Transactional(readOnly = true)
    public boolean isWithinLecturerWindow(RegistrationPeriod period, LocalDateTime now) {
        return period.getStatus() == PeriodStatus.PUBLISHED
                && !now.isBefore(period.getLecturerStartAt())
                && !now.isAfter(period.getLecturerEndAt());
    }

    /** Kiểm tra đợt có đang trong khung thời gian SV không. */
    @Transactional(readOnly = true)
    public boolean isWithinStudentWindow(RegistrationPeriod period, LocalDateTime now) {
        return period.getStatus() == PeriodStatus.PUBLISHED
                && !now.isBefore(period.getStudentStartAt())
                && !now.isAfter(period.getStudentEndAt());
    }

    private static String emptyToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
