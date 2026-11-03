package ru.rvsteal;

import org.luaj.vm2.*;
import org.luaj.vm2.compiler.LuaC;
import org.luaj.vm2.lib.*;

/** Each script has its own small Lua environment. No Java, file, network or OS API is exposed. */
final class LuaRuntime {
    interface Game {
        double health();
        int hunger();
        boolean module(String name, boolean enabled);
        void message(String text);
    }
    private final Globals globals = new Globals();
    private final LuaValue program;
    private final Budget budget = new Budget();
    private int messages;
    LuaRuntime(String source, Game game) {
        globals.load(new BaseLib());
        LuaTable pkg = new LuaTable(); pkg.set("loaded", new LuaTable()); globals.set("package", pkg);
        globals.load(new org.luaj.vm2.lib.jse.JseMathLib());
        globals.set("package", LuaValue.NIL);
        for (String name : new String[]{"dofile", "loadfile", "load", "loadstring", "require", "pcall", "xpcall", "print", "collectgarbage"}) globals.set(name, LuaValue.NIL);
        LuaC.install(globals);
        globals.debuglib = budget;
        globals.set("health", new ZeroArgFunction() { public LuaValue call() { return LuaValue.valueOf(game.health()); } });
        globals.set("hunger", new ZeroArgFunction() { public LuaValue call() { return LuaValue.valueOf(game.hunger()); } });
        globals.set("module", new TwoArgFunction() { public LuaValue call(LuaValue name, LuaValue enabled) { return LuaValue.valueOf(game.module(name.checkjstring(), enabled.checkboolean())); } });
        globals.set("message", new OneArgFunction() { public LuaValue call(LuaValue text) { if (++messages > 1) throw new LuaError("Не больше одного сообщения за тик"); game.message(text.checkjstring()); return NIL; } });
        program = globals.load(source, "custom-script");
    }
    void tick() { budget.remaining = 10000; messages = 0; program.call(); }
    private static final class Budget extends DebugLib {
        int remaining;
        @Override public void onCall(LuaFunction function) {}
        @Override public void onCall(LuaClosure closure, Varargs args, LuaValue[] stack) {}
        @Override public void onReturn() {}
        @Override public String traceback(int level) { return ""; }
        @Override public void onInstruction(int pc, Varargs v, int top) {
            if (--remaining < 0) throw new LuaError("Превышен лимит инструкций за тик");
        }
    }
}
