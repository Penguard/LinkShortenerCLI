package com.example.shortener.ui;

import java.awt.Desktop;
import java.net.URI;

public final class BrowserOpener {
    public void open(String url) throws Exception {
        if (!Desktop.isDesktopSupported()) throw new IllegalStateException("Desktop not supported");
        Desktop.getDesktop().browse(new URI(url));
    }
}