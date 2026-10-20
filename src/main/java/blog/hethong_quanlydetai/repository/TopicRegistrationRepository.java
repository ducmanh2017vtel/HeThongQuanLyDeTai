package blog.hethong_quanlydetai.repository;

import blog.hethong_quanlydetai.domain.RegistrationStatus;
import blog.hethong_quanlydetai.domain.TopicRegistration;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TopicRegistrationRepository extends JpaRepository<TopicRegistration, Long> {

    @EntityGraph(attributePaths = {"group", "topic", "approvedBy"})
    Optional<TopicRegistration> findByGroupId(Long groupId);

    @EntityGraph(attributePaths = {"group", "topic", "approvedBy"})
    List<TopicRegistration> findByTopicId(Long topicId);

    boolean existsByTopicIdAndStatus(Long topicId, RegistrationStatus status);

    /**
     * Lấy đăng ký đang PENDING hoặc APPROVED cho 1 topic (dùng để xác định
     * 1 đề tài chỉ gắn 1 nhóm).
     */
    @Query("""
           SELECT r FROM TopicRegistration r
           WHERE r.topic.id = :topicId
             AND r.status IN ('PENDING', 'APPROVED')
           """)
    List<TopicRegistration> findActiveByTopicId(@Param("topicId") Long topicId);

    long countByTopicIdAndStatus(Long topicId, RegistrationStatus status);
}
