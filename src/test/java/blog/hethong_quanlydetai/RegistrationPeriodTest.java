package blog.hethong_quanlydetai;

import blog.hethong_quanlydetai.domain.PeriodStatus;
import blog.hethong_quanlydetai.domain.PeriodType;
import blog.hethong_quanlydetai.domain.RegistrationPeriod;
import blog.hethong_quanlydetai.domain.Role;
import blog.hethong_quanlydetai.domain.User;
import blog.hethong_quanlydetai.domain.UserStatus;
import blog.hethong_quanlydetai.dto.RegistrationPeriodForm;
import blog.hethong_quanlydetai.repository.RegistrationPeriodRepository;
import blog.hethong_quanlydetai.repository.RoleRepository;
import blog.hethong_quanlydetai.repository.UserRepository;
import blog.hethong_quanlydetai.service.RegistrationPeriodService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Kiểm tra nghiệp vụ GĐ3 — Đợt đăng ký (mục 5.2).
 * Mỗi quy tắc có 1 ca thành công và ≥1 ca bị từ chối.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RegistrationPeriodTest {

    @Autowired RegistrationPeriodService periodService;
    @Autowired RegistrationPeriodRepository periodRepository;
    @Autowired UserRepository userRepository;
    @Autowired RoleRepository roleRepository;

    private User truongKhoa;

    @BeforeEach
    void setUp() {
        // Tạo role TRUONG_KHOA nếu chưa có (test dùng H2 + create-drop, không seed).
        Role tkRole = roleRepository.findByCode("TRUONG_KHOA").orElseGet(() ->
            roleRepository.save(Role.builder()
                .code("TRUONG_KHOA")
                .name("Trưởng khoa")
                .description("Trưởng khoa test")
                .systemFlag(true)
                .build()));
        truongKhoa = userRepository.save(User.builder()
            .username("tk_" + System.nanoTime())
            .password("{noop}x")
            .fullName("Trưởng khoa test")
            .email("tk_" + System.nanoTime() + "@test.local")
            .status(UserStatus.ACTIVE)
            .enabled(true)
            .roles(new HashSet<>(java.util.Set.of(tkRole)))
            .build());
    }

    private RegistrationPeriodForm validForm(PeriodType type) {
        RegistrationPeriodForm f = new RegistrationPeriodForm();
        f.setName("Đợt test " + System.nanoTime());
        f.setPeriodType(type);
        LocalDateTime now = LocalDateTime.now();
        f.setLecturerStartAt(now.plusDays(1));
        f.setLecturerEndAt(now.plusDays(2));
        f.setStudentStartAt(now.plusDays(2));
        f.setStudentEndAt(now.plusDays(3));
        if (type.requiresReviewDeadline()) {
            f.setReviewDeadline(now.plusDays(10));
        }
        if (type.requiresCouncilReportDate()) {
            f.setCouncilReportDate(now.plusDays(20));
        }
        return f;
    }

    // ---------- Happy path ----------

    @Test
    void create_TLCN_success() {
        RegistrationPeriod p = periodService.create(validForm(PeriodType.TLCN), truongKhoa);
        assertThat(p.getId()).isNotNull();
        assertThat(p.getStatus()).isEqualTo(PeriodStatus.DRAFT);
        assertThat(p.getReviewDeadline()).isNotNull();
    }

    @Test
    void create_KLTN_success() {
        RegistrationPeriod p = periodService.create(validForm(PeriodType.KLTN), truongKhoa);
        assertThat(p.getCouncilReportDate()).isNotNull();
    }

    @Test
    void create_MON_HOC_withoutReviewDeadline_success() {
        RegistrationPeriod p = periodService.create(validForm(PeriodType.MON_HOC), truongKhoa);
        assertThat(p.getReviewDeadline()).isNull();
    }

    // ---------- Quy tắc bị từ chối ----------

    @Test
    void lecturerEndBeforeLecturerStart_rejected() {
        RegistrationPeriodForm f = validForm(PeriodType.MON_HOC);
        f.setLecturerStartAt(LocalDateTime.now().plusDays(5));
        f.setLecturerEndAt(LocalDateTime.now().plusDays(4));
        assertThatThrownBy(() -> periodService.create(f, truongKhoa))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("bắt đầu GV phải trước");
    }

    @Test
    void studentStartBeforeLecturerEnd_rejected() {
        RegistrationPeriodForm f = validForm(PeriodType.MON_HOC);
        f.setStudentStartAt(f.getLecturerEndAt().minusMinutes(1));
        assertThatThrownBy(() -> periodService.create(f, truongKhoa))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Giai đoạn SV phải bắt đầu sau");
    }

    @Test
    void monHoc_withReviewDeadline_rejected() {
        RegistrationPeriodForm f = validForm(PeriodType.MON_HOC);
        f.setReviewDeadline(LocalDateTime.now().plusDays(10));
        assertThatThrownBy(() -> periodService.create(f, truongKhoa))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("không được có hạn chót GVPB");
    }

    @Test
    void nckh_withCouncilReportDate_rejected() {
        RegistrationPeriodForm f = validForm(PeriodType.NCKH);
        f.setCouncilReportDate(LocalDateTime.now().plusDays(20));
        assertThatThrownBy(() -> periodService.create(f, truongKhoa))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("không được có ngày báo cáo hội đồng");
    }

    @Test
    void tlcn_withoutReviewDeadline_rejected() {
        RegistrationPeriodForm f = validForm(PeriodType.TLCN);
        f.setReviewDeadline(null);
        assertThatThrownBy(() -> periodService.create(f, truongKhoa))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("bắt buộc phải có hạn chót GVPB");
    }

    @Test
    void kltn_reviewDeadlineBeforeStudentEnd_rejected() {
        RegistrationPeriodForm f = validForm(PeriodType.KLTN);
        f.setReviewDeadline(f.getStudentEndAt().minusMinutes(1));
        assertThatThrownBy(() -> periodService.create(f, truongKhoa))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Hạn chót GVPB phải sau");
    }

    @Test
    void kltn_councilBeforeReview_rejected() {
        RegistrationPeriodForm f = validForm(PeriodType.KLTN);
        f.setCouncilReportDate(f.getReviewDeadline().minusMinutes(1));
        assertThatThrownBy(() -> periodService.create(f, truongKhoa))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Ngày báo cáo hội đồng phải sau");
    }

    // ---------- Publish / Close / Delete ----------

    @Test
    void publish_thenChangeType_rejected() {
        RegistrationPeriod created = periodService.create(validForm(PeriodType.TLCN), truongKhoa);
        RegistrationPeriod published = periodService.publish(created.getId(), truongKhoa);
        assertThat(published.getStatus()).isEqualTo(PeriodStatus.PUBLISHED);

        RegistrationPeriodForm f = validForm(PeriodType.KLTN);
        f.setName(published.getName());
        assertThatThrownBy(() -> periodService.update(published.getId(), f, truongKhoa))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("không được đổi loại");
    }

    @Test
    void close_thenUpdate_rejected() {
        RegistrationPeriod created = periodService.create(validForm(PeriodType.MON_HOC), truongKhoa);
        RegistrationPeriod published = periodService.publish(created.getId(), truongKhoa);
        RegistrationPeriod closed = periodService.close(published.getId(), truongKhoa);
        assertThat(closed.getStatus()).isEqualTo(PeriodStatus.CLOSED);

        RegistrationPeriodForm f = validForm(PeriodType.MON_HOC);
        f.setName(closed.getName());
        assertThatThrownBy(() -> periodService.update(closed.getId(), f, truongKhoa))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("đã đóng");
    }

    @Test
    void deletePublished_rejected() {
        RegistrationPeriod created = periodService.create(validForm(PeriodType.MON_HOC), truongKhoa);
        RegistrationPeriod published = periodService.publish(created.getId(), truongKhoa);
        Long id = published.getId();
        assertThatThrownBy(() -> periodService.delete(id, truongKhoa))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Chỉ xoá được đợt ở trạng thái NHÁP");
    }
}
