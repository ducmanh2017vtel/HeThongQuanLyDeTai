package blog.hethong_quanlydetai.repository;

import blog.hethong_quanlydetai.domain.ReviewResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewResultRepository extends JpaRepository<ReviewResult, Long> {

    Optional<ReviewResult> findByTopicId(Long topicId);

    Optional<ReviewResult> findByGroupId(Long groupId);

    @Query("""
           SELECT r FROM ReviewResult r
           JOIN FETCH r.group g
           JOIN FETCH r.topic t
           WHERE r.published = true
           ORDER BY r.publishedAt DESC
           """)
    List<ReviewResult> findPublished();

    @Query("""
           SELECT r FROM ReviewResult r
           JOIN FETCH r.group g
           JOIN FETCH g.period
           JOIN FETCH r.topic t
           WHERE g.period.id = :periodId
           """)
    List<ReviewResult> findByPeriodId(@Param("periodId") Long periodId);
}
