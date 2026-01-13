package com.example.shortener;

import com.example.shortener.core.model.LinkStatus;
import com.example.shortener.core.model.ShortLink;
import com.example.shortener.core.service.CodeGenerator;
import com.example.shortener.core.service.ShortenerService;
import com.example.shortener.infra.InMemoryLinkRepository;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class ShortenerServiceTest {


    private static ShortenerService svc(Duration ttl) {
        return new ShortenerService(new InMemoryLinkRepository(), new CodeGenerator(8), "clck.ru", ttl);
    }

    @Test
    void createReturnsShortUrlAndExpiresAt() {
        var s = svc(Duration.ofSeconds(60));
        UUID u = UUID.randomUUID();

        var r = s.create(u, "https://example.com/path", 5);
        assertNotNull(r.code());
        assertEquals("clck.ru/" + r.code(), r.shortUrl());
        assertNotNull(r.expiresAt());
        assertEquals(u, r.userId());
    }

    @Test
    void sameUrlDifferentUsersProduceDifferentCodesUsually() {
        var s = svc(Duration.ofSeconds(60));
        String url = "https://dzen.ru";

        var a = s.create(UUID.randomUUID(), url, 5);
        var b = s.create(UUID.randomUUID(), url, 5);

        assertNotEquals(a.code(), b.code());
    }

    @Test
    void sameUserSameUrlTwoCreatesProduceDifferentCodes() {
        var s = svc(Duration.ofSeconds(60));
        UUID u = UUID.randomUUID();
        String url = "https://example.com";

        var a = s.create(u, url, 5);
        var b = s.create(u, url, 5);

        assertNotEquals(a.code(), b.code());
    }

    @Test
    void openNotFound() {
        var s = svc(Duration.ofSeconds(60));
        var r = s.open("NO_SUCH_CODE");
        assertEquals(LinkStatus.NOT_FOUND, r.status());
    }

    @Test
    void openCountsClicksExactly() {
        var s = svc(Duration.ofSeconds(60));
        UUID u = UUID.randomUUID();
        var c = s.create(u, "https://dzen.ru", 10);

        var r1 = s.open(c.code());
        var r2 = s.open(c.code());
        var r3 = s.open(c.code());

        assertEquals(LinkStatus.ACTIVE, r1.status());
        assertEquals(1, r1.clicks());
        assertEquals(2, r2.clicks());
        assertEquals(3, r3.clicks());
        assertEquals(10, r3.maxClicks());
    }

    @Test
    void limitBlocksAfterExactMaxClicks() {
        var s = svc(Duration.ofSeconds(60));
        UUID u = UUID.randomUUID();
        var c = s.create(u, "https://dzen.ru", 2);

        assertEquals(LinkStatus.ACTIVE, s.open(c.code()).status());
        assertEquals(LinkStatus.ACTIVE, s.open(c.code()).status());
        assertEquals(LinkStatus.LIMIT_EXHAUSTED, s.open(c.code()).status());
    }

    @Test
    void messageIndicatesLimitExhaustedOnLastAllowedOpen() {
        var s = svc(Duration.ofSeconds(60));
        UUID u = UUID.randomUUID();
        var c = s.create(u, "https://ya.ru", 1);

        var r = s.open(c.code());
        assertEquals(LinkStatus.ACTIVE, r.status());
        assertTrue(r.message().toLowerCase().contains("limit"));
    }

    @Test
    void updateLimitOwnerOnlyForbiddenForOthers() {
        var s = svc(Duration.ofSeconds(60));
        UUID owner = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        var c = s.create(owner, "https://ya.ru", 5);

        var r = s.updateLimit(other, c.code(), 10);
        assertEquals(LinkStatus.FORBIDDEN, r.status());
    }

    @Test
    void deleteOwnerOnlyForbiddenForOthers() {
        var s = svc(Duration.ofSeconds(60));
        UUID owner = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        var c = s.create(owner, "https://ya.ru", 5);

        var r = s.delete(other, c.code());
        assertEquals(LinkStatus.FORBIDDEN, r.status());
    }

    @Test
    void deleteRemovesLink() {
        var s = svc(Duration.ofSeconds(60));
        UUID owner = UUID.randomUUID();
        var c = s.create(owner, "https://ya.ru", 5);

        assertEquals(LinkStatus.ACTIVE, s.delete(owner, c.code()).status());
        assertEquals(LinkStatus.NOT_FOUND, s.open(c.code()).status());
    }

    @Test
    void listReturnsOnlyOwnersLinks() {
        var s = svc(Duration.ofSeconds(60));
        UUID u1 = UUID.randomUUID();
        UUID u2 = UUID.randomUUID();

        var a1 = s.create(u1, "https://example.com/a", 5);
        var a2 = s.create(u1, "https://example.com/b", 5);
        var b1 = s.create(u2, "https://example.com/c", 5);

        List<ShortLink> l1 = s.list(u1);
        List<ShortLink> l2 = s.list(u2);

        assertEquals(2, l1.size());
        assertEquals(1, l2.size());
        assertTrue(l1.stream().anyMatch(x -> x.code.equals(a1.code())));
        assertTrue(l1.stream().anyMatch(x -> x.code.equals(a2.code())));
        assertTrue(l2.stream().anyMatch(x -> x.code.equals(b1.code())));
    }

    @Test
    void updateLimitRejectsNonPositive() {
        var s = svc(Duration.ofSeconds(60));
        UUID owner = UUID.randomUUID();
        var c = s.create(owner, "https://ya.ru", 5);

        var r0 = s.updateLimit(owner, c.code(), 0);
        assertEquals(LinkStatus.INVALID_INPUT, r0.status());
    }

    @Test
    void updateLimitRejectsLessThanCurrentClicks() {
        var s = svc(Duration.ofSeconds(60));
        UUID owner = UUID.randomUUID();
        var c = s.create(owner, "https://ya.ru", 5);

        // make 3 clicks
        s.open(c.code());
        s.open(c.code());
        s.open(c.code());

        var r = s.updateLimit(owner, c.code(), 2);
        assertEquals(LinkStatus.INVALID_INPUT, r.status());
        assertTrue(r.message().toLowerCase().contains("current"));
    }

    @Test
    void updateLimitAllowsIncreaseAndThenMoreOpens() {
        var s = svc(Duration.ofSeconds(60));
        UUID owner = UUID.randomUUID();
        var c = s.create(owner, "https://ya.ru", 1);

        assertEquals(LinkStatus.ACTIVE, s.open(c.code()).status());
        assertEquals(LinkStatus.LIMIT_EXHAUSTED, s.open(c.code()).status());

        assertEquals(LinkStatus.ACTIVE, s.updateLimit(owner, c.code(), 3).status());

        // now we can open again (clicks currently 1, limit 3)
        assertEquals(LinkStatus.ACTIVE, s.open(c.code()).status());
        assertEquals(LinkStatus.ACTIVE, s.open(c.code()).status());
        assertEquals(LinkStatus.LIMIT_EXHAUSTED, s.open(c.code()).status());
    }

    @Test
    void invalidUrlRejected() {
        var s = svc(Duration.ofSeconds(60));
        UUID u = UUID.randomUUID();

        assertThrows(IllegalArgumentException.class, () -> s.create(u, "ftp://ya.ru", 1));
        assertThrows(IllegalArgumentException.class, () -> s.create(u, "not-a-url", 1));
        assertThrows(IllegalArgumentException.class, () -> s.create(u, "http://", 1));
    }

    @Test
    void maxClicksMustBePositive() {
        var s = svc(Duration.ofSeconds(60));
        UUID u = UUID.randomUUID();

        assertThrows(IllegalArgumentException.class, () -> s.create(u, "https://ya.ru", 0));
        assertThrows(IllegalArgumentException.class, () -> s.create(u, "https://ya.ru", -5));
    }

    @Test
    void ttlExpirationMakesLinkUnavailableAndRemovedByCleanupOnOpen() throws Exception {
        // small TTL to avoid long sleeps
        var s = svc(Duration.ofMillis(120));
        UUID u = UUID.randomUUID();
        var c = s.create(u, "https://ya.ru", 5);

        // first open OK
        assertEquals(LinkStatus.ACTIVE, s.open(c.code()).status());

        // wait to expire
        Thread.sleep(200);

        var r = s.open(c.code());
        assertEquals(LinkStatus.EXPIRED, r.status());

        // subsequent open -> NOT_FOUND (it was removed)
        var r2 = s.open(c.code());
        assertEquals(LinkStatus.NOT_FOUND, r2.status());
    }

    @Test
    void cleanupExpiredNowRemovesExpiredLinks() throws Exception {
        var s = svc(Duration.ofMillis(120));
        UUID u = UUID.randomUUID();
        var c1 = s.create(u, "https://example.com/a", 5);
        var c2 = s.create(u, "https://example.com/b", 5);

        Thread.sleep(200);
        int removed = s.cleanupExpiredNow();
        assertTrue(removed >= 2);

        assertEquals(LinkStatus.NOT_FOUND, s.open(c1.code()).status());
        assertEquals(LinkStatus.NOT_FOUND, s.open(c2.code()).status());
    }
}