package blog.hethong_quanlydetai;

import blog.hethong_quanlydetai.domain.*;
import blog.hethong_quanlydetai.dto.NotificationForm;
import blog.hethong_quanlydetai.repository.*;
import blog.hethong_quanlydetai.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

/**
 * GĐ10 — Thông báo (mục 5.9, A5).
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NotificationTest {

    @Autowired NotificationService notificationService;
    @Autowired NotificationRepository notificationRepository;
    @Autowired RoleRepository roleRepository;
    @Autowired UserRepository userRepository;

    private User admin, gv, sv;
    private Role roleGV, roleSV;

    @BeforeEach
    void setUp() {
        long ts = System.nanoTime();
        roleRepository.findByCode("GIANG_VIEN").orElseGet(() ->
            roleRepository.save(Role.builder()
                    .code("GIANG_VIEN").name("Giảng viên").systemFlag(true).build()));
        roleRepository.findByCode("SINH_VIEN").orElseGet(() ->
            roleRepository.save(Role.builder()
                    .code("SINH_VIEN").name("Sinh viên").systemFlag(true).build()));
        roleRepository.findByCode("TRUONG_KHOA").orElseGet(() ->
            roleRepository.save(Role.builder()
                    .code("TRUONG_KHOA").name("Trưởng khoa").systemFlag(true).build()));

        admin = userRepository.save(User.builder()
                .username("admin10_" + ts).password("{noop}x")
                .fullName("Admin 10").email("admin10_" + ts + "@t.local")
                .status(UserStatus.ACTIVE).enabled(true).build());
        gv = userRepository.save(User.builder()
                .username("gv10_" + ts).password("{noop}x")
                .fullName("GV 10").email("gv10_" + ts + "@t.local")
                .status(UserStatus.ACTIVE).enabled(true)
                .roles(new HashSet<>(Set.of(roleRepository.findByCode("GIANG_VIEN").orElseThrow())))
                .build());
        sv = userRepository.save(User.builder()
                .username("sv10_" + ts).password("{noop}x")
                .fullName("SV 10").email("sv10_" + ts + "@t.local")
                .status(UserStatus.ACTIVE).enabled(true)
                .roles(new HashSet<>(Set.of(roleRepository.findByCode("SINH_VIEN").orElseThrow())))
                .build());
        roleGV = roleRepository.findByCode("GIANG_VIEN").orElseThrow();
        roleSV = roleRepository.findByCode("SINH_VIEN").orElseThrow();
    }

    @Test
    void create_publish_visibleToTargetRoles() {
        NotificationForm form = NotificationForm.builder()
                .title("Lịch bảo vệ KLTN")
                .content("Hội đồng bảo vệ diễn ra từ 20/10/2026 đến 30/10/2026.")
                .type(NotificationType.FACULTY)
                .targetRoleCodes(new HashSet<>(Set.of("GIANG_VIEN", "SINH_VIEN")))
                .build();
        Notification n = notificationService.create(form, admin);
        assertThat(n.getId()).isNotNull();
        assertThat(n.getStatus()).isEqualTo(NotificationStatus.DRAFT);

        notificationService.publish(n.getId(), admin);
        Notification reloaded = notificationService.getById(n.getId());
        assertThat(reloaded.getStatus()).isEqualTo(NotificationStatus.PUBLISHED);
        assertThat(reloaded.getPublishedAt()).isNotNull();

        // GV & SV đều thấy
        List<Notification> gvView = notificationService.findPublishedForRoles(Set.of("GIANG_VIEN"));
        List<Notification> svView = notificationService.findPublishedForRoles(Set.of("SINH_VIEN"));
        assertThat(gvView).extracting(Notification::getId).contains(n.getId());
        assertThat(svView).extracting(Notification::getId).contains(n.getId());

        // Role khác (giả sử TRUONG_KHOA) nếu không nằm trong target thì vẫn thấy vì 1 thông báo có thể gửi cho 2 role
        // Ở test này targetRoles rỗng thì gửi cho tất cả - test riêng case dưới
    }

    @Test
    void publish_targetByRole_otherRoleNotVisible() {
        NotificationForm form = NotificationForm.builder()
                .title("Thông báo chỉ dành cho GV")
                .content("Thông báo nội bộ giảng viên")
                .type(NotificationType.SCHOOL)
                .targetRoleCodes(new HashSet<>(Set.of("GIANG_VIEN")))
                .build();
        Notification n = notificationService.create(form, admin);
        notificationService.publish(n.getId(), admin);

        List<Notification> gvView = notificationService.findPublishedForRoles(Set.of("GIANG_VIEN"));
        List<Notification> svView = notificationService.findPublishedForRoles(Set.of("SINH_VIEN"));
        assertThat(gvView).extracting(Notification::getId).contains(n.getId());
        assertThat(svView).extracting(Notification::getId).doesNotContain(n.getId());
    }

    @Test
    void create_emptyTargetRoles_meansAllRoles() {
        NotificationForm form = NotificationForm.builder()
                .title("Thông báo chung")
                .content("Thông báo gửi tới tất cả vai trò")
                .type(NotificationType.FACULTY)
                .targetRoleCodes(new HashSet<>())
                .build();
        Notification n = notificationService.create(form, admin);
        notificationService.publish(n.getId(), admin);

        // Bất kỳ role nào cũng thấy
        assertThat(notificationService.findPublishedForRoles(Set.of("GIANG_VIEN")))
                .extracting(Notification::getId).contains(n.getId());
        assertThat(notificationService.findPublishedForRoles(Set.of("SINH_VIEN")))
                .extracting(Notification::getId).contains(n.getId());
        assertThat(notificationService.findPublishedForRoles(Set.of("TRUONG_KHOA")))
                .extracting(Notification::getId).contains(n.getId());
    }

    @Test
    void publish_twice_rejected() {
        NotificationForm form = NotificationForm.builder()
                .title("Test publish 2 lần")
                .content("Nội dung test")
                .type(NotificationType.FACULTY)
                .targetRoleCodes(new HashSet<>(Set.of("GIANG_VIEN")))
                .build();
        Notification n = notificationService.create(form, admin);
        notificationService.publish(n.getId(), admin);
        assertThatThrownBy(() -> notificationService.publish(n.getId(), admin))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("trước đó");
    }

    @Test
    void hide_thenRepublish_rejected() {
        NotificationForm form = NotificationForm.builder()
                .title("Test hide + republish")
                .content("Nội dung test")
                .type(NotificationType.FACULTY)
                .targetRoleCodes(new HashSet<>(Set.of("GIANG_VIEN")))
                .build();
        Notification n = notificationService.create(form, admin);
        notificationService.publish(n.getId(), admin);
        notificationService.hide(n.getId(), admin);
        assertThatThrownBy(() -> notificationService.publish(n.getId(), admin))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ẩn");
    }

    @Test
    void update_publishedRejected() {
        NotificationForm form = NotificationForm.builder()
                .title("Test update published")
                .content("Nội dung test")
                .type(NotificationType.FACULTY)
                .targetRoleCodes(new HashSet<>(Set.of("GIANG_VIEN")))
                .build();
        Notification n = notificationService.create(form, admin);
        notificationService.publish(n.getId(), admin);
        assertThatThrownBy(() -> notificationService.update(n.getId(), form, admin))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("đã công bố");
    }

    @Test
    void delete_publishedRejected() {
        NotificationForm form = NotificationForm.builder()
                .title("Test delete published")
                .content("Nội dung test")
                .type(NotificationType.FACULTY)
                .targetRoleCodes(new HashSet<>(Set.of("GIANG_VIEN")))
                .build();
        Notification n = notificationService.create(form, admin);
        notificationService.publish(n.getId(), admin);
        assertThatThrownBy(() -> notificationService.delete(n.getId(), admin))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("công bố");
    }

    @Test
    void delete_draft_ok() {
        NotificationForm form = NotificationForm.builder()
                .title("Test delete draft")
                .content("Nội dung test")
                .type(NotificationType.FACULTY)
                .targetRoleCodes(new HashSet<>(Set.of("GIANG_VIEN")))
                .build();
        Notification n = notificationService.create(form, admin);
        notificationService.delete(n.getId(), admin);
        assertThatThrownBy(() -> notificationService.getById(n.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Không tìm thấy");
    }

    @Test
    void validation_emptyTitleRejected() {
        NotificationForm form = NotificationForm.builder()
                .title("")
                .content("Nội dung")
                .type(NotificationType.FACULTY)
                .build();
        assertThatThrownBy(() -> notificationService.create(form, admin))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Tiêu đề");
    }

    @Test
    void validation_emptyContentRejected() {
        NotificationForm form = NotificationForm.builder()
                .title("Tiêu đề")
                .content("")
                .type(NotificationType.FACULTY)
                .build();
        assertThatThrownBy(() -> notificationService.create(form, admin))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Nội dung");
    }
}
