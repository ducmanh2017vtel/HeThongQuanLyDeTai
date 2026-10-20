package blog.hethong_quanlydetai.repository;

import blog.hethong_quanlydetai.domain.Council;
import blog.hethong_quanlydetai.domain.CouncilStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CouncilRepository extends JpaRepository<Council, Long> {

    boolean existsByCouncilCode(String councilCode);

    @EntityGraph(attributePaths = {"period", "createdBy", "members", "members.lecturer", "members.lecturer.user", "members.lecturer.department"})
    Optional<Council> findWithDetailsById(Long id);

    @EntityGraph(attributePaths = {"period", "createdBy"})
    @Query("""
           SELECT c FROM Council c
           WHERE (:q IS NULL OR :q = '' OR LOWER(c.councilCode) LIKE LOWER(CONCAT('%', :q, '%'))
                                       OR LOWER(c.councilName) LIKE LOWER(CONCAT('%', :q, '%')))
             AND (:periodId IS NULL OR c.period.id = :periodId)
             AND (:status IS NULL OR c.status = :status)
           ORDER BY c.id DESC
           """)
    Page<Council> search(@Param("q") String q,
                         @Param("periodId") Long periodId,
                         @Param("status") CouncilStatus status,
                         Pageable pageable);

    @EntityGraph(attributePaths = {"period", "createdBy"})
    List<Council> findByPeriodId(Long periodId);

    @Query("SELECT COUNT(ct) > 0 FROM CouncilTopic ct WHERE ct.topic.id = :topicId")
    boolean isTopicAlreadyAssigned(@Param("topicId") Long topicId);
}
