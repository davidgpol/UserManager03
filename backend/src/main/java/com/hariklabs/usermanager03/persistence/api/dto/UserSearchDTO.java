package com.hariklabs.usermanager03.persistence.api.dto;

public record UserSearchDTO(int page, int size, String status, String nameText, String emailText, String dni) {}
