package com.hariklabs.usermanager03.persistence.api;

import java.util.Optional;
import java.util.UUID;

public interface UserStore {
    Optional<StoredUser> find(UUID id);
    Optional<StoredUser> lock(UUID id);
    StoredUsers search(UserSearch search);
    StoredUser save(StoredUser user);
}
