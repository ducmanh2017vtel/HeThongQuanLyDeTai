package blog.hethong_quanlydetai.repository;

import blog.hethong_quanlydetai.domain.GroupStatus;
import blog.hethong_quanlydetai.domain.StudentGroup;
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
public interface StudentGroupRepository extends JpaRepository<StudentGroup, Long> {

    boolean existsByGroupCode(String groupCode);

    Optional<StudentGroup> findByGroupCode(String groupCode);

    /** Các nhóm trong 1 đợt. */
    @EntityGraph(attributePaths = {"period", "topic", "members", "members.student", "members.student.user"})
    List<StudentGroup> findByPeriodId(Long periodId);

    /** Phân trang: nhóm của 1 đợt, có thể lọc trạng thái. */
    @EntityGraph(attributePaths = {"period", "topic", "members", "members.student", "members.student.user"})
    @Query("""
           SELECT g FROM StudentGroup g
           WHERE g.period.id = :periodId
             AND (:status IS NULL OR g.status = :status)
           ORDER BY g.id DESC
           """)
    Page<StudentGroup> searchByPeriod(@Param("periodId") Long periodId,
                                      @Param("status") GroupStatus status,
                                      Pageable pageable);

    /**
     * Các nhóm có thành viên là SV này trong 1 đợt (để chặn SV thuộc nhiều nhóm cùng đợt).
     */
    @EntityGraph(attributePaths = {"period", "topic", "members", "members.student"})
    @Query("""
           SELECT DISTINCT g FROM StudentGroup g
           JOIN g.members m
           WHERE m.student.id = :studentId
             AND g.period.id = :periodId
           """)
    List<StudentGroup> findByMemberStudentIdAndPeriodId(@Param("studentId") Long studentId,
                                                       @Param("periodId") Long periodId);

    /**
     * Các nhóm (chưa bị REJECTED) có đăng ký đề tài do GV này phụ trách.
     * Dùng cho màn GVHD duyệt.
     */
    @EntityGraph(attributePaths = {"period", "topic", "topic.supervisors", "members", "members.student", "members.student.user"})
    @Query("""
           SELECT DISTINCT g FROM StudentGroup g
           JOIN g.topic t
           JOIN t.supervisors ts
           WHERE ts.lecturer.id = :lecturerId
             AND ts.supervisorOrder = 1
             AND t.published = true
             AND (:status IS NULL OR g.status = :status)
           ORDER BY g.id DESC
           """)
    Page<StudentGroup> findGroupsForPrimarySupervisor(@Param("lecturerId") Long lecturerId,
                                                     @Param("status") GroupStatus status,
                                                     Pageable pageable);

    /**
     * Đếm nhóm đang ở trạng thái cho trước mà có GVHD chính là giảng viên chỉ định.
     * Dùng cho dashboard "Nhóm chờ duyệt".
     */
    @Query("""
           SELECT COUNT(DISTINCT g) FROM StudentGroup g
           JOIN g.topic t
           JOIN t.supervisors ts
           WHERE ts.lecturer.id = :lecturerId
             AND ts.supervisorOrder = 1
             AND g.status = :status
           """)
    long countByPrimarySupervisorAndStatus(@Param("lecturerId") Long lecturerId,
                                           @Param("status") GroupStatus status);

    @EntityGraph(attributePaths = {"period", "topic", "topic.supervisors", "topic.supervisors.lecturer",
                                  "topic.supervisors.lecturer.user", "members", "members.student", "members.student.user"})
    Optional<StudentGroup> findWithDetailsById(Long id);
}
