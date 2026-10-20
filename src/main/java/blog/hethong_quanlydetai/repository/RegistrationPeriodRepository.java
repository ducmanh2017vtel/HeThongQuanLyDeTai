package blog.hethong_quanlydetai.repository;

import blog.hethong_quanlydetai.domain.PeriodStatus;
import blog.hethong_quanlydetai.domain.PeriodType;
import blog.hethong_quanlydetai.domain.RegistrationPeriod;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RegistrationPeriodRepository extends JpaRepository<RegistrationPeriod, Long> {

    @EntityGraph(attributePaths = {"createdBy"})
    Page<RegistrationPeriod> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"createdBy"})
    Page<RegistrationPeriod> findByPeriodType(PeriodType type, Pageable pageable);

    @EntityGraph(attributePaths = {"createdBy"})
    Page<RegistrationPeriod> findByStatus(PeriodStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"createdBy"})
    Page<RegistrationPeriod> findByPeriodTypeAndStatus(PeriodType type, PeriodStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"createdBy"})
    @Query("""
           SELECT p FROM RegistrationPeriod p
           WHERE (:q IS NULL OR :q = '' OR LOWER(p.name) LIKE LOWER(CONCAT('%', :q, '%')))
             AND (:type IS NULL OR p.periodType = :type)
             AND (:status IS NULL OR p.status = :status)
           """)
    Page<RegistrationPeriod> search(@Param("q") String q,
                                    @Param("type") PeriodType type,
                                    @Param("status") PeriodStatus status,
                                    Pageable pageable);

    /**
     * Đợt hiện đang mở cho sinh viên (PUBLISHED, trong khung student_start..student_end).
     * Dùng cho dashboard/tổng quan.
     */
    @Query("""
           SELECT p FROM RegistrationPeriod p
           WHERE p.status = blog.hethong_quanlydetai.domain.PeriodStatus.PUBLISHED
             AND p.studentStartAt <= :now AND p.studentEndAt >= :now
           ORDER BY p.studentEndAt ASC
           """)
    List<RegistrationPeriod> findOpenForStudents(@Param("now") LocalDateTime now);

    /**
     * Đợt hiện đang mở cho giảng viên.
     */
    @Query("""
           SELECT p FROM RegistrationPeriod p
           WHERE p.status = blog.hethong_quanlydetai.domain.PeriodStatus.PUBLISHED
             AND p.lecturerStartAt <= :now AND p.lecturerEndAt >= :now
           ORDER BY p.lecturerEndAt ASC
           """)
    List<RegistrationPeriod> findOpenForLecturers(@Param("now") LocalDateTime now);

    Optional<RegistrationPeriod> findFirstByOrderByIdAsc();

    boolean existsByNameIgnoreCase(String name);

    @Query("SELECT COUNT(t) > 0 FROM Topic t WHERE t.period.id = :periodId")
    boolean hasAnyTopic(@Param("periodId") Long periodId);
}
