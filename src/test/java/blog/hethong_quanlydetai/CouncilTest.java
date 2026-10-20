package blog.hethong_quanlydetai;

import blog.hethong_quanlydetai.domain.*;
import blog.hethong_quanlydetai.dto.CouncilForm;
import blog.hethong_quanlydetai.dto.RegistrationPeriodForm;
import blog.hethong_quanlydetai.repository.*;
import blog.hethong_quanlydetai.service.CouncilService;
import blog.hethong_quanlydetai.service.RegistrationPeriodService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

/**
 * GĐ7 — Hội đồng (mục 5.6, A3).
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CouncilTest {

    @Autowired CouncilService councilService;
    @Autowired RegistrationPeriodService periodService;
    @Autowired RegistrationPeriodRepository periodRepository;
    @Autowired DepartmentRepository departmentRepository;
    @Autowired LecturerRepository lecturerRepository;
    @Autowired UserRepository userRepository;
    @Autowired TopicRepository topicRepository;
    @Autowired RoleRepository roleRepository;

    private User truongKhoa, lec1User, lec2User, lec3User, lec4User;
    private Lecturer lec1, lec2, lec3, lec4;
    private Department dept;
    private RegistrationPeriod kltnPeriod;
    private Topic topic1, topic2;

    @BeforeEach
    void setUp() {
        long ts = System.nanoTime();
        Role gvRole = roleRepository.findByCode("GIANG_VIEN").orElseGet(() ->
            roleRepository.save(Role.builder().code("GIANG_VIEN").name("GV").systemFlag(true).build()));

        dept = departmentRepository.save(Department.builder()
                .code("BM7" + ts).name("BM " + ts).status("ACTIVE").build());

        truongKhoa = userRepository.save(User.builder()
                .username("tk7_" + ts).password("{noop}x")
                .fullName("Trưởng khoa").email("tk7_" + ts + "@t.local")
                .status(UserStatus.ACTIVE).enabled(true).build());

        lec1User = makeGV(ts, 1);
        lec2User = makeGV(ts, 2);
        lec3User = makeGV(ts, 3);
        lec4User = makeGV(ts, 4);
        lec1 = makeLecturer(lec1User, 1, ts);
        lec2 = makeLecturer(lec2User, 2, ts);
        lec3 = makeLecturer(lec3User, 3, ts);
        lec4 = makeLecturer(lec4User, 4, ts);

        // Đợt KLTN
        RegistrationPeriodForm pf = new RegistrationPeriodForm();
        pf.setName("KLTN " + ts);
        pf.setPeriodType(PeriodType.KLTN);
        LocalDateTime now = LocalDateTime.now();
        pf.setLecturerStartAt(now.minusDays(30));
        pf.setLecturerEndAt(now.minusDays(15));
        pf.setStudentStartAt(now.minusDays(15));
        pf.setStudentEndAt(now);
        pf.setReviewDeadline(now.plusDays(1));
        pf.setCouncilReportDate(now.plusDays(7));
        kltnPeriod = periodService.create(pf, truongKhoa);
        periodService.publish(kltnPeriod.getId(), truongKhoa);

        // 2 đề tài APPROVED
        topic1 = topicRepository.save(Topic.builder()
                .period(kltnPeriod).department(dept).createdByLecturer(lec1)
                .topicCode("DT7A-" + ts).topicName("Đề tài KLTN 1 dài hơn 10 ký tự")
                .description("Mô tả chi tiết đề tài KLTN 1 dùng cho test hội đồng GĐ7.")
                .status(TopicStatus.APPROVED).approvedBy(truongKhoa).approvedAt(now).published(true).build());
        topic2 = topicRepository.save(Topic.builder()
                .period(kltnPeriod).department(dept).createdByLecturer(lec1)
                .topicCode("DT7B-" + ts).topicName("Đề tài KLTN 2 dài hơn 10 ký tự")
                .description("Mô tả chi tiết đề tài KLTN 2 dùng cho test hội đồng GĐ7.")
                .status(TopicStatus.APPROVED).approvedBy(truongKhoa).approvedAt(now).published(true).build());
    }

    private User makeGV(long ts, int n) {
        return userRepository.save(User.builder()
                .username("gv7_" + n + "_" + ts).password("{noop}x")
                .fullName("GV " + n).email("gv7_" + n + "_" + ts + "@t.local")
                .status(UserStatus.ACTIVE).enabled(true)
                .roles(new HashSet<>(Set.of(roleRepository.findByCode("GIANG_VIEN").orElseThrow())))
                .build());
    }
    private Lecturer makeLecturer(User u, int n, long ts) {
        return lecturerRepository.save(Lecturer.builder()
                .user(u).lecturerCode("L7" + n + "_" + ts)
                .academicDegree("ThS").department(dept).build());
    }

    private CouncilForm validForm() {
        return CouncilForm.builder()
                .councilCode("HD-" + System.nanoTime())
                .councilName("Hội đồng test")
                .periodId(kltnPeriod.getId())
                .reportDate(LocalDateTime.now().plusDays(5))
                .lecturerIds(List.of(lec1.getId(), lec2.getId(), lec3.getId()))
                .roles(List.of("CHAIRMAN", "SECRETARY", "MEMBER"))
                .topicIds(List.of(topic1.getId()))
                .build();
    }

    // ---- Happy ----

    @Test
    void create_withValidData_success() {
        Council c = councilService.create(validForm(), truongKhoa);
        assertThat(c.getId()).isNotNull();
        assertThat(c.getStatus()).isEqualTo(CouncilStatus.DRAFT);
        assertThat(c.getMembers()).hasSize(3);
    }

    @Test
    void markReady_with3MembersAnd1Topic_success() {
        Council c = councilService.create(validForm(), truongKhoa);
        Council ready = councilService.markReady(c.getId(), truongKhoa);
        assertThat(ready.getStatus()).isEqualTo(CouncilStatus.READY);
    }

    @Test
    void finish_afterReady_success() {
        Council c = councilService.create(validForm(), truongKhoa);
        councilService.markReady(c.getId(), truongKhoa);
        Council done = councilService.markFinished(c.getId(), truongKhoa);
        assertThat(done.getStatus()).isEqualTo(CouncilStatus.FINISHED);
    }

    // ---- Reject ----

    @Test
    void create_noChairman_rejected() {
        CouncilForm f = validForm();
        f.setRoles(List.of("SECRETARY", "MEMBER", "MEMBER"));
        assertThatThrownBy(() -> councilService.create(f, truongKhoa))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Chủ tịch");
    }

    @Test
    void create_twoChairmen_rejected() {
        CouncilForm f = validForm();
        f.setRoles(List.of("CHAIRMAN", "CHAIRMAN", "MEMBER"));
        assertThatThrownBy(() -> councilService.create(f, truongKhoa))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Chủ tịch");
    }

    @Test
    void create_tooFewMembers_rejected() {
        CouncilForm f = validForm();
        f.setLecturerIds(List.of(lec1.getId(), lec2.getId()));
        f.setRoles(List.of("CHAIRMAN", "MEMBER"));
        assertThatThrownBy(() -> councilService.create(f, truongKhoa))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("3..5");
    }

    @Test
    void create_duplicateLecturer_rejected() {
        CouncilForm f = validForm();
        // CHAIRMAN + SECRETARY + MEMBER nhưng MEMBER trùng CHAIRMAN
        f.setLecturerIds(List.of(lec1.getId(), lec2.getId(), lec1.getId()));
        f.setRoles(List.of("CHAIRMAN", "SECRETARY", "MEMBER"));
        assertThatThrownBy(() -> councilService.create(f, truongKhoa))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("trùng");
    }

    @Test
    void create_duplicateCouncilCode_rejected() {
        CouncilForm f = validForm();
        councilService.create(f, truongKhoa);
        assertThatThrownBy(() -> councilService.create(f, truongKhoa))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Mã hội đồng");
    }

    @Test
    void topicAlreadyAssigned_rejected() {
        CouncilForm f1 = validForm();
        councilService.create(f1, truongKhoa);

        CouncilForm f2 = CouncilForm.builder()
                .councilCode("HD2-" + System.nanoTime())
                .councilName("Hội đồng 2")
                .periodId(kltnPeriod.getId())
                .reportDate(LocalDateTime.now().plusDays(5))
                .lecturerIds(List.of(lec1.getId(), lec2.getId(), lec3.getId()))
                .roles(List.of("CHAIRMAN", "SECRETARY", "MEMBER"))
                .topicIds(List.of(topic1.getId())) // đã gán ở f1
                .build();
        assertThatThrownBy(() -> councilService.create(f2, truongKhoa))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("gán cho hội đồng khác");
    }

    @Test
    void markReady_noTopic_rejected() {
        CouncilForm f = validForm();
        f.setTopicIds(List.of());
        Council c = councilService.create(f, truongKhoa);
        assertThatThrownBy(() -> councilService.markReady(c.getId(), truongKhoa))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("1 đề tài");
    }
}
