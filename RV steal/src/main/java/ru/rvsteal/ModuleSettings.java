package ru.rvsteal;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import java.util.*;

final class ModuleSettings {
    static final class Esp {
        boolean box = true, name = true, hp = true, gear = true, enchants;
        boolean corners, healthBar = true, distance, tracers, shadow = true, background = true;
        int color = 0xc58cff, textColor = 0xffffff, outlineColor = 0x101018;
        int thickness = 1, fill = 0, scale = 100, padding = 0;
        int range = 64;
    }
    static final Esp players = new Esp(), mobs = new Esp();
    static boolean playerLegit = true, mobLegit = true, blockEsp, nightVision;
    static boolean watermark = true, hotkeysHud = true, potionsHud = true, staffHud = true;
    static boolean worldTheme, ambientParticles, itemEsp, autoTotem, noHurtCam, targetHud, hitboxes, pearlTrails, itemPhysics;
    static int hitboxSize = 30;
    static int skyColor = 0x9385da, fogColor = 0xc4adeb, cloudColor = 0xffd5ed;
    static int particleStyle, particleDensity = 2, blockColor = 0x65e5e1;
    static int blockRange = 16;
    static String blocks = "minecraft:chest,minecraft:spawner,minecraft:diamond_ore,minecraft:deepslate_diamond_ore";
    static final Map<String, Integer> binds = new HashMap<>();
    private static final Set<Integer> held = new HashSet<>();

    static void load(Properties p) {
        binds.clear();
        CustomFunctions.load(p);
        UserScripts.load(p);
        pearlTrails = Boolean.parseBoolean(p.getProperty("pearlTrails", "false"));
        itemPhysics = Boolean.parseBoolean(p.getProperty("itemPhysics", "false"));
        hitboxes = Boolean.parseBoolean(p.getProperty("hitboxes", "false"));
        hitboxSize = number(p, "hitboxSize", 30, 0, 300);
        targetHud = Boolean.parseBoolean(p.getProperty("targetHud", "false"));
        noHurtCam = Boolean.parseBoolean(p.getProperty("noHurtCam", "false"));
        autoTotem = Boolean.parseBoolean(p.getProperty("autoTotem", "false"));
        worldTheme = Boolean.parseBoolean(p.getProperty("worldTheme", "false"));
        ambientParticles = Boolean.parseBoolean(p.getProperty("ambientParticles", "false"));
        itemEsp = Boolean.parseBoolean(p.getProperty("itemEsp", "false"));
        skyColor = color(p, "skyColor", 0x9385da); fogColor = color(p, "fogColor", 0xc4adeb);
        cloudColor = color(p, "cloudColor", 0xffd5ed); blockColor = color(p, "blockColor", 0x65e5e1);
        particleStyle = number(p, "particleStyle", 0, 0, 2);
        particleDensity = number(p, "particleDensity", 2, 1, 5);
        watermark = Boolean.parseBoolean(p.getProperty("watermark", "true"));
        hotkeysHud = Boolean.parseBoolean(p.getProperty("hotkeysHud", "true"));
        potionsHud = Boolean.parseBoolean(p.getProperty("potionsHud", "true"));
        staffHud = Boolean.parseBoolean(p.getProperty("staffHud", "true"));
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
            e.corners = Boolean.parseBoolean(p.getProperty(k + ".corners", "false"));
            e.healthBar = Boolean.parseBoolean(p.getProperty(k + ".healthBar", "true"));
            e.distance = Boolean.parseBoolean(p.getProperty(k + ".distance", "false"));
            e.tracers = Boolean.parseBoolean(p.getProperty(k + ".tracers", "false"));
            e.shadow = Boolean.parseBoolean(p.getProperty(k + ".shadow", "true"));
            e.background = Boolean.parseBoolean(p.getProperty(k + ".background", "true"));
            e.color = color(p, k + ".color", 0xc58cff);
            e.textColor = color(p, k + ".textColor", 0xffffff);
            e.outlineColor = color(p, k + ".outlineColor", 0x101018);
            e.thickness = number(p, k + ".thickness", 1, 1, 4);
            e.fill = number(p, k + ".fill", 0, 0, 150);
            e.scale = number(p, k + ".scale", 100, 50, 200);
            e.padding = number(p, k + ".padding", 0, 0, 12);
        }
        for (String key : p.stringPropertyNames()) if (key.startsWith("bind.")) {
            int value = number(p, key, -1, -1, 348);
            if (value >= 32 && value != org.lwjgl.glfw.GLFW.GLFW_KEY_F7 && value != org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT) binds.put(key.substring(5), value);
        }
    }
    private static int number(Properties p, String key, int fallback, int min, int max) {
        try { return Math.clamp(Integer.parseInt(p.getProperty(key, "")), min, max); }
        catch (NumberFormatException ignored) { return fallback; }
    }
    static int color(Properties p, String key, int fallback) {
        String value = p.getProperty(key, "").replace("#", "");
        if (!value.matches("[0-9a-fA-F]{6}")) return fallback;
        return Integer.parseInt(value, 16);
    }
    static String hex(int color) { return String.format(Locale.ROOT, "%06X", color & 0xffffff); }
    static void store(Properties p) {
        CustomFunctions.store(p);
        UserScripts.store(p);
        p.setProperty("pearlTrails", "" + pearlTrails); p.setProperty("itemPhysics", "" + itemPhysics);
        p.setProperty("hitboxes", "" + hitboxes); p.setProperty("hitboxSize", "" + hitboxSize);
        p.setProperty("targetHud", "" + targetHud);
        p.setProperty("noHurtCam", "" + noHurtCam);
        p.setProperty("autoTotem", "" + autoTotem);
        p.setProperty("worldTheme", "" + worldTheme); p.setProperty("ambientParticles", "" + ambientParticles);
        p.setProperty("itemEsp", "" + itemEsp);
        p.setProperty("skyColor", hex(skyColor)); p.setProperty("fogColor", hex(fogColor));
        p.setProperty("cloudColor", hex(cloudColor)); p.setProperty("blockColor", hex(blockColor));
        p.setProperty("particleStyle", "" + particleStyle); p.setProperty("particleDensity", "" + particleDensity);
        p.setProperty("watermark", "" + watermark);
        p.setProperty("hotkeysHud", "" + hotkeysHud);
        p.setProperty("potionsHud", "" + potionsHud);
        p.setProperty("staffHud", "" + staffHud);
        p.setProperty("playerLegit", "" + playerLegit); p.setProperty("mobLegit", "" + mobLegit);
        p.setProperty("blockEsp", "" + blockEsp); p.setProperty("nightVision", "" + nightVision);
        p.setProperty("blockRange", "" + blockRange); p.setProperty("blocks", blocks);
        for (var entry : Map.of("players", players, "mobs", mobs).entrySet()) {
            String k = entry.getKey(); Esp e = entry.getValue();
            p.setProperty(k + ".box", "" + e.box); p.setProperty(k + ".name", "" + e.name);
            p.setProperty(k + ".hp", "" + e.hp); p.setProperty(k + ".gear", "" + e.gear);
            p.setProperty(k + ".enchants", "" + e.enchants); p.setProperty(k + ".range", "" + e.range);
            p.setProperty(k + ".corners", "" + e.corners); p.setProperty(k + ".healthBar", "" + e.healthBar);
            p.setProperty(k + ".distance", "" + e.distance); p.setProperty(k + ".tracers", "" + e.tracers);
            p.setProperty(k + ".shadow", "" + e.shadow); p.setProperty(k + ".background", "" + e.background);
            p.setProperty(k + ".color", hex(e.color)); p.setProperty(k + ".textColor", hex(e.textColor));
            p.setProperty(k + ".outlineColor", hex(e.outlineColor));
            p.setProperty(k + ".thickness", "" + e.thickness); p.setProperty(k + ".fill", "" + e.fill);
            p.setProperty(k + ".scale", "" + e.scale); p.setProperty(k + ".padding", "" + e.padding);
        }
        binds.forEach((key, value) -> p.setProperty("bind." + key, "" + value));
    }
    static void tickBinds(Minecraft client) {
        if (binds.isEmpty()) { held.clear(); return; }
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
