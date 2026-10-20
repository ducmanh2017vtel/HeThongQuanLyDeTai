package blog.hethong_quanlydetai.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "evaluations")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Evaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id", nullable = false, unique = true)
    private ReviewAssignment assignment;

    @Column(nullable = false, precision = 4, scale = 2)
    private BigDecimal score;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String comment;

    @Column(name = "evaluated_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime evaluatedAt = LocalDateTime.now();

    @Column(nullable = false, length = 12)
    private String status;  // DRAFT/SUBMITTED
}