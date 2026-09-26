package dev.nova.client.ui;

import dev.nova.client.ui.component.Rect;

/**
 * The NOVA window is authored in a fixed "design space" that matches the reference
 * screenshot pixel for pixel (a 1065x577 window on a 1536x864 screen). At render time
 * the whole window is scaled by a single factor so it keeps the same proportions on
 * every resolution and every Minecraft GUI scale.
 */
public final class MenuLayout {
    public static final int WIDTH = 1065;
    public static final int HEIGHT = 577;

    private static final float REFERENCE_SCREEN_WIDTH = 1536.0f;
    private static final float REFERENCE_SCREEN_HEIGHT = 864.0f;
    private static final int SCREEN_MARGIN = 6;
    private static final float VERTICAL_OFFSET = 15.5f;

    public static final int RADIUS = 16;
    public static final int PANEL_RADIUS = 12;

    public static final Rect SEARCH = new Rect(262, 18, 477, 47);
    public static final Rect GEAR_BUTTON = new Rect(947, 19, 46, 46);
    public static final Rect CLOSE_BUTTON = new Rect(1002, 19, 46, 46);

    public static final Rect SIDEBAR = new Rect(19, 82, 186, 479);
    public static final Rect CONTENT = new Rect(225, 82, 462, 479);
    public static final Rect SETTINGS = new Rect(702, 82, 345, 479);

    public static final int ROW_HEIGHT = 62;
    public static final int ROW_STEP = 68;

    /** Screen-space position of the design-space origin. */
    public final float originX;
    public final float originY;
    /** Screen units per design unit. */
    public final float scale;

    private MenuLayout(float originX, float originY, float scale) {
        this.originX = originX;
        this.originY = originY;
        this.scale = scale;
    }

    public static MenuLayout compute(int screenWidth, int screenHeight, float menuScale) {
        float base = Math.min(
                screenWidth / REFERENCE_SCREEN_WIDTH,
                screenHeight / REFERENCE_SCREEN_HEIGHT
        );
        float fit = Math.min(
                (screenWidth - SCREEN_MARGIN * 2) / (float) WIDTH,
                (screenHeight - SCREEN_MARGIN * 2) / (float) HEIGHT
        );
        float scale = Math.max(0.05f, Math.min(base * menuScale, fit));
        float originX = (screenWidth - WIDTH * scale) / 2.0f;
        // The reference window sits slightly above the screen center.
        float originY = Math.max(SCREEN_MARGIN, (screenHeight - HEIGHT * scale) / 2.0f - VERTICAL_OFFSET * scale);
        return new MenuLayout(originX, originY, scale);
    }

    public double toDesignX(double screenX) {
        return (screenX - originX) / scale;
    }

    public double toDesignY(double screenY) {
        return (screenY - originY) / scale;
    }

    public int toScreenX(double designX) {
        return Math.round(originX + (float) designX * scale);
    }

    public int toScreenY(double designY) {
        return Math.round(originY + (float) designY * scale);
    }

    public static Rect categoryRow(int index, int scroll) {
        return new Rect(SIDEBAR.x(), SIDEBAR.y() + index * ROW_STEP - scroll, SIDEBAR.width(), ROW_HEIGHT);
    }
}
