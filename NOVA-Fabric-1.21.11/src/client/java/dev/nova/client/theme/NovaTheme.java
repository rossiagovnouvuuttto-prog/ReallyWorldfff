package dev.nova.client.theme;

/** Colors sampled from the NOVA reference screenshot. */
public final class NovaTheme {
    public static final int WINDOW_SHADOW = 0x55000000;
    public static final int WINDOW_BORDER = 0xFF37354C;
    public static final int WINDOW = 0xFF0E0F14;

    public static final int PANEL = 0xFF16161E;
    public static final int PANEL_BORDER = 0xFF1F1F29;
    public static final int ROW = 0xFF18181F;
    public static final int ROW_BORDER = 0xFF22222C;
    public static final int ROW_HOVER = 0xFF1F1F29;
    public static final int CARD = 0xFF1C1C26;
    public static final int CARD_HOVER = 0xFF23232F;

    public static final int BUTTON = 0xFF1A1B25;
    public static final int BUTTON_HOVER = 0xFF242533;
    public static final int SEARCH = 0xFF101117;
    public static final int SEARCH_BORDER = 0xFF383A4E;
    public static final int KEY = 0xFF1C1E2A;
    public static final int KEY_BORDER = 0xFF282A39;
    public static final int RESET = 0xFF2B2D41;
    public static final int RESET_HOVER = 0xFF35384F;
    public static final int TRACK = 0xFF2A2B39;
    public static final int TOGGLE_OFF = 0xFF2E3042;
    public static final int DIVIDER = 0xFF24242F;

    public static final int TEXT = 0xFFF2F2F8;
    public static final int TEXT_CATEGORY = 0xFFD2D0EE;
    public static final int TEXT_LABEL = 0xFFC3C3D7;
    public static final int TEXT_MUTED = 0xFF9091AC;
    public static final int TEXT_PLACEHOLDER = 0x9C9DB4;
    public static final int ICON = 0xFFBAB7E8;
    public static final int ICON_DIM = 0xFF5B5D76;
    public static final int KNOB = 0xFFEEEAF8;

    public static final int[] ACCENTS = {
            0x8B3FF0,
            0x3E78F2,
            0x2BC6EE,
            0x2FD66C,
            0xF2DC3C,
            0xF7AE36,
            0xF45A3A,
            0xF2539B
    };

    private NovaTheme() {
    }

    public static int argb(int rgb) {
        return 0xFF000000 | (rgb & 0x00FFFFFF);
    }

    public static int withAlpha(int rgb, int alpha) {
        return ((alpha & 0xFF) << 24) | (rgb & 0x00FFFFFF);
    }

    /** Mixes {@code rgb} over {@code base} and returns an opaque color. */
    public static int mix(int base, int rgb, float amount) {
        float k = Math.max(0.0f, Math.min(1.0f, amount));
        int r = Math.round(((base >> 16) & 0xFF) * (1 - k) + ((rgb >> 16) & 0xFF) * k);
        int g = Math.round(((base >> 8) & 0xFF) * (1 - k) + ((rgb >> 8) & 0xFF) * k);
        int b = Math.round((base & 0xFF) * (1 - k) + (rgb & 0xFF) * k);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
}
