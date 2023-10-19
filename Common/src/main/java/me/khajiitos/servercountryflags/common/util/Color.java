package me.khajiitos.servercountryflags.common.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Color {
    private static final Pattern SAVE_PATTERN = Pattern.compile("\\((\\d+), (\\d+), (\\d+), (\\d+)\\)");
    public int r, g, b, a;

    public Color(int r, int g, int b, int a) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
    }

    public String toString() {
        return String.format("(%d, %d, %d, %d)", this.r, this.g, this.b, this.a);
    }

    // FIXME: this doesn't work!!
    public static Color fromString(String string) {
        Matcher matcher = SAVE_PATTERN.matcher(string);

        if (matcher.matches()) {
            try {
                int r = Integer.parseInt(matcher.group(1));
                int g = Integer.parseInt(matcher.group(2));
                int b = Integer.parseInt(matcher.group(3));
                int a = Integer.parseInt(matcher.group(4));

                return new Color(r, g, b, a < 0 ? 127 - a : a);
            } catch (NumberFormatException ignored) {}
        }
        return new Color(255, 255, 255, 255);
    }

    public static Color fromARGB(int argb) {
        int r = (argb & 0x00FF0000) >> 16;
        int g = (argb & 0x0000FF00) >> 8;
        int b = (argb & 0x000000FF);
        int a = (argb >> 24) & 0xFF;

        return new Color(r, g, b, a);
    }

    public int toARGB() {
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}