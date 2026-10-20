package blog.hethong_quanlydetai;

import blog.hethong_quanlydetai.domain.*;
import blog.hethong_quanlydetai.dto.CouncilForm;
import blog.hethong_quanlydetai.dto.EvaluationForm;
import blog.hethong_quanlydetai.dto.RegistrationPeriodForm;
import blog.hethong_quanlydetai.dto.StudentGroupForm;
import blog.hethong_quanlydetai.repository.*;
import blog.hethong_quanlydetai.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

/**
 * GĐ9 — Tổng hợp & công bố kết quả (mục 5.8, A4).
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ResultTest {

    @Autowired ResultService resultService;
    @Autowired EvaluationService evaluationService;
    @Autowired CouncilService councilService;
    @Autowired RegistrationService registrationService;
    @Autowired StudentGroupService groupService;
    @Autowired RegistrationPeriodService periodService;

    @Autowired RegistrationPeriodRepository periodRepository;
    @Autowired DepartmentRepository departmentRepository;
    @Autowired LecturerRepository lecturerRepository;
    @Autowired UserRepository userRepository;
    @Autowired StudentRepository studentRepository;
    @Autowired TopicRepository topicRepository;
    @Autowired TopicSupervisorRepository supervisorRepository;
    @Autowired RoleRepository roleRepository;

    private User tk, lec1User, lec2User, lec3User, sv1, sv2;
    private Lecturer lec1, lec2, lec3;
    private Department dept;
    private RegistrationPeriod kltnPeriod;
    private Topic topic;
    private Council council;
    private StudentGroup group;

    @BeforeEach
    void setUp() {
        long ts = System.nanoTime();
        Role gvRole = roleRepository.findByCode("GIANG_VIEN").orElseGet(() ->
            roleRepository.save(Role.builder().code("GIANG_VIEN").name("GV").systemFlag(true).build()));

        dept = departmentRepository.save(Department.builder()
                .code("BM9" + ts).name("BM " + ts).status("ACTIVE").build());

        tk = userRepository.save(User.builder()
                .username("tk9_" + ts).password("{noop}x")
                .fullName("Trưởng khoa").email("tk9_" + ts + "@t.local")
                .status(UserStatus.ACTIVE).enabled(true).build());

        lec1User = makeGV(ts, 1);
        lec2User = makeGV(ts, 2);
        lec3User = makeGV(ts, 3);
        lec1 = makeLecturer(lec1User, 1, ts);
        lec2 = makeLecturer(lec2User, 2, ts);
        lec3 = makeLecturer(lec3User, 3, ts);

        sv1 = userRepository.save(User.builder()
                .username("sv9_" + ts).password("{noop}x")
                .fullName("SV 9").email("sv9_" + ts + "@t.local")
                .status(UserStatus.ACTIVE).enabled(true).build());
        studentRepository.save(Student.builder().user(sv1)
                .studentCode("S9_" + ts).className("CNTT-K17").major("CNTT").build());

        sv2 = userRepository.save(User.builder()
                .username("sv9b_" + ts).password("{noop}x")
                .fullName("SV 9B").email("sv9b_" + ts + "@t.local")
                .status(UserStatus.ACTIVE).enabled(true).build());
        studentRepository.save(Student.builder().user(sv2)
                .studentCode("S9B_" + ts).className("CNTT-K17").major("CNTT").build());

        RegistrationPeriodForm pf = new RegistrationPeriodForm();
        pf.setName("Đợt 9 " + ts);
        pf.setPeriodType(PeriodType.KLTN);
        LocalDateTime now = LocalDateTime.now();
        pf.setLecturerStartAt(now.minusDays(30));
        pf.setLecturerEndAt(now.minusDays(1));
        pf.setStudentStartAt(now.minusDays(1));
        pf.setStudentEndAt(now.plusDays(1));
        pf.setReviewDeadline(now.plusDays(2));
        pf.setCouncilReportDate(now.plusDays(7));
        kltnPeriod = periodService.create(pf, tk);
        periodService.publish(kltnPeriod.getId(), tk);

        topic = topicRepository.save(Topic.builder()
                .period(kltnPeriod).department(dept).createdByLecturer(lec1)
                .topicCode("DT9-" + ts).topicName("Đề tài test tổng hợp kết quả GĐ9")
                .description("Mô tả chi tiết đề tài test tổng hợp kết quả của nhóm GĐ9.")
                .status(TopicStatus.APPROVED).approvedBy(tk).approvedAt(now).published(true).build());
        supervisorRepository.save(TopicSupervisor.builder()
                .topic(topic).lecturer(lec1).supervisorOrder(1).build());

        CouncilForm cf = CouncilForm.builder()
                .councilCode("HD9-" + ts)
                .councilName("HĐ 9 " + ts)
                .periodId(kltnPeriod.getId())
                .reportDate(now.plusDays(7))
                .lecturerIds(List.of(lec1.getId(), lec2.getId(), lec3.getId()))
                .roles(List.of("CHAIRMAN", "SECRETARY", "MEMBER"))
                .topicIds(List.of(topic.getId()))
                .build();
        council = councilService.create(cf, tk);
        councilService.markReady(council.getId(), tk);

        // Tạo nhóm
        StudentGroupForm gf = StudentGroupForm.builder()
                .periodId(kltnPeriod.getId())
                .groupCode("G9_" + ts)
                .groupName("Nhóm 9")
                .build();
        group = groupService.createDraft(gf, sv1);
        TopicRegistration reg = registrationService.register(group.getId(), topic.getId(), sv1);
        registrationService.approve(reg.getId(), lec1User);

        // Phân công + chấm
        ReviewAssignment a1 = evaluationService.assign(council.getId(), topic.getId(), lec2.getId(), tk);
        ReviewAssignment a2 = evaluationService.assign(council.getId(), topic.getId(), lec3.getId(), tk);
        EvaluationForm ef1 = new EvaluationForm();
        ef1.setScore(new BigDecimal("7.00"));
        ef1.setComment("Đề tài đạt yêu cầu, có thể cải thiện thêm.");
        evaluationService.submitEvaluation(a1.getId(), ef1, lec2User);
        EvaluationForm ef2 = new EvaluationForm();
        ef2.setScore(new BigDecimal("8.00"));
        ef2.setComment("Đề tài tốt, sản phẩm hoàn thiện và có demo.");
        evaluationService.submitEvaluation(a2.getId(), ef2, lec3User);

        // Hoàn tất HĐ
        councilService.markFinished(council.getId(), tk);
    }

    private User makeGV(long ts, int n) {
        return userRepository.save(User.builder()
                .username("gv9_" + n + "_" + ts).password("{noop}x")
                .fullName("GV " + n).email("gv9_" + n + "_" + ts + "@t.local")
                .status(UserStatus.ACTIVE).enabled(true)
                .roles(new HashSet<>(Set.of(roleRepository.findByCode("GIANG_VIEN").orElseThrow())))
                .build());
    }
    private Lecturer makeLecturer(User u, int n, long ts) {
        return lecturerRepository.save(Lecturer.builder()
                .user(u).lecturerCode("L9" + n + "_" + ts)
                .academicDegree("ThS").department(dept).build());
    }

    @Test
    void calculate_averageScore_correct() {
        ReviewResult r = resultService.calculate(topic.getId(), tk);
        assertThat(r.getFinalScore()).isEqualByComparingTo("7.50");
        assertThat(r.getResult()).isEqualTo(ReviewResultStatus.PASSED);
    }

    @Test
    void calculate_failedScore_marksFailed() {
        // Giảm điểm: cần thay đổi evaluation → tạo topic khác với điểm thấp
        long ts = System.nanoTime();
        Topic t2 = topicRepository.save(Topic.builder()
                .period(kltnPeriod).department(dept).createdByLecturer(lec1)
                .topicCode("DT9B-" + ts).topicName("Đề tài thấp điểm test công bố GĐ9")
                .description("Mô tả chi tiết đề tài thấp điểm test nghiệp vụ tổng hợp.")
                .status(TopicStatus.APPROVED).approvedBy(tk).approvedAt(LocalDateTime.now()).published(true).build());
        supervisorRepository.save(TopicSupervisor.builder()
                .topic(t2).lecturer(lec1).supervisorOrder(1).build());
        CouncilForm cf2 = CouncilForm.builder()
                .councilCode("HD9B-" + ts).councilName("HĐ 9B").periodId(kltnPeriod.getId())
                .reportDate(LocalDateTime.now().plusDays(7))
                .lecturerIds(List.of(lec1.getId(), lec2.getId(), lec3.getId()))
                .roles(List.of("CHAIRMAN", "SECRETARY", "MEMBER"))
                .topicIds(List.of(t2.getId())).build();
        Council c2 = councilService.create(cf2, tk);
        councilService.markReady(c2.getId(), tk);

        StudentGroupForm gf2 = StudentGroupForm.builder()
                .periodId(kltnPeriod.getId()).groupCode("G9B_" + ts).groupName("Nhóm 9B").build();
        StudentGroup g2 = groupService.createDraft(gf2, sv2);
        TopicRegistration reg2 = registrationService.register(g2.getId(), t2.getId(), sv2);
        registrationService.approve(reg2.getId(), lec1User);

        ReviewAssignment a = evaluationService.assign(c2.getId(), t2.getId(), lec2.getId(), tk);
        EvaluationForm ef = new EvaluationForm();
        ef.setScore(new BigDecimal("3.00"));
        ef.setComment("Đề tài chưa đạt yêu cầu, cần cải thiện nhiều mặt.");
        evaluationService.submitEvaluation(a.getId(), ef, lec2User);
        councilService.markFinished(c2.getId(), tk);

        ReviewResult r2 = resultService.calculate(t2.getId(), tk);
        assertThat(r2.getFinalScore()).isEqualByComparingTo("3.00");
        assertThat(r2.getResult()).isEqualTo(ReviewResultStatus.FAILED);
    }

    @Test
    void calculate_councilNotFinished_rejected() {
        // Tạo HĐ mới chưa FINISHED
        long ts = System.nanoTime();
        Topic t2 = topicRepository.save(Topic.builder()
                .period(kltnPeriod).department(dept).createdByLecturer(lec1)
                .topicCode("DT9C-" + ts).topicName("Đề tài HĐ chưa xong test GĐ9")
                .description("Mô tả đề tài HĐ chưa xong test nghiệp vụ tổng hợp GĐ9.")
                .status(TopicStatus.APPROVED).approvedBy(tk).approvedAt(LocalDateTime.now()).published(true).build());
        supervisorRepository.save(TopicSupervisor.builder()
                .topic(t2).lecturer(lec1).supervisorOrder(1).build());
        CouncilForm cf2 = CouncilForm.builder()
                .councilCode("HD9C-" + ts).councilName("HĐ 9C").periodId(kltnPeriod.getId())
                .reportDate(LocalDateTime.now().plusDays(7))
                .lecturerIds(List.of(lec1.getId(), lec2.getId(), lec3.getId()))
                .roles(List.of("CHAIRMAN", "SECRETARY", "MEMBER"))
                .topicIds(List.of(t2.getId())).build();
        Council c2 = councilService.create(cf2, tk);
        councilService.markReady(c2.getId(), tk);
        // Không markFinished
        assertThatThrownBy(() -> resultService.calculate(t2.getId(), tk))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("chưa hoàn tất");
    }

    @Test
    void publish_andViewByStudent_success() {
        ReviewResult r = resultService.calculate(topic.getId(), tk);
        assertThat(r.getPublished()).isFalse();
        resultService.publish(r.getId(), tk);
        ReviewResult reloaded = resultService.getById(r.getId());
        assertThat(reloaded.getPublished()).isTrue();
        assertThat(reloaded.getPublishedAt()).isNotNull();
        // Nhóm chuyển COMPLETED
        StudentGroup g = groupService.getById(group.getId());
        assertThat(g.getStatus()).isEqualTo(GroupStatus.COMPLETED);
    }

    @Test
    void publish_twice_rejected() {
        ReviewResult r = resultService.calculate(topic.getId(), tk);
        resultService.publish(r.getId(), tk);
        assertThatThrownBy(() -> resultService.publish(r.getId(), tk))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("trước đó");
    }

    @Test
    void calculate_assignmentIncomplete_rejected() {
        // Tạo thêm 1 đề tài với assignment chưa chấm
        long ts = System.nanoTime();
        Topic t2 = topicRepository.save(Topic.builder()
                .period(kltnPeriod).department(dept).createdByLecturer(lec1)
                .topicCode("DT9D-" + ts).topicName("Đề tài chưa chấm xong test GĐ9")
                .description("Mô tả đề tài có phân công chưa chấm test nghiệp vụ GĐ9.")
                .status(TopicStatus.APPROVED).approvedBy(tk).approvedAt(LocalDateTime.now()).published(true).build());
        supervisorRepository.save(TopicSupervisor.builder()
                .topic(t2).lecturer(lec1).supervisorOrder(1).build());
        CouncilForm cf2 = CouncilForm.builder()
                .councilCode("HD9D-" + ts).councilName("HĐ 9D").periodId(kltnPeriod.getId())
                .reportDate(LocalDateTime.now().plusDays(7))
                .lecturerIds(List.of(lec1.getId(), lec2.getId(), lec3.getId()))
                .roles(List.of("CHAIRMAN", "SECRETARY", "MEMBER"))
                .topicIds(List.of(t2.getId())).build();
        Council c2 = councilService.create(cf2, tk);
        councilService.markReady(c2.getId(), tk);

        StudentGroupForm gf2 = StudentGroupForm.builder()
                .periodId(kltnPeriod.getId()).groupCode("G9D_" + ts).groupName("Nhóm 9D").build();
        StudentGroup g2 = groupService.createDraft(gf2, sv2);
        TopicRegistration reg2 = registrationService.register(g2.getId(), t2.getId(), sv2);
        registrationService.approve(reg2.getId(), lec1User);

        // Phân công nhưng chưa chấm
        evaluationService.assign(c2.getId(), t2.getId(), lec2.getId(), tk);
        councilService.markFinished(c2.getId(), tk);
        assertThatThrownBy(() -> resultService.calculate(t2.getId(), tk))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("chưa chấm");
    }
}
