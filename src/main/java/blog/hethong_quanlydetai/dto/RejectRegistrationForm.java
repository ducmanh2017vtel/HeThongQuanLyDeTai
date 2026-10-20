package blog.hethong_quanlydetai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RejectRegistrationForm {

    @NotBlank(message = "Vui lòng nhập lý do từ chối")
    @Size(min = 10, max = 500, message = "Lý do từ chối từ 10 đến 500 ký tự")
    private String reason;
}
