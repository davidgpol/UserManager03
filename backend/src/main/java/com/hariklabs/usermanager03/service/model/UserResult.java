package com.hariklabs.usermanager03.service.model;

import java.time.Instant;
import java.util.UUID;

public record UserResult(UUID id, String firstName, String lastName, String dni, String email,
                         boolean active, Instant createdAt, Instant updatedAt) {}
