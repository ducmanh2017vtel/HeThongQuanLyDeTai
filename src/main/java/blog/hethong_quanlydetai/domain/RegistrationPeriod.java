package blog.hethong_quanlydetai.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "registration_periods")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RegistrationPeriod extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 180)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "period_type", nullable = false, length = 10)
    private PeriodType periodType;

    @Column(name = "lecturer_start_at", nullable = false)
    private LocalDateTime lecturerStartAt;

    @Column(name = "lecturer_end_at", nullable = false)
    private LocalDateTime lecturerEndAt;

    @Column(name = "student_start_at", nullable = false)
    private LocalDateTime studentStartAt;

    @Column(name = "student_end_at", nullable = false)
    private LocalDateTime studentEndAt;

    @Column(name = "review_deadline")
    private LocalDateTime reviewDeadline;

    @Column(name = "council_report_date")
    private LocalDateTime councilReportDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private PeriodStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

}