package blog.hethong_quanlydetai.service;

import blog.hethong_quanlydetai.audit.AuditLogger;
import blog.hethong_quanlydetai.domain.*;
import blog.hethong_quanlydetai.dto.NotificationForm;
import blog.hethong_quanlydetai.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Nghiệp vụ Thông báo / Tin tức (GĐ10 — mục 5.9, A5).
 *
 * Quy tắc:
 * - Trưởng khoa (TRUONG_KHOA) hoặc Admin có quyền NOTICE_PUBLISH: tạo / sửa / publish / hide.
 * - Khi publish: set status = PUBLISHED, published_at = now.
 * - Khi hide: status = HIDDEN, không hiển thị trên dashboard.
 * - targetRoles: nếu rỗng → gửi cho tất cả role đang active.
 * - Sinh viên / GV xem được các thông báo PUBLISHED mà role của họ nằm trong targetRoles
 *   (hoặc thông báo không giới hạn role).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final RoleRepository roleRepository;
    private final AuditLogger auditLogger;

    // ------------------------------------------------------------------
    // CRUD
    // ------------------------------------------------------------------

    @Transactional
    public Notification create(NotificationForm form, User actor) {
        validate(form);
        Notification n = Notification.builder()
                .title(form.getTitle().trim())
                .content(form.getContent().trim())
                .type(form.getType() != null ? form.getType() : NotificationType.FACULTY)
                .createdBy(actor)
                .status(NotificationStatus.DRAFT)
                .targetRoles(resolveTargetRoles(form.getTargetRoleCodes()))
                .build();
        Notification saved = notificationRepository.save(n);
        auditLogger.log("NOTICE_CREATE", "Notification", saved.getId(),
                "Tạo thông báo: " + saved.getTitle());
        return saved;
    }

    @Transactional
    public Notification update(Long id, NotificationForm form, User actor) {
        validate(form);
        Notification n = getById(id);
        if (n.getStatus() == NotificationStatus.PUBLISHED) {
            throw new IllegalArgumentException("Thông báo đã công bố, không thể sửa nội dung");
        }
        n.setTitle(form.getTitle().trim());
        n.setContent(form.getContent().trim());
        n.setType(form.getType() != null ? form.getType() : n.getType());
        n.setTargetRoles(resolveTargetRoles(form.getTargetRoleCodes()));
        Notification saved = notificationRepository.save(n);
        auditLogger.log("NOTICE_UPDATE", "Notification", saved.getId(),
                "Cập nhật thông báo: " + saved.getTitle());
        return saved;
    }

    @Transactional
    public Notification publish(Long id, User actor) {
        Notification n = getById(id);
        if (n.getStatus() == NotificationStatus.PUBLISHED) {
            throw new IllegalArgumentException("Thông báo đã được công bố trước đó");
        }
        if (n.getStatus() == NotificationStatus.HIDDEN) {
            throw new IllegalArgumentException("Thông báo đã bị ẩn, không thể công bố lại");
        }
        n.setStatus(NotificationStatus.PUBLISHED);
        n.setPublishedAt(LocalDateTime.now());
        Notification saved = notificationRepository.save(n);
        auditLogger.log("NOTICE_PUBLISH", "Notification", saved.getId(),
                "Công bố thông báo: " + saved.getTitle());
        return saved;
    }

    @Transactional
    public Notification hide(Long id, User actor) {
        Notification n = getById(id);
        if (n.getStatus() == NotificationStatus.HIDDEN) {
            throw new IllegalArgumentException("Thông báo đã được ẩn trước đó");
        }
        n.setStatus(NotificationStatus.HIDDEN);
        n.setPublishedAt(null);
        Notification saved = notificationRepository.save(n);
        auditLogger.log("NOTICE_HIDE", "Notification", saved.getId(),
                "Ẩn thông báo: " + saved.getTitle());
        return saved;
    }

    @Transactional
    public void delete(Long id, User actor) {
        Notification n = getById(id);
        if (n.getStatus() == NotificationStatus.PUBLISHED) {
            throw new IllegalArgumentException("Thông báo đã công bố, hãy ẩn trước khi xoá");
        }
        notificationRepository.delete(n);
        auditLogger.log("NOTICE_DELETE", "Notification", id, "Xoá thông báo: " + n.getTitle());
    }

    // ------------------------------------------------------------------
    // Query
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Notification getById(Long id) {
        return notificationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông báo #" + id));
    }

    @Transactional(readOnly = true)
    public List<Notification> findAll() {
        return notificationRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<Notification> findPublishedForRoles(Set<String> roleCodes) {
        return notificationRepository.findByStatusOrderByCreatedAtDesc(NotificationStatus.PUBLISHED)
                .stream()
                .filter(n -> isVisibleTo(n, roleCodes))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Notification> findPublished(int limit) {
        return notificationRepository.findByStatusOrderByCreatedAtDesc(NotificationStatus.PUBLISHED)
                .stream().limit(limit).toList();
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private void validate(NotificationForm form) {
        if (form.getTitle() == null || form.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Tiêu đề không được trống");
        }
        if (form.getTitle().length() > 255) {
            throw new IllegalArgumentException("Tiêu đề tối đa 255 ký tự");
        }
        if (form.getContent() == null || form.getContent().trim().isEmpty()) {
            throw new IllegalArgumentException("Nội dung không được trống");
        }
    }

    private Set<Role> resolveTargetRoles(Set<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return new HashSet<>(); // rỗng = gửi cho tất cả
        }
        Set<Role> roles = new HashSet<>();
        for (String code : codes) {
            roleRepository.findByCode(code).ifPresent(roles::add);
        }
        return roles;
    }

    private boolean isVisibleTo(Notification n, Set<String> roleCodes) {
        if (n.getTargetRoles() == null || n.getTargetRoles().isEmpty()) {
            return true; // gửi cho tất cả
        }
        return n.getTargetRoles().stream().anyMatch(r ->
                roleCodes.contains(r.getCode()));
    }
}
