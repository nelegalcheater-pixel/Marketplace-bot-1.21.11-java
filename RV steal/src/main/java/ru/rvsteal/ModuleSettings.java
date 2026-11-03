package ru.rvsteal;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import java.util.*;

final class ModuleSettings {
    static final class Esp {
        boolean box = true, name = true, hp = true, gear = true, enchants;
        int range = 64;
    }
    static final Esp players = new Esp(), mobs = new Esp();
    static boolean playerLegit = true, mobLegit = true, blockEsp, nightVision;
    static int blockRange = 16;
    static String blocks = "minecraft:chest,minecraft:spawner,minecraft:diamond_ore,minecraft:deepslate_diamond_ore";
    static final Map<String, Integer> binds = new HashMap<>();
    private static final Set<Integer> held = new HashSet<>();

    static void load(Properties p) {
        binds.clear();
        playerLegit = Boolean.parseBoolean(p.getProperty("playerLegit", "true"));
        mobLegit = Boolean.parseBoolean(p.getProperty("mobLegit", "true"));
        blockEsp = Boolean.parseBoolean(p.getProperty("blockEsp", "false"));
        nightVision = Boolean.parseBoolean(p.getProperty("nightVision", "false"));
        blockRange = number(p, "blockRange", 16, 8, 32);
        blocks = p.getProperty("blocks", blocks);
        for (var entry : Map.of("players", players, "mobs", mobs).entrySet()) {
            String k = entry.getKey(); Esp e = entry.getValue();
            e.box = Boolean.parseBoolean(p.getProperty(k + ".box", "true"));
            e.name = Boolean.parseBoolean(p.getProperty(k + ".name", "true"));
            e.hp = Boolean.parseBoolean(p.getProperty(k + ".hp", "true"));
            e.gear = Boolean.parseBoolean(p.getProperty(k + ".gear", "true"));
            e.enchants = Boolean.parseBoolean(p.getProperty(k + ".enchants", "false"));
            e.range = number(p, k + ".range", 64, 16, 128);
        }
        for (String key : p.stringPropertyNames()) if (key.startsWith("bind.")) {
            int value = number(p, key, -1, -1, 348);
            if (value >= 32) binds.put(key.substring(5), value);
        }
    }
    private static int number(Properties p, String key, int fallback, int min, int max) {
        try { return Math.clamp(Integer.parseInt(p.getProperty(key, "")), min, max); }
        catch (NumberFormatException ignored) { return fallback; }
    }
    static void store(Properties p) {
        p.setProperty("playerLegit", "" + playerLegit); p.setProperty("mobLegit", "" + mobLegit);
        p.setProperty("blockEsp", "" + blockEsp); p.setProperty("nightVision", "" + nightVision);
        p.setProperty("blockRange", "" + blockRange); p.setProperty("blocks", blocks);
        for (var entry : Map.of("players", players, "mobs", mobs).entrySet()) {
            String k = entry.getKey(); Esp e = entry.getValue();
            p.setProperty(k + ".box", "" + e.box); p.setProperty(k + ".name", "" + e.name);
            p.setProperty(k + ".hp", "" + e.hp); p.setProperty(k + ".gear", "" + e.gear);
            p.setProperty(k + ".enchants", "" + e.enchants); p.setProperty(k + ".range", "" + e.range);
        }
        binds.forEach((key, value) -> p.setProperty("bind." + key, "" + value));
    }
    static void tickBinds(Minecraft client) {
        Set<Integer> now = new HashSet<>();
        for (int key : binds.values()) if (InputConstants.isKeyDown(client.getWindow(), key)) now.add(key);
        if (client.screen == null && client.player != null) {
            boolean changed = false;
            for (var module : RvScreen.allModules()) {
                int key = binds.getOrDefault(module.name(), -1);
                if (now.contains(key) && !held.contains(key)) {
                    module.set().accept(!module.get().getAsBoolean()); changed = true;
                }
            }
            if (changed) RvSteal.save();
        }
        held.clear(); held.addAll(now);
    }
}
