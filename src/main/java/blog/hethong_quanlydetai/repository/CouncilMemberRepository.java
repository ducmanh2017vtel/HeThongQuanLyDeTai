package blog.hethong_quanlydetai.repository;

import blog.hethong_quanlydetai.domain.CouncilMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CouncilMemberRepository extends JpaRepository<CouncilMember, Long> {

    List<CouncilMember> findByCouncilId(Long councilId);

    Optional<CouncilMember> findByCouncilIdAndLecturerId(Long councilId, Long lecturerId);

    void deleteByCouncilId(Long councilId);

    boolean existsByCouncilIdAndLecturerId(Long councilId, Long lecturerId);
}
