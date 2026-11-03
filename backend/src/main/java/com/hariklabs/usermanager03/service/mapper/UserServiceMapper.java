package com.hariklabs.usermanager03.service.mapper;

import com.hariklabs.usermanager03.persistence.api.*;
import com.hariklabs.usermanager03.service.UserValues;
import com.hariklabs.usermanager03.service.model.*;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.util.UUID;

@Component
public class UserServiceMapper {
    public UserResult result(StoredUser user) {
        return new UserResult(user.id(), user.firstName(), user.lastName(), user.dni(), user.email(),
            user.active(), user.createdAt(), user.updatedAt());
    }

    public UserInput input(StoredUser user) {
        return new UserInput(user.firstName(), user.lastName(), user.dni(), user.email());
    }

    public StoredUser stored(UUID id, UserInput input, boolean active, Instant createdAt, Instant updatedAt) {
        return new StoredUser(id, input.firstName(), input.lastName(), input.dni(), input.email(),
            UserValues.nameSearch(input.firstName()), UserValues.nameSearch(input.lastName()),
            UserValues.emailSearch(input.email()), active, createdAt, updatedAt);
    }

    public StoredUser changed(StoredUser old, UserInput input, Instant updatedAt) {
        return stored(old.id(), input, old.active(), old.createdAt(), updatedAt);
    }

    public StoredUser inactive(StoredUser old, Instant updatedAt) {
        return new StoredUser(old.id(), old.firstName(), old.lastName(), old.dni(), old.email(),
            old.firstNameSearch(), old.lastNameSearch(), old.emailSearch(), false, old.createdAt(), updatedAt);
    }

    public UserSearch search(UserQuery query) {
        return new UserSearch(query.page(), query.size(), query.status(),
            UserValues.nameSearch(query.q()), UserValues.emailSearch(query.q()), UserValues.canonicalDni(query.q()));
    }
}
