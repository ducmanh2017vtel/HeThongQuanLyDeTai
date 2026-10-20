package blog.hethong_quanlydetai.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Form tạo/sửa tài khoản (GĐ2 mục 5.1). */
@Data
public class UserForm {

    private Long id;  // null nếu tạo mới

    @NotBlank(message = "Tên đăng nhập không được để trống")
    @Size(min = 3, max = 60, message = "Tên đăng nhập 3–60 ký tự")
    @Pattern(regexp = "^[A-Za-z0-9_.\\-]+$",
             message = "Tên đăng nhập chỉ gồm chữ, số, gạch dưới, gạch ngang")
    private String username;

    @NotBlank(message = "Họ tên không được để trống")
    @Size(max = 150, message = "Họ tên tối đa 150 ký tự")
    private String fullName;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    @Size(max = 160, message = "Email tối đa 160 ký tự")
    private String email;

    @Size(max = 20, message = "Số điện thoại tối đa 20 ký tự")
    private String phone;

    /** Nếu tạo mới và tài khoản là SV/GV thì phải cung cấp; nếu sửa có thể bỏ trống. */
    private String roleCodes;       // nhiều vai trò, phân tách dấu phẩy

    // Profile SV
    private String studentCode;
    private String className;
    private String major;

    // Profile GV
    private String lecturerCode;
    private String academicDegree;
    private Long departmentId;
}