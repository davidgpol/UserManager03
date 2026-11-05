package com.hariklabs.usermanager03.persistence.api.dto;

import java.time.Instant;
import java.util.UUID;

public record StoredUserDTO(UUID id, String firstName, String lastName, String dni, String email,
                         String firstNameSearch, String lastNameSearch, String emailSearch,
                         boolean active, Instant createdAt, Instant updatedAt) {}
