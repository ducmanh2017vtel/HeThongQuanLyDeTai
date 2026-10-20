package blog.hethong_quanlydetai.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "lecturers")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Lecturer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "lecturer_code", nullable = false, unique = true, length = 20)
    private String lecturerCode;

    @Column(name = "academic_degree", length = 80)
    private String academicDegree;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;
}