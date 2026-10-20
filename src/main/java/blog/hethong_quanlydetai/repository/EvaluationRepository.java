package blog.hethong_quanlydetai.repository;

import blog.hethong_quanlydetai.domain.Evaluation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EvaluationRepository extends JpaRepository<Evaluation, Long> {

    Optional<Evaluation> findByAssignmentId(Long assignmentId);

    @Query("""
           SELECT e FROM Evaluation e
           JOIN FETCH e.assignment a
           JOIN FETCH a.topic
           WHERE a.topic.id = :topicId
           """)
    List<Evaluation> findByTopicId(@Param("topicId") Long topicId);

    @Query("""
           SELECT e FROM Evaluation e
           JOIN FETCH e.assignment a
           JOIN FETCH a.topic
           JOIN FETCH a.lecturer l
           JOIN FETCH l.user
           WHERE a.lecturer.id = :lecturerId
           ORDER BY e.evaluatedAt DESC
           """)
    List<Evaluation> findByLecturerId(@Param("lecturerId") Long lecturerId);
}
