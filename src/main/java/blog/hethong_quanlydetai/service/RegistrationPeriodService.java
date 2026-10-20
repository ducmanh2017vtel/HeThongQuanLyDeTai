package blog.hethong_quanlydetai.service;

import blog.hethong_quanlydetai.audit.AuditLogger;
import blog.hethong_quanlydetai.domain.PeriodStatus;
import blog.hethong_quanlydetai.domain.PeriodType;
import blog.hethong_quanlydetai.domain.RegistrationPeriod;
import blog.hethong_quanlydetai.domain.User;
import blog.hethong_quanlydetai.dto.RegistrationPeriodForm;
import blog.hethong_quanlydetai.repository.RegistrationPeriodRepository;
import blog.hethong_quanlydetai.repository.TopicRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Nghiệp vụ quản lý đợt đăng ký (mục 5.2). Mọi ràng buộc được kiểm tra ở service.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RegistrationPeriodService {

    private final RegistrationPeriodRepository periodRepository;
    private final TopicRepository topicRepository;
    private final AuditLogger auditLogger;

    /** Trang danh sách có lọc. */
    @Transactional(readOnly = true)
    public Page<RegistrationPeriod> search(String q, PeriodType type, PeriodStatus status, Pageable pageable) {
        return periodRepository.search(emptyToNull(q), type, status, pageable);
    }

    @Transactional(readOnly = true)
    public RegistrationPeriod getById(Long id) {
        return periodRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đợt đăng ký #" + id));
    }

    @Transactional(readOnly = true)
    public List<RegistrationPeriod> findOpenForStudents(LocalDateTime now) {
        return periodRepository.findOpenForStudents(now);
    }

    @Transactional(readOnly = true)
    public List<RegistrationPeriod> findOpenForLecturers(LocalDateTime now) {
        return periodRepository.findOpenForLecturers(now);
    }

    /** Tạo mới (luôn DRAFT). */
    @Transactional
    public RegistrationPeriod create(RegistrationPeriodForm form, User creator) {
        validateBusinessRules(form, null);
        if (periodRepository.existsByNameIgnoreCase(form.getName().trim())) {
            throw new IllegalArgumentException("Tên đợt đăng ký đã tồn tại");
        }
        RegistrationPeriod p = RegistrationPeriod.builder()
            .name(form.getName().trim())
            .periodType(form.getPeriodType())
            .lecturerStartAt(form.getLecturerStartAt())
            .lecturerEndAt(form.getLecturerEndAt())
            .studentStartAt(form.getStudentStartAt())
            .studentEndAt(form.getStudentEndAt())
            .reviewDeadline(form.getReviewDeadline())
            .councilReportDate(form.getCouncilReportDate())
            .status(PeriodStatus.DRAFT)
            .createdBy(creator)
            .build();
        RegistrationPeriod saved = periodRepository.save(p);
        auditLogger.log("PERIOD_CREATE", "RegistrationPeriod", saved.getId(),
                "Tạo đợt " + saved.getName());
        return saved;
    }

    /** Sửa (DRAFT: sửa tất cả; PUBLISHED: chỉ sửa mốc thời gian, không đổi loại; CLOSED: khoá). */
    @Transactional
    public RegistrationPeriod update(Long id, RegistrationPeriodForm form, User actor) {
        RegistrationPeriod p = getById(id);
        if (p.getStatus() == PeriodStatus.CLOSED) {
            throw new IllegalArgumentException("Đợt đã đóng, không thể chỉnh sửa");
        }
        boolean canEditAll = (p.getStatus() == PeriodStatus.DRAFT);
        if (!canEditAll) {
            // PUBLISHED: chỉ sửa mốc thời gian
            if (form.getPeriodType() != p.getPeriodType()) {
                throw new IllegalArgumentException("Đợt đã công bố: không được đổi loại đợt");
            }
            if (!form.getName().trim().equalsIgnoreCase(p.getName())) {
                throw new IllegalArgumentException("Đợt đã công bố: không được đổi tên");
            }
        } else {
            // DRAFT: nếu đã có đề tài, không cho đổi loại
            if (form.getPeriodType() != p.getPeriodType()
                    && periodRepository.hasAnyTopic(p.getId())) {
                throw new IllegalArgumentException("Đợt đã có đề tài: không thể đổi loại đợt");
            }
        }
        validateBusinessRules(form, p.getPeriodType());
        p.setName(form.getName().trim());
        p.setPeriodType(form.getPeriodType());
        p.setLecturerStartAt(form.getLecturerStartAt());
        p.setLecturerEndAt(form.getLecturerEndAt());
        p.setStudentStartAt(form.getStudentStartAt());
        p.setStudentEndAt(form.getStudentEndAt());
        p.setReviewDeadline(form.getReviewDeadline());
        p.setCouncilReportDate(form.getCouncilReportDate());
        RegistrationPeriod saved = periodRepository.save(p);
        auditLogger.log("PERIOD_UPDATE", "RegistrationPeriod", saved.getId(),
                "Cập nhật đợt " + saved.getName());
        return saved;
    }

    /** Công bố: DRAFT → PUBLISHED. */
    @Transactional
    public RegistrationPeriod publish(Long id, User actor) {
        RegistrationPeriod p = getById(id);
        if (p.getStatus() != PeriodStatus.DRAFT) {
            throw new IllegalArgumentException("Chỉ công bố được đợt ở trạng thái NHÁP");
        }
        // Không cho công bố nếu mốc thời gian không hợp lệ
        validateBusinessRules(toForm(p), p.getPeriodType());
        p.setStatus(PeriodStatus.PUBLISHED);
        RegistrationPeriod saved = periodRepository.save(p);
        auditLogger.log("PERIOD_PUBLISH", "RegistrationPeriod", saved.getId(),
                "Công bố đợt " + saved.getName());
        return saved;
    }

    /** Đóng đợt. */
    @Transactional
    public RegistrationPeriod close(Long id, User actor) {
        RegistrationPeriod p = getById(id);
        if (p.getStatus() == PeriodStatus.CLOSED) {
            throw new IllegalArgumentException("Đợt đã ở trạng thái ĐÃ ĐÓNG");
        }
        p.setStatus(PeriodStatus.CLOSED);
        RegistrationPeriod saved = periodRepository.save(p);
        auditLogger.log("PERIOD_CLOSE", "RegistrationPeriod", saved.getId(),
                "Đóng đợt " + saved.getName());
        return saved;
    }

    /** Xoá: chỉ DRAFT và chưa có đề tài. */
    @Transactional
    public void delete(Long id, User actor) {
        RegistrationPeriod p = getById(id);
        if (p.getStatus() != PeriodStatus.DRAFT) {
            throw new IllegalArgumentException("Chỉ xoá được đợt ở trạng thái NHÁP");
        }
        if (periodRepository.hasAnyTopic(p.getId())) {
            throw new IllegalArgumentException("Đợt đã có đề tài, không thể xoá");
        }
        Long deletedId = p.getId();
        String name = p.getName();
        periodRepository.delete(p);
        auditLogger.log("PERIOD_DELETE", "RegistrationPeriod", deletedId,
                "Xoá đợt " + name);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /**
     * Tổng hợp quy tắc nghiệp vụ (mục 5.2). {@code currentType} là loại hiện tại của
     * đợt khi đang sửa (để cho phép giữ nguyên trong khi sửa mốc thời gian).
     * Nếu {@code currentType} == null thì đây là tạo mới, dùng {@code form.periodType}.
     */
    private void validateBusinessRules(RegistrationPeriodForm form, PeriodType currentType) {
        if (form.getLecturerStartAt() == null || form.getLecturerEndAt() == null
                || form.getStudentStartAt() == null || form.getStudentEndAt() == null) {
            throw new IllegalArgumentException("Vui lòng nhập đầy đủ mốc thời gian");
        }
        if (!form.getLecturerStartAt().isBefore(form.getLecturerEndAt())) {
            throw new IllegalArgumentException("Thời gian bắt đầu GV phải trước thời gian kết thúc GV");
        }
        if (!form.getStudentStartAt().isBefore(form.getStudentEndAt())) {
            throw new IllegalArgumentException("Thời gian bắt đầu SV phải trước thời gian kết thúc SV");
        }
        if (form.getStudentStartAt().isBefore(form.getLecturerEndAt())) {
            throw new IllegalArgumentException("Giai đoạn SV phải bắt đầu sau khi giai đoạn GV kết thúc");
        }
        PeriodType type = currentType != null ? currentType : form.getPeriodType();
        if (type == null) {
            throw new IllegalArgumentException("Vui lòng chọn loại đợt");
        }
        if (type.requiresReviewDeadline()) {
            if (form.getReviewDeadline() == null) {
                throw new IllegalArgumentException("Đợt " + type + " bắt buộc phải có hạn chót GVPB nộp điểm");
            }
            if (!form.getReviewDeadline().isAfter(form.getStudentEndAt())) {
                throw new IllegalArgumentException("Hạn chót GVPB phải sau khi SV kết thúc đăng ký");
            }
        } else {
            if (form.getReviewDeadline() != null) {
                throw new IllegalArgumentException("Đợt " + type + " không được có hạn chót GVPB");
            }
        }
        if (type.requiresCouncilReportDate()) {
            if (form.getCouncilReportDate() == null) {
                throw new IllegalArgumentException("Đợt KLTN bắt buộc phải có ngày báo cáo hội đồng");
            }
            LocalDateTime compare = form.getReviewDeadline() != null
                ? form.getReviewDeadline() : form.getStudentEndAt();
            if (!form.getCouncilReportDate().isAfter(compare)) {
                throw new IllegalArgumentException("Ngày báo cáo hội đồng phải sau hạn chót GVPB");
            }
        } else {
            if (form.getCouncilReportDate() != null) {
                throw new IllegalArgumentException("Đợt " + type + " không được có ngày báo cáo hội đồng");
            }
        }
    }

    /** Kiểm tra khung thời gian hiện tại có cho phép thao tác không. */
    @Transactional(readOnly = true)
    public boolean isWithinLecturerWindow(RegistrationPeriod p, LocalDateTime now) {
        return p.getStatus() == PeriodStatus.PUBLISHED
            && !now.isBefore(p.getLecturerStartAt())
            && !now.isAfter(p.getLecturerEndAt());
    }

    @Transactional(readOnly = true)
    public boolean isWithinStudentWindow(RegistrationPeriod p, LocalDateTime now) {
        return p.getStatus() == PeriodStatus.PUBLISHED
            && !now.isBefore(p.getStudentStartAt())
            && !now.isAfter(p.getStudentEndAt());
    }

    private RegistrationPeriodForm toForm(RegistrationPeriod p) {
        RegistrationPeriodForm f = new RegistrationPeriodForm();
        f.setName(p.getName());
        f.setPeriodType(p.getPeriodType());
        f.setLecturerStartAt(p.getLecturerStartAt());
        f.setLecturerEndAt(p.getLecturerEndAt());
        f.setStudentStartAt(p.getStudentStartAt());
        f.setStudentEndAt(p.getStudentEndAt());
        f.setReviewDeadline(p.getReviewDeadline());
        f.setCouncilReportDate(p.getCouncilReportDate());
        return f;
    }

    private static String emptyToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
