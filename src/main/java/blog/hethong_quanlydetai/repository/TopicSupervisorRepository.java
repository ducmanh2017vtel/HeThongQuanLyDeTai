package blog.hethong_quanlydetai.repository;

import blog.hethong_quanlydetai.domain.TopicSupervisor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TopicSupervisorRepository extends JpaRepository<TopicSupervisor, Long> {

    List<TopicSupervisor> findByTopicId(Long topicId);

    Optional<TopicSupervisor> findByTopicIdAndSupervisorOrder(Long topicId, int supervisorOrder);

    boolean existsByTopicIdAndLecturerId(Long topicId, Long lecturerId);

    void deleteByTopicId(Long topicId);

    @Query("SELECT ts FROM TopicSupervisor ts WHERE ts.lecturer.id = :lecturerId")
    List<TopicSupervisor> findByLecturerId(@Param("lecturerId") Long lecturerId);

    @Query("SELECT COUNT(ts) FROM TopicSupervisor ts WHERE ts.topic.id = :topicId")
    long countByTopicId(@Param("topicId") Long topicId);
}
