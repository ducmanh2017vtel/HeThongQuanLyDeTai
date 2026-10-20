package blog.hethong_quanlydetai.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "councils")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Council extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "period_id", nullable = false)
    private RegistrationPeriod period;

    @Column(name = "council_code", nullable = false, unique = true, length = 40)
    private String councilCode;

    @Column(name = "council_name", nullable = false, length = 180)
    private String councilName;

    @Column(name = "report_date")
    private LocalDateTime reportDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private CouncilStatus status = CouncilStatus.DRAFT;

    @OneToMany(mappedBy = "council", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<CouncilMember> members = new HashSet<>();
}