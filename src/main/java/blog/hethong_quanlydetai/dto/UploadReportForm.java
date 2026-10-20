package blog.hethong_quanlydetai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Form upload báo cáo (GĐ6).
 * File thực tế được upload qua multipart riêng; form chỉ chứa metadata.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UploadReportForm {

    @NotBlank
    @Size(max = 255)
    private String reportTitle;

    @Size(max = 2000)
    private String description;
}
