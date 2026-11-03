package ru.rvsteal;
import org.junit.Test;
import org.luaj.vm2.LuaError;
import java.util.Properties;
import static org.junit.Assert.*;
public class LuaRuntimeTest {
    private static final class Fake implements LuaRuntime.Game {
        double hp = 8; int calls; boolean enabled; String target;
        public double health() { return hp; }
        public int hunger() { return 5; }
        public boolean module(String name, boolean value) { target = name; enabled = value; calls++; return true; }
        public void message(String text) { }
    }
    @Test public void evaluatesConditionsAndPersistsVariablesBetweenTicks() {
        Fake game = new Fake();
        var runtime = new LuaRuntime("count=(count or 0)+1; if health() < 10 and count == 2 then module('Auto Sprint', true) end", game);
        runtime.tick(); assertEquals(0, game.calls); runtime.tick(); assertEquals(1, game.calls); assertTrue(game.enabled); assertEquals("Auto Sprint", game.target);
    }
    @Test public void blocksUnboundedLoopsAndDoesNotExposeJavaOrFiles() {
        var game = new Fake(); var runtime = new LuaRuntime("while true do end", game);
        try { runtime.tick(); fail("loop should be stopped"); } catch (LuaError expected) { assertTrue(expected.getMessage().contains("лимит")); }
        new LuaRuntime("assert(io == nil and os == nil and luajava == nil and require == nil and loadfile == nil)", game).tick();
    }
    @Test public void savesCodeButRequiresExplicitEnableAfterImport() throws Exception {
        UserScripts.scripts.clear(); CustomFunctions.functions.clear();
        UserScripts.create("Test Script", "module('Night Vision', true)"); UserScripts.modules().getFirst().set().accept(true);
        Properties p = new Properties(); UserScripts.store(p);
        Properties decoded = ConfigCodec.decode(ConfigCodec.encode(p)); UserScripts.load(decoded);
        assertEquals("module('Night Vision', true)", UserScripts.scripts.get("Test Script").code()); assertFalse(UserScripts.scripts.get("Test Script").enabled()); UserScripts.delete("Test Script");
    }
}
