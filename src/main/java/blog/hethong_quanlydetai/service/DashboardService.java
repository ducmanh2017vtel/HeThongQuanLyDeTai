package blog.hethong_quanlydetai.service;

import blog.hethong_quanlydetai.domain.*;
import blog.hethong_quanlydetai.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Dashboard theo vai trò (GĐ1 nền + mở rộng qua các giai đoạn).
 *
 * GĐ5 bổ sung:
 * - GVHD: "Nhóm chờ duyệt đăng ký" (group.status=REGISTERED, topic thuộc GVHD chính).
 * - SV:   "Đăng ký đề tài" nếu SV có nhóm DRAFT.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final RegistrationPeriodRepository periodRepo;
    private final TopicRepository topicRepo;
    private final NotificationRepository notificationRepo;
    private final UserRepository userRepo;
    private final StudentRepository studentRepo;
    private final LecturerRepository lecturerRepo;
    private final StudentGroupRepository groupRepo;

    public List<Map<String, String>> todoFor(Long userId, Set<String> roleCodes) {
        List<Map<String, String>> todos = new ArrayList<>();
        if (roleCodes.contains("TRUONG_KHOA") || roleCodes.contains("ADMIN")) {
            long pendingTopics = topicRepo.findAll().stream()
                    .filter(t -> t.getStatus() == TopicStatus.PENDING).count();
            todos.add(Map.of(
                    "code", "TOPICS_PENDING",
                    "title", "Đề tài chờ duyệt",
                    "count", String.valueOf(pendingTopics),
                    "link", "/topics?status=PENDING"
            ));
        }
        if (roleCodes.contains("GIANG_VIEN") || roleCodes.contains("ADMIN")
                || roleCodes.contains("CHU_TICH_HOI_DONG")) {
            // GVHD: đếm nhóm cần duyệt (sử dụng JPQL join thay vì stream lazy)
            lecturerRepo.findByUserId(userId).ifPresent(lec -> {
                long pendingGroups = groupRepo.countByPrimarySupervisorAndStatus(
                        lec.getId(), GroupStatus.REGISTERED);
                if (pendingGroups > 0) {
                    todos.add(Map.of(
                            "code", "REGISTRATIONS_PENDING",
                            "title", "Nhóm chờ duyệt đăng ký",
                            "count", String.valueOf(pendingGroups),
                            "link", "/registrations?status=REGISTERED"
                    ));
                }
            });
        }
        if (roleCodes.contains("SINH_VIEN")) {
            // SV: nếu có nhóm DRAFT trong đợt đang mở → nhắc đăng ký đề tài
            studentRepo.findByUserId(userId).ifPresent(st -> {
                boolean hasDraft = groupRepo.findAll().stream().anyMatch(g ->
                        g.getStatus() == GroupStatus.DRAFT
                        && g.getMembers() != null
                        && g.getMembers().stream().anyMatch(m ->
                            m.getStudent().getId().equals(st.getId())));
                if (hasDraft) {
                    todos.add(Map.of(
                            "code", "REGISTER_TOPIC",
                            "title", "Đăng ký đề tài cho nhóm của bạn",
                            "count", "1",
                            "link", "/groups"
                    ));
                }
            });
        }
        return todos;
    }

    public Map<String, Object> summary() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("userCount",    userRepo.count());
        m.put("topicCount",   topicRepo.count());
        m.put("periodCount",  periodRepo.count());
        return m;
    }

    public List<RegistrationPeriod> openPeriods() {
        return new ArrayList<>(periodRepo.findOpenForStudents(LocalDateTime.now()));
    }

    public List<Notification> latestNotices(Set<String> roleCodes) {
        return notificationRepo.findByStatus(NotificationStatus.PUBLISHED).stream()
                .filter(n -> n.getStatus() == NotificationStatus.PUBLISHED)
                .filter(n -> n.getTargetRoles() == null
                        || n.getTargetRoles().isEmpty()
                        || n.getTargetRoles().stream().anyMatch(r ->
                                roleCodes.contains(r.getCode())))
                .sorted(Comparator.comparing(Notification::getPublishedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(5)
                .toList();
    }
}
