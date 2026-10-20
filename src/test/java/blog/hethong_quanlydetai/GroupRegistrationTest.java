package blog.hethong_quanlydetai;

import blog.hethong_quanlydetai.domain.*;
import blog.hethong_quanlydetai.dto.AddMemberForm;
import blog.hethong_quanlydetai.dto.RegistrationPeriodForm;
import blog.hethong_quanlydetai.dto.RejectRegistrationForm;
import blog.hethong_quanlydetai.dto.StudentGroupForm;
import blog.hethong_quanlydetai.repository.*;
import blog.hethong_quanlydetai.service.RegistrationPeriodService;
import blog.hethong_quanlydetai.service.RegistrationService;
import blog.hethong_quanlydetai.service.StudentGroupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Kiểm tra nghiệp vụ GĐ5 — Nhóm SV & đăng ký đề tài (mục 5.4).
 *
 * Quy tắc trọng tâm:
 * - Nhóm 1..3 SV, cùng lớp, cùng đợt; mỗi SV chỉ ở 1 nhóm/đợt.
 * - Chỉ nhóm trưởng đăng ký / huỷ đề tài.
 * - Đề tài phải APPROVED + published, cùng đợt.
 * - 1 đề tài ↔ 1 nhóm (PENDING/APPROVED).
 * - Đổi đề tài chỉ trước khi GVHD duyệt.
 * - Chỉ GVHD chính (supervisor_order=1) duyệt / từ chối.
 * - Từ chối: lý do ≥ 10 ký tự; nhóm về DRAFT.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class GroupRegistrationTest {

    @Autowired StudentGroupService groupService;
    @Autowired RegistrationService registrationService;
    @Autowired RegistrationPeriodService periodService;
    @Autowired RegistrationPeriodRepository periodRepository;
    @Autowired DepartmentRepository departmentRepository;
    @Autowired LecturerRepository lecturerRepository;
    @Autowired UserRepository userRepository;
    @Autowired StudentRepository studentRepository;
    @Autowired RoleRepository roleRepository;
    @Autowired TopicRepository topicRepository;
    @Autowired TopicSupervisorRepository supervisorRepository;
    @Autowired StudentGroupRepository groupRepository;
    @Autowired TopicRegistrationRepository registrationRepository;
    @Autowired GroupMemberRepository memberRepository;

    private User giangVien;
    private Lecturer lec;
    private User sv1, sv2, sv3, sv4;
    private Student s1, s2, s3, s4;
    private Department dept;
    private RegistrationPeriod openForStudent;
    private Topic approvedTopic;

    @BeforeEach
    void setUp() {
        long ts = System.nanoTime();

        // Vai trò
        Role gvRole = roleRepository.findByCode("GIANG_VIEN").orElseGet(() ->
            roleRepository.save(Role.builder().code("GIANG_VIEN").name("Giảng viên").systemFlag(true).build()));

        // Bộ môn
        dept = departmentRepository.save(Department.builder()
                .code("BM" + ts).name("Bộ môn test " + ts).status("ACTIVE").build());

        // Giảng viên
        giangVien = userRepository.save(User.builder()
                .username("gv_gd5_" + ts)
                .password("{noop}x")
                .fullName("GV GĐ5")
                .email("gv_gd5_" + ts + "@test.local")
                .status(UserStatus.ACTIVE)
                .enabled(true)
                .roles(new HashSet<>(Set.of(gvRole)))
                .build());
        lec = lecturerRepository.save(Lecturer.builder()
                .user(giangVien)
                .lecturerCode("LEC" + ts)
                .academicDegree("ThS")
                .department(dept)
                .build());

        // Sinh viên cùng lớp CNTT-K15
        sv1 = userRepository.save(User.builder()
                .username("sv1_" + ts).password("{noop}x")
                .fullName("SV 1").email("sv1_" + ts + "@test.local")
                .status(UserStatus.ACTIVE).enabled(true).build());
        s1 = studentRepository.save(Student.builder().user(sv1)
                .studentCode("SV01_" + ts).className("CNTT-K15").major("CNTT").build());

        sv2 = userRepository.save(User.builder()
                .username("sv2_" + ts).password("{noop}x")
                .fullName("SV 2").email("sv2_" + ts + "@test.local")
                .status(UserStatus.ACTIVE).enabled(true).build());
        s2 = studentRepository.save(Student.builder().user(sv2)
                .studentCode("SV02_" + ts).className("CNTT-K15").major("CNTT").build());

        sv3 = userRepository.save(User.builder()
                .username("sv3_" + ts).password("{noop}x")
                .fullName("SV 3").email("sv3_" + ts + "@test.local")
                .status(UserStatus.ACTIVE).enabled(true).build());
        s3 = studentRepository.save(Student.builder().user(sv3)
                .studentCode("SV03_" + ts).className("CNTT-K15").major("CNTT").build());

        // SV khác lớp
        sv4 = userRepository.save(User.builder()
                .username("sv4_" + ts).password("{noop}x")
                .fullName("SV 4").email("sv4_" + ts + "@test.local")
                .status(UserStatus.ACTIVE).enabled(true).build());
        s4 = studentRepository.save(Student.builder().user(sv4)
                .studentCode("SV04_" + ts).className("KHMT-K15").major("KHMT").build());

        // Đợt PUBLISHED đang mở cho cả GV (để setUp tạo được đề tài) và SV
        RegistrationPeriodForm pf = new RegistrationPeriodForm();
        pf.setName("Đợt GĐ5 " + ts);
        pf.setPeriodType(PeriodType.TLCN);
        LocalDateTime now = LocalDateTime.now();
        pf.setLecturerStartAt(now.minusDays(14));
        pf.setLecturerEndAt(now);
        pf.setStudentStartAt(now);
        pf.setStudentEndAt(now.plusDays(7));
        pf.setReviewDeadline(now.plusDays(30));
        openForStudent = periodService.create(pf, giangVien);
        periodService.publish(openForStudent.getId(), giangVien);

        // Tạo + duyệt + công bố 1 đề tài (insert trực tiếp qua repository để không phụ thuộc khung GV)
        long ts2 = System.nanoTime();
        Topic t = Topic.builder()
                .period(openForStudent)
                .department(dept)
                .createdByLecturer(lec)
                .topicCode("DT-GD5-" + ts2)
                .topicName("Đề tài nghiên cứu GĐ5 sinh viên đăng ký")
                .description("Mô tả chi tiết đề tài GĐ5 dùng cho test nghiệp vụ nhóm và đăng ký đề tài của sinh viên.")
                .status(TopicStatus.APPROVED)
                .approvedBy(giangVien)
                .approvedAt(LocalDateTime.now())
                .published(true)
                .build();
        approvedTopic = topicRepository.save(t);
        TopicSupervisor ts1 = TopicSupervisor.builder()
                .topic(approvedTopic).lecturer(lec).supervisorOrder(1).build();
        supervisorRepository.save(ts1);
    }

    private StudentGroupForm form(String code, Set<Long> memberUserIds) {
        return StudentGroupForm.builder()
                .periodId(openForStudent.getId())
                .groupCode(code)
                .groupName("Nhóm test " + code)
                .memberUserIds(memberUserIds == null ? null : new java.util.ArrayList<>(memberUserIds))
                .build();
    }

    // ------------------------------------------------------------------
    // Happy path — Nhóm
    // ------------------------------------------------------------------

    @Test
    void createGroup_oneMember_success() {
        StudentGroup g = groupService.createDraft(form("G1_" + System.nanoTime(), null), sv1);
        assertThat(g.getId()).isNotNull();
        assertThat(g.getStatus()).isEqualTo(GroupStatus.DRAFT);
        assertThat(g.getMembers()).hasSize(1);
    }

    @Test
    void createGroup_threeMembers_sameClass_success() {
        StudentGroup g = groupService.createDraft(
                form("G2_" + System.nanoTime(), Set.of(sv2.getId(), sv3.getId())),
                sv1);
        assertThat(g.getMembers()).hasSize(3);
    }

    @Test
    void addMemberByCode_sameClass_success() {
        StudentGroup g = groupService.createDraft(form("G3_" + System.nanoTime(), null), sv1);
        AddMemberForm af = new AddMemberForm();
        af.setStudentCode(s2.getStudentCode());
        StudentGroup after = groupService.addMember(g.getId(), af, sv1);
        assertThat(after.getMembers()).hasSize(2);
    }

    @Test
    void changeLeader_success() {
        StudentGroup g = groupService.createDraft(
                form("G4_" + System.nanoTime(), Set.of(sv2.getId())), sv1);
        groupService.changeLeader(g.getId(), s2.getId(), sv1);
        StudentGroup after = groupService.getById(g.getId());
        // sv2 giờ là trưởng
        boolean sv2IsLeader = after.getMembers().stream()
                .anyMatch(m -> m.getStudent().getId().equals(s2.getId()) && Boolean.TRUE.equals(m.getIsLeader()));
        assertThat(sv2IsLeader).isTrue();
    }

    // ------------------------------------------------------------------
    // Từ chối — Nhóm
    // ------------------------------------------------------------------

    @Test
    void addMember_differentClass_rejected() {
        StudentGroup g = groupService.createDraft(form("G5_" + System.nanoTime(), null), sv1);
        AddMemberForm af = new AddMemberForm();
        af.setStudentCode(s4.getStudentCode()); // khác lớp
        assertThatThrownBy(() -> groupService.addMember(g.getId(), af, sv1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cùng lớp");
    }

    @Test
    void addMember_alreadyInOtherGroup_rejected() {
        // sv1 tạo nhóm, sv2 vào nhóm 1
        groupService.createDraft(form("G6A_" + System.nanoTime(), Set.of(sv2.getId())), sv1);
        // sv3 tạo nhóm khác, thử thêm sv2
        StudentGroup g2 = groupService.createDraft(form("G6B_" + System.nanoTime(), null), sv3);
        AddMemberForm af = new AddMemberForm();
        af.setStudentCode(s2.getStudentCode());
        assertThatThrownBy(() -> groupService.addMember(g2.getId(), af, sv3))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nhóm khác");
    }

    @Test
    void addMember_exceedThree_rejected() {
        // sv1 tạo nhóm 3 người (cần thêm 1 sv cùng lớp — tạm thời dùng sv1,sv2,sv3)
        StudentGroup g = groupService.createDraft(form("G7_" + System.nanoTime(), Set.of(sv2.getId(), sv3.getId())), sv1);
        // Tạo user sv5 cùng lớp
        long ts = System.nanoTime();
        User sv5 = userRepository.save(User.builder()
                .username("sv5_" + ts).password("{noop}x")
                .fullName("SV 5").email("sv5_" + ts + "@test.local")
                .status(UserStatus.ACTIVE).enabled(true).build());
        studentRepository.save(Student.builder().user(sv5)
                .studentCode("SV05_" + ts).className("CNTT-K15").major("CNTT").build());

        AddMemberForm af = new AddMemberForm();
        af.setStudentCode("SV05_" + ts);
        assertThatThrownBy(() -> groupService.addMember(g.getId(), af, sv1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("đủ");
    }

    @Test
    void duplicateGroupCode_rejected() {
        String code = "DUPCODE_" + System.nanoTime();
        groupService.createDraft(form(code, null), sv1);
        assertThatThrownBy(() -> groupService.createDraft(form(code, null), sv2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Mã nhóm");
    }

    @Test
    void createGroup_outsideStudentWindow_rejected() {
        // Tạo đợt đã đóng khung SV
        RegistrationPeriodForm pf = new RegistrationPeriodForm();
        pf.setName("Closed " + System.nanoTime());
        pf.setPeriodType(PeriodType.TLCN);
        LocalDateTime now = LocalDateTime.now();
        pf.setLecturerStartAt(now.minusDays(30));
        pf.setLecturerEndAt(now.minusDays(20));
        pf.setStudentStartAt(now.minusDays(20));
        pf.setStudentEndAt(now.minusDays(10));
        pf.setReviewDeadline(now.plusDays(1));
        RegistrationPeriod closed = periodService.create(pf, giangVien);
        periodService.publish(closed.getId(), giangVien);

        StudentGroupForm f = form("CLOSED_" + System.nanoTime(), null);
        f.setPeriodId(closed.getId());
        assertThatThrownBy(() -> groupService.createDraft(f, sv1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("khung thời gian");
    }

    // ------------------------------------------------------------------
    // Happy path — Đăng ký đề tài
    // ------------------------------------------------------------------

    @Test
    void registerTopic_byLeader_success() {
        StudentGroup g = groupService.createDraft(form("R1_" + System.nanoTime(), null), sv1);
        TopicRegistration reg = registrationService.register(g.getId(), approvedTopic.getId(), sv1);
        assertThat(reg.getStatus()).isEqualTo(RegistrationStatus.PENDING);
        StudentGroup after = groupService.getById(g.getId());
        assertThat(after.getStatus()).isEqualTo(GroupStatus.REGISTERED);
        assertThat(after.getTopic().getId()).isEqualTo(approvedTopic.getId());
    }

    @Test
    void switchTopic_beforeApprove_success() {
        // Tạo đề tài thứ 2 (insert trực tiếp để APPROVED+published)
        long ts = System.nanoTime();
        final Topic t2 = Topic.builder()
                .period(openForStudent).department(dept).createdByLecturer(lec)
                .topicCode("DT-GD5B-" + ts)
                .topicName("Đề tài thứ hai cho test chuyển đề tài GĐ5")
                .description("Mô tả chi tiết đề tài thứ hai dùng cho test chuyển đề tài trước khi duyệt.")
                .status(TopicStatus.APPROVED)
                .approvedBy(giangVien).approvedAt(LocalDateTime.now())
                .published(true).build();
        final Topic savedT2 = topicRepository.save(t2);
        supervisorRepository.save(TopicSupervisor.builder().topic(savedT2).lecturer(lec).supervisorOrder(1).build());

        StudentGroup g = groupService.createDraft(form("R2_" + System.nanoTime(), null), sv1);
        registrationService.register(g.getId(), approvedTopic.getId(), sv1);
        TopicRegistration reg2 = registrationService.register(g.getId(), savedT2.getId(), sv1);
        assertThat(reg2.getTopic().getId()).isEqualTo(savedT2.getId());
    }

    @Test
    void cancelRegistration_beforeApprove_success() {
        StudentGroup g = groupService.createDraft(form("R3_" + System.nanoTime(), null), sv1);
        registrationService.register(g.getId(), approvedTopic.getId(), sv1);
        registrationService.cancel(g.getId(), sv1);
        StudentGroup after = groupService.getById(g.getId());
        assertThat(after.getStatus()).isEqualTo(GroupStatus.DRAFT);
        assertThat(after.getTopic()).isNull();
    }

    @Test
    void approve_byPrimarySupervisor_success() {
        StudentGroup g = groupService.createDraft(form("R4_" + System.nanoTime(), null), sv1);
        TopicRegistration reg = registrationService.register(g.getId(), approvedTopic.getId(), sv1);
        TopicRegistration approved = registrationService.approve(reg.getId(), giangVien);
        assertThat(approved.getStatus()).isEqualTo(RegistrationStatus.APPROVED);
        assertThat(groupService.getById(g.getId()).getStatus()).isEqualTo(GroupStatus.APPROVED);
    }

    @Test
    void reject_byPrimarySupervisor_resetsToDraft() {
        StudentGroup g = groupService.createDraft(form("R5_" + System.nanoTime(), null), sv1);
        TopicRegistration reg = registrationService.register(g.getId(), approvedTopic.getId(), sv1);

        RejectRegistrationForm rf = new RejectRegistrationForm();
        rf.setReason("Lý do từ chối hợp lệ dài hơn 10 ký tự");
        registrationService.reject(reg.getId(), rf, giangVien);

        StudentGroup after = groupService.getById(g.getId());
        assertThat(after.getStatus()).isEqualTo(GroupStatus.DRAFT);
        assertThat(after.getTopic()).isNull();
    }

    // ------------------------------------------------------------------
    // Từ chối — Đăng ký
    // ------------------------------------------------------------------

    @Test
    void register_byNonLeader_rejected() {
        // Tạo nhóm 2 người: sv1 (trưởng) + sv2
        StudentGroup g = groupService.createDraft(form("X1_" + System.nanoTime(), Set.of(sv2.getId())), sv1);
        assertThatThrownBy(() -> registrationService.register(g.getId(), approvedTopic.getId(), sv2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nhóm trưởng");
    }

    @Test
    void register_unpublishedTopic_rejected() {
        // Tạo đề tài APPROVED nhưng chưa publish
        long ts = System.nanoTime();
        final Topic t = Topic.builder()
                .period(openForStudent).department(dept).createdByLecturer(lec)
                .topicCode("DT-UNPUB-" + ts)
                .topicName("Đề tài chưa công bố dùng test nghiệp vụ")
                .description("Mô tả đề tài chưa công bố dùng cho test đăng ký — phải bị từ chối.")
                .status(TopicStatus.APPROVED)
                .approvedBy(giangVien).approvedAt(LocalDateTime.now())
                .published(false).build();
        final Topic savedUnpub = topicRepository.save(t);
        supervisorRepository.save(TopicSupervisor.builder().topic(savedUnpub).lecturer(lec).supervisorOrder(1).build());

        StudentGroup g = groupService.createDraft(form("X2_" + System.nanoTime(), null), sv1);
        assertThatThrownBy(() -> registrationService.register(g.getId(), savedUnpub.getId(), sv1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("công bố");
    }

    @Test
    void register_topicAlreadyTaken_rejected() {
        // Nhóm 1 đăng ký trước
        StudentGroup g1 = groupService.createDraft(form("T1_" + System.nanoTime(), null), sv1);
        registrationService.register(g1.getId(), approvedTopic.getId(), sv1);

        // Nhóm 2 (sv3) thử đăng ký cùng topic
        StudentGroup g2 = groupService.createDraft(form("T2_" + System.nanoTime(), null), sv3);
        assertThatThrownBy(() -> registrationService.register(g2.getId(), approvedTopic.getId(), sv3))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("đã có nhóm");
    }

    @Test
    void cancel_afterApprove_rejected() {
        StudentGroup g = groupService.createDraft(form("X3_" + System.nanoTime(), null), sv1);
        TopicRegistration reg = registrationService.register(g.getId(), approvedTopic.getId(), sv1);
        registrationService.approve(reg.getId(), giangVien);
        assertThatThrownBy(() -> registrationService.cancel(g.getId(), sv1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("đã được duyệt");
    }

    @Test
    void switchTopic_afterApprove_rejected() {
        // Tạo đề tài thứ 2 (insert trực tiếp)
        long ts = System.nanoTime();
        final Topic t2 = Topic.builder()
                .period(openForStudent).department(dept).createdByLecturer(lec)
                .topicCode("DT-SW-" + ts)
                .topicName("Đề tài thứ hai test chuyển sau duyệt")
                .description("Mô tả đề tài thứ hai dùng để test không được chuyển sau khi đã duyệt.")
                .status(TopicStatus.APPROVED)
                .approvedBy(giangVien).approvedAt(LocalDateTime.now())
                .published(true).build();
        final Topic savedT2 = topicRepository.save(t2);
        supervisorRepository.save(TopicSupervisor.builder().topic(savedT2).lecturer(lec).supervisorOrder(1).build());

        StudentGroup g = groupService.createDraft(form("X4_" + System.nanoTime(), null), sv1);
        TopicRegistration reg = registrationService.register(g.getId(), approvedTopic.getId(), sv1);
        registrationService.approve(reg.getId(), giangVien);
        assertThatThrownBy(() -> registrationService.register(g.getId(), savedT2.getId(), sv1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("APPROVED");
    }

    @Test
    void reject_shortReason_rejected() {
        StudentGroup g = groupService.createDraft(form("X5_" + System.nanoTime(), null), sv1);
        TopicRegistration reg = registrationService.register(g.getId(), approvedTopic.getId(), sv1);
        RejectRegistrationForm rf = new RejectRegistrationForm();
        rf.setReason("ngắn");
        assertThatThrownBy(() -> registrationService.reject(reg.getId(), rf, giangVien))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("10 ký tự");
    }

    @Test
    void approve_byNonPrimarySupervisor_rejected() {
        // Tạo GV thứ 2 thuộc cùng bộ môn
        long ts = System.nanoTime();
        User gv2User = userRepository.save(User.builder()
                .username("gv2_gd5_" + ts).password("{noop}x")
                .fullName("GV2").email("gv2_gd5_" + ts + "@test.local")
                .status(UserStatus.ACTIVE).enabled(true)
                .roles(new HashSet<>(Set.of(roleRepository.findByCode("GIANG_VIEN").orElseThrow())))
                .build());
        Lecturer lec2 = lecturerRepository.save(Lecturer.builder()
                .user(gv2User).lecturerCode("LEC2_" + ts)
                .academicDegree("TS").department(dept).build());

        // Tạo đề tài có 2 GVHD: lec (chính), lec2 (phụ)
        final Topic t2 = Topic.builder()
                .period(openForStudent).department(dept).createdByLecturer(lec)
                .topicCode("DT-2GV-" + ts)
                .topicName("Đề tài hai GVHD test quyền duyệt của GVHD chính")
                .description("Mô tả đề tài có hai GVHD, chỉ GVHD chính mới được duyệt đăng ký của nhóm sinh viên.")
                .status(TopicStatus.APPROVED)
                .approvedBy(giangVien).approvedAt(LocalDateTime.now())
                .published(true).build();
        final Topic savedT2 = topicRepository.save(t2);
        supervisorRepository.save(TopicSupervisor.builder().topic(savedT2).lecturer(lec).supervisorOrder(1).build());
        supervisorRepository.save(TopicSupervisor.builder().topic(savedT2).lecturer(lec2).supervisorOrder(2).build());

        StudentGroup g = groupService.createDraft(form("X6_" + System.nanoTime(), null), sv1);
        TopicRegistration reg = registrationService.register(g.getId(), savedT2.getId(), sv1);
        // GVHD phụ (lec2) thử duyệt → từ chối
        assertThatThrownBy(() -> registrationService.approve(reg.getId(), gv2User))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("GVHD chính");
    }
}
