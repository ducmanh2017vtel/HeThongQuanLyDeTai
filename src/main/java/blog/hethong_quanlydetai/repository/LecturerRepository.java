package blog.hethong_quanlydetai.repository;

import blog.hethong_quanlydetai.domain.Lecturer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LecturerRepository extends JpaRepository<Lecturer, Long> {

    Optional<Lecturer> findByUserId(Long userId);

    Optional<Lecturer> findByLecturerCode(String lecturerCode);

    /** Tìm giảng viên đang hoạt động (user enabled=true) theo bộ môn. */
    @Query("""
           SELECT l FROM Lecturer l
           JOIN FETCH l.user u
           JOIN FETCH l.department d
           WHERE l.department.id = :departmentId
             AND u.enabled = true
             AND u.status = 'ACTIVE'
           ORDER BY u.fullName
           """)
    List<Lecturer> findActiveByDepartmentId(@Param("departmentId") Long departmentId);

    /** Tìm giảng viên đang hoạt động (user enabled=true) theo bộ môn (phân trang). */
    @Query("""
           SELECT l FROM Lecturer l
           JOIN FETCH l.user u
           JOIN FETCH l.department d
           WHERE (:departmentId IS NULL OR l.department.id = :departmentId)
             AND u.enabled = true
             AND u.status = 'ACTIVE'
           ORDER BY u.fullName
           """)
    Page<Lecturer> findActiveLecturers(@Param("departmentId") Long departmentId, Pageable pageable);
}
