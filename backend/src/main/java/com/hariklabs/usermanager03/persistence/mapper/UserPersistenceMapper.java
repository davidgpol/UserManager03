package com.hariklabs.usermanager03.persistence.mapper;

import com.hariklabs.usermanager03.persistence.UserEntity;
import com.hariklabs.usermanager03.persistence.api.dto.StoredUserDTO;
import org.springframework.stereotype.Component;

@Component
public class UserPersistenceMapper {
    public StoredUserDTO fromEntity(UserEntity entity) {
        return new StoredUserDTO(entity.getId(), entity.getFirstName(), entity.getLastName(), entity.getDni(), entity.getEmail(),
            entity.getFirstNameSearch(), entity.getLastNameSearch(), entity.getEmailSearch(), entity.isActive(), entity.getCreatedAt(), entity.getUpdatedAt());
    }

    public UserEntity toEntity(StoredUserDTO user) {
        var entity = new UserEntity(user.id(), user.createdAt());
        entity.edit(user.firstName(), user.lastName(), user.dni(), user.email(), user.firstNameSearch(),
            user.lastNameSearch(), user.emailSearch(), user.updatedAt());
        if (!user.active()) entity.deactivate(user.updatedAt());
        return entity;
    }
}
