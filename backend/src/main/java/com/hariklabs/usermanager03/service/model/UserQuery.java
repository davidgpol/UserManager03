package com.hariklabs.usermanager03.service.model;

public record UserQuery(int page, int size, String status, String q) {}
