package com.example.shortener.ui;

import java.util.UUID;

public final class Session {
    private UUID userId;

    public UUID ensureUser() {
        if (userId == null) userId = UUID.randomUUID();
        return userId;
    }

    public UUID userId() { return userId; }

    public void login(UUID id) { this.userId = id; }
    public void newUser() { this.userId = UUID.randomUUID(); }
}