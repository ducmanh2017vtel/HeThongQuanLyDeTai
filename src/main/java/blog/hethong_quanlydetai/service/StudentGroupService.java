package blog.hethong_quanlydetai.service;

import blog.hethong_quanlydetai.audit.AuditLogger;
import blog.hethong_quanlydetai.domain.*;
import blog.hethong_quanlydetai.dto.AddMemberForm;
import blog.hethong_quanlydetai.dto.StudentGroupForm;
import blog.hethong_quanlydetai.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Nghiệp vụ Nhóm sinh viên (GĐ5).
 *
 * Quy tắc:
 * - Nhóm chỉ tạo/sửa trong khung thời gian SV của đợt PUBLISHED.
 * - Sĩ số: 1..3 (mặc định theo thực tế: SV tự lập nhóm 1–3 người để đăng ký đề tài).
 * - Tất cả thành viên phải cùng LỚP (students.class_name).
 * - Mỗi SV tại 1 thời điểm chỉ thuộc 1 nhóm trong 1 đợt.
 * - Có 1 nhóm trưởng; người tạo mặc định là trưởng.
 * - Sau khi nhóm đã APPROVED (GVHD duyệt đề tài): KHÔNG thêm/xoá thành viên, KHÔNG đổi trưởng.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StudentGroupService {

    /** Sĩ số tối đa một nhóm (mặc định thực tế: 1–3 SV). */
    public static final int MAX_MEMBERS = 3;

    private final StudentGroupRepository groupRepository;
    private final GroupMemberRepository memberRepository;
    private final RegistrationPeriodRepository periodRepository;
    private final StudentRepository studentRepository;
    private final TopicRegistrationRepository registrationRepository;
    private final UserRepository userRepository;
    private final TopicService topicService;
    private final AuditLogger auditLogger;

    // ------------------------------------------------------------------
    // Query
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public StudentGroup getById(Long id) {
        return groupRepository.findWithDetailsById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhóm #" + id));
    }

    @Transactional(readOnly = true)
    public Page<StudentGroup> searchByPeriod(Long periodId, GroupStatus status, Pageable pageable) {
        return groupRepository.searchByPeriod(periodId, status, pageable);
    }

    @Transactional(readOnly = true)
    public Page<StudentGroup> findGroupsForSupervisor(Long lecturerId, GroupStatus status, Pageable pageable) {
        return groupRepository.findGroupsForPrimarySupervisor(lecturerId, status, pageable);
    }

    /** Lấy các nhóm trong đợt mà SV này đang là thành viên. */
    @Transactional(readOnly = true)
    public List<StudentGroup> findMyGroups(Long studentId, Long periodId) {
        if (periodId != null) {
            return groupRepository.findByMemberStudentIdAndPeriodId(studentId, periodId);
        }
        // Không có periodId: tìm theo tất cả các đợt mà SV là thành viên
        List<Long> periodIds = new ArrayList<>();
        // dùng search với mọi period - tạm thời trả về qua findByMemberStudentIdAndPeriodId lặp
        // để đơn giản: controller truyền periodId cụ thể.
        return List.of();
    }

    // ------------------------------------------------------------------
    // Create draft group
    // ------------------------------------------------------------------

    /**
     * SV tạo nhóm mới. Người tạo là nhóm trưởng.
     */
    @Transactional
    public StudentGroup createDraft(StudentGroupForm form, User creator) {
        // 1. Validate period
        RegistrationPeriod period = periodRepository.findById(form.getPeriodId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đợt đăng ký #" + form.getPeriodId()));
        LocalDateTime now = LocalDateTime.now();
        if (!topicService.isWithinStudentWindow(period, now)) {
            throw new IllegalArgumentException("Hiện không trong khung thời gian sinh viên đăng ký của đợt này");
        }

        // 2. Validate creator là SV
        Student creatorStudent = studentRepository.findByUserId(creator.getId())
                .orElseThrow(() -> new IllegalArgumentException("Tài khoản không phải sinh viên"));

        // 3. Validate mã nhóm duy nhất
        String code = form.getGroupCode().trim();
        if (groupRepository.existsByGroupCode(code)) {
            throw new IllegalArgumentException("Mã nhóm '" + code + "' đã tồn tại");
        }

        // 4. Tạo nhóm
        StudentGroup group = StudentGroup.builder()
                .period(period)
                .groupCode(code)
                .groupName(form.getGroupName().trim())
                .status(GroupStatus.DRAFT)
                .build();
        StudentGroup saved = groupRepository.save(group);

        // 5. Thêm nhóm trưởng (set cả 2 chiều để JPA flush đúng)
        GroupMember leader = GroupMember.builder()
                .group(saved)
                .student(creatorStudent)
                .isLeader(true)
                .build();
        saved.getMembers().add(leader);
        memberRepository.saveAndFlush(leader);

        // 6. Thêm thành viên khác (nếu có) - enforce cùng lớp & chưa ở nhóm nào trong đợt
        if (form.getMemberUserIds() != null && !form.getMemberUserIds().isEmpty()) {
            for (Long userId : form.getMemberUserIds()) {
                addMemberInternal(saved, userId, creatorStudent, period);
            }
        }

        auditLogger.log("GROUP_CREATE", "StudentGroup", saved.getId(),
                "SV " + creator.getFullName() + " tạo nhóm " + saved.getGroupCode());

        return getById(saved.getId());
    }

    // ------------------------------------------------------------------
    // Add / Remove members
    // ------------------------------------------------------------------

    /**
     * Thêm 1 SV vào nhóm (dùng khi nhóm đã tạo, SV thêm bạn).
     */
    @Transactional
    public StudentGroup addMember(Long groupId, AddMemberForm form, User actor) {
        StudentGroup group = getById(groupId);
        ensureMutable(group, actor);
        RegistrationPeriod period = group.getPeriod();
        if (!topicService.isWithinStudentWindow(period, LocalDateTime.now())) {
            throw new IllegalArgumentException("Đã hết khung thời gian đăng ký của đợt");
        }

        Student actorStudent = studentRepository.findByUserId(actor.getId())
                .orElseThrow(() -> new IllegalArgumentException("Tài khoản không phải sinh viên"));

        Student newMember = studentRepository.findByStudentCode(form.getStudentCode().trim())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sinh viên có mã " + form.getStudentCode()));

        addMemberInternal(group, newMember.getUser().getId(), actorStudent, period);
        auditLogger.log("GROUP_ADD_MEMBER", "StudentGroup", groupId,
                "SV " + actor.getFullName() + " thêm " + newMember.getUser().getFullName() + " vào nhóm " + group.getGroupCode());

        return getById(groupId);
    }

    private void addMemberInternal(StudentGroup group, Long newUserId, Student actorStudent, RegistrationPeriod period) {
        // Giới hạn sĩ số
        if (group.getMembers() != null && group.getMembers().size() >= MAX_MEMBERS) {
            throw new IllegalArgumentException("Nhóm đã đủ " + MAX_MEMBERS + " thành viên");
        }

        // Trùng SV
        if (group.getMembers() != null) {
            for (GroupMember gm : group.getMembers()) {
                if (gm.getStudent().getUser().getId().equals(newUserId)) {
                    throw new IllegalArgumentException("Sinh viên này đã ở trong nhóm");
                }
            }
        }

        // Tìm student profile
        Student newStudent = studentRepository.findByUserId(newUserId)
                .orElseThrow(() -> new IllegalArgumentException("User #" + newUserId + " không phải sinh viên"));

        // Cùng lớp
        if (!newStudent.getClassName().equalsIgnoreCase(actorStudent.getClassName())) {
            throw new IllegalArgumentException("Sinh viên phải cùng lớp (" + actorStudent.getClassName() + ") mới vào được nhóm");
        }

        // SV chưa ở nhóm khác trong đợt
        List<Long> existing = memberRepository.findGroupIdsByStudentAndPeriod(newStudent.getId(), period.getId());
        if (!existing.isEmpty()) {
            throw new IllegalArgumentException("Sinh viên này đang ở nhóm khác trong đợt này");
        }

        GroupMember member = GroupMember.builder()
                .group(group)
                .student(newStudent)
                .isLeader(false)
                .build();
        memberRepository.saveAndFlush(member);
        group.getMembers().add(member);
    }

    @Transactional
    public StudentGroup removeMember(Long groupId, Long studentId, User actor) {
        StudentGroup group = getById(groupId);
        ensureMutable(group, actor);

        // Không xoá nhóm trưởng qua action này (phải đổi trưởng trước)
        for (GroupMember gm : group.getMembers()) {
            if (gm.getStudent().getId().equals(studentId) && Boolean.TRUE.equals(gm.getIsLeader())) {
                throw new IllegalArgumentException("Không thể xoá nhóm trưởng. Hãy đổi trưởng nhóm trước");
            }
        }

        int deleted = memberRepository.deleteByGroupIdAndStudentId(groupId, studentId);
        if (deleted == 0) {
            throw new IllegalArgumentException("Thành viên không tồn tại trong nhóm");
        }
        auditLogger.log("GROUP_REMOVE_MEMBER", "StudentGroup", groupId,
                "SV " + actor.getFullName() + " xoá SV #" + studentId + " khỏi nhóm " + group.getGroupCode());
        return getById(groupId);
    }

    @Transactional
    public StudentGroup leaveGroup(Long groupId, User actor) {
        StudentGroup group = getById(groupId);
        ensureMutable(group, actor);
        Student actorStudent = studentRepository.findByUserId(actor.getId())
                .orElseThrow(() -> new IllegalArgumentException("Tài khoản không phải sinh viên"));
        // Nhóm trưởng không được rời (phải chuyển quyền trưởng trước hoặc xoá nhóm)
        for (GroupMember gm : group.getMembers()) {
            if (gm.getStudent().getId().equals(actorStudent.getId()) && Boolean.TRUE.equals(gm.getIsLeader())) {
                throw new IllegalArgumentException("Nhóm trưởng không thể rời nhóm. Hãy chuyển quyền trưởng hoặc xoá nhóm");
            }
        }
        memberRepository.deleteByGroupIdAndStudentId(groupId, actorStudent.getId());
        auditLogger.log("GROUP_LEAVE", "StudentGroup", groupId,
                "SV " + actor.getFullName() + " rời nhóm " + group.getGroupCode());
        return getById(groupId);
    }

    @Transactional
    public StudentGroup changeLeader(Long groupId, Long newLeaderStudentId, User actor) {
        StudentGroup group = getById(groupId);
        ensureMutable(group, actor);
        if (group.getMembers() == null || group.getMembers().size() < 2) {
            throw new IllegalArgumentException("Cần ít nhất 2 thành viên để chuyển trưởng nhóm");
        }
        GroupMember newLeader = null;
        for (GroupMember gm : group.getMembers()) {
            if (gm.getStudent().getId().equals(newLeaderStudentId)) {
                newLeader = gm;
                break;
            }
        }
        if (newLeader == null) {
            throw new IllegalArgumentException("Thành viên không thuộc nhóm");
        }
        for (GroupMember gm : group.getMembers()) {
            gm.setIsLeader(gm.getId().equals(newLeader.getId()));
        }
        memberRepository.saveAll(group.getMembers());
        auditLogger.log("GROUP_CHANGE_LEADER", "StudentGroup", groupId,
                "Đổi trưởng nhóm " + group.getGroupCode() + " sang SV #" + newLeaderStudentId);
        return getById(groupId);
    }

    @Transactional
    public void deleteGroup(Long groupId, User actor) {
        StudentGroup group = getById(groupId);
        ensureMutable(group, actor);
        // Nhóm chỉ xoá được khi DRAFT
        if (group.getStatus() != GroupStatus.DRAFT) {
            throw new IllegalArgumentException("Chỉ xoá được nhóm đang ở trạng thái NHÁP");
        }
        // Đã có đăng ký đề tài → không xoá
        if (group.getTopic() != null) {
            throw new IllegalArgumentException("Nhóm đã đăng ký đề tài, không thể xoá");
        }
        String code = group.getGroupCode();
        // Xoá members (orphanRemoval=true ở entity sẽ lo, nhưng để chắc chắn xoá tường minh)
        for (GroupMember gm : new java.util.ArrayList<>(group.getMembers())) {
            memberRepository.delete(gm);
        }
        group.getMembers().clear();
        groupRepository.delete(group);
        auditLogger.log("GROUP_DELETE", "StudentGroup", groupId, "Xoá nhóm " + code);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private void ensureMutable(StudentGroup group, User actor) {
        if (group.getStatus() == GroupStatus.APPROVED) {
            throw new IllegalArgumentException("Nhóm đã được GVHD duyệt, không thể thay đổi thành viên");
        }
        // Phải là thành viên của nhóm mới được sửa
        // So sánh qua studentId (an toàn, không phụ thuộc việc fetch user của member)
        Student actorStudent = studentRepository.findByUserId(actor.getId()).orElse(null);
        Long actorStudentId = actorStudent != null ? actorStudent.getId() : null;
        if (actorStudentId == null) {
            // Người dùng không có hồ sơ SV (vd admin cố truy cập) → chặn
            throw new IllegalArgumentException("Bạn không có hồ sơ sinh viên");
        }
        boolean isMember = false;
        for (GroupMember gm : group.getMembers()) {
            if (gm.getStudent().getId().equals(actorStudentId)) {
                isMember = true;
                break;
            }
        }
        if (!isMember) {
            throw new IllegalArgumentException("Bạn không phải thành viên của nhóm này");
        }
    }
}
