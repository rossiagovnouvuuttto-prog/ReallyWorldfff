package dev.nova.client.config;

import dev.nova.client.theme.NovaTheme;

public final class NovaConfig {
    public static final int DEFAULT_ACCENT = NovaTheme.ACCENTS[0];
    public static final float MIN_SCALE = 0.5f;
    public static final float MAX_SCALE = 1.25f;

    public int accentColor = DEFAULT_ACCENT;
    public float menuScale = 1.0f;
    public boolean animations = true;

    public void sanitize() {
        accentColor &= 0x00FFFFFF;
        boolean known = false;
        for (int accent : NovaTheme.ACCENTS) {
            known |= accent == accentColor;
        }
        if (!known) {
            accentColor = DEFAULT_ACCENT;
        }
        if (!Float.isFinite(menuScale)) {
            menuScale = 1.0f;
        }
        menuScale = Math.max(MIN_SCALE, Math.min(MAX_SCALE, menuScale));
    }
}
