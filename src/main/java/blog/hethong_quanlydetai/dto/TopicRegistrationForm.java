package blog.hethong_quanlydetai.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TopicRegistrationForm {

    @NotNull(message = "Vui lòng chọn đề tài")
    private Long topicId;
}
