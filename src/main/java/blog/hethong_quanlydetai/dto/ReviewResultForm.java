package blog.hethong_quanlydetai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Form cập nhật nhận xét tổng hợp cho kết quả (GĐ9).
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ReviewResultForm {
    @Size(max = 2000)
    private String summaryComment;
}
