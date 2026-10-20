package blog.hethong_quanlydetai.repository;

import blog.hethong_quanlydetai.domain.Department;
import blog.hethong_quanlydetai.domain.Topic;
import blog.hethong_quanlydetai.domain.TopicStatus;
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
public interface TopicRepository extends JpaRepository<Topic, Long> {

    boolean existsByTopicCode(String topicCode);

    @EntityGraph(attributePaths = {"period", "department", "createdByLecturer", "createdByLecturer.user", "supervisors", "supervisors.lecturer", "supervisors.lecturer.user", "approvedBy"})
    Page<Topic> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"period", "department", "createdByLecturer", "createdByLecturer.user", "supervisors", "supervisors.lecturer", "supervisors.lecturer.user", "approvedBy"})
    Page<Topic> findByStatus(TopicStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"period", "department", "createdByLecturer", "createdByLecturer.user", "supervisors", "supervisors.lecturer", "supervisors.lecturer.user", "approvedBy"})
    Page<Topic> findByDepartmentId(Long departmentId, Pageable pageable);

    @EntityGraph(attributePaths = {"period", "department", "createdByLecturer", "createdByLecturer.user", "supervisors", "supervisors.lecturer", "supervisors.lecturer.user", "approvedBy"})
    Page<Topic> findByPeriodId(Long periodId, Pageable pageable);

    @EntityGraph(attributePaths = {"period", "department", "createdByLecturer", "createdByLecturer.user", "supervisors", "supervisors.lecturer", "supervisors.lecturer.user", "approvedBy"})
    @Query("""
           SELECT t FROM Topic t
           WHERE (:q IS NULL OR :q = '' OR LOWER(t.topicCode) LIKE LOWER(CONCAT('%', :q, '%'))
                                    OR LOWER(t.topicName) LIKE LOWER(CONCAT('%', :q, '%')))
             AND (:status IS NULL OR t.status = :status)
             AND (:periodId IS NULL OR t.period.id = :periodId)
             AND (:departmentId IS NULL OR t.department.id = :departmentId)
             AND (:published IS NULL OR t.published = :published)
           ORDER BY t.id DESC
           """)
    Page<Topic> search(@Param("q") String q,
                      @Param("status") TopicStatus status,
                      @Param("periodId") Long periodId,
                      @Param("departmentId") Long departmentId,
                      @Param("published") Boolean published,
                      Pageable pageable);

    @EntityGraph(attributePaths = {"period", "department", "createdByLecturer", "createdByLecturer.user", "supervisors", "supervisors.lecturer", "supervisors.lecturer.user"})
    List<Topic> findByPeriodIdAndPublishedTrue(Long periodId);

    @EntityGraph(attributePaths = {"period", "department", "createdByLecturer", "createdByLecturer.user", "supervisors", "supervisors.lecturer", "supervisors.lecturer.user", "studentGroups"})
    Optional<Topic> findWithDetailsById(Long id);

    @Query("SELECT t FROM Topic t WHERE t.createdByLecturer.id = :lecturerId AND t.status IN :statuses")
    List<Topic> findByCreatorIdAndStatusIn(@Param("lecturerId") Long lecturerId, @Param("statuses") List<TopicStatus> statuses);

    long countByStatus(TopicStatus status);

    long countByPeriodId(Long periodId);

    @Query("SELECT t.published = true AND t.status = blog.hethong_quanlydetai.domain.TopicStatus.APPROVED FROM Topic t WHERE t.id = :topicId")
    boolean isPublishedAndApproved(@Param("topicId") Long topicId);

    @Query("SELECT COUNT(r) > 0 FROM TopicRegistration r WHERE r.topic.id = :topicId AND r.status = 'APPROVED'")
    boolean hasApprovedRegistration(@Param("topicId") Long topicId);
}
