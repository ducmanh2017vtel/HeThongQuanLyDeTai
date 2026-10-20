package blog.hethong_quanlydetai.config;

import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Helper dùng trong Thymeleaf để chuyển mã trạng thái → nhãn tiếng Việt + lớp CSS badge.
 * Đăng ký như Bean để gọi từ template: ${@statusHelper.label('PENDING')}
 */
@Component("statusHelper")
public class StatusHelper {

    private static final Map<String, StatusInfo> STATUS_MAP = Map.ofEntries(
        // === Period Status ===
        Map.entry("DRAFT",       new StatusInfo("Nháp",           "neutral")),
        Map.entry("PUBLISHED",   new StatusInfo("Đã công bố",    "info")),
        Map.entry("CLOSED",      new StatusInfo("Đã đóng",       "neutral")),

        // === Topic Status ===
        Map.entry("PENDING",     new StatusInfo("Chờ duyệt",     "warning")),
        Map.entry("APPROVED",    new StatusInfo("Đã duyệt",      "success")),
        Map.entry("REJECTED",    new StatusInfo("Từ chối",       "danger")),

        // === Report / Submission ===
        Map.entry("SUBMITTED",   new StatusInfo("Đã nộp",        "info")),
        Map.entry("COMPLETED",   new StatusInfo("Hoàn tất",      "success")),

        // === Evaluation ===
        Map.entry("ASSIGNED",    new StatusInfo("Đã phân công",   "info")),
        Map.entry("EVALUATED",   new StatusInfo("Đã chấm",       "success")),

        // === Council ===
        Map.entry("PLANNED",     new StatusInfo("Dự kiến",       "neutral")),

        // === Result ===
        Map.entry("PASS",        new StatusInfo("Đạt",            "success")),
        Map.entry("FAIL",        new StatusInfo("Không đạt",      "danger")),

        // === User ===
        Map.entry("ACTIVE",      new StatusInfo("Hoạt động",     "success")),
        Map.entry("LOCKED",      new StatusInfo("Đã khóa",       "danger")),

        // === Notification ===
        Map.entry("HIDDEN",     new StatusInfo("Đã thu hồi",    "neutral"))
    );

    private static final Map<String, String> PERIOD_TYPE_LABELS = Map.of(
        "MON_HOC", "Môn học",
        "NCKH",    "NCKH",
        "TLCN",    "TLCN",
        "KLTN",    "KLTN"
    );

    public String label(String code) {
        if (code == null) return "?";
        StatusInfo info = STATUS_MAP.get(code);
        if (info != null) return info.label;
        return code; // fallback: hiển thị mã gốc
    }

    public String badgeClass(String code) {
        if (code == null) return "neutral";
        StatusInfo info = STATUS_MAP.get(code);
        if (info != null) return info.badgeClass;
        return "neutral";
    }

    public String periodTypeLabel(String code) {
        if (code == null) return "?";
        return PERIOD_TYPE_LABELS.getOrDefault(code, code);
    }

    /** Chuyển trạng thái đăng ký (RegistrationStatus) → nhãn hiển thị */
    public String registrationLabel(String status) {
        if (status == null) return "?";
        return switch (status) {
            case "PENDING"   -> "Chờ xác nhận";
            case "APPROVED"  -> "Đã chấp thuận";
            case "REJECTED"  -> "Từ chối";
            case "CANCELLED" -> "Đã hủy";
            default -> status;
        };
    }

    /** Chuyển trạng thái nhóm (GroupStatus) → nhãn hiển thị */
    public String groupLabel(String status) {
        if (status == null) return "?";
        return switch (status) {
            case "DRAFT"     -> "Nháp";
            case "REGISTERED"-> "Đã đăng ký";
            case "APPROVED"  -> "Đã chấp thuận";
            case "SUBMITTED" -> "Đã nộp";
            case "COMPLETED" -> "Hoàn tất";
            default -> status;
        };
    }

    /** Chuyển trạng thái hội đồng (CouncilStatus) → nhãn hiển thị */
    public String councilLabel(String status) {
        if (status == null) return "?";
        return switch (status) {
            case "PLANNED"   -> "Dự kiến";
            case "ACTIVE"    -> "Hoạt động";
            case "COMPLETED" -> "Hoàn tất";
            case "CLOSED"   -> "Đã đóng";
            default -> status;
        };
    }

    private record StatusInfo(String label, String badgeClass) {}
}
