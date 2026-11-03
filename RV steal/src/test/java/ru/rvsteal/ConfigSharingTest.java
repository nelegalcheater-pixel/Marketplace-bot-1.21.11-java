package ru.rvsteal;
import java.util.Properties;
import org.junit.Test;
import static org.junit.Assert.*;

public class ConfigSharingTest {
    @Test public void copyAndImportExcludesActivationAndKeepsLayout() throws Exception {
        HudLayout.reset(); HudLayout.move("Target", 480, 270, 960, 540);
        Properties original = new Properties(); HudLayout.store(original);
        original.setProperty("hitboxes", "true"); original.setProperty("hitboxSize", "130"); original.setProperty("access.key", "secret");
        Properties imported = ConfigCodec.decode(ConfigCodec.encode(original));
        assertNull(imported.getProperty("access.key"));
        ModuleSettings.load(imported); HudLayout.reset(); HudLayout.load(imported);
        assertTrue(ModuleSettings.hitboxes); assertEquals(130, ModuleSettings.hitboxSize);
        assertEquals(960, HudLayout.x("Target", 1920, 0)); assertEquals(540, HudLayout.y("Target", 1080, 0));
        HudLayout.reset(); ModuleSettings.load(new Properties());
    }
    @Test public void rejectsInvalidOrHugeInput() throws Exception {
        for (String input : new String[]{"", "KILLERKC2:1:!!!!", "KILLERKC2:1:" + "a".repeat(65536), "KILLERKC2:2:Zm9v"}) {
            try { ConfigCodec.decode(input); fail("expected invalid config"); } catch (IllegalArgumentException expected) { }
        }
    }
}
