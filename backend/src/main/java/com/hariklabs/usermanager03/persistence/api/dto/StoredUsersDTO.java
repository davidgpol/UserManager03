package com.hariklabs.usermanager03.persistence.api.dto;

import java.util.List;

public record StoredUsersDTO(List<StoredUserDTO> items, long total) {}
