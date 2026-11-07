package com.cse3230.university.repository;

import com.cse3230.university.entity.*;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
    Optional<UserAccount> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByEmailAndIdNot(String email, Long id);
    boolean existsByRoleAndEnabledTrue(Role role);

    // Serialize admin deletions so concurrent requests cannot both remove the last admins.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from UserAccount a where a.role = :role order by a.id")
    List<UserAccount> lockAccountsByRole(@Param("role") Role role);
}
