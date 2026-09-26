package dev.nova.client.ui;

import dev.nova.client.NovaKeybinds;
import dev.nova.client.config.ConfigManager;
import dev.nova.client.config.NovaConfig;
import dev.nova.client.module.Category;
import dev.nova.client.module.ModuleRegistry;
import dev.nova.client.module.NovaModule;
import dev.nova.client.module.ToggleableNovaModule;
import dev.nova.client.theme.NovaTheme;
import dev.nova.client.ui.component.NovaText;
import dev.nova.client.ui.component.NovaText.Weight;
import dev.nova.client.ui.component.Rect;
import dev.nova.client.ui.component.UiRender;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Locale;

/**
 * NOVA menu. Everything inside the window is drawn in {@link MenuLayout}'s design space
 * (the reference screenshot's pixels) under a single scale transform. Text uses the
 * bundled Inter font through {@link NovaText}, rasterized at the real screen resolution.
 */
public final class NovaScreen extends Screen {
    // Font sizes in design pixels, measured on the reference screenshot.
    private static final float CATEGORY_TEXT = 17.0f;
    private static final float TITLE_TEXT = 18.0f;
    private static final float LABEL_TEXT = 15.0f;
    private static final float ROW_LABEL_TEXT = 14.6f;
    private static final float KEY_TEXT = 13.5f;
    private static final float BUTTON_TEXT = 14.6f;
    private static final float EMPTY_TEXT = 17.4f;
    private static final float SEARCH_TEXT = 14.5f;
    private static final float MODULE_NAME_TEXT = 16.0f;
    private static final float MODULE_DESCRIPTION_TEXT = 13.0f;

    // Right-hand "Menu settings" panel, in design space.
    private static final int SETTINGS_LEFT = 725;
    private static final int SETTINGS_RIGHT = 1026;
    private static final int SETTINGS_TITLE_Y = 115;
    private static final int ACCENT_LABEL_Y = 159;
    private static final int SWATCH_Y = 199;
    private static final int[] SWATCH_X = {745, 786, 823, 860, 897, 934, 971, 1008};
    private static final int SWATCH_RADIUS = 11;
    private static final int SCALE_LABEL_Y = 247;
    private static final Rect SCALE_TRACK = new Rect(SETTINGS_LEFT, 274, 238, 8);
    private static final int ANIMATIONS_Y = 328;
    private static final Rect ANIMATIONS_TOGGLE = new Rect(975, 313, 51, 30);
    private static final int DIVIDER_Y = 359;
    private static final int CONTROLS_TITLE_Y = 385;
    private static final int OPEN_MENU_ROW_Y = 408;
    private static final int KILLAURA_ROW_Y = 450;
    private static final int KEY_HEIGHT = 36;
    private static final int KEY_MIN_WIDTH = 99;
    private static final Rect RESET_BUTTON = new Rect(SETTINGS_LEFT, 503, SETTINGS_RIGHT - SETTINGS_LEFT, 48);

    // Search field text area, in design space.
    private static final int SEARCH_TEXT_LEFT = MenuLayout.SEARCH.x() + 47;
    private static final int SEARCH_TEXT_RIGHT = MenuLayout.SEARCH.right() - 22;
    private static final int SEARCH_MAX_LENGTH = 80;

    // Module list in the center panel, in design space.
    private static final int CONTENT_PADDING = 16;
    private static final int CARD_HEIGHT = 64;
    private static final int CARD_GAP = 10;

    private Category selectedCategory = Category.COMBAT;
    private MenuLayout layout;
    private String searchQuery = "";
    private boolean searchFocused;
    private float openProgress;
    private float pop = 1.0f;
    private int contentScroll;
    private boolean draggingScale;

    public NovaScreen() {
        super(Text.translatable("screen.nova_client.title"));
    }

    @Override
    protected void init() {
        layout = MenuLayout.compute(width, height, ConfigManager.get().menuScale);
        clampScroll();
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // The reference keeps the world fully visible: no blur and no dark overlay.
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        NovaConfig config = ConfigManager.get();
        if (layout == null) {
            layout = MenuLayout.compute(width, height, config.menuScale);
        }

        if (!config.animations) {
            openProgress = 1.0f;
        } else if (openProgress < 1.0f) {
            openProgress = Math.min(1.0f, openProgress + 0.12f * Math.max(0.25f, delta));
        }
        float eased = 1.0f - (1.0f - openProgress) * (1.0f - openProgress) * (1.0f - openProgress);
        pop = 0.94f + 0.06f * eased;

        double designMouseX = layout.toDesignX(mouseX);
        double designMouseY = layout.toDesignY(mouseY);
        int accent = NovaTheme.argb(config.accentColor);

        MatrixStack matrices = context.getMatrices();
        pushDesign(matrices);
        renderWindow(context, designMouseX, designMouseY, accent);
        popDesign(matrices);

        // Module cards are clipped to the center panel. The scissor rectangle is given in
        // screen coordinates while no transform is active.
        List<NovaModule> modules = visibleModules();
        if (!modules.isEmpty()) {
            Rect viewport = contentViewport();
            context.enableScissor(
                    (int) Math.floor(screenX(viewport.x())), (int) Math.floor(screenY(viewport.y())),
                    (int) Math.ceil(screenX(viewport.right())), (int) Math.ceil(screenY(viewport.bottom())));
            pushDesign(matrices);
            renderModuleCards(context, modules, designMouseX, designMouseY, accent);
            popDesign(matrices);
            context.disableScissor();

            pushDesign(matrices);
            renderContentScrollbar(context, modules.size(), accent);
            popDesign(matrices);
        }
    }

    private void pushDesign(MatrixStack matrices) {
        float centerX = width / 2.0f;
        float centerY = height / 2.0f;
        matrices.push();
        matrices.translate(centerX, centerY, 0.0f);
        matrices.scale(pop, pop, 1.0f);
        matrices.translate(-centerX, -centerY, 0.0f);
        matrices.translate(layout.originX, layout.originY, 0.0f);
        matrices.scale(layout.scale, layout.scale, 1.0f);
    }

    private static void popDesign(MatrixStack matrices) {
        matrices.pop();
    }

    /** Screen x of a design x, including the opening animation. */
    private float screenX(float designX) {
        float centerX = width / 2.0f;
        return centerX + (layout.originX + designX * layout.scale - centerX) * pop;
    }

    private float screenY(float designY) {
        float centerY = height / 2.0f;
        return centerY + (layout.originY + designY * layout.scale - centerY) * pop;
    }

    private void renderWindow(DrawContext context, double mouseX, double mouseY, int accent) {
        int w = MenuLayout.WIDTH;
        int h = MenuLayout.HEIGHT;
        int r = MenuLayout.RADIUS;

        UiRender.roundedRect(context, -8, 2, w + 16, h + 14, r + 8, 0x22000000);
        UiRender.roundedRect(context, -4, 1, w + 8, h + 8, r + 4, 0x33000000);
        UiRender.roundedBox(context, new Rect(0, 0, w, h), r, 1, NovaTheme.WINDOW_BORDER, NovaTheme.WINDOW);

        renderLogo(context, 34, 26, accent);
        renderSearch(context, mouseX, mouseY, accent);
        renderHeaderButton(context, MenuLayout.GEAR_BUTTON, mouseX, mouseY, false);
        renderHeaderButton(context, MenuLayout.CLOSE_BUTTON, mouseX, mouseY, true);
        renderCategories(context, mouseX, mouseY, accent);
        renderContentPanel(context);
        renderSettings(context, mouseX, mouseY, accent);
    }

    // ---------------------------------------------------------------- header

    private void renderLogo(DrawContext context, float x, float y, int accent) {
        int top = NovaTheme.mix(accent, 0xFFFFFF, 0.22f);
        int bottom = NovaTheme.mix(accent, 0x000000, 0.04f);
        int slashTop = NovaTheme.mix(accent, 0xFFFFFF, 0.45f);
        int slashBottom = NovaTheme.mix(accent, 0xFFFFFF, 0.30f);

        // N
        UiRender.polygon(context, top, bottom, shape(x, y,
                0, 27, 0, 0, 8, 0, 23, 18.5f, 23, 0, 31, 0, 31, 27, 23, 27, 8, 8.5f, 8, 27));
        // O (outer contour plus counter; the even-odd fill leaves the counter open)
        UiRender.polygon(context, top, bottom,
                shape(x, y, 40, 0, 60, 0, 65, 5, 65, 22, 60, 27, 40, 27, 35, 22, 35, 5),
                shape(x, y, 44.5f, 8, 55.5f, 8, 57, 9.5f, 57, 17.5f, 55.5f, 19, 44.5f, 19, 43, 17.5f, 43, 9.5f));
        // V
        UiRender.polygon(context, top, bottom, shape(x, y,
                67, 0, 75.5f, 0, 83, 19, 90.5f, 0, 99, 0, 87.5f, 27, 78.5f, 27));
        // A
        UiRender.polygon(context, top, bottom, shape(x, y,
                100, 27, 110.5f, 0, 119.5f, 0, 130, 27, 121.5f, 27, 115, 9.5f, 108.5f, 27));
        UiRender.polygon(context, UiRender.lerpColor(top, bottom, 0.68f), UiRender.lerpColor(top, bottom, 0.8f),
                shape(x, y, 107, 18, 123, 18, 124.2f, 21.5f, 105.8f, 21.5f));
        // Trailing slash
        UiRender.polygon(context, slashTop, slashBottom, shape(x, y, 132, 0, 138.5f, 0, 145.5f, 27, 139, 27));
    }

    private void renderSearch(DrawContext context, double mouseX, double mouseY, int accent) {
        Rect search = MenuLayout.SEARCH;
        boolean hovered = search.contains(mouseX, mouseY);
        int border = searchFocused ? NovaTheme.mix(NovaTheme.SEARCH_BORDER, accent, 0.55f)
                : hovered ? NovaTheme.mix(NovaTheme.SEARCH_BORDER, 0xFFFFFF, 0.08f)
                : NovaTheme.SEARCH_BORDER;
        UiRender.roundedBox(context, search, search.height() / 2, 1, border, NovaTheme.SEARCH);

        int iconX = search.x() + 23;
        int iconY = search.y() + 21;
        int iconColor = 0xFFB7B8CF;
        UiRender.ring(context, iconX, iconY, 7, 5, iconColor, NovaTheme.SEARCH);
        UiRender.line(context, iconX + 4.5f, iconY + 4.5f, iconX + 9.5f, iconY + 9.5f, 2.4f, iconColor);

        float centerY = search.y() + search.height() / 2.0f;
        float available = SEARCH_TEXT_RIGHT - SEARCH_TEXT_LEFT;
        if (searchQuery.isEmpty()) {
            if (!searchFocused) {
                drawText(context, Text.translatable("screen.nova_client.search").getString(), Weight.REGULAR,
                        SEARCH_TEXT, SEARCH_TEXT_LEFT, centerY, NovaTheme.argb(NovaTheme.TEXT_PLACEHOLDER));
            }
        } else {
            // Keep the end of the query (where the caret is) visible.
            String visible = searchQuery;
            while (visible.length() > 1 && NovaText.width(visible, Weight.REGULAR, SEARCH_TEXT) > available - 4) {
                visible = visible.substring(1);
            }
            drawText(context, visible, Weight.REGULAR, SEARCH_TEXT, SEARCH_TEXT_LEFT, centerY, NovaTheme.TEXT);
        }

        if (searchFocused && (Util.getMeasuringTimeMs() / 530L) % 2L == 0L) {
            float textWidth = Math.min(available - 2, NovaText.width(searchQuery, Weight.REGULAR, SEARCH_TEXT));
            int caretX = Math.round(SEARCH_TEXT_LEFT + textWidth + 1);
            context.fill(caretX, Math.round(centerY - 9), caretX + 2, Math.round(centerY + 9), NovaTheme.TEXT);
        }
    }

    private void renderHeaderButton(DrawContext context, Rect rect, double mouseX, double mouseY, boolean close) {
        boolean hovered = rect.contains(mouseX, mouseY);
        int fill = hovered ? NovaTheme.BUTTON_HOVER : NovaTheme.BUTTON;
        UiRender.roundedRect(context, rect.x(), rect.y(), rect.width(), rect.height(), 9, fill);
        int cx = rect.x() + rect.width() / 2;
        int cy = rect.y() + rect.height() / 2;
        if (close) {
            int color = hovered ? 0xFFFF8A92 : NovaTheme.ICON;
            UiRender.line(context, cx - 7, cy - 7, cx + 7, cy + 7, 3.0f, color);
            UiRender.line(context, cx + 7, cy - 7, cx - 7, cy + 7, 3.0f, color);
        } else {
            renderGearIcon(context, cx, cy, hovered ? NovaTheme.TEXT : NovaTheme.ICON, fill);
        }
    }

    // ---------------------------------------------------------------- sidebar

    private void renderCategories(DrawContext context, double mouseX, double mouseY, int accent) {
        Category[] categories = Category.values();
        for (int i = 0; i < categories.length; i++) {
            Category category = categories[i];
            Rect row = MenuLayout.categoryRow(i, 0);
            boolean selected = category == selectedCategory;
            boolean hovered = row.contains(mouseX, mouseY);

            int fill;
            if (selected) {
                fill = NovaTheme.mix(0x16141F, accent, hovered ? 0.24f : 0.2f);
                UiRender.roundedRect(context, row.x() - 4, row.y() - 4, row.width() + 8, row.height() + 8, 16,
                        NovaTheme.withAlpha(accent, 0x14));
                UiRender.roundedRect(context, row.x() - 2, row.y() - 2, row.width() + 4, row.height() + 4, 14,
                        NovaTheme.withAlpha(accent, 0x26));
                UiRender.roundedBox(context, row, 12, 2, accent, fill);
            } else {
                fill = hovered ? NovaTheme.ROW_HOVER : NovaTheme.ROW;
                UiRender.roundedBox(context, row, 12, 1, NovaTheme.ROW_BORDER, fill);
            }

            int iconColor = selected ? 0xFFFFFFFF : NovaTheme.ICON;
            renderCategoryIcon(context, category, row.x() + 32, row.y() + row.height() / 2, iconColor, fill);
            drawText(context, category.displayName().getString(), Weight.MEDIUM, CATEGORY_TEXT,
                    row.x() + 60, row.y() + row.height() / 2.0f, selected ? NovaTheme.TEXT : NovaTheme.TEXT_CATEGORY);
        }
    }

    // ---------------------------------------------------------------- center

    private void renderContentPanel(DrawContext context) {
        Rect panel = MenuLayout.CONTENT;
        UiRender.roundedBox(context, panel, MenuLayout.PANEL_RADIUS, 1, NovaTheme.PANEL_BORDER, NovaTheme.PANEL);

        if (visibleModules().isEmpty()) {
            String message = (searchQuery.isBlank()
                    ? Text.translatable("screen.nova_client.empty")
                    : Text.translatable("screen.nova_client.no_results")).getString();
            int centerX = panel.x() + panel.width() / 2;
            renderCubeIcon(context, centerX, 295, NovaTheme.ICON_DIM);
            float textWidth = NovaText.width(message, Weight.REGULAR, EMPTY_TEXT);
            drawText(context, message, Weight.REGULAR, EMPTY_TEXT, centerX - textWidth / 2.0f, 352, NovaTheme.TEXT_MUTED);
        }
    }

    private void renderModuleCards(DrawContext context, List<NovaModule> modules, double mouseX, double mouseY, int accent) {
        Rect viewport = contentViewport();
        int maxScroll = maxContentScroll(modules.size());
        contentScroll = UiRender.clamp(contentScroll, 0, maxScroll);
        int cardWidth = viewport.width() - (maxScroll > 0 ? 8 : 0);

        int y = viewport.y() - contentScroll;
        for (NovaModule module : modules) {
            Rect card = new Rect(viewport.x(), y, cardWidth, CARD_HEIGHT);
            if (card.bottom() >= viewport.y() && card.y() <= viewport.bottom()) {
                boolean hovered = card.contains(mouseX, mouseY) && viewport.contains(mouseX, mouseY);
                UiRender.roundedBox(context, card, 10, 1, NovaTheme.ROW_BORDER,
                        hovered ? NovaTheme.CARD_HOVER : NovaTheme.CARD);
                drawText(context, module.name().getString(), Weight.MEDIUM, MODULE_NAME_TEXT,
                        card.x() + 18, card.y() + 23, NovaTheme.TEXT);
                String description = trimToWidth(module.description().getString(), Weight.REGULAR,
                        MODULE_DESCRIPTION_TEXT, card.width() - 36 - 60);
                drawText(context, description, Weight.REGULAR, MODULE_DESCRIPTION_TEXT,
                        card.x() + 18, card.y() + 44, NovaTheme.TEXT_MUTED);
                if (module instanceof ToggleableNovaModule toggleable) {
                    Rect toggle = new Rect(card.right() - 18 - 44, card.y() + (CARD_HEIGHT - 26) / 2, 44, 26);
                    renderToggle(context, toggle, toggleable.isEnabled(), accent);
                }
            }
            y += CARD_HEIGHT + CARD_GAP;
        }
    }

    private void renderContentScrollbar(DrawContext context, int moduleCount, int accent) {
        int maxScroll = maxContentScroll(moduleCount);
        if (maxScroll <= 0) {
            return;
        }
        Rect viewport = contentViewport();
        int total = moduleCount * (CARD_HEIGHT + CARD_GAP) - CARD_GAP;
        int thumb = Math.max(24, viewport.height() * viewport.height() / total);
        int thumbY = viewport.y() + (viewport.height() - thumb) * contentScroll / maxScroll;
        UiRender.roundedRect(context, viewport.right() - 3, viewport.y(), 3, viewport.height(), 1, NovaTheme.TRACK);
        UiRender.roundedRect(context, viewport.right() - 4, thumbY, 5, thumb, 2, accent);
    }

    private Rect contentViewport() {
        Rect panel = MenuLayout.CONTENT;
        return new Rect(panel.x() + CONTENT_PADDING, panel.y() + CONTENT_PADDING,
                panel.width() - CONTENT_PADDING * 2, panel.height() - CONTENT_PADDING * 2);
    }

    private List<NovaModule> visibleModules() {
        if (selectedCategory == Category.SETTINGS) {
            return List.of();
        }
        return ModuleRegistry.find(selectedCategory, searchQuery);
    }

    private int maxContentScroll(int moduleCount) {
        int total = moduleCount == 0 ? 0 : moduleCount * (CARD_HEIGHT + CARD_GAP) - CARD_GAP;
        return Math.max(0, total - contentViewport().height());
    }

    // ---------------------------------------------------------------- settings panel

    private void renderSettings(DrawContext context, double mouseX, double mouseY, int accent) {
        NovaConfig config = ConfigManager.get();
        Rect panel = MenuLayout.SETTINGS;
        UiRender.roundedBox(context, panel, MenuLayout.PANEL_RADIUS, 1, NovaTheme.PANEL_BORDER, NovaTheme.PANEL);

        drawText(context, translated("screen.nova_client.settings_menu"), Weight.SEMIBOLD, TITLE_TEXT,
                SETTINGS_LEFT, SETTINGS_TITLE_Y, NovaTheme.TEXT);

        drawText(context, translated("screen.nova_client.accent"), Weight.REGULAR, LABEL_TEXT,
                SETTINGS_LEFT, ACCENT_LABEL_Y, NovaTheme.TEXT_LABEL);
        for (int i = 0; i < NovaTheme.ACCENTS.length; i++) {
            int color = NovaTheme.argb(NovaTheme.ACCENTS[i]);
            int cx = SWATCH_X[i];
            boolean selected = config.accentColor == NovaTheme.ACCENTS[i];
            if (selected) {
                UiRender.ring(context, cx, SWATCH_Y, 20, 18, color, NovaTheme.PANEL);
                UiRender.circle(context, cx, SWATCH_Y, 14, color);
            } else {
                if (swatchHit(i, mouseX, mouseY)) {
                    UiRender.circle(context, cx, SWATCH_Y, SWATCH_RADIUS + 3, 0x40FFFFFF);
                }
                UiRender.circle(context, cx, SWATCH_Y, SWATCH_RADIUS, color);
            }
        }

        drawText(context, translated("screen.nova_client.scale"), Weight.REGULAR, LABEL_TEXT,
                SETTINGS_LEFT, SCALE_LABEL_Y, NovaTheme.TEXT_LABEL);
        String percent = String.format(Locale.ROOT, "%d%%", Math.round(config.menuScale * 100.0f));
        drawText(context, percent, Weight.REGULAR, LABEL_TEXT,
                SETTINGS_RIGHT - NovaText.width(percent, Weight.REGULAR, LABEL_TEXT),
                SCALE_TRACK.y() + SCALE_TRACK.height() / 2.0f, NovaTheme.TEXT_LABEL);
        renderSlider(context, config.menuScale, accent, mouseX, mouseY);

        drawText(context, translated("screen.nova_client.animations"), Weight.REGULAR, LABEL_TEXT,
                SETTINGS_LEFT, ANIMATIONS_Y, NovaTheme.TEXT_LABEL);
        renderToggle(context, ANIMATIONS_TOGGLE, config.animations, accent);

        context.fill(SETTINGS_LEFT, DIVIDER_Y, SETTINGS_RIGHT, DIVIDER_Y + 1, NovaTheme.DIVIDER);

        drawText(context, translated("screen.nova_client.controls"), Weight.SEMIBOLD, TITLE_TEXT,
                SETTINGS_LEFT, CONTROLS_TITLE_Y, NovaTheme.TEXT);
        renderBindingRow(context, OPEN_MENU_ROW_Y, translated("screen.nova_client.open_menu"),
                NovaKeybinds.openMenuKeyText().getString());
        renderBindingRow(context, KILLAURA_ROW_Y, translated("screen.nova_client.toggle_killaura"),
                NovaKeybinds.killAuraKeyText().getString());

        boolean resetHovered = RESET_BUTTON.contains(mouseX, mouseY);
        UiRender.roundedRect(context, RESET_BUTTON.x(), RESET_BUTTON.y(), RESET_BUTTON.width(), RESET_BUTTON.height(),
                10, resetHovered ? NovaTheme.RESET_HOVER : NovaTheme.RESET);
        String reset = translated("screen.nova_client.reset");
        float resetWidth = NovaText.width(reset, Weight.MEDIUM, BUTTON_TEXT);
        drawText(context, reset, Weight.MEDIUM, BUTTON_TEXT,
                RESET_BUTTON.x() + (RESET_BUTTON.width() - resetWidth) / 2.0f,
                RESET_BUTTON.y() + RESET_BUTTON.height() / 2.0f, NovaTheme.TEXT);
    }

    private void renderSlider(DrawContext context, float scale, int accent, double mouseX, double mouseY) {
        Rect track = SCALE_TRACK;
        int radius = track.height() / 2;
        UiRender.roundedRect(context, track.x(), track.y(), track.width(), track.height(), radius, NovaTheme.TRACK);
        float t = (scale - NovaConfig.MIN_SCALE) / (NovaConfig.MAX_SCALE - NovaConfig.MIN_SCALE);
        int knobX = track.x() + Math.round(track.width() * Math.max(0.0f, Math.min(1.0f, t)));
        UiRender.roundedRect(context, track.x(), track.y(), Math.max(track.height(), knobX - track.x()), track.height(),
                radius, accent);
        int knobY = track.y() + radius;
        boolean active = draggingScale || sliderHit().contains(mouseX, mouseY);
        UiRender.circle(context, knobX, knobY + 1, 11, 0x40000000);
        UiRender.circle(context, knobX, knobY, active ? 11 : 10, NovaTheme.KNOB);
    }

    private void renderToggle(DrawContext context, Rect toggle, boolean enabled, int accent) {
        int radius = toggle.height() / 2;
        UiRender.roundedRect(context, toggle.x(), toggle.y(), toggle.width(), toggle.height(), radius,
                enabled ? accent : NovaTheme.TOGGLE_OFF);
        int knobRadius = radius - 4;
        int knobX = enabled ? toggle.right() - radius : toggle.x() + radius;
        UiRender.circle(context, knobX, toggle.y() + radius, knobRadius, 0xFFFFFFFF);
    }

    private void renderBindingRow(DrawContext context, int y, String label, String key) {
        float keyTextWidth = NovaText.width(key, Weight.MEDIUM, KEY_TEXT);
        int keyWidth = Math.max(KEY_MIN_WIDTH, Math.round(keyTextWidth) + 24);
        Rect pill = new Rect(SETTINGS_RIGHT - keyWidth, y, keyWidth, KEY_HEIGHT);
        float centerY = y + KEY_HEIGHT / 2.0f;

        String labelText = trimToWidth(label, Weight.REGULAR, ROW_LABEL_TEXT, pill.x() - SETTINGS_LEFT - 8);
        drawText(context, labelText, Weight.REGULAR, ROW_LABEL_TEXT, SETTINGS_LEFT, centerY, NovaTheme.TEXT_LABEL);

        UiRender.roundedBox(context, pill, 7, 1, NovaTheme.KEY_BORDER, NovaTheme.KEY);
        drawText(context, key, Weight.MEDIUM, KEY_TEXT, pill.x() + (pill.width() - keyTextWidth) / 2.0f, centerY,
                NovaTheme.TEXT);
    }

    // ---------------------------------------------------------------- input

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT || layout == null) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        double x = layout.toDesignX(mouseX);
        double y = layout.toDesignY(mouseY);

        searchFocused = MenuLayout.SEARCH.contains(x, y);
        if (searchFocused) {
            return true;
        }

        if (MenuLayout.CLOSE_BUTTON.contains(x, y)) {
            close();
            return true;
        }
        if (MenuLayout.GEAR_BUTTON.contains(x, y)) {
            selectCategory(Category.SETTINGS);
            return true;
        }

        Category[] categories = Category.values();
        for (int i = 0; i < categories.length; i++) {
            if (MenuLayout.categoryRow(i, 0).contains(x, y)) {
                selectCategory(categories[i]);
                return true;
            }
        }

        if (MenuLayout.SETTINGS.contains(x, y)) {
            return handleSettingsClick(x, y);
        }

        NovaModule module = moduleAt(x, y);
        if (module instanceof ToggleableNovaModule toggleable) {
            toggleable.toggle();
            return true;
        }
        return MenuLayout.CONTENT.contains(x, y) || super.mouseClicked(mouseX, mouseY, button);
    }

    private void selectCategory(Category category) {
        selectedCategory = category;
        contentScroll = 0;
    }

    private boolean handleSettingsClick(double x, double y) {
        NovaConfig config = ConfigManager.get();
        for (int i = 0; i < NovaTheme.ACCENTS.length; i++) {
            if (swatchHit(i, x, y)) {
                config.accentColor = NovaTheme.ACCENTS[i];
                ConfigManager.save();
                return true;
            }
        }

        if (sliderHit().contains(x, y)) {
            draggingScale = true;
            setScaleFromDesignX(x);
            return true;
        }

        Rect animationsRow = new Rect(SETTINGS_LEFT, ANIMATIONS_TOGGLE.y() - 4,
                SETTINGS_RIGHT - SETTINGS_LEFT, ANIMATIONS_TOGGLE.height() + 8);
        if (animationsRow.contains(x, y)) {
            config.animations = !config.animations;
            ConfigManager.save();
            return true;
        }

        if (RESET_BUTTON.contains(x, y)) {
            ConfigManager.resetToDefaults();
            rebuildLayout();
            return true;
        }
        return true;
    }

    private static boolean swatchHit(int index, double x, double y) {
        double dx = x - SWATCH_X[index];
        double dy = y - SWATCH_Y;
        return dx * dx + dy * dy <= 17 * 17;
    }

    private static Rect sliderHit() {
        return new Rect(SCALE_TRACK.x() - 12, SCALE_TRACK.y() - 12, SCALE_TRACK.width() + 24, SCALE_TRACK.height() + 24);
    }

    private void setScaleFromDesignX(double x) {
        double t = (x - SCALE_TRACK.x()) / SCALE_TRACK.width();
        t = Math.max(0.0, Math.min(1.0, t));
        float value = NovaConfig.MIN_SCALE + (float) t * (NovaConfig.MAX_SCALE - NovaConfig.MIN_SCALE);
        // Snap to whole 5% steps so the readout matches the reference ("100%").
        ConfigManager.get().menuScale = Math.round(value * 20.0f) / 20.0f;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (draggingScale && layout != null) {
            // The window keeps its current size while dragging so the slider does not move under the cursor.
            setScaleFromDesignX(layout.toDesignX(mouseX));
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (draggingScale) {
            draggingScale = false;
            ConfigManager.save();
            rebuildLayout();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (layout != null && MenuLayout.CONTENT.contains(layout.toDesignX(mouseX), layout.toDesignY(mouseY))) {
            contentScroll -= (int) Math.round(verticalAmount * 40.0);
            clampScroll();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        if (searchFocused) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                if (!searchQuery.isEmpty()) {
                    if (Screen.hasControlDown()) {
                        String trimmed = searchQuery.stripTrailing();
                        int cut = trimmed.lastIndexOf(' ');
                        setSearch(cut < 0 ? "" : trimmed.substring(0, cut + 1));
                    } else {
                        setSearch(searchQuery.substring(0, searchQuery.offsetByCodePoints(searchQuery.length(), -1)));
                    }
                }
                return true;
            }
            if (Screen.isPaste(keyCode) && client != null) {
                setSearch(searchQuery + sanitize(client.keyboard.getClipboard()));
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                searchFocused = false;
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (searchFocused && chr >= ' ' && chr != 127) {
            setSearch(searchQuery + chr);
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    private void setSearch(String value) {
        String next = value.length() > SEARCH_MAX_LENGTH ? value.substring(0, SEARCH_MAX_LENGTH) : value;
        if (!next.equals(searchQuery)) {
            searchQuery = next;
            contentScroll = 0;
        }
    }

    private static String sanitize(String text) {
        StringBuilder out = new StringBuilder();
        text.codePoints().filter(c -> c >= ' ' && c != 127).forEach(out::appendCodePoint);
        return out.toString();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private NovaModule moduleAt(double x, double y) {
        Rect viewport = contentViewport();
        if (!viewport.contains(x, y)) {
            return null;
        }
        List<NovaModule> modules = visibleModules();
        int cardWidth = viewport.width() - (maxContentScroll(modules.size()) > 0 ? 8 : 0);
        int cardY = viewport.y() - contentScroll;
        for (NovaModule module : modules) {
            if (new Rect(viewport.x(), cardY, cardWidth, CARD_HEIGHT).contains(x, y)) {
                return module;
            }
            cardY += CARD_HEIGHT + CARD_GAP;
        }
        return null;
    }

    private void rebuildLayout() {
        layout = MenuLayout.compute(width, height, ConfigManager.get().menuScale);
        clampScroll();
    }

    private void clampScroll() {
        contentScroll = UiRender.clamp(contentScroll, 0, maxContentScroll(visibleModules().size()));
    }

    // ---------------------------------------------------------------- icons

    private void renderCategoryIcon(DrawContext context, Category category, int cx, int cy, int color, int background) {
        switch (category) {
            case COMBAT -> renderSwordIcon(context, cx, cy, color);
            case MOVEMENT -> renderRunnerIcon(context, cx, cy, color);
            case VISUAL -> renderEyeIcon(context, cx, cy, color);
            case PLAYER -> renderPlayerIcon(context, cx, cy, color);
            case MISC -> renderGridIcon(context, cx, cy, color);
            case SETTINGS -> renderGearIcon(context, cx, cy, color, background);
        }
    }

    private void renderSwordIcon(DrawContext context, int cx, int cy, int color) {
        // Blade with a pointed tip, pointing to the top right.
        UiRender.polygon(context, color, color, new float[]{
                cx - 5.0f, cy + 1.0f,
                cx + 6.5f, cy - 10.5f,
                cx + 11.5f, cy - 11.5f,
                cx + 10.5f, cy - 6.5f,
                cx - 1.0f, cy + 5.0f
        });
        UiRender.line(context, cx - 8, cy - 2, cx + 2, cy + 8, 3.4f, color);
        UiRender.line(context, cx - 3.5f, cy + 3.5f, cx - 10, cy + 10, 4.0f, color);
    }

    private void renderRunnerIcon(DrawContext context, int cx, int cy, int color) {
        UiRender.circle(context, cx + 3, cy - 8, 3, color);
        float t = 3.2f;
        UiRender.line(context, cx + 1, cy - 4, cx - 2, cy + 3, t + 0.8f, color);
        UiRender.line(context, cx + 1, cy - 3, cx - 4, cy - 1, t - 0.6f, color);
        UiRender.line(context, cx - 4, cy - 1, cx - 6, cy + 2, t - 0.6f, color);
        UiRender.line(context, cx + 1, cy - 3, cx + 4, cy + 0, t - 0.6f, color);
        UiRender.line(context, cx + 4, cy + 0, cx + 8, cy - 1, t - 0.6f, color);
        UiRender.line(context, cx - 2, cy + 3, cx + 3, cy + 6, t, color);
        UiRender.line(context, cx + 3, cy + 6, cx + 2, cy + 11, t, color);
        UiRender.line(context, cx - 2, cy + 3, cx - 5, cy + 8, t, color);
        UiRender.line(context, cx - 5, cy + 8, cx - 10, cy + 8, t, color);
    }

    private void renderEyeIcon(DrawContext context, int cx, int cy, int color) {
        UiRender.polygon(context, color, color, almond(cx, cy, 12.0f, 8.5f), almond(cx, cy, 8.5f, 5.5f));
        UiRender.circle(context, cx, cy, 4, color);
    }

    private static float[] almond(float cx, float cy, float halfWidth, float halfHeight) {
        int steps = 16;
        float[] points = new float[steps * 4];
        for (int i = 0; i < steps; i++) {
            double a = Math.PI * i / steps;
            float x = (float) (-Math.cos(a) * halfWidth);
            float y = (float) (Math.pow(Math.sin(a), 1.3) * halfHeight);
            points[i * 2] = cx + x;
            points[i * 2 + 1] = cy - y;
            points[(steps + i) * 2] = cx - x;
            points[(steps + i) * 2 + 1] = cy + y;
        }
        return points;
    }

    private void renderPlayerIcon(DrawContext context, int cx, int cy, int color) {
        UiRender.circle(context, cx, cy - 6, 5, color);
        int steps = 14;
        float[] body = new float[(steps + 1) * 2];
        for (int i = 0; i <= steps; i++) {
            double a = Math.PI * i / steps;
            body[i * 2] = cx + (float) (Math.cos(a) * 9.5);
            body[i * 2 + 1] = cy + 11 - (float) (Math.sin(a) * 9.0);
        }
        UiRender.polygon(context, color, color, body);
    }

    private void renderGridIcon(DrawContext context, int cx, int cy, int color) {
        int size = 10;
        UiRender.roundedRect(context, cx - 11, cy - 11, size, size, 2, color);
        UiRender.roundedRect(context, cx + 1, cy - 11, size, size, 2, color);
        UiRender.roundedRect(context, cx - 11, cy + 1, size, size, 2, color);
        UiRender.roundedRect(context, cx + 1, cy + 1, size, size, 2, color);
    }

    private void renderGearIcon(DrawContext context, int cx, int cy, int color, int background) {
        for (int i = 0; i < 8; i++) {
            double a = Math.PI * i / 4.0;
            float cos = (float) Math.cos(a);
            float sin = (float) Math.sin(a);
            UiRender.line(context, cx + cos * 6, cy + sin * 6, cx + cos * 10.5f, cy + sin * 10.5f, 4.6f, color);
        }
        UiRender.circle(context, cx, cy, 8, color);
        UiRender.circle(context, cx, cy, 3, background);
    }

    private void renderCubeIcon(DrawContext context, int cx, int cy, int color) {
        float[][] v = {
                {cx, cy - 27}, {cx + 24, cy - 14}, {cx + 24, cy + 14},
                {cx, cy + 27}, {cx - 24, cy + 14}, {cx - 24, cy - 14}
        };
        float t = 3.0f;
        for (int i = 0; i < v.length; i++) {
            float[] a = v[i];
            float[] b = v[(i + 1) % v.length];
            UiRender.line(context, a[0], a[1], b[0], b[1], t, color);
            UiRender.circle(context, Math.round(a[0]), Math.round(a[1]), 1, color);
        }
        UiRender.line(context, cx, cy, cx - 24, cy - 14, t, color);
        UiRender.line(context, cx, cy, cx + 24, cy - 14, t, color);
        UiRender.line(context, cx, cy, cx, cy + 27, t, color);
    }

    // ---------------------------------------------------------------- text helpers

    private static float[] shape(float x, float y, float... points) {
        float[] out = new float[points.length];
        for (int i = 0; i < points.length; i += 2) {
            out[i] = x + points[i];
            out[i + 1] = y + points[i + 1];
        }
        return out;
    }

    private static String translated(String key) {
        return Text.translatable(key).getString();
    }

    private double guiScale() {
        return client != null ? client.getWindow().getScaleFactor() : 1.0;
    }

    /**
     * Draws smooth text whose capital letters are vertically centered on {@code centerY}.
     * Coordinates are in design space; the label texture is placed on whole device pixels
     * so it is drawn 1:1 and stays sharp.
     */
    private void drawText(DrawContext context, String text, Weight weight, float size, float x, float centerY, int color) {
        if (text.isEmpty()) {
            return;
        }
        double gui = guiScale();
        float pixelsPerUnit = (float) (layout.scale * gui);
        NovaText.Entry entry = NovaText.get(text, weight, size * pixelsPerUnit);

        double deviceLeft = Math.round((layout.originX + x * layout.scale) * gui) - entry.padding;
        double deviceTop = Math.round((layout.originY + centerY * layout.scale) * gui
                - (entry.baseline - entry.capHeight / 2.0f));
        float designLeft = (float) ((deviceLeft / gui - layout.originX) / layout.scale);
        float designTop = (float) ((deviceTop / gui - layout.originY) / layout.scale);

        // Flush pending shapes so the label is layered after everything drawn before it.
        context.draw();
        MatrixStack matrices = context.getMatrices();
        matrices.push();
        matrices.translate(designLeft, designTop, 0.0f);
        matrices.scale(1.0f / pixelsPerUnit, 1.0f / pixelsPerUnit, 1.0f);
        context.drawTexture(RenderLayer::getGuiTextured, entry.texture, 0, 0, 0.0f, 0.0f,
                entry.width, entry.height, entry.width, entry.height, color);
        matrices.pop();
    }

    private static String trimToWidth(String text, Weight weight, float size, float maxWidth) {
        if (NovaText.width(text, weight, size) <= maxWidth) {
            return text;
        }
        String ellipsis = "…";
        String trimmed = text;
        while (!trimmed.isEmpty() && NovaText.width(trimmed + ellipsis, weight, size) > maxWidth) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed.stripTrailing() + ellipsis;
    }
}
