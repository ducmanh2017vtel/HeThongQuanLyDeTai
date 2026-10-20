package blog.hethong_quanlydetai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AddMemberForm {

    @NotBlank(message = "Vui lòng nhập mã sinh viên")
    @Size(max = 20, message = "Mã sinh viên tối đa 20 ký tự")
    private String studentCode;
}
