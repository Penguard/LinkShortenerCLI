package com.example.shortener.core.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.UUID;

public final class CodeGenerator {
    private final SecureRandom rnd = new SecureRandom();
    private final int codeLength;

    public CodeGenerator(int codeLength) {
        if (codeLength < 6 || codeLength > 16) throw new IllegalArgumentException("codeLength must be 6..16");
        this.codeLength = codeLength;
    }

    public String generate(UUID userId, String url) {
        try {
            byte[] randNum = new byte[16];
            rnd.nextBytes(randNum);

            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(userId.toString().getBytes(StandardCharsets.UTF_8));
            md.update((byte) '|');
            md.update(url.getBytes(StandardCharsets.UTF_8));
            md.update((byte) '|');
            md.update(randNum);

            String base62 = Base62.encode(md.digest());
            if (base62.length() < codeLength) {
                base62 = base62 + Base62.encode(md.digest(base62.getBytes(StandardCharsets.UTF_8)));
            }
            return base62.substring(0, codeLength);
        } catch (Exception e) {
            throw new RuntimeException("Cannot generate code", e);
        }
    }
}