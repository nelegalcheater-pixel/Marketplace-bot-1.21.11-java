package ru.rvsteal;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import net.minecraft.resources.Identifier;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class RvSteal implements ClientModInitializer {
    private static final Path CONFIG = FabricLoader.getInstance().getConfigDir().resolve("rvsteal.properties");
    private static boolean hidden, f7Held;
    private static int f7Presses;
    public static boolean active() { return AccessKey.unlocked() && !hidden; }
    static void hide() {
        hidden = true; f7Presses = 0; ClientModules.suspend(Minecraft.getInstance());
        Minecraft.getInstance().setScreen(null);
    }
    public static boolean nightVisionEnabled() { return active() && ModuleSettings.nightVision; }
    public static boolean itemPhysicsEnabled() { return active() && ModuleSettings.itemPhysics; }
    public static boolean noHurtCamEnabled() { return active() && ModuleSettings.noHurtCam; }
    static boolean betterNear;
    static boolean armorHud;
    static boolean coordinatesHud;
    static boolean attackPlayers;
    static boolean attackMobs;
    static boolean autoSprint;
    static boolean autoRespawn;
    public static boolean playerEsp, mobEsp;
    static boolean playerAura, mobAura, autoTool, bunnyHop, bright;
    static boolean healthHud;

    @Override public void onInitializeClient() {
        load();
        var category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("rvsteal", "main"));
        var key = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.rvsteal.open", InputConstants.Type.KEYSYM, InputConstants.KEY_RSHIFT, category));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            boolean f7 = InputConstants.isKeyDown(client.getWindow(), GLFW.GLFW_KEY_F7);
            if (hidden && f7 && !f7Held && ++f7Presses >= 5) { hidden = false; f7Presses = 0; }
            f7Held = f7;
            while (key.consumeClick()) if (!hidden && client.screen == null) client.setScreen(AccessKey.unlocked() ? new RvScreen() : new ActivationScreen());
            if (!active()) return;
            ScreenHud.tick(client);
            ModuleSettings.tickBinds(client);
            UserScripts.tick(client);
            EspOverlay.tick(client);
            PearlTrails.tick(client);
            ClientModules.tick(client);
            WorldVisuals.tick(client);
            HudCache.tick(client);
        });
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath("rvsteal", "hud"), (graphics, delta) -> { if (!active()) return; ScreenHud.render(graphics); HudCache.render(graphics); EspOverlay.render(graphics, delta.getGameTimeDeltaPartialTick(false)); PearlTrails.render(graphics, delta.getGameTimeDeltaPartialTick(false)); });
    }

    private static void load() {
        Properties properties = new Properties();
        if (!Files.exists(CONFIG)) return;
        try (InputStream in = Files.newInputStream(CONFIG)) {
            properties.load(in);
            AccessKey.load(properties);
            apply(properties);
        } catch (IOException e) {
            org.slf4j.LoggerFactory.getLogger("rvsteal").warn("Could not read configuration", e);
        }
    }
    static void apply(Properties properties) {
        ModuleSettings.load(properties);
        HudLayout.load(properties);
        betterNear = Boolean.parseBoolean(properties.getProperty("betterNear", "false"));
        armorHud = Boolean.parseBoolean(properties.getProperty("armorHud", "false"));
        coordinatesHud = Boolean.parseBoolean(properties.getProperty("coordinatesHud", "false"));
        autoRespawn = Boolean.parseBoolean(properties.getProperty("autoRespawn", "false"));
        autoSprint = Boolean.parseBoolean(properties.getProperty("autoSprint", "false"));
        attackMobs = Boolean.parseBoolean(properties.getProperty("attackMobs", "false"));
        attackPlayers = Boolean.parseBoolean(properties.getProperty("attackPlayers", "false"));
        playerEsp = Boolean.parseBoolean(properties.getProperty("playerEsp", "false"));
        mobEsp = Boolean.parseBoolean(properties.getProperty("mobEsp", "false"));
        playerAura = Boolean.parseBoolean(properties.getProperty("playerAura", "false"));
        mobAura = Boolean.parseBoolean(properties.getProperty("mobAura", "false"));
        autoTool = Boolean.parseBoolean(properties.getProperty("autoTool", "false"));
        bunnyHop = Boolean.parseBoolean(properties.getProperty("bunnyHop", "false"));
        bright = Boolean.parseBoolean(properties.getProperty("bright", "false"));
        healthHud = Boolean.parseBoolean(properties.getProperty("healthHud", "false"));
        EspOverlay.resetBlocks();
    }
    static Properties snapshot() {
        Properties properties = new Properties();
        properties.setProperty("betterNear", Boolean.toString(betterNear));
        properties.setProperty("armorHud", Boolean.toString(armorHud));
        properties.setProperty("coordinatesHud", Boolean.toString(coordinatesHud));
        properties.setProperty("autoRespawn", Boolean.toString(autoRespawn));
        properties.setProperty("autoSprint", Boolean.toString(autoSprint));
        properties.setProperty("attackMobs", Boolean.toString(attackMobs));
        properties.setProperty("attackPlayers", Boolean.toString(attackPlayers));
        properties.setProperty("playerEsp", Boolean.toString(playerEsp));
        properties.setProperty("mobEsp", Boolean.toString(mobEsp));
        properties.setProperty("playerAura", Boolean.toString(playerAura));
        properties.setProperty("mobAura", Boolean.toString(mobAura));
        properties.setProperty("autoTool", Boolean.toString(autoTool));
        properties.setProperty("bunnyHop", Boolean.toString(bunnyHop));
        properties.setProperty("bright", Boolean.toString(bright));
        properties.setProperty("healthHud", Boolean.toString(healthHud));
        ModuleSettings.store(properties);
        HudLayout.store(properties);
        return properties;
    }
    static void save() {
        Properties properties = snapshot();
        AccessKey.store(properties);
        try {
            Files.createDirectories(CONFIG.getParent());
            try (OutputStream out = Files.newOutputStream(CONFIG)) { properties.store(out, "KillerKC2 client"); }
        } catch (IOException e) {
            org.slf4j.LoggerFactory.getLogger("rvsteal").warn("Could not save configuration", e);
        }
    }
}
