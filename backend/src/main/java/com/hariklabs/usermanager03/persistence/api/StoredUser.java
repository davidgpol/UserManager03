package com.hariklabs.usermanager03.persistence.api;

import java.time.Instant;
import java.util.UUID;

public record StoredUser(UUID id, String firstName, String lastName, String dni, String email,
                         String firstNameSearch, String lastNameSearch, String emailSearch,
                         boolean active, Instant createdAt, Instant updatedAt) {}
