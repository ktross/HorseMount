package com.ktross.horsemount;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MountProfileTest {

    @Test
    void defaultProfileIsValidAndStable() {
        assertEquals(
                new MountProfile(
                        MountProfile.Variant.HORSE,
                        MountProfile.Style.DEFAULT,
                        MountProfile.Color.WHITE,
                        MountProfile.Armor.NONE),
                MountProfile.defaultProfile());
    }

    @Test
    void parserNormalizesWhitespaceAndCase() {
        assertEquals(
                new MountProfile(
                        MountProfile.Variant.HORSE,
                        MountProfile.Style.WHITEFIELD,
                        MountProfile.Color.DARKBROWN,
                        MountProfile.Armor.DIAMOND),
                MountProfile.parse(" HORSE ", "WhiteField", "DarkBrown", "DIAMOND")
                        .orElseThrow());
    }

    @Test
    void everyCanonicalEnumKeyRoundTrips() {
        for (MountProfile.Variant value : MountProfile.Variant.values()) {
            assertEquals(value, MountProfile.Variant.parse(value.key()).orElseThrow());
        }
        for (MountProfile.Style value : MountProfile.Style.values()) {
            assertEquals(value, MountProfile.Style.parse(value.key()).orElseThrow());
        }
        for (MountProfile.Color value : MountProfile.Color.values()) {
            assertEquals(value, MountProfile.Color.parse(value.key()).orElseThrow());
        }
        for (MountProfile.Armor value : MountProfile.Armor.values()) {
            assertEquals(value, MountProfile.Armor.parse(value.key()).orElseThrow());
        }
    }

    @Test
    void legacyUndeadVariantMapsToCanonicalZombie() {
        MountProfile.Variant variant = MountProfile.Variant.parse("undead").orElseThrow();

        assertEquals(MountProfile.Variant.ZOMBIE, variant);
        assertEquals("zombie", variant.key());
    }

    @Test
    void parserRejectsUnknownOrMissingValues() {
        assertTrue(MountProfile.parse(null, "default", "white", "none").isEmpty());
        assertTrue(MountProfile.parse("camel", "default", "white", "none").isEmpty());
        assertTrue(MountProfile.parse("horse", "spots", "white", "none").isEmpty());
        assertTrue(MountProfile.parse("horse", "default", "purple", "none").isEmpty());
        assertTrue(MountProfile.parse("horse", "default", "white", "netherite").isEmpty());
    }

    @Test
    void parserRejectsIncoherentAppearances() {
        assertTrue(MountProfile.parse("horse", "none", "none", "none").isEmpty());
        assertTrue(MountProfile.parse("donkey", "default", "white", "none").isEmpty());
    }

    @Test
    void constructorRejectsIncoherentAppearances() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new MountProfile(
                        MountProfile.Variant.HORSE,
                        MountProfile.Style.NONE,
                        MountProfile.Color.NONE,
                        MountProfile.Armor.NONE));
        assertThrows(
                IllegalArgumentException.class,
                () -> new MountProfile(
                        MountProfile.Variant.MULE,
                        MountProfile.Style.DEFAULT,
                        MountProfile.Color.WHITE,
                        MountProfile.Armor.NONE));
    }

    @Test
    void withArmorCreatesAnUpdatedValue() {
        MountProfile original = MountProfile.defaultProfile();
        MountProfile updated = original.withArmor(MountProfile.Armor.GOLD);

        assertNotSame(original, updated);
        assertEquals(MountProfile.Armor.NONE, original.armor());
        assertEquals(MountProfile.Armor.GOLD, updated.armor());
        assertEquals(original.variant(), updated.variant());
        assertEquals(original.style(), updated.style());
        assertEquals(original.color(), updated.color());
    }

    @Test
    void enumParsersDoNotGuessAtInvalidInput() {
        assertFalse(MountProfile.Variant.parse("").isPresent());
        assertFalse(MountProfile.Style.parse("white-field").isPresent());
        assertFalse(MountProfile.Color.parse("dark_brown").isPresent());
        assertFalse(MountProfile.Armor.parse("golden").isPresent());
    }
}
