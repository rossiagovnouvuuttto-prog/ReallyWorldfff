package dev.nova.client.ui.component;

import net.minecraft.client.gui.DrawContext;

import java.util.Arrays;

public final class UiRender {
    private UiRender() {
    }

    public static void roundedRect(DrawContext context, int x, int y, int width, int height, int radius, int color) {
        if (width <= 0 || height <= 0) {
            return;
        }
        int r = Math.max(0, Math.min(radius, Math.min(width, height) / 2));
        if (r == 0) {
            context.fill(x, y, x + width, y + height, color);
            return;
        }

        context.fill(x, y + r, x + width, y + height - r, color);

        for (int dy = 0; dy < r; dy++) {
            double yy = r - dy - 0.5;
            int dx = (int) Math.round(Math.sqrt(Math.max(0.0, r * r - yy * yy)));
            int left = x + r - dx;
            int right = x + width - r + dx;
            context.fill(left, y + dy, right, y + dy + 1, color);
            context.fill(left, y + height - dy - 1, right, y + height - dy, color);
        }
    }

    /** Rounded rectangle with a solid border of the given thickness and a separate fill color. */
    public static void roundedBox(DrawContext context, Rect rect, int radius, int thickness, int border, int fill) {
        roundedRect(context, rect.x(), rect.y(), rect.width(), rect.height(), radius, border);
        roundedRect(context, rect.x() + thickness, rect.y() + thickness,
                rect.width() - thickness * 2, rect.height() - thickness * 2,
                Math.max(0, radius - thickness), fill);
    }

    public static void circle(DrawContext context, int centerX, int centerY, int radius, int color) {
        if (radius <= 0) {
            context.fill(centerX, centerY, centerX + 1, centerY + 1, color);
            return;
        }
        for (int dy = -radius; dy < radius; dy++) {
            double yy = dy + 0.5;
            int dx = (int) Math.round(Math.sqrt(Math.max(0.0, radius * radius - yy * yy)));
            context.fill(centerX - dx, centerY + dy, centerX + dx, centerY + dy + 1, color);
        }
    }

    /** Draws a filled ring by stamping an outer circle and an inner circle in the background color. */
    public static void ring(DrawContext context, int centerX, int centerY, int outerRadius, int innerRadius, int color, int innerColor) {
        circle(context, centerX, centerY, outerRadius, color);
        if (innerRadius > 0) {
            circle(context, centerX, centerY, innerRadius, innerColor);
        }
    }

    /** A straight stroke with square caps, drawn as a filled quad. */
    public static void line(DrawContext context, float x0, float y0, float x1, float y1, float thickness, int color) {
        float dx = x1 - x0;
        float dy = y1 - y0;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length < 0.001f) {
            return;
        }
        float half = thickness / 2.0f;
        float nx = -dy / length * half;
        float ny = dx / length * half;
        polygon(context, color, color, new float[]{
                x0 + nx, y0 + ny,
                x1 + nx, y1 + ny,
                x1 - nx, y1 - ny,
                x0 - nx, y0 - ny
        });
    }

    /**
     * Scanline fill of one or more closed contours using the even-odd rule, so a second
     * contour inside the first becomes a hole. The fill color is a vertical gradient
     * from {@code topColor} to {@code bottomColor} across the shape's bounds.
     * Every contour is a flat array of x,y pairs.
     */
    public static void polygon(DrawContext context, int topColor, int bottomColor, float[]... contours) {
        float minY = Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;
        int edgeCount = 0;
        for (float[] contour : contours) {
            for (int i = 1; i < contour.length; i += 2) {
                minY = Math.min(minY, contour[i]);
                maxY = Math.max(maxY, contour[i]);
            }
            edgeCount += contour.length / 2;
        }
        if (edgeCount < 3 || maxY <= minY) {
            return;
        }

        float[] hits = new float[edgeCount];
        int startY = (int) Math.floor(minY);
        int endY = (int) Math.ceil(maxY);
        for (int y = startY; y < endY; y++) {
            float sampleY = y + 0.5f;
            int count = 0;
            for (float[] contour : contours) {
                int points = contour.length / 2;
                for (int i = 0; i < points; i++) {
                    int j = (i + 1) % points;
                    float ax = contour[i * 2];
                    float ay = contour[i * 2 + 1];
                    float bx = contour[j * 2];
                    float by = contour[j * 2 + 1];
                    if ((ay <= sampleY && by > sampleY) || (by <= sampleY && ay > sampleY)) {
                        hits[count++] = ax + (sampleY - ay) / (by - ay) * (bx - ax);
                    }
                }
            }
            if (count < 2) {
                continue;
            }
            Arrays.sort(hits, 0, count);
            float t = (sampleY - minY) / (maxY - minY);
            int color = topColor == bottomColor ? topColor : lerpColor(topColor, bottomColor, t);
            for (int i = 0; i + 1 < count; i += 2) {
                int left = Math.round(hits[i]);
                int right = Math.round(hits[i + 1]);
                if (right > left) {
                    context.fill(left, y, right, y + 1, color);
                }
            }
        }
    }

    public static int lerpColor(int from, int to, float t) {
        float k = Math.max(0.0f, Math.min(1.0f, t));
        int a = Math.round(((from >>> 24) & 0xFF) + ((((to >>> 24) & 0xFF) - ((from >>> 24) & 0xFF)) * k));
        int r = Math.round(((from >> 16) & 0xFF) + ((((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)) * k));
        int g = Math.round(((from >> 8) & 0xFF) + ((((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)) * k));
        int b = Math.round((from & 0xFF) + (((to & 0xFF) - (from & 0xFF)) * k));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
