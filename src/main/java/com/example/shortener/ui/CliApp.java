package com.example.shortener.ui;

import com.example.shortener.core.model.ShortLink;
import com.example.shortener.core.service.CodeGenerator;
import com.example.shortener.core.service.ShortenerService;
import com.example.shortener.infra.ExpiryCleanup;
import com.example.shortener.infra.AppConfig;
import com.example.shortener.infra.InMemoryLinkRepository;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.UUID;

public final class CliApp {
    public static void main(String[] args) throws Exception {
        AppConfig cfg = AppConfig.load();

        var repo = new InMemoryLinkRepository();
        var gen = new CodeGenerator(cfg.codeLength);
        var service = new ShortenerService(repo, gen, cfg.baseUrl, cfg.ttl);

        var session = new Session();
        var parser = new CliParser();
        var opener = new BrowserOpener();

        try (var cleanup = new ExpiryCleanup(service, cfg.cleanupInterval)) {
            System.out.println("URL Shortener CLI");
            System.out.println("Type 'help'.");

            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            while (true) {
                System.out.print("> ");
                String line = br.readLine();
                if (line == null) break;

                line = normalizeShortLinkInput(line, cfg.baseUrl);

                var cmd = parser.parse(line);
                if (cmd.name().isEmpty()) continue;

                try {
                    switch (cmd.name()) {
                        case "help" -> help();
                        case "whoami" -> System.out.println("user=" + session.ensureUser());
                        case "new-user" -> { session.newUser(); System.out.println("user=" + session.userId()); }
                        case "login" -> {
                            if (cmd.args().size() != 1) { System.out.println("Usage: login <uuid>"); break; }
                            session.login(UUID.fromString(cmd.args().get(0)));
                            System.out.println("user=" + session.userId());
                        }
                        case "create" -> {
                            if (cmd.args().size() != 2) { System.out.println("Usage: create \"<url>\" <maxClicks>"); break; }
                            UUID u = session.ensureUser();
                            String url = cmd.args().get(0);
                            int max = Integer.parseInt(cmd.args().get(1));
                            var res = service.create(u, url, max);
                            System.out.printf("OK short=%s code=%s expiresAt=%s user=%s%n",
                                    res.shortUrl(), res.code(), res.expiresAt(), res.userId());
                        }
                        case "open" -> {
                            if (cmd.args().size() != 1) { System.out.println("Usage: open <code>"); break; }
                            String code = cmd.args().get(0);
                            var res = service.open(code);
                            System.out.printf("status=%s clicks=%d/%d expiresAt=%s msg=%s%n",
                                    res.status(), res.clicks(), res.maxClicks(), res.expiresAt(), res.message());

                            if (res.status().name().equals("ACTIVE") && res.url() != null) {
                                if (cfg.openBrowser) {
                                    opener.open(res.url());
                                    System.out.println("Opened: " + res.url());
                                } else {
                                    System.out.println("URL: " + res.url());
                                }
                            }
                        }
                        case "list" -> {
                            UUID u = session.ensureUser();
                            List<ShortLink> links = service.list(u);
                            if (links.isEmpty()) {
                                System.out.println("No links for user=" + u);
                            } else {
                                for (ShortLink l : links) {
                                    System.out.printf("- code=%s url=%s clicks=%d/%d expiresAt=%s%n",
                                            l.code, l.originalUrl, l.clicks, l.maxClicks, l.expiresAt);
                                }
                            }
                        }
                        case "update-limit" -> {
                            if (cmd.args().size() != 2) { System.out.println("Usage: update-limit <code> <newLimit>"); break; }
                            UUID u = session.ensureUser();
                            var r = service.updateLimit(u, cmd.args().get(0), Integer.parseInt(cmd.args().get(1)));
                            System.out.printf("status=%s msg=%s%n", r.status(), r.message());
                        }
                        case "delete" -> {
                            if (cmd.args().size() != 1) { System.out.println("Usage: delete <code>"); break; }
                            UUID u = session.ensureUser();
                            var r = service.delete(u, cmd.args().get(0));
                            System.out.printf("status=%s msg=%s%n", r.status(), r.message());
                        }
                        case "cleanup" -> System.out.println("removed=" + service.cleanupExpiredNow());
                        case "exit", "quit" -> { System.out.println("Bye"); return; }
                        default -> System.out.println("Unknown command. Type 'help'.");
                    }
                } catch (Exception e) {
                    System.out.println("ERROR: " + e.getMessage());
                }
            }
        }
    }

    private static void help() {
        System.out.println("""
Commands:
  help
  whoami
  new-user
  login <uuid>
  create "<url>" <maxClicks>
  open <code>
  list
  update-limit <code> <newLimit>
  delete <code>
  cleanup
  exit|quit
""");
    }
    // для работы строки по baseUrl/code или https.//baseUrl/code
    public static String normalizeShortLinkInput(String line, String baseUrl) {
        if (line == null) return "";
        String s = line.trim();
        if (s.isEmpty()) return s;

        if (s.contains(" ")) return s;

        String b = baseUrl == null ? "" : baseUrl.trim();
        if (b.isEmpty()) return s;


        String sNoScheme = stripScheme(s);
        String bNoScheme = stripScheme(b);

        // точное совпадение префикса "baseUrl/"
        String prefix = bNoScheme.endsWith("/") ? bNoScheme : (bNoScheme + "/");
        if (sNoScheme.startsWith(prefix) && sNoScheme.length() > prefix.length()) {
            String code = sNoScheme.substring(prefix.length());
            code = trimSlashes(code);
            if (!code.isEmpty()) return "open " + code;
        }

        return s;
    }

    private static String stripScheme(String s) {
        String x = s.trim();
        if (x.regionMatches(true, 0, "http://", 0, 7)) return x.substring(7);
        if (x.regionMatches(true, 0, "https://", 0, 8)) return x.substring(8);
        return x;
    }

    private static String trimSlashes(String s) {
        int start = 0, end = s.length();
        while (start < end && s.charAt(start) == '/') start++;
        while (end > start && s.charAt(end - 1) == '/') end--;
        return s.substring(start, end);
    }
}

