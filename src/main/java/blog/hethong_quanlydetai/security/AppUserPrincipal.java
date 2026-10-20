package blog.hethong_quanlydetai.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;

/** Principal chứa thêm id người dùng (dùng cho audit, controller,...). */
@Getter
public class AppUserPrincipal extends User {

    private final Long userId;

    public AppUserPrincipal(Long userId,
                            String username,
                            String password,
                            boolean enabled,
                            Collection<? extends GrantedAuthority> authorities) {
        super(username, password, enabled, true, true, true, authorities);
        this.userId = userId;
    }
}