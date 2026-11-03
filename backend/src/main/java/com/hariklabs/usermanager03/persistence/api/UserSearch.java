package com.hariklabs.usermanager03.persistence.api;

public record UserSearch(int page, int size, String status, String nameText, String emailText, String dni) {}
