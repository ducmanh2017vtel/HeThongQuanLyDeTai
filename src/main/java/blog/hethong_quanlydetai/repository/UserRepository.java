package blog.hethong_quanlydetai.repository;

import blog.hethong_quanlydetai.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    @EntityGraph(attributePaths = {"roles", "roles.permissions"})
    Optional<User> findByUsername(String username);

    @EntityGraph(attributePaths = {"roles", "roles.permissions"})
    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    @Query("SELECT DISTINCT u FROM User u " +
           "LEFT JOIN u.roles r " +
           "WHERE (:q IS NULL OR :q = '' OR LOWER(u.username) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "       OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "       OR LOWER(u.email)    LIKE LOWER(CONCAT('%', :q, '%'))) " +
           "AND (:roleId IS NULL OR r.id = :roleId) " +
           "AND (:status IS NULL OR u.status = :status)")
    Page<User> search(@Param("q") String q,
                      @Param("roleId") Long roleId,
                      @Param("status") String status,
                      Pageable pageable);

    @Modifying
    @Query("UPDATE User u SET u.failedAttempts = :attempts, u.lockedUntil = :lockedUntil WHERE u.id = :id")
    void updateLockState(@Param("id") Long id,
                         @Param("attempts") Integer attempts,
                         @Param("lockedUntil") LocalDateTime lockedUntil);

    @Query("SELECT u FROM User u WHERE u.lockedUntil IS NOT NULL AND u.lockedUntil < :now")
    List<User> findExpiredLocks(@Param("now") LocalDateTime now);
}