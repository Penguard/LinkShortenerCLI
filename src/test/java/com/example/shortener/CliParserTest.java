package com.example.shortener;

import com.example.shortener.ui.CliParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CliParserTest {

    @Test
    void parsesQuotedUrl() {
        CliParser p = new CliParser();
        var cmd = p.parse("create \"https://example.com/a?q=1\" 5");
        assertEquals("create", cmd.name());
        assertEquals(2, cmd.args().size());
        assertEquals("https://example.com/a?q=1", cmd.args().get(0));
        assertEquals("5", cmd.args().get(1));
    }

    @Test
    void emptyLineReturnsEmptyCmd() {
        CliParser p = new CliParser();
        var cmd = p.parse("   ");
        assertEquals("", cmd.name());
        assertEquals(0, cmd.args().size());
    }

    @Test
    void parsesSimpleCommand() {
        CliParser p = new CliParser();
        var cmd = p.parse("open ABC123xy");
        assertEquals("open", cmd.name());
        assertEquals(1, cmd.args().size());
        assertEquals("ABC123xy", cmd.args().get(0));
    }
}