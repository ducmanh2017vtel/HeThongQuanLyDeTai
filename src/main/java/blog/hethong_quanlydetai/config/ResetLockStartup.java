package blog.hethong_quanlydetai.config;

import blog.hethong_quanlydetai.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ResetLockStartup - tạm thời reset tất cả failed_attempts và locked_until khi khởi động.
 * Dùng để mở khóa tài khoản do quá trình test. Sẽ xóa sau khi hoàn thành.
 */
@Configuration
public class ResetLockStartup {

    private static final Logger log = LoggerFactory.getLogger(ResetLockStartup.class);

    @Bean
    public CommandLineRunner resetAllLocks(ResetLockService service) {
        return args -> service.resetAll();
    }

    @Service
    public static class ResetLockService {
        private static final Logger log = LoggerFactory.getLogger(ResetLockService.class);
        private final UserRepository userRepository;

        public ResetLockService(UserRepository userRepository) {
            this.userRepository = userRepository;
        }

        @Transactional
        public void resetAll() {
            userRepository.findAll().forEach(u -> {
                if (u.getFailedAttempts() != null && u.getFailedAttempts() > 0) {
                    log.warn("[reset-lock] Resetting lock for user: {}", u.getUsername());
                }
                userRepository.updateLockState(u.getId(), 0, null);
            });
            log.info("[reset-lock] All user locks cleared.");
        }
    }
}
