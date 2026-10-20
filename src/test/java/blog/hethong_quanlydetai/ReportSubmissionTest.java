package blog.hethong_quanlydetai;

import blog.hethong_quanlydetai.domain.*;
import blog.hethong_quanlydetai.dto.RegistrationPeriodForm;
import blog.hethong_quanlydetai.dto.StudentGroupForm;
import blog.hethong_quanlydetai.dto.UploadReportForm;
import blog.hethong_quanlydetai.repository.*;
import blog.hethong_quanlydetai.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

/**
 * GĐ6 — Nộp báo cáo (mục 5.5, A7).
 *
 * Quy tắc:
 * - Chỉ nhóm trưởng nộp.
 * - Nhóm phải ở APPROVED.
 * - File PDF/DOC/DOCX, ≤ 20MB.
 * - Version tăng dần; bản cũ chuyển SUPERSEDED.
 * - Nhóm chuyển sang SUBMITTED sau lần nộp đầu tiên.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ReportSubmissionTest {

    @Autowired ReportService reportService;
    @Autowired StudentGroupService groupService;
    @Autowired RegistrationService registrationService;
    @Autowired RegistrationPeriodService periodService;

    @Autowired RegistrationPeriodRepository periodRepository;
    @Autowired DepartmentRepository departmentRepository;
    @Autowired LecturerRepository lecturerRepository;
    @Autowired UserRepository userRepository;
    @Autowired StudentRepository studentRepository;
    @Autowired TopicRepository topicRepository;
    @Autowired TopicSupervisorRepository supervisorRepository;
    @Autowired StudentGroupRepository groupRepository;
    @Autowired ReportRepository reportRepository;
    @Autowired RoleRepository roleRepository;

    private User giangVien, sv1, sv2;
    private Student s1, s2;
    private Department dept;
    private RegistrationPeriod openPeriod;
    private Topic approvedTopic;
    private StudentGroup approvedGroup;

    @BeforeEach
    void setUp() {
        long ts = System.nanoTime();
        Role gvRole = roleRepository.findByCode("GIANG_VIEN").orElseGet(() ->
            roleRepository.save(Role.builder().code("GIANG_VIEN").name("GV")
                .systemFlag(true).build()));

        dept = departmentRepository.save(Department.builder()
                .code("BM" + ts).name("BM test " + ts).status("ACTIVE").build());

        giangVien = userRepository.save(User.builder()
                .username("gv_r6_" + ts).password("{noop}x")
                .fullName("GV Báo cáo").email("gv_r6_" + ts + "@t.local")
                .status(UserStatus.ACTIVE).enabled(true)
                .roles(new HashSet<>(Set.of(gvRole))).build());
        Lecturer lec = lecturerRepository.save(Lecturer.builder()
                .user(giangVien).lecturerCode("L" + ts)
                .academicDegree("ThS").department(dept).build());

        sv1 = userRepository.save(User.builder()
                .username("sv1r6_" + ts).password("{noop}x")
                .fullName("SV 1").email("sv1r6_" + ts + "@t.local")
                .status(UserStatus.ACTIVE).enabled(true).build());
        s1 = studentRepository.save(Student.builder().user(sv1)
                .studentCode("S1_" + ts).className("CNTT-K16").major("CNTT").build());

        sv2 = userRepository.save(User.builder()
                .username("sv2r6_" + ts).password("{noop}x")
                .fullName("SV 2").email("sv2r6_" + ts + "@t.local")
                .status(UserStatus.ACTIVE).enabled(true).build());
        s2 = studentRepository.save(Student.builder().user(sv2)
                .studentCode("S2_" + ts).className("CNTT-K16").major("CNTT").build());

        // Đợt mở
        RegistrationPeriodForm pf = new RegistrationPeriodForm();
        pf.setName("Đợt R6 " + ts);
        pf.setPeriodType(PeriodType.TLCN);
        LocalDateTime now = LocalDateTime.now();
        pf.setLecturerStartAt(now.minusDays(14));
        pf.setLecturerEndAt(now);
        pf.setStudentStartAt(now);
        pf.setStudentEndAt(now.plusDays(7));
        pf.setReviewDeadline(now.plusDays(30));
        openPeriod = periodService.create(pf, giangVien);
        periodService.publish(openPeriod.getId(), giangVien);

        // Đề tài APPROVED + published
        long ts2 = System.nanoTime();
        approvedTopic = topicRepository.save(Topic.builder()
                .period(openPeriod).department(dept).createdByLecturer(lec)
                .topicCode("DT-R6-" + ts2)
                .topicName("Đề tài test nộp báo cáo GĐ6 dài hơn 10 ký tự")
                .description("Mô tả chi tiết đề tài dùng cho test nộp báo cáo, kiểm tra version tăng dần.")
                .status(TopicStatus.APPROVED)
                .approvedBy(giangVien).approvedAt(now)
                .published(true).build());
        supervisorRepository.save(TopicSupervisor.builder()
                .topic(approvedTopic).lecturer(lec).supervisorOrder(1).build());

        // Tạo nhóm + duyệt
        StudentGroupForm f = StudentGroupForm.builder()
                .periodId(openPeriod.getId())
                .groupCode("GR6_" + ts)
                .groupName("Nhóm R6")
                .build();
        approvedGroup = groupService.createDraft(f, sv1);
        TopicRegistration reg = registrationService.register(approvedGroup.getId(), approvedTopic.getId(), sv1);
        registrationService.approve(reg.getId(), giangVien);
        approvedGroup = groupService.getById(approvedGroup.getId());
    }

    private MockMultipartFile pdf(String name) {
        return new MockMultipartFile("file", name, "application/pdf",
                "%PDF-1.4 dummy content".getBytes());
    }

    private UploadReportForm form() {
        UploadReportForm f = new UploadReportForm();
        f.setReportTitle("Báo cáo lần 1");
        f.setDescription("Mô tả báo cáo");
        return f;
    }

    // ---- Happy path ----

    @Test
    void submit_byLeader_approvedGroup_success() {
        Report r = reportService.submit(approvedGroup.getId(), form(), pdf("bc1.pdf"), sv1);
        assertThat(r.getId()).isNotNull();
        assertThat(r.getVersion()).isEqualTo(1);
        assertThat(r.getStatus()).isEqualTo("SUBMITTED");
        // Nhóm chuyển SUBMITTED
        assertThat(groupService.getById(approvedGroup.getId()).getStatus()).isEqualTo(GroupStatus.SUBMITTED);
    }

    @Test
    void submit_secondVersion_marksOldSuperseded() {
        reportService.submit(approvedGroup.getId(), form(), pdf("bc1.pdf"), sv1);
        UploadReportForm f2 = new UploadReportForm();
        f2.setReportTitle("Báo cáo lần 2 sửa chữa");
        Report r2 = reportService.submit(approvedGroup.getId(), f2, pdf("bc2.pdf"), sv1);
        assertThat(r2.getVersion()).isEqualTo(2);
        // Bản cũ SUPERSEDED
        assertThat(reportRepository.findActiveByGroupId(approvedGroup.getId()))
                .extracting(Report::getVersion).containsExactly(2);
        assertThat(reportRepository.countByGroupId(approvedGroup.getId())).isEqualTo(2);
    }

    // ---- Rejected ----

    @Test
    void submit_byNonLeader_rejected() {
        assertThatThrownBy(() -> reportService.submit(approvedGroup.getId(), form(), pdf("x.pdf"), sv2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nhóm trưởng");
    }

    @Test
    void submit_groupNotApproved_rejected() {
        // Tạo nhóm mới chưa duyệt
        long ts = System.nanoTime();
        StudentGroupForm f = StudentGroupForm.builder()
                .periodId(openPeriod.getId())
                .groupCode("G_DRAFT_" + ts)
                .groupName("Nhóm DRAFT")
                .build();
        StudentGroup g = groupService.createDraft(f, sv2);
        assertThatThrownBy(() -> reportService.submit(g.getId(), form(), pdf("x.pdf"), sv2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("chưa được GVHD");
    }

    @Test
    void submit_invalidExtension_rejected() {
        assertThatThrownBy(() -> reportService.submit(approvedGroup.getId(), form(),
                new MockMultipartFile("file", "x.txt", "text/plain", "abc".getBytes()), sv1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("pdf");
    }

    @Test
    void submit_emptyFile_rejected() {
        assertThatThrownBy(() -> reportService.submit(approvedGroup.getId(), form(),
                new MockMultipartFile("file", "x.pdf", "application/pdf", new byte[0]), sv1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("chọn file");
    }
}
