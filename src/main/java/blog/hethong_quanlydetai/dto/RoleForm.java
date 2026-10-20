package blog.hethong_quanlydetai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Form tạo/sửa vai trò. */
@Data
public class RoleForm {

    private Long id;

    @NotBlank(message = "Mã vai trò không được để trống")
    @Size(max = 40)
    @Pattern(regexp = "^[A-Z0-9_]+$",
             message = "Mã vai trò chỉ gồm chữ in hoa, số, gạch dưới")
    private String code;

    @NotBlank(message = "Tên vai trò không được để trống")
    @Size(max = 120)
    private String name;

    @Size(max = 255)
    private String description;
}