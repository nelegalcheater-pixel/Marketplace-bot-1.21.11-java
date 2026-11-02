package ru.rvsteal;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class RvSteal implements ClientModInitializer {
    private static final Path CONFIG = FabricLoader.getInstance().getConfigDir().resolve("rvsteal.properties");
    static boolean betterNear;
    static boolean armorHud;
    static boolean coordinatesHud;
    static boolean attackPlayers;
    static boolean attackMobs;
    static boolean autoSprint;
    static boolean autoRespawn;

    @Override public void onInitializeClient() {
        load();
        var category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("rvsteal", "main"));
        var key = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.rvsteal.open", InputConstants.Type.KEYSYM, InputConstants.KEY_RSHIFT, category));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (key.consumeClick()) if (client.screen == null) client.setScreen(new RvScreen());
            ClientModules.tick(client);
            HudCache.tick(client);
        });
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath("rvsteal", "hud"), (graphics, delta) -> HudCache.render(graphics));
    }

    private static void load() {
        Properties properties = new Properties();
        if (!Files.exists(CONFIG)) return;
        try (InputStream in = Files.newInputStream(CONFIG)) {
            properties.load(in);
            betterNear = Boolean.parseBoolean(properties.getProperty("betterNear", "false"));
            armorHud = Boolean.parseBoolean(properties.getProperty("armorHud", "false"));
            coordinatesHud = Boolean.parseBoolean(properties.getProperty("coordinatesHud", "false"));
            autoRespawn = Boolean.parseBoolean(properties.getProperty("autoRespawn", "false"));
            autoSprint = Boolean.parseBoolean(properties.getProperty("autoSprint", "false"));
            attackMobs = Boolean.parseBoolean(properties.getProperty("attackMobs", "false"));
            attackPlayers = Boolean.parseBoolean(properties.getProperty("attackPlayers", "false"));
        } catch (IOException e) {
            org.slf4j.LoggerFactory.getLogger("rvsteal").warn("Could not read configuration", e);
        }
    }
    static void save() {
        Properties properties = new Properties();
        properties.setProperty("betterNear", Boolean.toString(betterNear));
        properties.setProperty("armorHud", Boolean.toString(armorHud));
        properties.setProperty("coordinatesHud", Boolean.toString(coordinatesHud));
        properties.setProperty("autoRespawn", Boolean.toString(autoRespawn));
        properties.setProperty("autoSprint", Boolean.toString(autoSprint));
        properties.setProperty("attackMobs", Boolean.toString(attackMobs));
        properties.setProperty("attackPlayers", Boolean.toString(attackPlayers));
        try {
            Files.createDirectories(CONFIG.getParent());
            try (OutputStream out = Files.newOutputStream(CONFIG)) { properties.store(out, "KillerKC2 client"); }
        } catch (IOException e) {
            org.slf4j.LoggerFactory.getLogger("rvsteal").warn("Could not save configuration", e);
        }
    }
}
