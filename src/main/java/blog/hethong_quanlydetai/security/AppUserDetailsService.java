package blog.hethong_quanlydetai.security;

import blog.hethong_quanlydetai.domain.User;
import blog.hethong_quanlydetai.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

/** UserDetailsService dùng bảng users + roles + permissions. */
@Service
@RequiredArgsConstructor
public class AppUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User u = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy tài khoản"));

        Set<GrantedAuthority> authorities = new HashSet<>();
        u.getRoles().forEach(r -> {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + r.getCode()));
            r.getPermissions().forEach(p ->
                authorities.add(new SimpleGrantedAuthority(p.getCode())));
        });

        return new AppUserPrincipal(
                u.getId(),
                u.getUsername(),
                u.getPassword(),
                Boolean.TRUE.equals(u.getEnabled()) && !u.isLocked(),
                authorities
        );
    }
}