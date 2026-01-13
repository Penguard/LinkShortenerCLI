package com.example.shortener;

import com.example.shortener.ui.CliApp;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CliAppNormalizeTest {

    @Test
    void baseUrlSlashIsRecognized() {
        String out = CliApp.normalizeShortLinkInput("clck.ru/ABC123xy", "clck.ru");
        assertEquals("open ABC123xy", out);
    }

    @Test
    void httpsSchemeIsRecognized() {
        String out = CliApp.normalizeShortLinkInput("https://clck.ru/ABC123xy", "clck.ru");
        assertEquals("open ABC123xy", out);
    }

    @Test
    void commandInputNotTouched() {
        String out = CliApp.normalizeShortLinkInput("open ABC123xy", "clck.ru");
        assertEquals("open ABC123xy", out);
    }

    @Test
    void unrelatedTextNotTouched() {
        String out = CliApp.normalizeShortLinkInput("example.com/ABC", "clck.ru");
        assertEquals("example.com/ABC", out);
    }
}