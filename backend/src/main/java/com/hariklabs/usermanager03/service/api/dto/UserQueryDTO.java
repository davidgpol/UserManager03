package com.hariklabs.usermanager03.service.api.dto;

public record UserQueryDTO(int page, int size, String status, String q) {}
