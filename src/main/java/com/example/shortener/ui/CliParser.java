package com.example.shortener.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class CliParser {
    public record Cmd(String name, List<String> args) {}

    public Cmd parse(String line) {
        String s = (line == null) ? "" : line.trim();
        if (s.isEmpty()) return new Cmd("", List.of());
        List<String> parts = split(s);
        return new Cmd(parts.get(0).toLowerCase(Locale.ROOT), parts.subList(1, parts.size()));
    }

    private static List<String> split(String s) {
        ArrayList<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean q = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"') { q = !q; continue; }
            if (!q && Character.isWhitespace(c)) {
                if (cur.length() > 0) { out.add(cur.toString()); cur.setLength(0); }
            } else cur.append(c);
        }
        if (cur.length() > 0) out.add(cur.toString());
        return out;
    }
}