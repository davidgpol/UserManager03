package com.hariklabs.usermanager03.persistence.api;

import java.util.List;

public record StoredUsers(List<StoredUser> items, long total) {}
