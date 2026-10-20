package blog.hethong_quanlydetai.dto;

import blog.hethong_quanlydetai.domain.NotificationStatus;
import blog.hethong_quanlydetai.domain.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationForm {

    @NotBlank
    @Size(max = 255)
    private String title;

    @NotBlank
    private String content;

    private NotificationType type;

    /**
     * Mã các role được nhận thông báo. Nếu rỗng → gửi cho tất cả role active.
     */
    @Builder.Default
    private Set<String> targetRoleCodes = new HashSet<>();

    private NotificationStatus status;
}
