package dev.nova.client.ui.component;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.TextAttribute;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Smooth UI text. Minecraft's bitmap font looks blocky once it is scaled up, so NOVA
 * rasterizes its labels with the bundled Inter font at the exact on-screen pixel size
 * and caches every label as a small texture.
 */
public final class NovaText {
    /** Inter's capital height relative to the font size. */
    public static final float CAP_HEIGHT = 0.7275f;

    private static final int PADDING = 2;
    private static final int MAX_CACHED = 384;
    private static final FontRenderContext FRC = new FontRenderContext(null, true, true);

    private static final Map<Weight, Font> BASE_FONTS = new EnumMap<>(Weight.class);
    private static final Map<Key, Entry> CACHE = new HashMap<>();
    private static int nextId;

    public enum Weight {
        REGULAR("Inter-Regular.ttf"),
        MEDIUM("Inter-Medium.ttf"),
        SEMIBOLD("Inter-SemiBold.ttf");

        private final String file;

        Weight(String file) {
            this.file = file;
        }
    }

    /** One rasterized label. All sizes are in device pixels. */
    public static final class Entry {
        public final Identifier texture;
        public final int width;
        public final int height;
        /** Distance from the texture's top edge to the text baseline. */
        public final int baseline;
        /** Horizontal padding before the first glyph. */
        public final int padding;
        public final float capHeight;

        private Entry(Identifier texture, int width, int height, int baseline, int padding, float capHeight) {
            this.texture = texture;
            this.width = width;
            this.height = height;
            this.baseline = baseline;
            this.padding = padding;
            this.capHeight = capHeight;
        }
    }

    private record Key(String text, Weight weight, int pixelSizeHundredths) {
    }

    private NovaText() {
    }

    /** Advance width of {@code text} at {@code size}, in the same units as {@code size}. */
    public static float width(String text, Weight weight, float size) {
        if (text.isEmpty()) {
            return 0.0f;
        }
        return (float) font(weight, size).getStringBounds(text, FRC).getWidth();
    }

    /** Returns the cached texture for {@code text} rendered at {@code pixelSize} device pixels. */
    public static Entry get(String text, Weight weight, float pixelSize) {
        Key key = new Key(text, weight, Math.round(pixelSize * 100.0f));
        Entry entry = CACHE.get(key);
        if (entry != null) {
            return entry;
        }
        if (CACHE.size() >= MAX_CACHED) {
            clear();
        }
        entry = rasterize(text, weight, key.pixelSizeHundredths() / 100.0f);
        CACHE.put(key, entry);
        return entry;
    }

    /** Releases every cached label texture. */
    public static void clear() {
        MinecraftClient client = MinecraftClient.getInstance();
        for (Iterator<Entry> it = CACHE.values().iterator(); it.hasNext(); ) {
            Entry entry = it.next();
            if (entry.texture != null) {
                client.getTextureManager().destroyTexture(entry.texture);
            }
            it.remove();
        }
    }

    private static Entry rasterize(String text, Weight weight, float pixelSize) {
        Font font = font(weight, pixelSize);
        var metrics = font.getLineMetrics(text.isEmpty() ? " " : text, FRC);
        int baseline = PADDING + (int) Math.ceil(metrics.getAscent());
        int width = Math.max(1, (int) Math.ceil(font.getStringBounds(text, FRC).getWidth()) + PADDING * 2);
        int height = baseline + (int) Math.ceil(metrics.getDescent()) + PADDING;
        float capHeight = pixelSize * CAP_HEIGHT;

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setColor(Color.WHITE);
        g.setFont(font);
        g.drawString(text, (float) PADDING, (float) baseline);
        g.dispose();

        // Slightly boost coverage: white-on-dark text otherwise looks thinner than in a browser.
        int[] pixels = image.getRGB(0, 0, width, height, null, 0, width);
        for (int i = 0; i < pixels.length; i++) {
            int alpha = pixels[i] >>> 24;
            if (alpha != 0 && alpha != 255) {
                alpha = Math.min(255, Math.round((float) Math.pow(alpha / 255.0, 0.8) * 255.0f));
            }
            pixels[i] = (alpha << 24) | 0x00FFFFFF;
        }
        image.setRGB(0, 0, width, height, pixels, 0, width);

        return new Entry(upload(image), width, height, baseline, PADDING, capHeight);
    }

    private static Identifier upload(BufferedImage image) {
        try {
            ByteArrayOutputStream png = new ByteArrayOutputStream();
            ImageIO.write(image, "png", png);
            NativeImage nativeImage = NativeImage.read(new ByteArrayInputStream(png.toByteArray()));
            NativeImageBackedTexture texture = new NativeImageBackedTexture(nativeImage);
            texture.setFilter(true, false);
            Identifier id = Identifier.of("nova_client", "text/" + nextId++);
            MinecraftClient.getInstance().getTextureManager().registerTexture(id, texture);
            return id;
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to upload NOVA text texture", exception);
        }
    }

    private static Font font(Weight weight, float size) {
        Font base = BASE_FONTS.computeIfAbsent(weight, NovaText::load);
        return base.deriveFont(size);
    }

    private static Font load(Weight weight) {
        String path = "/assets/nova_client/fonts/" + weight.file;
        try (InputStream stream = NovaText.class.getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalStateException("Missing font " + path);
            }
            Font font = Font.createFont(Font.TRUETYPE_FONT, stream);
            return font.deriveFont(Map.of(
                    TextAttribute.KERNING, TextAttribute.KERNING_ON,
                    TextAttribute.LIGATURES, TextAttribute.LIGATURES_ON
            ));
        } catch (IOException | FontFormatException exception) {
            throw new IllegalStateException("Unable to load font " + path, exception);
        }
    }
}
