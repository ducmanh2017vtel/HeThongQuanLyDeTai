package blog.hethong_quanlydetai.security;

import blog.hethong_quanlydetai.domain.User;
import blog.hethong_quanlydetai.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Cơ chế khoá tạm khi đăng nhập sai (không dùng thư viện ngoài).
 * Đếm số lần fail theo tài khoản; khi vượt ngưỡng sẽ set lockedUntil.
 * Cron reset các khoá hết hạn có thể chạy ở mức ứng dụng (xem releaseExpiredLocks).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LoginAttemptService {

    private final UserRepository userRepository;

    @Value("${app.security.login.max-fail}")
    private int maxFail;

    @Value("${app.security.login.lock-minutes}")
    private int lockMinutes;

    @Transactional
    public void loginFailed(String username) {
        userRepository.findByUsername(username).ifPresent(u -> {
            // Nếu đang khoá tạm mà hết hạn thì reset
            if (u.getLockedUntil() != null && u.getLockedUntil().isBefore(LocalDateTime.now())) {
                u.resetFailedAttempts();
            }
            int attempts = (u.getFailedAttempts() == null ? 0 : u.getFailedAttempts()) + 1;
            LocalDateTime lockedUntil = null;
            if (attempts >= maxFail) {
                lockedUntil = LocalDateTime.now().plusMinutes(lockMinutes);
                log.warn("Khoá tạm tài khoản {} sau {} lần sai, đến {}",
                        username, attempts, lockedUntil);
            }
            userRepository.updateLockState(u.getId(), attempts, lockedUntil);
        });
    }

    @Transactional
    public void loginSucceeded(String username) {
        userRepository.findByUsername(username).ifPresent(u ->
            userRepository.updateLockState(u.getId(), 0, null));
    }

    public boolean isLocked(User u) {
        return u.isLocked();
    }

    @Transactional
    public int releaseExpiredLocks() {
        int n = userRepository.findExpiredLocks(LocalDateTime.now()).size();
        for (User u : userRepository.findExpiredLocks(LocalDateTime.now())) {
            userRepository.updateLockState(u.getId(), 0, null);
        }
        return n;
    }
}