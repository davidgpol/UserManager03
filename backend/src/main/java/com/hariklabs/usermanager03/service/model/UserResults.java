package com.hariklabs.usermanager03.service.model;

import java.util.List;

public record UserResults(List<UserResult> items, int page, int size, long total) {}
