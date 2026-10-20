package blog.hethong_quanlydetai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Form tạo/sửa hội đồng (GĐ7).
 * - lecturerIds + roles: danh sách thành viên (1 CTHD, 1 thư ký, 1-3 thành viên)
 * - topicIds: các đề tài được gán (chỉ cùng đợt, đã được duyệt)
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CouncilForm {

    @NotBlank
    @Size(max = 40)
    private String councilCode;

    @NotBlank
    @Size(max = 180)
    private String councilName;

    @NotNull
    private Long periodId;

    /** Ngày báo cáo (bắt buộc với KLTN). */
    private LocalDateTime reportDate;

    /** Danh sách ID thành viên hội đồng. */
    private List<Long> lecturerIds;

    /** Vai trò tương ứng (CHAIRMAN/SECRETARY/MEMBER). */
    private List<String> roles;

    /** Danh sách ID đề tài được gán vào hội đồng. */
    private List<Long> topicIds;
}
