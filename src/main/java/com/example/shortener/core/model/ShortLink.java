package com.example.shortener.core.model;

import java.time.Instant;
import java.util.UUID;

public final class ShortLink {
    public final String code;
    public final UUID ownerId;
    public final String originalUrl;
    public final Instant createdAt;
    public final Instant expiresAt;

    public int clicks;
    public int maxClicks;

    public ShortLink(String code, UUID ownerId, String originalUrl,
                     Instant createdAt, Instant expiresAt, int maxClicks) {
        this.code = code;
        this.ownerId = ownerId;
        this.originalUrl = originalUrl;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.maxClicks = maxClicks;
        this.clicks = 0;
    }

    public boolean expired(Instant now) {
        return !now.isBefore(expiresAt);
    }

    public boolean exhausted() {
        return clicks >= maxClicks;
    }
}