package com.example.shortener.infra;

import com.example.shortener.core.model.ShortLink;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryLinkRepository {
    private final ConcurrentHashMap<String, ShortLink> byCode = new ConcurrentHashMap<>();

    public boolean exists(String code) {
        return byCode.containsKey(code);
    }

    public void save(ShortLink link) {
        byCode.put(link.code, link);
    }

    public ShortLink get(String code) {
        return byCode.get(code);
    }

    public boolean delete(String code) {
        return byCode.remove(code) != null;
    }

    public List<ShortLink> listByOwner(UUID ownerId) {
        ArrayList<ShortLink> res = new ArrayList<>();
        for (ShortLink l : byCode.values()) {
            if (l.ownerId.equals(ownerId)) res.add(l);
        }
        res.sort((a, b) -> b.createdAt.compareTo(a.createdAt));
        return res;
    }

    public int deleteExpired(Instant now) {
        int removed = 0;
        for (var e : byCode.entrySet()) {
            ShortLink l = e.getValue();
            if (l.expired(now)) {
                if (byCode.remove(e.getKey(), l)) removed++;
            }
        }
        return removed;
    }
}