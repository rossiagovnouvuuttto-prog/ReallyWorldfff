package dev.nova.client.ui;

import dev.nova.client.NovaKeybinds;
import dev.nova.client.config.ConfigManager;
import dev.nova.client.config.NovaConfig;
import dev.nova.client.module.Category;
import dev.nova.client.module.ModuleRegistry;
import dev.nova.client.module.NovaModule;
import dev.nova.client.module.ToggleableNovaModule;
import dev.nova.client.theme.NovaTheme;
import dev.nova.client.ui.component.Rect;
import dev.nova.client.ui.component.UiRender;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Locale;

/**
 * NOVA menu. Everything inside the window is drawn in {@link MenuLayout}'s design space
 * (the reference screenshot's pixels) under a single scale transform.
 */
public final class NovaScreen extends Screen {
    // Font scales in design space. Minecraft's font is 7 px tall for capitals.
    private static final float CATEGORY_TEXT = 1.95f;
    private static final float TITLE_TEXT = 1.95f;
    private static final float LABEL_TEXT = 1.5f;
    private static final float ROW_LABEL_TEXT = 1.45f;
    private static final float KEY_TEXT = 1.4f;
    private static final float BUTTON_TEXT = 1.45f;
    private static final float EMPTY_TEXT = 1.7f;
    private static final float SEARCH_TEXT = 1.45f;
    private static final float MODULE_NAME_TEXT = 1.7f;
    private static final float MODULE_DESCRIPTION_TEXT = 1.3f;

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

    // Module list in the center panel, in design space.
    private static final int CONTENT_PADDING = 16;
    private static final int CARD_HEIGHT = 64;
    private static final int CARD_GAP = 10;

    private Category selectedCategory = Category.COMBAT;
    private TextFieldWidget searchField;
    private MenuLayout layout;
    private String searchQuery = "";
    private float openProgress;
    private int contentScroll;
    private boolean draggingScale;

    public NovaScreen() {
        super(Text.translatable("screen.nova_client.title"));
    }

    @Override
    protected void init() {
        String previousSearch = searchField == null ? searchQuery : searchField.getText();
        layout = MenuLayout.compute(width, height, ConfigManager.get().menuScale);
        createSearchField(previousSearch);
        clampScroll();
    }

    private float searchTextScale() {
        return SEARCH_TEXT * layout.scale;
    }

    private void createSearchField(String value) {
        Rect search = MenuLayout.SEARCH;
        float textScale = searchTextScale();
        int x = layout.toScreenX(search.x() + 47);
        int y = Math.round(layout.originY + (search.y() + search.height() / 2.0f) * layout.scale - 4.0f * textScale);
        int widthOnScreen = Math.round((search.width() - 47 - 22) * layout.scale / textScale);

        searchField = new TextFieldWidget(textRenderer, x, y, Math.max(16, widthOnScreen), 9,
                Text.translatable("screen.nova_client.search"));
        searchField.setDrawsBackground(false);
        searchField.setEditableColor(NovaTheme.TEXT);
        searchField.setUneditableColor(NovaTheme.TEXT_MUTED);
        searchField.setTextShadow(false);
        searchField.setPlaceholder(Text.translatable("screen.nova_client.search").withColor(NovaTheme.TEXT_PLACEHOLDER));
        searchField.setMaxLength(80);
        searchField.setText(value == null ? "" : value);
        searchQuery = searchField.getText();
        searchField.setChangedListener(text -> {
            searchQuery = text;
            contentScroll = 0;
        });
        // Rendered manually so it can share the window's scale; still receives keyboard input.
        addSelectableChild(searchField);
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        // The reference keeps the world fully visible: no blur and no dark overlay.
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        NovaConfig config = ConfigManager.get();
        if (layout == null) {
            layout = MenuLayout.compute(width, height, config.menuScale);
        }

        if (!config.animations) {
            openProgress = 1.0f;
        } else if (openProgress < 1.0f) {
            openProgress = Math.min(1.0f, openProgress + 0.12f * Math.max(0.25f, deltaTicks));
        }
        float eased = 1.0f - (1.0f - openProgress) * (1.0f - openProgress) * (1.0f - openProgress);
        float pop = 0.94f + 0.06f * eased;

        var matrices = context.getMatrices();
        matrices.pushMatrix();
        float centerX = width / 2.0f;
        float centerY = height / 2.0f;
        matrices.translate(centerX, centerY);
        matrices.scale(pop, pop);
        matrices.translate(-centerX, -centerY);

        matrices.pushMatrix();
        matrices.translate(layout.originX, layout.originY);
        matrices.scale(layout.scale, layout.scale);
        double designMouseX = layout.toDesignX(mouseX);
        double designMouseY = layout.toDesignY(mouseY);
        renderWindow(context, designMouseX, designMouseY);
        matrices.popMatrix();

        if (searchField != null) {
            float textScale = searchTextScale();
            matrices.pushMatrix();
            matrices.translate(searchField.getX(), searchField.getY());
            matrices.scale(textScale, textScale);
            matrices.translate(-searchField.getX(), -searchField.getY());
            searchField.render(context, mouseX, mouseY, deltaTicks);
            matrices.popMatrix();
        }
        matrices.popMatrix();
    }

    private void renderWindow(DrawContext context, double mouseX, double mouseY) {
        int accent = NovaTheme.argb(ConfigManager.get().accentColor);
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
        renderContent(context, mouseX, mouseY, accent);
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
        boolean focused = searchField != null && searchField.isFocused();
        boolean hovered = search.contains(mouseX, mouseY);
        int border = focused ? NovaTheme.mix(NovaTheme.SEARCH_BORDER, accent, 0.55f)
                : hovered ? NovaTheme.mix(NovaTheme.SEARCH_BORDER, 0xFFFFFF, 0.08f)
                : NovaTheme.SEARCH_BORDER;
        UiRender.roundedBox(context, search, search.height() / 2, 1, border, NovaTheme.SEARCH);

        int iconX = search.x() + 23;
        int iconY = search.y() + 21;
        int iconColor = 0xFFB7B8CF;
        UiRender.ring(context, iconX, iconY, 7, 5, iconColor, NovaTheme.SEARCH);
        UiRender.line(context, iconX + 4.5f, iconY + 4.5f, iconX + 9.5f, iconY + 9.5f, 2.4f, iconColor);
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
            drawText(context, category.displayName(), row.x() + 60, row.y() + row.height() / 2.0f,
                    CATEGORY_TEXT, selected ? NovaTheme.TEXT : NovaTheme.TEXT_CATEGORY);
        }
    }

    // ---------------------------------------------------------------- center

    private void renderContent(DrawContext context, double mouseX, double mouseY, int accent) {
        Rect panel = MenuLayout.CONTENT;
        UiRender.roundedBox(context, panel, MenuLayout.PANEL_RADIUS, 1, NovaTheme.PANEL_BORDER, NovaTheme.PANEL);

        List<NovaModule> modules = visibleModules();
        if (modules.isEmpty()) {
            Text message = searchQuery.isBlank()
                    ? Text.translatable("screen.nova_client.empty")
                    : Text.translatable("screen.nova_client.no_results");
            int centerX = panel.x() + panel.width() / 2;
            renderCubeIcon(context, centerX, 295, NovaTheme.ICON_DIM);
            float textWidth = textRenderer.getWidth(message) * EMPTY_TEXT;
            drawText(context, message, centerX - textWidth / 2.0f, 352, EMPTY_TEXT, NovaTheme.TEXT_MUTED);
            return;
        }

        Rect viewport = contentViewport();
        int maxScroll = maxContentScroll(modules.size());
        contentScroll = UiRender.clamp(contentScroll, 0, maxScroll);
        int cardWidth = viewport.width() - (maxScroll > 0 ? 8 : 0);

        context.enableScissor(viewport.x(), viewport.y(), viewport.right(), viewport.bottom());
        int y = viewport.y() - contentScroll;
        for (NovaModule module : modules) {
            Rect card = new Rect(viewport.x(), y, cardWidth, CARD_HEIGHT);
            if (card.bottom() >= viewport.y() && card.y() <= viewport.bottom()) {
                boolean hovered = card.contains(mouseX, mouseY) && viewport.contains(mouseX, mouseY);
                UiRender.roundedBox(context, card, 10, 1, NovaTheme.ROW_BORDER,
                        hovered ? NovaTheme.CARD_HOVER : NovaTheme.CARD);
                drawText(context, module.name(), card.x() + 18, card.y() + 22, MODULE_NAME_TEXT, NovaTheme.TEXT);
                String description = trimToWidth(module.description().getString(),
                        card.width() - 36 - 60, MODULE_DESCRIPTION_TEXT);
                drawText(context, Text.literal(description), card.x() + 18, card.y() + 44,
                        MODULE_DESCRIPTION_TEXT, NovaTheme.TEXT_MUTED);
                if (module instanceof ToggleableNovaModule toggleable) {
                    Rect toggle = new Rect(card.right() - 18 - 44, card.y() + (CARD_HEIGHT - 26) / 2, 44, 26);
                    renderToggle(context, toggle, toggleable.isEnabled(), accent);
                }
            }
            y += CARD_HEIGHT + CARD_GAP;
        }
        context.disableScissor();

        if (maxScroll > 0) {
            int total = modules.size() * (CARD_HEIGHT + CARD_GAP) - CARD_GAP;
            int thumb = Math.max(24, viewport.height() * viewport.height() / total);
            int thumbY = viewport.y() + (viewport.height() - thumb) * contentScroll / maxScroll;
            UiRender.roundedRect(context, viewport.right() - 3, viewport.y(), 3, viewport.height(), 1, NovaTheme.TRACK);
            UiRender.roundedRect(context, viewport.right() - 4, thumbY, 5, thumb, 2, accent);
        }
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

        drawText(context, Text.translatable("screen.nova_client.settings_menu"), SETTINGS_LEFT, SETTINGS_TITLE_Y,
                TITLE_TEXT, NovaTheme.TEXT);

        drawText(context, Text.translatable("screen.nova_client.accent"), SETTINGS_LEFT, ACCENT_LABEL_Y,
                LABEL_TEXT, NovaTheme.TEXT_LABEL);
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

        drawText(context, Text.translatable("screen.nova_client.scale"), SETTINGS_LEFT, SCALE_LABEL_Y,
                LABEL_TEXT, NovaTheme.TEXT_LABEL);
        String percent = String.format(Locale.ROOT, "%d%%", Math.round(config.menuScale * 100.0f));
        drawText(context, Text.literal(percent), SETTINGS_RIGHT - textRenderer.getWidth(percent) * LABEL_TEXT,
                SCALE_TRACK.y() + SCALE_TRACK.height() / 2.0f, LABEL_TEXT, NovaTheme.TEXT_LABEL);
        renderSlider(context, config.menuScale, accent, mouseX, mouseY);

        drawText(context, Text.translatable("screen.nova_client.animations"), SETTINGS_LEFT, ANIMATIONS_Y,
                LABEL_TEXT, NovaTheme.TEXT_LABEL);
        renderToggle(context, ANIMATIONS_TOGGLE, config.animations, accent);

        context.fill(SETTINGS_LEFT, DIVIDER_Y, SETTINGS_RIGHT, DIVIDER_Y + 1, NovaTheme.DIVIDER);

        drawText(context, Text.translatable("screen.nova_client.controls"), SETTINGS_LEFT, CONTROLS_TITLE_Y,
                TITLE_TEXT, NovaTheme.TEXT);
        renderBindingRow(context, OPEN_MENU_ROW_Y, Text.translatable("screen.nova_client.open_menu"),
                NovaKeybinds.openMenuKeyText());
        renderBindingRow(context, KILLAURA_ROW_Y, Text.translatable("screen.nova_client.toggle_killaura"),
                NovaKeybinds.killAuraKeyText());

        boolean resetHovered = RESET_BUTTON.contains(mouseX, mouseY);
        UiRender.roundedRect(context, RESET_BUTTON.x(), RESET_BUTTON.y(), RESET_BUTTON.width(), RESET_BUTTON.height(),
                10, resetHovered ? NovaTheme.RESET_HOVER : NovaTheme.RESET);
        Text reset = Text.translatable("screen.nova_client.reset");
        float resetWidth = textRenderer.getWidth(reset) * BUTTON_TEXT;
        drawText(context, reset, RESET_BUTTON.x() + (RESET_BUTTON.width() - resetWidth) / 2.0f,
                RESET_BUTTON.y() + RESET_BUTTON.height() / 2.0f, BUTTON_TEXT, NovaTheme.TEXT);
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

    private void renderBindingRow(DrawContext context, int y, Text label, Text key) {
        float keyTextWidth = textRenderer.getWidth(key) * KEY_TEXT;
        int keyWidth = Math.max(KEY_MIN_WIDTH, Math.round(keyTextWidth) + 24);
        Rect pill = new Rect(SETTINGS_RIGHT - keyWidth, y, keyWidth, KEY_HEIGHT);
        float centerY = y + KEY_HEIGHT / 2.0f;

        String labelText = trimToWidth(label.getString(), pill.x() - SETTINGS_LEFT - 8, ROW_LABEL_TEXT);
        drawText(context, Text.literal(labelText), SETTINGS_LEFT, centerY, ROW_LABEL_TEXT, NovaTheme.TEXT_LABEL);

        UiRender.roundedBox(context, pill, 7, 1, NovaTheme.KEY_BORDER, NovaTheme.KEY);
        drawText(context, key, pill.x() + (pill.width() - keyTextWidth) / 2.0f, centerY, KEY_TEXT, NovaTheme.TEXT);
    }

    // ---------------------------------------------------------------- input

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (click.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT || layout == null) {
            return super.mouseClicked(click, doubled);
        }

        double x = layout.toDesignX(click.x());
        double y = layout.toDesignY(click.y());

        if (MenuLayout.SEARCH.contains(x, y)) {
            setFocused(searchField);
            return true;
        }
        setFocused(null);

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
        return MenuLayout.CONTENT.contains(x, y) || super.mouseClicked(click, doubled);
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
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        if (draggingScale && layout != null) {
            // The window keeps its current size while dragging so the slider does not move under the cursor.
            setScaleFromDesignX(layout.toDesignX(click.x()));
            return true;
        }
        return super.mouseDragged(click, offsetX, offsetY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (draggingScale) {
            draggingScale = false;
            ConfigManager.save();
            rebuildLayout();
            return true;
        }
        return super.mouseReleased(click);
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
    public boolean keyPressed(KeyInput input) {
        if (input.isEscape()) {
            close();
            return true;
        }
        return super.keyPressed(input);
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
        String value = searchField == null ? searchQuery : searchField.getText();
        clearChildren();
        layout = MenuLayout.compute(width, height, ConfigManager.get().menuScale);
        createSearchField(value);
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

    // ---------------------------------------------------------------- helpers

    private static float[] shape(float x, float y, float... points) {
        float[] out = new float[points.length];
        for (int i = 0; i < points.length; i += 2) {
            out[i] = x + points[i];
            out[i + 1] = y + points[i + 1];
        }
        return out;
    }

    /** Draws text so that its capital letters are vertically centered on {@code centerY}. */
    private void drawText(DrawContext context, Text text, float x, float centerY, float scale, int color) {
        var matrices = context.getMatrices();
        matrices.pushMatrix();
        matrices.translate(x, centerY - 3.5f * scale);
        matrices.scale(scale, scale);
        context.drawText(textRenderer, text, 0, 0, color, false);
        matrices.popMatrix();
    }

    private String trimToWidth(String text, float designWidth, float scale) {
        int available = Math.max(0, (int) (designWidth / scale));
        if (textRenderer.getWidth(text) <= available) {
            return text;
        }
        return textRenderer.trimToWidth(text, Math.max(0, available - textRenderer.getWidth("..."))) + "...";
    }
}
