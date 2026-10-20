package blog.hethong_quanlydetai.repository;

import blog.hethong_quanlydetai.domain.ReviewAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewAssignmentRepository extends JpaRepository<ReviewAssignment, Long> {

    List<ReviewAssignment> findByCouncilId(Long councilId);

    List<ReviewAssignment> findByTopicId(Long topicId);

    List<ReviewAssignment> findByLecturerId(Long lecturerId);

    Optional<ReviewAssignment> findByCouncilIdAndTopicIdAndLecturerId(Long councilId, Long topicId, Long lecturerId);

    boolean existsByCouncilIdAndTopicIdAndLecturerId(Long councilId, Long topicId, Long lecturerId);

    @Query("""
           SELECT a FROM ReviewAssignment a
           JOIN FETCH a.council
           JOIN FETCH a.topic
           JOIN FETCH a.lecturer l
           JOIN FETCH l.user
           WHERE a.lecturer.id = :lecturerId
           ORDER BY a.assignedAt DESC
           """)
    List<ReviewAssignment> findDetailedByLecturerId(@Param("lecturerId") Long lecturerId);

    @Query("SELECT COUNT(a) FROM ReviewAssignment a WHERE a.topic.id = :topicId")
    long countByTopicId(@Param("topicId") Long topicId);
}
