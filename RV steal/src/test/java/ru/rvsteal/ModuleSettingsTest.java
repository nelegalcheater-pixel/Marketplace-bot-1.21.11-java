package ru.rvsteal;
import java.util.Properties;

/** Runs without a game session; checks the on-disk settings contract. */
public final class ModuleSettingsTest {
    @org.junit.Test
    public void customAppearanceRoundTripAndBounds() {
        var p = new Properties();
        ModuleSettings.load(p);
        ModuleSettings.players.color = 0x123456; ModuleSettings.players.textColor = 0x12abef;
        ModuleSettings.players.outlineColor = 0xffeeaa; ModuleSettings.players.thickness = 3;
        ModuleSettings.players.fill = 90; ModuleSettings.players.scale = 175; ModuleSettings.players.padding = 8;
        ModuleSettings.players.corners = true; ModuleSettings.players.tracers = true;
        ModuleSettings.players.distance = true; ModuleSettings.players.background = false;
        ModuleSettings.worldTheme = true; ModuleSettings.ambientParticles = true; ModuleSettings.itemEsp = true;
        ModuleSettings.skyColor = 0xabcdef; ModuleSettings.particleStyle = 2;
        ModuleSettings.store(p); ModuleSettings.load(new Properties()); ModuleSettings.load(p);
        require(ModuleSettings.players.color == 0x123456 && ModuleSettings.players.textColor == 0x12abef
            && ModuleSettings.players.outlineColor == 0xffeeaa, "Arbitrary RGB colors round trip");
        require(ModuleSettings.players.thickness == 3 && ModuleSettings.players.fill == 90
            && ModuleSettings.players.scale == 175 && ModuleSettings.players.padding == 8, "Sizing round trip");
        require(ModuleSettings.players.corners && ModuleSettings.players.tracers && ModuleSettings.players.distance
            && !ModuleSettings.players.background && !ModuleSettings.mobs.corners, "Independent appearance settings");
        require(ModuleSettings.worldTheme && ModuleSettings.ambientParticles && ModuleSettings.itemEsp
            && ModuleSettings.skyColor == 0xabcdef && ModuleSettings.particleStyle == 2, "World visuals round trip");
        p.setProperty("players.color", "ZZZZZZ"); p.setProperty("players.scale", "5000");
        p.setProperty("players.thickness", "-5"); p.setProperty("players.fill", "999");
        p.setProperty("particleDensity", "999"); p.setProperty("particleStyle", "-1");
        ModuleSettings.load(p);
        require(ModuleSettings.players.color == 0xc58cff && ModuleSettings.players.scale == 200
            && ModuleSettings.players.thickness == 1 && ModuleSettings.players.fill == 150, "Appearance validation");
        require(ModuleSettings.particleDensity == 5 && ModuleSettings.particleStyle == 0, "Particle budget bounds");
        ModuleSettings.load(new Properties());
    }
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    @org.junit.Test
    public void settingsRoundTrip() {
        var saved = new Properties();
        ModuleSettings.watermark = false; ModuleSettings.potionsHud = false;
        ModuleSettings.playerLegit = false; ModuleSettings.mobLegit = true;
        ModuleSettings.blockEsp = true; ModuleSettings.nightVision = true;
        ModuleSettings.blockRange = 24; ModuleSettings.blocks = "minecraft:chest,minecraft:spawner";
        ModuleSettings.players.name = false; ModuleSettings.players.enchants = true;
        ModuleSettings.players.range = 96; ModuleSettings.mobs.gear = false;
        ModuleSettings.binds.put("Player Aura", 71);
        ModuleSettings.store(saved);
        ModuleSettings.load(new Properties());
        require(ModuleSettings.binds.isEmpty(), "Reload must discard removed binds");
        require(ModuleSettings.playerLegit && !ModuleSettings.blockEsp && !ModuleSettings.nightVision, "Safe defaults");
        ModuleSettings.load(saved);
        require(!ModuleSettings.watermark && !ModuleSettings.potionsHud && ModuleSettings.staffHud, "HUD visibility round trip");
        require(!ModuleSettings.playerLegit && ModuleSettings.mobLegit, "Independent aura modes");
        require(ModuleSettings.blockEsp && ModuleSettings.nightVision && ModuleSettings.blockRange == 24, "Vision settings round trip");
        require(ModuleSettings.blocks.equals("minecraft:chest,minecraft:spawner"), "Custom block IDs round trip");
        require(!ModuleSettings.players.name && ModuleSettings.players.enchants && ModuleSettings.players.range == 96, "Player ESP options round trip");
        require(!ModuleSettings.mobs.gear && ModuleSettings.mobs.name, "Independent mob ESP options");
        require(ModuleSettings.binds.get("Player Aura") == 71, "Bind round trip");
        var corrupt = new Properties();
        corrupt.setProperty("blockRange", "999999"); corrupt.setProperty("players.range", "not-a-number");
        corrupt.setProperty("mobs.range", "-10"); corrupt.setProperty("bind.Mob Aura", "invalid");
        ModuleSettings.load(corrupt);
        require(ModuleSettings.blockRange == 32 && ModuleSettings.players.range == 64 && ModuleSettings.mobs.range == 16, "Bounded values and invalid-number recovery");
        require(ModuleSettings.binds.isEmpty(), "Invalid binds ignored");
        System.out.println("Module settings persistence checks passed");
    }
}
