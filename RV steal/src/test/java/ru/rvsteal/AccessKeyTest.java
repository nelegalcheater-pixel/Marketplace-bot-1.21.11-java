package ru.rvsteal;
import java.util.Properties;
import org.junit.Test;
import static org.junit.Assert.*;
public class AccessKeyTest {
    @Test public void activationSurvivesReloadAndRejectsInvalidKeys() {
        AccessKey.load(new Properties()); assertFalse(AccessKey.unlocked());
        assertFalse(AccessKey.activate("killerkc2")); assertFalse(AccessKey.activate(""));
        assertFalse(AccessKey.activate("KillerKC2-expired")); assertFalse(AccessKey.unlocked());
        assertTrue(AccessKey.activate("KillerKC2")); assertTrue(AccessKey.unlocked());
        var p = new Properties(); AccessKey.store(p);
        AccessKey.load(new Properties()); assertFalse(AccessKey.unlocked());
        AccessKey.load(p); assertTrue(AccessKey.unlocked());
        p.setProperty("access.key", "wrong"); AccessKey.load(p); assertFalse(AccessKey.unlocked());
    }
}
