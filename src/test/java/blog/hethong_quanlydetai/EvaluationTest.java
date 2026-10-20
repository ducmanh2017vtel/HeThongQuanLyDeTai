package blog.hethong_quanlydetai;

import blog.hethong_quanlydetai.domain.*;
import blog.hethong_quanlydetai.dto.CouncilForm;
import blog.hethong_quanlydetai.dto.EvaluationForm;
import blog.hethong_quanlydetai.dto.RegistrationPeriodForm;
import blog.hethong_quanlydetai.repository.*;
import blog.hethong_quanlydetai.service.CouncilService;
import blog.hethong_quanlydetai.service.EvaluationService;
import blog.hethong_quanlydetai.service.RegistrationPeriodService;
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
 * GĐ8 — Phân công chấm & nhập điểm (mục 5.7, A4).
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class EvaluationTest {

    @Autowired EvaluationService evaluationService;
    @Autowired CouncilService councilService;
    @Autowired RegistrationPeriodService periodService;

    @Autowired RegistrationPeriodRepository periodRepository;
    @Autowired DepartmentRepository departmentRepository;
    @Autowired LecturerRepository lecturerRepository;
    @Autowired UserRepository userRepository;
    @Autowired TopicRepository topicRepository;
    @Autowired ReviewAssignmentRepository assignmentRepository;
    @Autowired RoleRepository roleRepository;

    private User tk, lec1User, lec2User, lec3User;
    private Lecturer lec1, lec2, lec3;
    private Department dept;
    private RegistrationPeriod kltnPeriod;
    private Topic topic;
    private Council council;

    @BeforeEach
    void setUp() {
        long ts = System.nanoTime();
        Role gvRole = roleRepository.findByCode("GIANG_VIEN").orElseGet(() ->
            roleRepository.save(Role.builder().code("GIANG_VIEN").name("GV").systemFlag(true).build()));

        dept = departmentRepository.save(Department.builder()
                .code("BM8" + ts).name("BM " + ts).status("ACTIVE").build());

        tk = userRepository.save(User.builder()
                .username("tk8_" + ts).password("{noop}x")
                .fullName("Trưởng khoa").email("tk8_" + ts + "@t.local")
                .status(UserStatus.ACTIVE).enabled(true).build());

        lec1User = makeGV(ts, 1);
        lec2User = makeGV(ts, 2);
        lec3User = makeGV(ts, 3);
        lec1 = makeLecturer(lec1User, 1, ts);
        lec2 = makeLecturer(lec2User, 2, ts);
        lec3 = makeLecturer(lec3User, 3, ts);

        RegistrationPeriodForm pf = new RegistrationPeriodForm();
        pf.setName("Đợt 8 " + ts);
        pf.setPeriodType(PeriodType.KLTN);
        LocalDateTime now = LocalDateTime.now();
        pf.setLecturerStartAt(now.minusDays(30));
        pf.setLecturerEndAt(now.minusDays(15));
        pf.setStudentStartAt(now.minusDays(15));
        pf.setStudentEndAt(now);
        pf.setReviewDeadline(now.plusDays(1));
        pf.setCouncilReportDate(now.plusDays(7));
        kltnPeriod = periodService.create(pf, tk);
        periodService.publish(kltnPeriod.getId(), tk);

        topic = topicRepository.save(Topic.builder()
                .period(kltnPeriod).department(dept).createdByLecturer(lec1)
                .topicCode("DT8-" + ts).topicName("Đề tài test chấm điểm GĐ8 dài")
                .description("Mô tả chi tiết đề tài test chấm điểm GĐ8 của hệ thống.")
                .status(TopicStatus.APPROVED).approvedBy(tk).approvedAt(now).published(true).build());

        CouncilForm cf = CouncilForm.builder()
                .councilCode("HD8-" + ts)
                .councilName("HĐ 8 " + ts)
                .periodId(kltnPeriod.getId())
                .reportDate(now.plusDays(7))
                .lecturerIds(List.of(lec1.getId(), lec2.getId(), lec3.getId()))
                .roles(List.of("CHAIRMAN", "SECRETARY", "MEMBER"))
                .topicIds(List.of(topic.getId()))
                .build();
        council = councilService.create(cf, tk);
        councilService.markReady(council.getId(), tk);
    }

    private User makeGV(long ts, int n) {
        return userRepository.save(User.builder()
                .username("gv8_" + n + "_" + ts).password("{noop}x")
                .fullName("GV " + n).email("gv8_" + n + "_" + ts + "@t.local")
                .status(UserStatus.ACTIVE).enabled(true)
                .roles(new HashSet<>(Set.of(roleRepository.findByCode("GIANG_VIEN").orElseThrow())))
                .build());
    }
    private Lecturer makeLecturer(User u, int n, long ts) {
        return lecturerRepository.save(Lecturer.builder()
                .user(u).lecturerCode("L8" + n + "_" + ts)
                .academicDegree("ThS").department(dept).build());
    }

    // ---- Happy ----

    @Test
    void assign_andEvaluate_success() {
        ReviewAssignment a = evaluationService.assign(council.getId(), topic.getId(), lec2.getId(), tk);
        assertThat(a.getId()).isNotNull();

        EvaluationForm ef = new EvaluationForm();
        ef.setScore(new BigDecimal("7.50"));
        ef.setComment("Đề tài làm tốt, đầy đủ nội dung và có sản phẩm demo.");
        Evaluation e = evaluationService.submitEvaluation(a.getId(), ef, lec2User);
        assertThat(e.getScore()).isEqualByComparingTo("7.50");
        assertThat(e.getStatus()).isEqualTo("SUBMITTED");
        // Assignment → COMPLETED
        ReviewAssignment reloaded = assignmentRepository.findById(a.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo("COMPLETED");
    }

    @Test
    void averageScore_twoReviewers_correct() {
        ReviewAssignment a1 = evaluationService.assign(council.getId(), topic.getId(), lec2.getId(), tk);
        ReviewAssignment a2 = evaluationService.assign(council.getId(), topic.getId(), lec3.getId(), tk);
        EvaluationForm ef1 = new EvaluationForm();
        ef1.setScore(new BigDecimal("7.00"));
        ef1.setComment("Đề tài đạt yêu cầu cơ bản, có thể cải thiện thêm.");
        evaluationService.submitEvaluation(a1.getId(), ef1, lec2User);
        EvaluationForm ef2 = new EvaluationForm();
        ef2.setScore(new BigDecimal("8.00"));
        ef2.setComment("Đề tài tốt, đề xuất đầy đủ, sản phẩm hoàn thiện.");
        evaluationService.submitEvaluation(a2.getId(), ef2, lec3User);
        BigDecimal avg = evaluationService.averageScoreForTopic(topic.getId());
        assertThat(avg).isEqualByComparingTo("7.50");
    }

    // ---- Reject ----

    @Test
    void assign_duplicate_rejected() {
        evaluationService.assign(council.getId(), topic.getId(), lec2.getId(), tk);
        assertThatThrownBy(() -> evaluationService.assign(council.getId(), topic.getId(), lec2.getId(), tk))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("đã được phân công");
    }

    @Test
    void assign_topicNotInCouncil_rejected() {
        // Tạo đề tài thứ 2 không thuộc HĐ
        long ts = System.nanoTime();
        Topic t2 = topicRepository.save(Topic.builder()
                .period(kltnPeriod).department(dept).createdByLecturer(lec1)
                .topicCode("DT8B-" + ts).topicName("Đề tài ngoài hội đồng test GĐ8")
                .description("Mô tả đề tài không thuộc hội đồng nào để test phân công.")
                .status(TopicStatus.APPROVED).approvedBy(tk).approvedAt(LocalDateTime.now()).published(true).build());
        assertThatThrownBy(() -> evaluationService.assign(council.getId(), t2.getId(), lec2.getId(), tk))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("chưa được gán");
    }

    @Test
    void submitEvaluation_byNonAssignedLecturer_rejected() {
        ReviewAssignment a = evaluationService.assign(council.getId(), topic.getId(), lec2.getId(), tk);
        EvaluationForm ef = new EvaluationForm();
        ef.setScore(new BigDecimal("7.00"));
        ef.setComment("Nhận xét của GV không được phân công vẫn cố chấm.");
        assertThatThrownBy(() -> evaluationService.submitEvaluation(a.getId(), ef, lec3User))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("không được phân công");
    }

    @Test
    void submitEvaluation_shortComment_rejected() {
        ReviewAssignment a = evaluationService.assign(council.getId(), topic.getId(), lec2.getId(), tk);
        EvaluationForm ef = new EvaluationForm();
        ef.setScore(new BigDecimal("7.00"));
        ef.setComment("ngắn");
        assertThatThrownBy(() -> evaluationService.submitEvaluation(a.getId(), ef, lec2User))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("10 ký tự");
    }

    @Test
    void removeAssignment_afterEvaluation_rejected() {
        ReviewAssignment a = evaluationService.assign(council.getId(), topic.getId(), lec2.getId(), tk);
        EvaluationForm ef = new EvaluationForm();
        ef.setScore(new BigDecimal("7.00"));
        ef.setComment("Đề tài đạt yêu cầu, có thể cải thiện thêm phần demo.");
        evaluationService.submitEvaluation(a.getId(), ef, lec2User);
        assertThatThrownBy(() -> evaluationService.removeAssignment(a.getId(), tk))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Đã có điểm");
    }
}
