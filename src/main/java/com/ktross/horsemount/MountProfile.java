package com.ktross.horsemount;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * A validated, Bukkit-independent description of a mount.
 *
 * <p>Keeping configuration and command parsing here makes the rules easy to test without
 * starting a Minecraft server.</p>
 */
public record MountProfile(Variant variant, Style style, Color color, Armor armor) {

    public MountProfile {
        Objects.requireNonNull(variant, "variant");
        Objects.requireNonNull(style, "style");
        Objects.requireNonNull(color, "color");
        Objects.requireNonNull(armor, "armor");

        if (variant == Variant.HORSE && (style == Style.NONE || color == Color.NONE)) {
            throw new IllegalArgumentException("Horse mounts require both a style and a color");
        }
        if (variant != Variant.HORSE && (style != Style.NONE || color != Color.NONE)) {
            throw new IllegalArgumentException("Only horse mounts can have a style or color");
        }
    }

    public static MountProfile defaultProfile() {
        return new MountProfile(Variant.HORSE, Style.DEFAULT, Color.WHITE, Armor.NONE);
    }

    public static Optional<MountProfile> parse(
            String variant, String style, String color, String armor) {
        Optional<Variant> parsedVariant = Variant.parse(variant);
        Optional<Style> parsedStyle = Style.parse(style);
        Optional<Color> parsedColor = Color.parse(color);
        Optional<Armor> parsedArmor = Armor.parse(armor);

        if (parsedVariant.isEmpty()
                || parsedStyle.isEmpty()
                || parsedColor.isEmpty()
                || parsedArmor.isEmpty()) {
            return Optional.empty();
        }

        try {
            return Optional.of(new MountProfile(
                    parsedVariant.get(), parsedStyle.get(), parsedColor.get(), parsedArmor.get()));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    public MountProfile withArmor(Armor newArmor) {
        return new MountProfile(variant, style, color, newArmor);
    }

    public String description() {
        if (variant == Variant.HORSE) {
            return "Variant: " + variant.key()
                    + ", Style: " + style.key()
                    + ", Color: " + color.key()
                    + ", Armor: " + armor.key();
        }
        return "Variant: " + variant.key() + ", Armor: " + armor.key();
    }

    static String normalizeKey(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    public enum Variant {
        HORSE("horse"),
        MULE("mule"),
        DONKEY("donkey"),
        SKELETON("skeleton"),
        ZOMBIE("zombie");

        private final String key;

        Variant(String key) {
            this.key = key;
        }

        public String key() {
            return key;
        }

        public static Optional<Variant> parse(String value) {
            String normalized = normalizeKey(value);
            // The original README called zombie horses "undead" even though the command used
            // "zombie". Accept both so old signs and remembered commands continue to work.
            if (normalized.equals("undead")) {
                return Optional.of(ZOMBIE);
            }
            for (Variant variant : values()) {
                if (variant.key.equals(normalized)) {
                    return Optional.of(variant);
                }
            }
            return Optional.empty();
        }
    }

    public enum Style {
        NONE("none"),
        DEFAULT("default"),
        WHITE("white"),
        WHITEFIELD("whitefield"),
        WHITEDOTS("whitedots"),
        BLACKDOTS("blackdots");

        private final String key;

        Style(String key) {
            this.key = key;
        }

        public String key() {
            return key;
        }

        public static Optional<Style> parse(String value) {
            String normalized = normalizeKey(value);
            for (Style style : values()) {
                if (style.key.equals(normalized)) {
                    return Optional.of(style);
                }
            }
            return Optional.empty();
        }
    }

    public enum Color {
        NONE("none"),
        WHITE("white"),
        CREAMY("creamy"),
        CHESTNUT("chestnut"),
        BROWN("brown"),
        BLACK("black"),
        GRAY("gray"),
        DARKBROWN("darkbrown");

        private final String key;

        Color(String key) {
            this.key = key;
        }

        public String key() {
            return key;
        }

        public static Optional<Color> parse(String value) {
            String normalized = normalizeKey(value);
            for (Color color : values()) {
                if (color.key.equals(normalized)) {
                    return Optional.of(color);
                }
            }
            return Optional.empty();
        }
    }

    public enum Armor {
        NONE("none"),
        IRON("iron"),
        GOLD("gold"),
        DIAMOND("diamond");

        private final String key;

        Armor(String key) {
            this.key = key;
        }

        public String key() {
            return key;
        }

        public static Optional<Armor> parse(String value) {
            String normalized = normalizeKey(value);
            for (Armor armor : values()) {
                if (armor.key.equals(normalized)) {
                    return Optional.of(armor);
                }
            }
            return Optional.empty();
        }
    }
}
