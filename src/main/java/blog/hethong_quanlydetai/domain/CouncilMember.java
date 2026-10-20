package blog.hethong_quanlydetai.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "council_members")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CouncilMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "council_id", nullable = false)
    private Council council;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lecturer_id", nullable = false)
    private Lecturer lecturer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private CouncilMemberRole role;
}