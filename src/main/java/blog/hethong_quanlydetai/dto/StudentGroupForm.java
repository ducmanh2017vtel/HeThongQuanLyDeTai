package blog.hethong_quanlydetai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StudentGroupForm {

    @NotNull(message = "Vui lòng chọn đợt đăng ký")
    private Long periodId;

    @NotBlank(message = "Mã nhóm không được để trống")
    @Size(min = 3, max = 40, message = "Mã nhóm từ 3 đến 40 ký tự")
    private String groupCode;

    @NotBlank(message = "Tên nhóm không được để trống")
    @Size(min = 3, max = 180, message = "Tên nhóm từ 3 đến 180 ký tự")
    private String groupName;

    /**
     * Danh sách userId của SV sẽ tham gia nhóm (không bao gồm người tạo).
     * Nếu rỗng → nhóm 1 người.
     */
    private java.util.List<Long> memberUserIds;
}
