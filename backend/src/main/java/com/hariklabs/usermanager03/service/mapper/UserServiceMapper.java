package com.hariklabs.usermanager03.service.mapper;

import com.hariklabs.usermanager03.persistence.api.dto.*;
import com.hariklabs.usermanager03.service.api.dto.*;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.util.UUID;

@Component
public class UserServiceMapper {
    public UserResultDTO result(StoredUserDTO user) {
        return new UserResultDTO(user.id(), user.firstName(), user.lastName(), user.dni(), user.email(),
            user.active(), user.createdAt(), user.updatedAt());
    }

    public UserInputDTO input(StoredUserDTO user) {
        return new UserInputDTO(user.firstName(), user.lastName(), user.dni(), user.email());
    }

    public StoredUserDTO stored(UUID id, UserInputDTO input, String firstNameSearch, String lastNameSearch,
                                String emailSearch, boolean active, Instant createdAt, Instant updatedAt) {
        return new StoredUserDTO(id, input.firstName(), input.lastName(), input.dni(), input.email(),
            firstNameSearch, lastNameSearch, emailSearch, active, createdAt, updatedAt);
    }

    public UserSearchDTO search(UserQueryDTO query, String nameText, String emailText, String dni) {
        return new UserSearchDTO(query.page(), query.size(), query.status(),
            nameText, emailText, dni);
    }
}
