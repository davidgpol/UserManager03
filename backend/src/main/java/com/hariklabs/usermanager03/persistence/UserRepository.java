package com.hariklabs.usermanager03.persistence;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.*;

public interface UserRepository extends JpaRepository<UserEntity,UUID>,JpaSpecificationExecutor<UserEntity> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from UserEntity u where u.id=:id")
    Optional<UserEntity> lockById(@Param("id") UUID id);
}
