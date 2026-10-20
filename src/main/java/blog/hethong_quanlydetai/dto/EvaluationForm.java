package blog.hethong_quanlydetai.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Form nhập điểm phản biện (GĐ8).
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EvaluationForm {

    @NotNull
    @DecimalMin("0.00")
    @DecimalMax("10.00")
    @Digits(integer = 2, fraction = 2)
    private BigDecimal score;

    @NotBlank
    @Size(min = 10, max = 2000)
    private String comment;
}
