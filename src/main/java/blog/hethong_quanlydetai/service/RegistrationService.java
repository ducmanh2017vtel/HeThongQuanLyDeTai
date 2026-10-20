package blog.hethong_quanlydetai.service;

import blog.hethong_quanlydetai.audit.AuditLogger;
import blog.hethong_quanlydetai.domain.*;
import blog.hethong_quanlydetai.dto.RejectRegistrationForm;
import blog.hethong_quanlydetai.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Nghiệp vụ đăng ký đề tài của nhóm SV (GĐ5).
 *
 * Quy tắc:
 * - Chỉ nhóm trưởng được đăng ký / huỷ đề tài.
 * - Đề tài phải APPROVED + published=true và cùng đợt với nhóm.
 * - 1 đề tài ↔ 1 nhóm (mỗi topic chỉ gắn 1 group ở trạng thái PENDING/APPROVED).
 * - Trong khung thời gian SV của đợt.
 * - Sau khi GVHD duyệt (group APPROVED) → KHÔNG được huỷ/đổi đề tài.
 * - GVHD chính (supervisor_order=1) mới được duyệt / từ chối.
 * - Từ chối: bắt buộc lý do ≥ 10 ký tự; nhóm về DRAFT (xoá topic_id) để đăng ký lại.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RegistrationService {

    private final StudentGroupRepository groupRepository;
    private final TopicRegistrationRepository registrationRepository;
    private final TopicRepository topicRepository;
    private final TopicSupervisorRepository supervisorRepository;
    private final RegistrationPeriodRepository periodRepository;
    private final StudentRepository studentRepository;
    private final LecturerRepository lecturerRepository;
    private final TopicService topicService;
    private final AuditLogger auditLogger;

    // ------------------------------------------------------------------
    // Register / Cancel (SV)
    // ------------------------------------------------------------------

    /**
     * Nhóm trưởng đăng ký đề tài cho nhóm.
     */
    @Transactional
    public TopicRegistration register(Long groupId, Long topicId, User actor) {
        StudentGroup group = groupRepository.findWithDetailsById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhóm #" + groupId));

        // 1. Phải là nhóm trưởng
        Student actorStudent = studentRepository.findByUserId(actor.getId())
                .orElseThrow(() -> new IllegalArgumentException("Tài khoản không phải sinh viên"));
        final Long actorStudentId = actorStudent.getId();
        boolean isLeader = false;
        for (GroupMember gm : group.getMembers()) {
            if (gm.getStudent().getId().equals(actorStudentId) && Boolean.TRUE.equals(gm.getIsLeader())) {
                isLeader = true;
                break;
            }
        }
        if (!isLeader) {
            throw new IllegalArgumentException("Chỉ nhóm trưởng mới được đăng ký đề tài");
        }

        // 2. Nhóm phải DRAFT hoặc REGISTERED (cho phép đổi đề tài trước khi GVHD duyệt)
        if (group.getStatus() != GroupStatus.DRAFT && group.getStatus() != GroupStatus.REGISTERED) {
            throw new IllegalArgumentException("Nhóm đang ở trạng thái " + group.getStatus()
                    + ", không thể đăng ký. Trạng thái cho phép: DRAFT hoặc REGISTERED");
        }

        // 3. Khung thời gian SV
        RegistrationPeriod period = group.getPeriod();
        if (!topicService.isWithinStudentWindow(period, LocalDateTime.now())) {
            throw new IllegalArgumentException("Đã hết khung thời gian đăng ký của đợt");
        }

        // 4. Validate đề tài
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đề tài #" + topicId));
        if (topic.getStatus() != TopicStatus.APPROVED || !Boolean.TRUE.equals(topic.getPublished())) {
            throw new IllegalArgumentException("Đề tài chưa được công bố");
        }
        if (!topic.getPeriod().getId().equals(period.getId())) {
            throw new IllegalArgumentException("Đề tài không thuộc đợt đăng ký của nhóm");
        }

        // 5. 1 topic ↔ 1 nhóm: nếu đã có group PENDING/APPROVED gắn vào topic
        List<TopicRegistration> active = registrationRepository.findActiveByTopicId(topicId);
        if (!active.isEmpty()) {
            throw new IllegalArgumentException("Đề tài này đã có nhóm đăng ký");
        }

        // 6. Xoá đăng ký cũ của nhóm (nếu đổi đề tài trước khi duyệt)
        TopicRegistration old = registrationRepository.findByGroupId(groupId).orElse(null);
        if (old != null) {
            if (old.getStatus() == RegistrationStatus.APPROVED) {
                throw new IllegalArgumentException("Đề tài đã được duyệt, không thể đổi");
            }
            registrationRepository.delete(old);
        }

        // 7. Tạo đăng ký
        TopicRegistration reg = TopicRegistration.builder()
                .group(group)
                .topic(topic)
                .status(RegistrationStatus.PENDING)
                .build();
        TopicRegistration saved = registrationRepository.save(reg);

        // 8. Cập nhật nhóm: gắn topic, chuyển REGISTERED
        group.setTopic(topic);
        group.setStatus(GroupStatus.REGISTERED);
        groupRepository.save(group);

        auditLogger.log("REGISTER", "TopicRegistration", saved.getId(),
                "Nhóm " + group.getGroupCode() + " đăng ký đề tài " + topic.getTopicCode());

        return saved;
    }

    /**
     * Nhóm trưởng huỷ đăng ký (chỉ trước khi GVHD duyệt).
     */
    @Transactional
    public void cancel(Long groupId, User actor) {
        StudentGroup group = groupRepository.findWithDetailsById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhóm #" + groupId));
        if (group.getStatus() == GroupStatus.APPROVED) {
            throw new IllegalArgumentException("Đề tài đã được duyệt, không thể huỷ");
        }
        Student actorStudent = studentRepository.findByUserId(actor.getId())
                .orElseThrow(() -> new IllegalArgumentException("Tài khoản không phải sinh viên"));
        final Long actorStudentId = actorStudent.getId();
        boolean isLeader = false;
        for (GroupMember gm : group.getMembers()) {
            if (gm.getStudent().getId().equals(actorStudentId) && Boolean.TRUE.equals(gm.getIsLeader())) {
                isLeader = true;
                break;
            }
        }
        if (!isLeader) {
            throw new IllegalArgumentException("Chỉ nhóm trưởng mới được huỷ đăng ký");
        }
        TopicRegistration reg = registrationRepository.findByGroupId(groupId).orElse(null);
        if (reg != null) {
            registrationRepository.delete(reg);
        }
        group.setTopic(null);
        group.setStatus(GroupStatus.DRAFT);
        groupRepository.save(group);
        auditLogger.log("REGISTER_CANCEL", "StudentGroup", groupId,
                "Nhóm " + group.getGroupCode() + " huỷ đăng ký đề tài");
    }

    // ------------------------------------------------------------------
    // Approve / Reject (GVHD chính)
    // ------------------------------------------------------------------

    @Transactional
    public TopicRegistration approve(Long registrationId, User actor) {
        TopicRegistration reg = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đăng ký #" + registrationId));
        if (reg.getStatus() != RegistrationStatus.PENDING) {
            throw new IllegalArgumentException("Đăng ký không ở trạng thái CHỜ DUYỆT");
        }

        Topic topic = reg.getTopic();
        Lecturer actorLecturer = lecturerRepository.findByUserId(actor.getId())
                .orElseThrow(() -> new IllegalArgumentException("Tài khoản không phải giảng viên"));

        // Chỉ GVHD chính (supervisor_order = 1) mới duyệt được
        TopicSupervisor primary = supervisorRepository.findByTopicIdAndSupervisorOrder(topic.getId(), 1)
                .orElseThrow(() -> new IllegalArgumentException("Đề tài chưa có GVHD chính"));
        if (!primary.getLecturer().getId().equals(actorLecturer.getId())) {
            throw new IllegalArgumentException("Chỉ GVHD chính mới được duyệt đăng ký");
        }

        reg.setStatus(RegistrationStatus.APPROVED);
        reg.setApprovedBy(actor);
        reg.setApprovedAt(LocalDateTime.now());
        TopicRegistration saved = registrationRepository.save(reg);

        StudentGroup group = reg.getGroup();
        group.setStatus(GroupStatus.APPROVED);
        groupRepository.save(group);

        auditLogger.log("REGISTRATION_APPROVE", "TopicRegistration", saved.getId(),
                "GV " + actor.getFullName() + " duyệt đăng ký của nhóm " + group.getGroupCode() + " cho đề tài " + topic.getTopicCode());
        return saved;
    }

    @Transactional
    public TopicRegistration reject(Long registrationId, RejectRegistrationForm form, User actor) {
        if (form.getReason() == null || form.getReason().trim().length() < 10) {
            throw new IllegalArgumentException("Lý do từ chối phải có ít nhất 10 ký tự");
        }
        TopicRegistration reg = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đăng ký #" + registrationId));
        if (reg.getStatus() != RegistrationStatus.PENDING) {
            throw new IllegalArgumentException("Đăng ký không ở trạng thái CHỜ DUYỆT");
        }

        Topic topic = reg.getTopic();
        Lecturer actorLecturer = lecturerRepository.findByUserId(actor.getId())
                .orElseThrow(() -> new IllegalArgumentException("Tài khoản không phải giảng viên"));
        TopicSupervisor primary = supervisorRepository.findByTopicIdAndSupervisorOrder(topic.getId(), 1)
                .orElseThrow(() -> new IllegalArgumentException("Đề tài chưa có GVHD chính"));
        if (!primary.getLecturer().getId().equals(actorLecturer.getId())) {
            throw new IllegalArgumentException("Chỉ GVHD chính mới được từ chối đăng ký");
        }

        reg.setStatus(RegistrationStatus.REJECTED);
        reg.setRejectionReason(form.getReason().trim());
        reg.setApprovedBy(actor);
        reg.setApprovedAt(LocalDateTime.now());
        TopicRegistration saved = registrationRepository.save(reg);

        // Nhóm về DRAFT (xoá topic_id) để đăng ký lại
        StudentGroup group = reg.getGroup();
        group.setTopic(null);
        group.setStatus(GroupStatus.DRAFT);
        groupRepository.save(group);

        auditLogger.log("REGISTRATION_REJECT", "TopicRegistration", saved.getId(),
                "GV " + actor.getFullName() + " từ chối đăng ký của nhóm " + group.getGroupCode() + " cho đề tài " + topic.getTopicCode()
                        + ". Lý do: " + form.getReason());
        return saved;
    }

    // ------------------------------------------------------------------
    // Query helpers
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public TopicRegistration getById(Long id) {
        return registrationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đăng ký #" + id));
    }

    @Transactional(readOnly = true)
    public TopicRegistration findByGroupId(Long groupId) {
        return registrationRepository.findByGroupId(groupId).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<TopicRegistration> findByTopicId(Long topicId) {
        return registrationRepository.findByTopicId(topicId);
    }
}
