package blog.hethong_quanlydetai.dto;

import blog.hethong_quanlydetai.domain.PeriodType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Form tạo/sửa đợt đăng ký.
 *
 * Quy tắc:
 *  - lecturerStartAt < lecturerEndAt; studentStartAt < studentEndAt.
 *  - studentStartAt >= lecturerEndAt.
 *  - reviewDeadline: chỉ yêu cầu khi TLCN/KLTN; nếu có thì > studentEndAt.
 *  - councilReportDate: chỉ yêu cầu khi KLTN; nếu có thì > reviewDeadline.
 * Ở service kiểm tra lại và trả thông báo tiếng Việt.
 */
@Getter
@Setter
public class RegistrationPeriodForm {

    @NotBlank
    @Size(max = 180)
    private String name;

    @NotNull
    private PeriodType periodType;

    @NotNull
    private LocalDateTime lecturerStartAt;

    @NotNull
    private LocalDateTime lecturerEndAt;

    @NotNull
    private LocalDateTime studentStartAt;

    @NotNull
    private LocalDateTime studentEndAt;

    /** Có thể null với MON_HOC/NCKH. */
    private LocalDateTime reviewDeadline;

    /** Có thể null với loại khác KLTN. */
    private LocalDateTime councilReportDate;
}
