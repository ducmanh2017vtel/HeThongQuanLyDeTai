package blog.hethong_quanlydetai;

import blog.hethong_quanlydetai.domain.*;
import blog.hethong_quanlydetai.repository.*;
import blog.hethong_quanlydetai.service.DashboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

/**
 * GĐ11 — Dashboard theo vai trò & việc cần làm.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DashboardTest {

    @Autowired DashboardService dashboardService;
    @Autowired UserRepository userRepository;
    @Autowired RoleRepository roleRepository;
    @Autowired LecturerRepository lecturerRepository;
    @Autowired StudentRepository studentRepository;
    @Autowired DepartmentRepository departmentRepository;
    @Autowired NotificationRepository notificationRepository;
    @Autowired RegistrationPeriodRepository periodRepository;
    @Autowired TopicRepository topicRepository;
    @Autowired TopicSupervisorRepository supervisorRepository;
    @Autowired StudentGroupRepository groupRepository;

    @BeforeEach
    void setUp() {
        long ts = System.nanoTime();
        // Roles
        roleRepository.findByCode("GIANG_VIEN").orElseGet(() ->
            roleRepository.save(Role.builder().code("GIANG_VIEN").name("Giảng viên").systemFlag(true).build()));
        roleRepository.findByCode("SINH_VIEN").orElseGet(() ->
            roleRepository.save(Role.builder().code("SINH_VIEN").name("Sinh viên").systemFlag(true).build()));
        roleRepository.findByCode("TRUONG_KHOA").orElseGet(() ->
            roleRepository.save(Role.builder().code("TRUONG_KHOA").name("Trưởng khoa").systemFlag(true).build()));
        roleRepository.findByCode("CHU_TICH_HOI_DONG").orElseGet(() ->
            roleRepository.save(Role.builder().code("CHU_TICH_HOI_DONG").name("Chủ tịch HĐ").systemFlag(true).build()));

        // Department
        Department dept = departmentRepository.save(Department.builder()
                .code("D11" + ts).name("BM 11 " + ts).status("ACTIVE").build());

        // GV
        User gvUser = userRepository.save(User.builder()
                .username("gv11_" + ts).password("{noop}x")
                .fullName("GV 11").email("gv11_" + ts + "@t.local")
                .status(UserStatus.ACTIVE).enabled(true)
                .roles(new HashSet<>(Set.of(roleRepository.findByCode("GIANG_VIEN").orElseThrow())))
                .build());
        lecturerRepository.save(Lecturer.builder()
                .user(gvUser).lecturerCode("L11" + ts)
                .academicDegree("ThS").department(dept).build());

        // Period mở
        RegistrationPeriod p = new RegistrationPeriod();
        p.setName("Đợt 11 " + ts);
        p.setPeriodType(PeriodType.KLTN);
        p.setStatus(PeriodStatus.PUBLISHED);
        LocalDateTime now = LocalDateTime.now();
        p.setLecturerStartAt(now.minusDays(5));
        p.setLecturerEndAt(now.plusDays(5));
        p.setStudentStartAt(now.minusDays(2));
        p.setStudentEndAt(now.plusDays(10));
        p.setReviewDeadline(now.plusDays(20));
        p.setCouncilReportDate(now.plusDays(30));
        p.setCreatedBy(gvUser);
        p.setCreatedAt(now);
        p.setUpdatedAt(now);
        periodRepository.save(p);
    }

    @Test
    void summary_returnsCounts() {
        Map<String, Object> s = dashboardService.summary();
        assertThat(s).containsKeys("userCount", "topicCount", "periodCount");
        assertThat(s.get("userCount")).isInstanceOf(Long.class);
        assertThat(s.get("periodCount")).isInstanceOf(Long.class);
    }

    @Test
    void openPeriods_returnsPublishedWithinWindow() {
        List<RegistrationPeriod> periods = dashboardService.openPeriods();
        assertThat(periods).isNotEmpty();
        // Tất cả phải ở trạng thái PUBLISHED
        assertThat(periods).allMatch(p -> p.getStatus() == PeriodStatus.PUBLISHED);
    }

    @Test
    void todos_truongKhoa_seesPendingTopics() {
        // Tạo 1 đề tài PENDING
        Department dept = departmentRepository.findAll().get(0);
        Lecturer lec = lecturerRepository.findAll().get(0);
        RegistrationPeriod p = periodRepository.findAll().get(0);
        Topic pendingTopic = topicRepository.save(Topic.builder()
                .period(p).department(dept).createdByLecturer(lec)
                .topicCode("DT11A").topicName("Đề tài PENDING cho dashboard")
                .description("Mô tả đề tài PENDING để test dashboard")
                .status(TopicStatus.PENDING).build());

        List<Map<String, String>> todos = dashboardService.todoFor(0L, Set.of("TRUONG_KHOA"));
        assertThat(todos).extracting(td -> td.get("code")).contains("TOPICS_PENDING");
        Map<String, String> pendingTodo = todos.stream()
                .filter(td -> "TOPICS_PENDING".equals(td.get("code"))).findFirst().orElseThrow();
        assertThat(pendingTodo.get("count")).isEqualTo("1");
    }

    @Test
    void todos_giangVien_seesPendingRegistrationsWhenSupervisor() {
        long ts = System.nanoTime();
        Department dept = departmentRepository.findAll().get(0);
        Lecturer lec = lecturerRepository.findAll().get(0);
        User gv = lec.getUser();
        RegistrationPeriod p = periodRepository.findAll().get(0);

        // Tạo đề tài có GV này làm GVHD chính
        Topic t = topicRepository.save(Topic.builder()
                .period(p).department(dept).createdByLecturer(lec)
                .topicCode("DT11B" + ts).topicName("Đề tài dashboard GVHD")
                .description("Mô tả đề tài dashboard")
                .status(TopicStatus.APPROVED).published(true).build());
        supervisorRepository.save(TopicSupervisor.builder()
                .topic(t).lecturer(lec).supervisorOrder(1).build());

        // Tạo SV
        User sv = userRepository.save(User.builder()
                .username("sv11_" + ts).password("{noop}x")
                .fullName("SV 11").email("sv11_" + ts + "@t.local")
                .status(UserStatus.ACTIVE).enabled(true).build());
        studentRepository.save(Student.builder().user(sv)
                .studentCode("S11" + ts).className("CNTT").major("CNTT").build());

        // Tạo nhóm REGISTERED
        StudentGroup g = groupRepository.save(StudentGroup.builder()
                .groupCode("G11" + ts).groupName("Nhóm 11")
                .period(p).status(GroupStatus.REGISTERED).topic(t).build());

        List<Map<String, String>> todos = dashboardService.todoFor(gv.getId(), Set.of("GIANG_VIEN"));
        boolean hasPendingReg = todos.stream()
                .anyMatch(td -> "REGISTRATIONS_PENDING".equals(td.get("code")));
        assertThat(hasPendingReg).isTrue();
    }

    @Test
    void latestNotices_publishedAreVisible() {
        long ts = System.nanoTime();
        User admin = userRepository.save(User.builder()
                .username("admin11_" + ts).password("{noop}x")
                .fullName("Admin 11").email("admin11_" + ts + "@t.local")
                .status(UserStatus.ACTIVE).enabled(true).build());

        Notification n = Notification.builder()
                .title("Thông báo dashboard test")
                .content("Nội dung thông báo dashboard test cho GĐ11")
                .type(NotificationType.FACULTY)
                .status(NotificationStatus.PUBLISHED)
                .createdBy(admin)
                .publishedAt(LocalDateTime.now())
                .build();
        notificationRepository.save(n);

        List<Notification> notices = dashboardService.latestNotices(Set.of("GIANG_VIEN", "SINH_VIEN", "TRUONG_KHOA"));
        assertThat(notices).extracting(Notification::getId).contains(n.getId());
    }

    @Test
    void latestNotices_hiddenNotShown() {
        long ts = System.nanoTime();
        User admin = userRepository.save(User.builder()
                .username("admin11b_" + ts).password("{noop}x")
                .fullName("Admin 11b").email("admin11b_" + ts + "@t.local")
                .status(UserStatus.ACTIVE).enabled(true).build());

        Notification hidden = Notification.builder()
                .title("Thông báo ẩn")
                .content("Ẩn rồi")
                .type(NotificationType.FACULTY)
                .status(NotificationStatus.HIDDEN)
                .createdBy(admin)
                .build();
        notificationRepository.save(hidden);

        List<Notification> notices = dashboardService.latestNotices(Set.of("GIANG_VIEN"));
        assertThat(notices).extracting(Notification::getId).doesNotContain(hidden.getId());
    }
}
