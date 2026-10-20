package blog.hethong_quanlydetai.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "council_topics")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CouncilTopic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "council_id", nullable = false)
    private Council council;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id", nullable = false, unique = true)
    private Topic topic;
}