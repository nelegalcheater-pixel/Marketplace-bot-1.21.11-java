package ru.rvsteal;
import java.util.Properties;

/** Runs without a game session; checks the on-disk settings contract. */
public final class ModuleSettingsTest {
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
