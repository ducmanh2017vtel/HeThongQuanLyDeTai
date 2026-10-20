package blog.hethong_quanlydetai.repository;

import blog.hethong_quanlydetai.domain.GroupMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {

    List<GroupMember> findByGroupId(Long groupId);

    Optional<GroupMember> findByGroupIdAndStudentId(Long groupId, Long studentId);

    boolean existsByGroupIdAndStudentId(Long groupId, Long studentId);

    @Modifying
    @Query("DELETE FROM GroupMember gm WHERE gm.group.id = :groupId AND gm.student.id = :studentId")
    int deleteByGroupIdAndStudentId(@Param("groupId") Long groupId,
                                    @Param("studentId") Long studentId);

    /**
     * Trả về các group_id mà SV này đang là thành viên (cùng đợt, dùng cho check "SV đã ở nhóm khác").
     */
    @Query("""
           SELECT gm.group.id FROM GroupMember gm
           WHERE gm.student.id = :studentId AND gm.group.period.id = :periodId
           """)
    List<Long> findGroupIdsByStudentAndPeriod(@Param("studentId") Long studentId,
                                              @Param("periodId") Long periodId);
}
