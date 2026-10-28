package com.example.punchsnowball;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Visual-only formatter. Never changes gameplay tags, enchantments, attributes or item models. */
final class FrostTooltips {
    private record Ability(String title, String keys, String description, String cooldownPath, int defaultSeconds) {}
    private record Profile(String id, String title, String motto, String passive,
                           int colorFrom, int colorTo, Ability[] abilities) {}

    private static Profile p(String id, String title, String motto, String passive,
                            int a, int b, Ability... abilities) {
        return new Profile(id, title, motto, passive, a, b, abilities);
    }
    private static Ability a(String title, String keys, String desc, String path, int seconds) {
        return new Ability(title, keys, desc, path, seconds);
    }

    static String identify(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return null;
        var data = item.getItemMeta().getPersistentDataContainer();
        if (data.has(NamespacedKey.fromString("punchsnowball:punch_snowball"), PersistentDataType.BYTE)) return "snowball";
        if (data.has(NamespacedKey.fromString("punchsnowball:infinite_tnt"), PersistentDataType.BYTE)) return "tnt";
        if (data.has(NamespacedKey.fromString("punchsnowball:freeze_windcharge"), PersistentDataType.BYTE)) return "windcharge";
        if (data.has(NamespacedKey.fromString("punchsnowball:storm_trident"), PersistentDataType.BYTE)) return "grapple";
        if (data.has(NamespacedKey.fromString("punchsnowball:riptide_trident"), PersistentDataType.BYTE)) return "riptide";
        if (data.has(NamespacedKey.fromString("punchsnowball:homing_firework"), PersistentDataType.BYTE)) return "firework";
        if (data.has(NamespacedKey.fromString("punchsnowball:lava_tsunami"), PersistentDataType.BYTE)) return "lava";
        if (data.has(NamespacedKey.fromString("punchsnowball:battleaxe"), PersistentDataType.BYTE)) return "battleaxe";
        if (data.has(NamespacedKey.fromString("punchsnowball:frost_shield"), PersistentDataType.BYTE)) return "shield";
        if (data.has(NamespacedKey.fromString("punchsnowball:frozen_sword"), PersistentDataType.BYTE)) return "sword";
        if (data.has(NamespacedKey.fromString("punchsnowball:frozen_spear"), PersistentDataType.BYTE)) return "spear";
        if (data.has(NamespacedKey.fromString("punchsnowball:frozen_fortune_pickaxe"), PersistentDataType.BYTE)
                || data.has(NamespacedKey.fromString("punchsnowball:frozen_silk_pickaxe"), PersistentDataType.BYTE)) {
            String mode = data.get(NamespacedKey.fromString("punchsnowball:pickaxe_mode"), PersistentDataType.STRING);
            if ("silk".equals(mode)) return "pickaxe_silk";
            return item.getItemMeta().hasEnchant(org.bukkit.enchantments.Enchantment.SILK_TOUCH) ? "pickaxe_silk" : "pickaxe_fortune";
        }
        if (data.has(NamespacedKey.fromString("punchsnowball:frozen_bow"), PersistentDataType.BYTE)) return "bow";
        if (data.has(NamespacedKey.fromString("punchsnowball:frozen_crossbow"), PersistentDataType.BYTE)) return "crossbow";
        if (data.has(NamespacedKey.fromString("punchsnowball:frozen_helmet"), PersistentDataType.BYTE)) return "helmet";
        if (data.has(NamespacedKey.fromString("punchsnowball:frozen_chestplate"), PersistentDataType.BYTE)) return "chestplate";
        if (data.has(NamespacedKey.fromString("punchsnowball:frozen_leggings"), PersistentDataType.BYTE)) return "leggings";
        if (data.has(NamespacedKey.fromString("punchsnowball:frozen_boots"), PersistentDataType.BYTE)) return "boots";
        if (data.has(NamespacedKey.fromString("punchsnowball:frozen_fishing_rod"), PersistentDataType.BYTE)) return "rod";
        if (data.has(NamespacedKey.fromString("punchsnowball:frozen_horn"), PersistentDataType.BYTE)) return "horn";
        if (data.has(NamespacedKey.fromString("punchsnowball:frozen_totem"), PersistentDataType.BYTE)) return "totem";
        return null;
    }

    private static Profile profile(String id) {
        return switch (id) {
            case "sword" -> p("sword", "Frozen Sword", "Winter bows to no one.", "Permafrost: melee builds 3 Frost Marks; the final strike shatters them.", 0xD7F8FF, 0x297FE9,
                a("Absolute Zero", "F", "Crescent ice wave; marks increase shatter damage.", "reborn.sword.cooldown-seconds", 35));
            case "battleaxe" -> p("battleaxe", "Frozen Battleaxe", "The glacier remembers every impact.", "Brittle Edge: attacks slow opponents briefly.", 0xC7EBFF, 0x5072DB,
                a("Glacier Breaker", "F", "A telegraphed hammering burst shatters the ground.", "reborn.battleaxe.cooldown-seconds", 32));
            case "spear" -> p("spear", "Frozen Spear", "The earth itself grows teeth.", "Deep Chill: melee hits inflict Slowness.", 0xA8F8FF, 0x309DB9,
                a("Frost Spires", "F", "A rising line of ice spikes launches enemies.", "reborn.spear.cooldown-seconds", 28));
            case "bow" -> p("bow", "Frozen Bow", "The clouds belong to winter.", "Coldshot: normal arrows slow targets.", 0xC6EFFF, 0x5078D9,
                a("Arrowfall", "F", "Call down waves of freezing shards at your aim.", "reborn.bow.cooldown-seconds", 42));
            case "crossbow" -> p("crossbow", "Frozen Crossbow", "Hear the sculk beneath the snow.", "Icicle Rounds: ordinary bolts create frosty shots.", 0xA8EFFF, 0x3669C0,
                a("Sonic Frost", "F", "A directed sonic wave damages and slows.", "reborn.crossbow.cooldown-seconds", 38));
            case "grapple" -> p("grapple", "Frozen Trident of Loyalty", "Winter draws everything closer.", "Loyalty: thrown trident returns to the wielder.", 0xC9F8FF, 0x2476CE,
                a("Frozen Tether", "F", "Aim at a target to pull them toward you.", "reborn.grapple.cooldown-seconds", 24));
            case "riptide" -> p("riptide", "Frozen Trident of the Depths", "The sea remembers thunder.", "Deep Current: swim faster when held underwater.", 0x86E9FF, 0x565EDB,
                a("Thunderfall", "F", "Mark an enemy and call three lightning strikes.", "reborn.riptide.cooldown-seconds", 26));
            case "firework" -> p("firework", "Frozen Firework of Cold Skies", "Five stars rise into an icy sky.", "Glinting Trail: homing hits reveal enemies.", 0xA9F6FF, 0xB86BE6,
                a("Sky Hunters", "F", "Call five frost rockets onto nearby enemies.", "reborn.firework.cooldown-seconds", 48));
            case "shield" -> p("shield", "Frozen Shield of Defiance", "A wall that refuses to break.", "Iron Winter: blocks regular frontal attacks.", 0x9CE9E1, 0x3978D8,
                a("Crystal Guard", "F", "Reduce incoming damage for eight seconds.", "reborn.shield.cooldown-seconds", 42));
            case "totem" -> p("totem", "Frozen Totem of Winter", "The blizzard protects its own.", "Winterheart: Regeneration while held.", 0x99FFE5, 0x806FDC,
                a("Winter Blessing", "F", "Heal allies and weaken enemies in a moving aura.", "reborn.totem.cooldown-seconds", 46));
            case "pickaxe_fortune" -> p("pickaxe_fortune", "Frozen Pickaxe of Duality", "Two faces of the glacier.", "Frost Excavation: mine a 3x3 plane.", 0xC8FAFF, 0x3F8BCD,
                a("Duality Switch", "F", "Switch to Silk Touch mode.", null, 0));
            case "pickaxe_silk" -> p("pickaxe_silk", "Frozen Pickaxe of Duality", "Two faces of the glacier.", "Frost Excavation: mine a 3x3 plane.", 0xA2FFDE, 0x6069D6,
                a("Duality Switch", "F", "Switch to Fortune V mode.", null, 0));
            case "rod" -> p("rod", "Frozen Fishing Rod", "Your anchor is the horizon.", "Tethered: retain your fishing rod after casting.", 0xA8EDFF, 0x3879C8,
                a("Ice Anchor", "F", "Grapple toward the solid block you target.", "reborn.rod.cooldown-seconds", 16));
            case "horn" -> p("horn", "Frozen Horn of the Hordes", "Answer the call of winter.", "Winterborn: gain brief Resistance while held.", 0xDEEFFF, 0x657CDF,
                a("Call of Winter", "F", "Summon three temporary icy Stray allies.", "reborn.horn.cooldown-seconds", 60));
            case "helmet" -> p("helmet", "Frozen Helmet of Revelation", "Eyes above the storm.", "Strength II while worn.", 0xD3F7FF, 0x5A87CF,
                a("Armor Passive", "EQUIP", "Increases melee strength.", null, 0));
            case "chestplate" -> p("chestplate", "Frozen Chestplate of Fortitude", "Endure the coldest night.", "Fortitude: +5 maximum hearts while worn.", 0xEBFEFF, 0x6179D8,
                a("Armor Passive", "EQUIP", "No damage-reflection mechanic.", null, 0));
            case "leggings" -> p("leggings", "Frozen Leggings of Emberguard", "Even fire fears winter.", "Emberguard: Fire Resistance while worn.", 0xB8F6FF, 0x6083D4,
                a("Armor Passive", "EQUIP", "Ignore ordinary fire and lava burning.", null, 0));
            case "boots" -> p("boots", "Frozen Boots of Freedom", "Run across the edge of the world.", "Speed II and fall immunity while worn.", 0xC9F5FF, 0x4589C9,
                a("Armor Passive", "EQUIP", "Travel faster and land safely.", null, 0));
            default -> null;
        };
    }
    private static Component noItalic(Component c) { return c.decoration(TextDecoration.ITALIC, false); }
    private static Component solid(String value, int color) {
        return noItalic(Component.text(value, TextColor.color(color)));
    }
    private static Component gradient(String value, int start, int end) {
        Component output = Component.empty();
        int steps = Math.max(1, value.length() - 1);
        for (int i = 0; i < value.length(); i++) {
            double t = (double) i / steps;
            int r = (int) Math.round(((start >> 16) & 255) * (1 - t) + ((end >> 16) & 255) * t);
            int g = (int) Math.round(((start >> 8) & 255) * (1 - t) + ((end >> 8) & 255) * t);
            int b = (int) Math.round((start & 255) * (1 - t) + (end & 255) * t);
            output = output.append(solid(String.valueOf(value.charAt(i)), (r << 16) | (g << 8) | b));
        }
        return output;
    }
    private static void addWrapped(List<Component> lore, String text, int color) {
        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();
        for (String word : words) {
            if (line.length() > 0 && line.length() + word.length() + 1 > 43) {
                lore.add(solid(line.toString(), color));
                line.setLength(0);
            }
            if (line.length() > 0) line.append(' ');
            line.append(word);
        }
        if (line.length() > 0) lore.add(solid(line.toString(), color));
    }

    static boolean apply(JavaPlugin plugin, ItemStack item) {
        String id = identify(item);
        if (id == null) return false;
        Profile profile = profile(id);
        if (profile == null) return false;
        ItemMeta meta = item.getItemMeta();
        if (plugin.getConfig().getBoolean("tooltips.gradient-text", true)) {
            meta.displayName(gradient("✦ " + profile.title() + " ✦", profile.colorFrom(), profile.colorTo()));
            List<Component> lore = new ArrayList<>();
            lore.add(gradient("✧ FROSTPVP REBORN • F KEY ✧", profile.colorFrom(), profile.colorTo()));
            lore.add(solid(profile.motto(), 0xB4BDCE));
            lore.add(Component.empty());
            lore.add(gradient("PASSIVE   |   WINTER'S GIFT", profile.colorFrom(), profile.colorTo()));
            addWrapped(lore, profile.passive(), 0xA8F2F9);
            for (int i = 0; i < profile.abilities().length; i++) {
                Ability ability = profile.abilities()[i];
                lore.add(Component.empty());
                lore.add(gradient((i + 1) + "  |  " + ability.title().toUpperCase(), profile.colorFrom(), profile.colorTo()));
                String detail = ability.keys();
                if (ability.cooldownPath() != null) {
                    detail += "  •  " + plugin.getConfig().getInt(ability.cooldownPath(), ability.defaultSeconds()) + "s";
                }
                lore.add(solid(detail, 0x89C7FF));
                addWrapped(lore, ability.description(), 0xD2D8E6);
            }
            lore.add(Component.empty());
            lore.add(gradient("❄ FROST CLAIMS THE WORTHY ❄", profile.colorFrom(), profile.colorTo()));
            meta.lore(lore);
        }
        // Only customizes tooltips, NEVER the item model or worn armor texture.
        meta.setTooltipStyle(NamespacedKey.fromString("frost:" + id));
        item.setItemMeta(meta);
        return true;
    }
}
