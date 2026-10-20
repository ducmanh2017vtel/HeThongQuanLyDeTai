package blog.hethong_quanlydetai.audit;

import blog.hethong_quanlydetai.domain.AuditLog;
import blog.hethong_quanlydetai.domain.User;
import blog.hethong_quanlydetai.repository.AuditLogRepository;
import blog.hethong_quanlydetai.security.AppUserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Ghi nhật ký các hành động quan trọng (mục 4). */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogger {

    private final AuditLogRepository repo;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String action, String entity, Long entityId, String detail) {
        try {
            AuditLog entry = AuditLog.builder()
                    .user(currentUser())
                    .action(action)
                    .entity(entity)
                    .entityId(entityId)
                    .detail(detail)
                    .build();
            repo.save(entry);
        } catch (Exception e) {
            // Không để lỗi audit phá vỡ nghiệp vụ chính
            log.warn("Không ghi được audit log: {}", e.getMessage());
        }
    }

    private User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        if (auth.getPrincipal() instanceof AppUserPrincipal p) {
            User u = new User();
            u.setId(p.getUserId());
            return u;
        }
        return null;
    }
}