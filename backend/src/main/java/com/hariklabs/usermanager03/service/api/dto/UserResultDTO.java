package com.hariklabs.usermanager03.service.api.dto;

import java.time.Instant;
import java.util.UUID;

public record UserResultDTO(UUID id, String firstName, String lastName, String dni, String email,
                         boolean active, Instant createdAt, Instant updatedAt) {}
