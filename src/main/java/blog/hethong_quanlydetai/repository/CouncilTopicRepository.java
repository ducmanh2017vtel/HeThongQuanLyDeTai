package blog.hethong_quanlydetai.repository;

import blog.hethong_quanlydetai.domain.CouncilTopic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CouncilTopicRepository extends JpaRepository<CouncilTopic, Long> {

    List<CouncilTopic> findByCouncilId(Long councilId);

    Optional<CouncilTopic> findByTopicId(Long topicId);

    void deleteByCouncilId(Long councilId);
}
