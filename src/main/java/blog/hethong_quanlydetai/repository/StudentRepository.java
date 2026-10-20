package blog.hethong_quanlydetai.repository;

import blog.hethong_quanlydetai.domain.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

    Optional<Student> findByStudentCode(String studentCode);

    Optional<Student> findByUserId(Long userId);

    @Query("SELECT s FROM Student s WHERE " +
           "(LOWER(s.studentCode) LIKE LOWER(CONCAT('%', :q, '%')) " +
           " OR LOWER(s.user.fullName) LIKE LOWER(CONCAT('%', :q, '%')) " +
           " OR LOWER(s.user.username)  LIKE LOWER(CONCAT('%', :q, '%')))")
    List<Student> search(@Param("q") String q);
}