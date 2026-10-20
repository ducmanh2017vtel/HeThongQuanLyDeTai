package blog.hethong_quanlydetai.repository;

import blog.hethong_quanlydetai.domain.Report;
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
public interface ReportRepository extends JpaRepository<Report, Long> {

    @EntityGraph(attributePaths = {"group", "group.period", "group.topic", "submittedBy"})
    Optional<Report> findWithDetailsById(Long id);

    @EntityGraph(attributePaths = {"group", "group.period", "group.topic", "submittedBy"})
    Page<Report> findByGroupIdOrderByVersionDesc(Long groupId, Pageable pageable);

    @EntityGraph(attributePaths = {"group", "group.period", "group.topic", "submittedBy"})
    @Query("""
           SELECT r FROM Report r
           WHERE r.group.id = :groupId
             AND r.status = 'SUBMITTED'
           ORDER BY r.version DESC
           """)
    List<Report> findActiveByGroupId(@Param("groupId") Long groupId);

    @Query("SELECT COALESCE(MAX(r.version), 0) FROM Report r WHERE r.group.id = :groupId")
    int findMaxVersionByGroupId(@Param("groupId") Long groupId);

    @Query("SELECT COUNT(r) FROM Report r WHERE r.group.id = :groupId")
    long countByGroupId(@Param("groupId") Long groupId);

    /**
     * Báo cáo theo đề tài (dùng cho GVHD xem tất cả các nhóm làm đề tài của mình).
     */
    @EntityGraph(attributePaths = {"group", "group.period", "group.topic", "submittedBy"})
    @Query("""
           SELECT r FROM Report r
           WHERE r.group.topic.id = :topicId
           ORDER BY r.submittedAt DESC
           """)
    Page<Report> findByTopicId(@Param("topicId") Long topicId, Pageable pageable);

    @EntityGraph(attributePaths = {"group", "group.period", "group.topic", "submittedBy"})
    @Query("""
           SELECT r FROM Report r
           WHERE (:q IS NULL OR :q = ''
                  OR LOWER(r.reportTitle) LIKE LOWER(CONCAT('%', :q, '%'))
                  OR LOWER(r.fileName)     LIKE LOWER(CONCAT('%', :q, '%')))
           ORDER BY r.submittedAt DESC
           """)
    Page<Report> search(@Param("q") String q, Pageable pageable);
}
