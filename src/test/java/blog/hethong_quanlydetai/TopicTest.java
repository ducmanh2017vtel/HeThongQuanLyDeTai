package blog.hethong_quanlydetai;

import blog.hethong_quanlydetai.domain.*;
import blog.hethong_quanlydetai.dto.RegistrationPeriodForm;
import blog.hethong_quanlydetai.dto.TopicForm;
import blog.hethong_quanlydetai.repository.*;
import blog.hethong_quanlydetai.service.RegistrationPeriodService;
import blog.hethong_quanlydetai.service.TopicService;
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
 * Kiểm tra nghiệp vụ GĐ4 — Đề tài (mục 5.3).
 * Mỗi quy tắc có 1 ca thành công và ≥1 ca bị từ chối.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TopicTest {

    @Autowired TopicService topicService;
    @Autowired TopicRepository topicRepository;
    @Autowired TopicSupervisorRepository supervisorRepository;
    @Autowired RegistrationPeriodService periodService;
    @Autowired RegistrationPeriodRepository periodRepository;
    @Autowired DepartmentRepository departmentRepository;
    @Autowired LecturerRepository lecturerRepository;
    @Autowired UserRepository userRepository;
    @Autowired RoleRepository roleRepository;

    private User giangVien1;
    private User giangVien2;
    private User giangVien3;
    private Lecturer lec1;
    private Lecturer lec2;
    private Lecturer lec3;
    private Department deptCNTT;
    private Department deptKHMT;
    private RegistrationPeriod openPeriod;

    @BeforeEach
    void setUp() {
        // Tạo role GIANG_VIEN
        Role gvRole = roleRepository.findByCode("GIANG_VIEN").orElseGet(() ->
            roleRepository.save(Role.builder()
                .code("GIANG_VIEN")
                .name("Giảng viên")
                .description("Giảng viên test")
                .systemFlag(true)
                .build()));

        // Tạo 2 bộ môn
        deptCNTT = departmentRepository.save(Department.builder()
            .code("CNTT")
            .name("Công nghệ thông tin")
            .status("ACTIVE")
            .build());
        deptKHMT = departmentRepository.save(Department.builder()
            .code("KHMT")
            .name("Khoa học máy tính")
            .status("ACTIVE")
            .build());

        long ts = System.nanoTime();

        // Tạo 3 GV thuộc 2 bộ môn
        giangVien1 = userRepository.save(User.builder()
            .username("gv1_" + ts)
            .password("{noop}x")
            .fullName("Nguyễn Văn A")
            .email("gv1_" + ts + "@test.local")
            .status(UserStatus.ACTIVE)
            .enabled(true)
            .roles(new HashSet<>(Set.of(gvRole)))
            .build());
        lec1 = lecturerRepository.save(Lecturer.builder()
            .user(giangVien1)
            .lecturerCode("GV001_" + ts)
            .academicDegree("ThS")
            .department(deptCNTT)
            .build());

        giangVien2 = userRepository.save(User.builder()
            .username("gv2_" + ts)
            .password("{noop}x")
            .fullName("Trần Thị B")
            .email("gv2_" + ts + "@test.local")
            .status(UserStatus.ACTIVE)
            .enabled(true)
            .roles(new HashSet<>(Set.of(gvRole)))
            .build());
        lec2 = lecturerRepository.save(Lecturer.builder()
            .user(giangVien2)
            .lecturerCode("GV002_" + ts)
            .academicDegree("TS")
            .department(deptCNTT)
            .build());

        giangVien3 = userRepository.save(User.builder()
            .username("gv3_" + ts)
            .password("{noop}x")
            .fullName("Lê Văn C")
            .email("gv3_" + ts + "@test.local")
            .status(UserStatus.ACTIVE)
            .enabled(true)
            .roles(new HashSet<>(Set.of(gvRole)))
            .build());
        lec3 = lecturerRepository.save(Lecturer.builder()
            .user(giangVien3)
            .lecturerCode("GV003_" + ts)
            .academicDegree("PGS")
            .department(deptKHMT)
            .build());

        // Tạo đợt PUBLISHED đang mở cho GV
        RegistrationPeriodForm pf = new RegistrationPeriodForm();
        pf.setName("Đợt TLCN test " + ts);
        pf.setPeriodType(PeriodType.TLCN);
        LocalDateTime now = LocalDateTime.now();
        pf.setLecturerStartAt(now.minusDays(1));
        pf.setLecturerEndAt(now.plusDays(7));
        pf.setStudentStartAt(now.plusDays(7));
        pf.setStudentEndAt(now.plusDays(14));
        pf.setReviewDeadline(now.plusDays(30));
        openPeriod = periodService.create(pf, giangVien1);
        periodService.publish(openPeriod.getId(), giangVien1);
    }

    private TopicForm validForm(Long supervisor1Id, Long supervisor2Id) {
        TopicForm f = new TopicForm();
        f.setTopicCode("DT-" + System.nanoTime());
        f.setTopicName("Đề tài nghiên cứu hệ thống quản lý đề tài sinh viên");
        f.setDescription("Mô tả chi tiết về đề tài nghiên cứu, phạm vi, công nghệ sử dụng, kết quả dự kiến và ý nghĩa khoa học.");
        f.setDepartmentId(deptCNTT.getId());
        f.setPeriodId(openPeriod.getId());
        f.setSupervisor1Id(supervisor1Id);
        f.setSupervisor2Id(supervisor2Id);
        return f;
    }

    // ------------------------------------------------------------------
    // Happy path
    // ------------------------------------------------------------------

    @Test
    void create_withOneSupervisor_success() {
        TopicForm f = validForm(lec1.getId(), null);
        Topic t = topicService.create(f, giangVien1);
        assertThat(t.getId()).isNotNull();
        assertThat(t.getStatus()).isEqualTo(TopicStatus.PENDING);
        assertThat(supervisorRepository.countByTopicId(t.getId())).isEqualTo(1);
    }

    @Test
    void create_withTwoSupervisors_success() {
        TopicForm f = validForm(lec1.getId(), lec2.getId());
        Topic t = topicService.create(f, giangVien1);
        assertThat(t.getId()).isNotNull();
        assertThat(supervisorRepository.countByTopicId(t.getId())).isEqualTo(2);
    }

    @Test
    void update_PENDING_success() {
        Topic t = topicService.create(validForm(lec1.getId(), null), giangVien1);
        TopicForm f = validForm(lec1.getId(), lec2.getId());
        f.setTopicCode(t.getTopicCode());
        Topic updated = topicService.update(t.getId(), f, giangVien1);
        assertThat(supervisorRepository.countByTopicId(updated.getId())).isEqualTo(2);
    }

    @Test
    void update_REJECTED_resubmitsAsPENDING() {
        Topic t = topicService.create(validForm(lec1.getId(), null), giangVien1);
        topicService.reject(t.getId(), "Lý do từ chối không phù hợp", giangVien1);

        TopicForm f = validForm(lec1.getId(), null);
        f.setTopicCode(t.getTopicCode());
        Topic updated = topicService.update(t.getId(), f, giangVien1);

        assertThat(updated.getStatus()).isEqualTo(TopicStatus.PENDING);
        assertThat(updated.getRejectReason()).isNull();
    }

    @Test
    void approve_pendingToApproved() {
        Topic t = topicService.create(validForm(lec1.getId(), null), giangVien1);
        Topic approved = topicService.approve(t.getId(), giangVien1);
        assertThat(approved.getStatus()).isEqualTo(TopicStatus.APPROVED);
        assertThat(approved.getApprovedBy()).isNotNull();
        assertThat(approved.getApprovedAt()).isNotNull();
    }

    @Test
    void reject_pendingToRejected() {
        Topic t = topicService.create(validForm(lec1.getId(), null), giangVien1);
        Topic rejected = topicService.reject(t.getId(), "Lý do từ chối hợp lệ", giangVien1);
        assertThat(rejected.getStatus()).isEqualTo(TopicStatus.REJECTED);
        assertThat(rejected.getRejectReason()).isEqualTo("Lý do từ chối hợp lệ");
    }

    @Test
    void publish_approvedToPublished() {
        Topic t = topicService.create(validForm(lec1.getId(), null), giangVien1);
        topicService.approve(t.getId(), giangVien1);
        Topic published = topicService.publish(t.getId(), giangVien1);
        assertThat(published.getPublished()).isTrue();
    }

    // ------------------------------------------------------------------
    // Quy tắc bị từ chối
    // ------------------------------------------------------------------

    @Test
    void duplicateTopicCode_rejected() {
        topicService.create(validForm(lec1.getId(), null), giangVien1);
        TopicForm f2 = validForm(lec2.getId(), null);
        f2.setTopicCode(topicRepository.findAll().get(0).getTopicCode());
        assertThatThrownBy(() -> topicService.create(f2, giangVien2))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Mã đề tài");
    }

    @Test
    void descriptionTooShort_rejected() {
        TopicForm f = validForm(lec1.getId(), null);
        f.setDescription("Ngắn quá");
        assertThatThrownBy(() -> topicService.create(f, giangVien1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("30 ký tự");
    }

    @Test
    void sameSupervisor_rejected() {
        TopicForm f = validForm(lec1.getId(), lec1.getId());
        assertThatThrownBy(() -> topicService.create(f, giangVien1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("trùng");
    }

    @Test
    void departmentMismatch_rejected() {
        TopicForm f = validForm(lec1.getId(), null);
        f.setDepartmentId(deptKHMT.getId()); // GV thuộc CNTT nhưng chọn KHMT
        assertThatThrownBy(() -> topicService.create(f, giangVien1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("bộ môn");
    }

    @Test
    void outsideLecturerWindow_rejected() {
        // Tạo đợt đã đóng
        RegistrationPeriodForm pf = new RegistrationPeriodForm();
        pf.setName("Đợt đã đóng " + System.nanoTime());
        pf.setPeriodType(PeriodType.MON_HOC);
        LocalDateTime now = LocalDateTime.now();
        pf.setLecturerStartAt(now.minusDays(30));
        pf.setLecturerEndAt(now.minusDays(20));
        pf.setStudentStartAt(now.minusDays(20));
        pf.setStudentEndAt(now.minusDays(10));
        RegistrationPeriod closed = periodService.create(pf, giangVien1);
        periodService.publish(closed.getId(), giangVien1);

        TopicForm f = validForm(lec1.getId(), null);
        f.setPeriodId(closed.getId());
        assertThatThrownBy(() -> topicService.create(f, giangVien1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("khung thời gian");
    }

    @Test
    void reject_shortReason_rejected() {
        Topic t = topicService.create(validForm(lec1.getId(), null), giangVien1);
        assertThatThrownBy(() -> topicService.reject(t.getId(), "ngắn", giangVien1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("10 ký tự");
    }

    @Test
    void reject_alreadyApproved_rejected() {
        Topic t = topicService.create(validForm(lec1.getId(), null), giangVien1);
        topicService.approve(t.getId(), giangVien1);
        assertThatThrownBy(() -> topicService.reject(t.getId(), "Lý do từ chối hợp lệ", giangVien1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("CHỜ DUYỆT");
    }

    @Test
    void update_notOwner_rejected() {
        Topic t = topicService.create(validForm(lec1.getId(), null), giangVien1);
        // giangVien2 cố sửa đề tài của giangVien1
        TopicForm f = validForm(lec1.getId(), null);
        f.setTopicCode(t.getTopicCode());
        assertThatThrownBy(() -> topicService.update(t.getId(), f, giangVien2))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("quyền sửa");
    }

    @Test
    void update_APPROVED_succeeds_whenNoApprovedRegistration() {
        Topic t = topicService.create(validForm(lec1.getId(), null), giangVien1);
        topicService.approve(t.getId(), giangVien1);
        // Chưa có đăng ký được duyệt, nên sửa được
        TopicForm f = validForm(lec1.getId(), null);
        f.setTopicCode(t.getTopicCode());
        Topic updated = topicService.update(t.getId(), f, giangVien1);
        assertThat(updated.getId()).isEqualTo(t.getId());
    }

    @Test
    void publish_alreadyPublished_rejected() {
        Topic t = topicService.create(validForm(lec1.getId(), null), giangVien1);
        topicService.approve(t.getId(), giangVien1);
        topicService.publish(t.getId(), giangVien1);
        assertThatThrownBy(() -> topicService.publish(t.getId(), giangVien1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("trước đó");
    }

    @Test
    void delete_APPROVED_rejected() {
        Topic t = topicService.create(validForm(lec1.getId(), null), giangVien1);
        topicService.approve(t.getId(), giangVien1);
        assertThatThrownBy(() -> topicService.delete(t.getId(), giangVien1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("CHỜ DUYỆT");
    }
}
