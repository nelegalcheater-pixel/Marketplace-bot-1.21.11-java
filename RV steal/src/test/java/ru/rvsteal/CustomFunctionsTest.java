package ru.rvsteal;
import java.util.Properties;
import org.junit.Test;
import static org.junit.Assert.*;
public class CustomFunctionsTest {
    @Test public void russianActionsRoundTrip() {
        var f = CustomFunctions.parse("Ночной режим", "включить ночное зрение; координаты; Night Vision");
        assertEquals(2, f.actions().size()); assertEquals("Night Vision", f.actions().getFirst());
        CustomFunctions.functions.clear(); CustomFunctions.functions.put(f.name(), f);
        Properties p = new Properties(); CustomFunctions.store(p); CustomFunctions.load(p);
        assertEquals(f, CustomFunctions.functions.get(f.name()));
        CustomFunctions.functions.clear();
    }
    @Test public void rejectsUnknownCodeAndBadNames() {
        for (String action : new String[]{"", "скачать файл", "Runtime.exec()", "ночное зрение;"}) {
            try { CustomFunctions.parse("Функция", action); fail(); } catch (IllegalArgumentException expected) { }
        }
        try { CustomFunctions.parse("a.b", "ночное зрение"); fail(); } catch (IllegalArgumentException expected) { }
    }
}
