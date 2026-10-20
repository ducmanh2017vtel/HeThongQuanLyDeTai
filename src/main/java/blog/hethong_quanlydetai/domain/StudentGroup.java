package blog.hethong_quanlydetai.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "student_groups")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StudentGroup extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "period_id", nullable = false)
    private RegistrationPeriod period;

    @Column(name = "group_code", nullable = false, unique = true, length = 40)
    private String groupCode;

    @Column(name = "group_name", nullable = false, length = 180)
    private String groupName;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id", unique = true)
    private Topic topic;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    @Builder.Default
    private GroupStatus status = GroupStatus.DRAFT;

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<GroupMember> members = new HashSet<>();
}