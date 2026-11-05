package com.hariklabs.usermanager03.persistence.api;

import com.hariklabs.usermanager03.persistence.api.dto.*;
import java.util.Optional;
import java.util.UUID;

public interface UserStore {
    Optional<StoredUserDTO> find(UUID id);
    Optional<StoredUserDTO> lock(UUID id);
    StoredUsersDTO search(UserSearchDTO search);
    StoredUserDTO save(StoredUserDTO user);
}
