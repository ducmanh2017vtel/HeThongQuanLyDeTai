package blog.hethong_quanlydetai.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "topic_supervisors")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TopicSupervisor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id", nullable = false)
    private Topic topic;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lecturer_id", nullable = false)
    private Lecturer lecturer;

    @Column(name = "supervisor_order", nullable = false)
    private Integer supervisorOrder;   // 1 hoặc 2
}