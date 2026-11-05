package com.hariklabs.usermanager03.service.api.dto;

import java.util.List;

public record UserResultsDTO(List<UserResultDTO> items, int page, int size, long total) {}
