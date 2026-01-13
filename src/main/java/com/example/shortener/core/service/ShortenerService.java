package com.example.shortener.core.service;

import com.example.shortener.core.model.LinkStatus;
import com.example.shortener.core.model.ShortLink;
import com.example.shortener.infra.InMemoryLinkRepository;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class ShortenerService {

    public record CreateResult(UUID userId, String code, String shortUrl, Instant expiresAt) {}
    public record OpenResult(LinkStatus status, String url, int clicks, int maxClicks, Instant expiresAt, String message) {}
    public record OpResult(LinkStatus status, String message) {}

    private final InMemoryLinkRepository repo;
    private final CodeGenerator gen;
    private final String baseUrl;
    private final Duration ttl;

    public ShortenerService(InMemoryLinkRepository repo, CodeGenerator gen, String baseUrl, Duration ttl) {
        this.repo = repo;
        this.gen = gen;
        this.baseUrl = baseUrl;
        this.ttl = ttl;
    }

    public CreateResult create(UUID userIdOrNull, String url, int maxClicks) {
        UUID userId = (userIdOrNull == null) ? UUID.randomUUID() : userIdOrNull;

        String normalized = validateUrl(url);
        if (maxClicks <= 0) throw new IllegalArgumentException("maxClicks must be > 0");

        Instant now = Instant.now();
        Instant expiresAt = now.plus(ttl);

        String code;
        int attempts = 0;
        do {
            if (++attempts > 50) throw new IllegalStateException("Too many collisions");
            code = gen.generate(userId, normalized);
        } while (repo.exists(code));

        repo.save(new ShortLink(code, userId, normalized, now, expiresAt, maxClicks));
        return new CreateResult(userId, code, baseUrl + "/" + code, expiresAt);
    }

    public OpenResult open(String code) {
        //repo.deleteExpired(Instant.now());

        ShortLink link = repo.get(code);
        if (link == null) {
            return new OpenResult(LinkStatus.NOT_FOUND, null, 0, 0, null, "Link not found");
        }

        Instant now = Instant.now();
        if (link.expired(now)) {
            repo.delete(code);
            return new OpenResult(LinkStatus.EXPIRED, null, link.clicks, link.maxClicks, link.expiresAt,
                    "Link expired (removed)");
        }

        synchronized (link) {
            if (link.exhausted()) {
                return new OpenResult(LinkStatus.LIMIT_EXHAUSTED, null, link.clicks, link.maxClicks, link.expiresAt,
                        "Click limit exhausted");
            }
            link.clicks++;
            String msg = (link.clicks >= link.maxClicks)
                    ? "OK (limit exhausted after this open)"
                    : "OK";
            return new OpenResult(LinkStatus.ACTIVE, link.originalUrl, link.clicks, link.maxClicks, link.expiresAt, msg);
        }
    }

    public List<ShortLink> list(UUID userId) {
        repo.deleteExpired(Instant.now());
        return repo.listByOwner(userId);
    }

    public OpResult updateLimit(UUID userId, String code, int newLimit) {
        repo.deleteExpired(Instant.now());

        ShortLink link = repo.get(code);
        if (link == null) return new OpResult(LinkStatus.NOT_FOUND, "Link not found");
        if (!link.ownerId.equals(userId)) return new OpResult(LinkStatus.FORBIDDEN, "Only owner can update");
        if (newLimit <= 0) return new OpResult(LinkStatus.INVALID_INPUT, "newLimit must be > 0");

        synchronized (link) {
            if (newLimit < link.clicks) return new OpResult(LinkStatus.INVALID_INPUT, "newLimit must be >= current clicks");
            link.maxClicks = newLimit;
            return new OpResult(LinkStatus.ACTIVE, "Limit updated");
        }
    }

    public OpResult delete(UUID userId, String code) {
        repo.deleteExpired(Instant.now());

        ShortLink link = repo.get(code);
        if (link == null) return new OpResult(LinkStatus.NOT_FOUND, "Link not found");
        if (!link.ownerId.equals(userId)) return new OpResult(LinkStatus.FORBIDDEN, "Only owner can delete");

        repo.delete(code);
        return new OpResult(LinkStatus.ACTIVE, "Deleted");
    }

    public int cleanupExpiredNow() {
        return repo.deleteExpired(Instant.now());
    }

    private static String validateUrl(String url) {
        try {
            URI uri = URI.create(url.trim());
            String scheme = uri.getScheme();
            if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
                throw new IllegalArgumentException("URL must start with http:// or https://");
            }
            if (uri.getHost() == null || uri.getHost().isBlank()) {
                throw new IllegalArgumentException("URL must have host");
            }
            return uri.toString();
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid URL");
        }
    }
}