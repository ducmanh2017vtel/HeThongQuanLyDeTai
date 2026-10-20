package blog.hethong_quanlydetai.repository;

import blog.hethong_quanlydetai.domain.Notification;
import blog.hethong_quanlydetai.domain.NotificationStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @EntityGraph(attributePaths = {"createdBy", "targetRoles"})
    List<Notification> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"createdBy", "targetRoles"})
    List<Notification> findByStatusOrderByCreatedAtDesc(NotificationStatus status);

    @EntityGraph(attributePaths = {"targetRoles"})
    List<Notification> findByStatus(NotificationStatus status);
}
