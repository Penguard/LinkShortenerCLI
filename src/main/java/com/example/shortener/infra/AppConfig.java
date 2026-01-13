package com.example.shortener.infra;

import java.io.FileInputStream;
import java.time.Duration;
import java.util.Properties;

public final class AppConfig {
    public final String baseUrl;
    public final Duration ttl;
    public final Duration cleanupInterval;
    public final int codeLength;
    public final boolean openBrowser;

    private AppConfig(String baseUrl, Duration ttl, Duration cleanupInterval, int codeLength, boolean openBrowser) {
        this.baseUrl = baseUrl;
        this.ttl = ttl;
        this.cleanupInterval = cleanupInterval;
        this.codeLength = codeLength;
        this.openBrowser = openBrowser;
    }

    public static AppConfig load() {
        Properties p = new Properties();
        try (FileInputStream in = new FileInputStream("config/application.properties")) {
            p.load(in);
        } catch (Exception ignored) {}

        String baseUrl = p.getProperty("app.baseUrl", "MyLink");
        long ttlSec = Long.parseLong(p.getProperty("app.ttlSeconds", "86400"));
        long cleanupSec = Long.parseLong(p.getProperty("app.cleanupIntervalSeconds", "30"));
        int codeLen = Integer.parseInt(p.getProperty("app.codeLength", "8"));
        boolean openBrowser = Boolean.parseBoolean(p.getProperty("app.openBrowser", "true"));

        return new AppConfig(baseUrl, Duration.ofSeconds(ttlSec), Duration.ofSeconds(cleanupSec), codeLen, openBrowser);
    }
}