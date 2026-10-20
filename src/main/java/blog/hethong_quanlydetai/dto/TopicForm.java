package blog.hethong_quanlydetai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Form tạo/sửa đề tài.
 *
 * Quy tắc:
 *  - topicCode: duy nhất.
 *  - topicName: bắt buộc, tối thiểu 10 ký tự.
 *  - description: bắt buộc, tối thiểu 30 ký tự.
 *  - departmentId: bắt buộc, phải khớp với bộ môn của GV tạo.
 *  - periodId: bắt buộc, đợt phải ở trạng thái PUBLISHED và trong khung thời gian GV.
 *  - supervisor1Id: bắt buộc, là GV đang hoạt động.
 *  - supervisor2Id: tùy chọn, phải khác supervisor1, là GV đang hoạt động.
 *
 * Ở service kiểm tra lại và trả thông báo tiếng Việt.
 */
@Getter
@Setter
public class TopicForm {

    /** Mã đề tài duy nhất. */
    @NotBlank
    @Size(max = 30)
    private String topicCode;

    /** Tên đề tài. */
    @NotBlank
    @Size(min = 10, max = 300)
    private String topicName;

    /** Mô tả/nội dung chi tiết đề tài. */
    @NotBlank
    @Size(min = 30, max = 4000)
    private String description;

    /** ID bộ môn mà đề tài thuộc về. */
    @NotNull
    private Long departmentId;

    /** ID đợt đăng ký. */
    @NotNull
    private Long periodId;

    /** ID giảng viên hướng dẫn chính (bắt buộc). */
    @NotNull
    private Long supervisor1Id;

    /** ID giảng viên hướng dẫn thứ hai (tùy chọn, có thể null). */
    private Long supervisor2Id;
}
