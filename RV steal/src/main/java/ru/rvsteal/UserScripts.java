package ru.rvsteal;

import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

final class UserScripts {
    static final int MAX_SCRIPTS = 8, MAX_CODE = 4096;
    record Script(String name, String code, boolean enabled) {}
    static final LinkedHashMap<String, Script> scripts = new LinkedHashMap<>();
    private static final Map<String, LuaRuntime> runtimes = new HashMap<>();
    private static final LuaRuntime.Game GAME = new LuaRuntime.Game() {
        public double health() { var p = Minecraft.getInstance().player; return p == null ? 0 : p.getHealth(); }
        public int hunger() { var p = Minecraft.getInstance().player; return p == null ? 0 : p.getFoodData().getFoodLevel(); }
        public boolean module(String name, boolean enabled) {
            for (var m : RvScreen.baseModules()) if (m.name().equalsIgnoreCase(name)) { if (m.get().getAsBoolean() != enabled) { m.set().accept(enabled); RvSteal.save(); } return true; }
            return false;
        }
        public void message(String text) { var p = Minecraft.getInstance().player; if (p != null) p.displayClientMessage(Component.literal(text.substring(0, Math.min(text.length(), 160))), false); }
    };
    static void create(String name, String code) {
        name = name.trim();
        if (name.isEmpty() || name.length() > 32 || name.contains(".") || name.contains("\n") || name.contains("\r")) throw new IllegalArgumentException("Название: 1–32 символа без точки");
        if (code.isBlank() || code.length() > MAX_CODE) throw new IllegalArgumentException("Код: 1–4096 символов");
        if (scripts.size() >= MAX_SCRIPTS && !scripts.containsKey(name)) throw new IllegalArgumentException("Не более 8 скриптов");
        String checked = name;
        if (RvScreen.baseModules().stream().anyMatch(m -> m.name().equalsIgnoreCase(checked)) || CustomFunctions.functions.keySet().stream().anyMatch(n -> n.equalsIgnoreCase(checked)) || Set.of("Configs", "HUD editor", "Function builder", "Script editor").stream().anyMatch(n -> n.equalsIgnoreCase(checked))) throw new IllegalArgumentException("Название занято");
        if (scripts.keySet().stream().anyMatch(n -> n.equalsIgnoreCase(checked) && !n.equals(checked))) throw new IllegalArgumentException("Название занято");
        Properties sizeCheck = new Properties();
        for (var item : scripts.values()) if (!item.name().equals(name)) sizeCheck.setProperty(item.name(), item.code());
        sizeCheck.setProperty(name, code);
        try {
            var bytes = new java.io.ByteArrayOutputStream(); sizeCheck.store(bytes, null);
            if (bytes.size() > 22000) throw new IllegalArgumentException("Скрипты слишком большие для обмена конфигом");
        } catch (java.io.IOException e) { throw new IllegalArgumentException("Ошибка проверки размера", e); }
        try { new LuaRuntime(code, GAME); } catch (RuntimeException | StackOverflowError e) { throw new IllegalArgumentException("Ошибка Lua: " + e.getMessage()); }
        scripts.put(name, new Script(name, code, false)); runtimes.remove(name);
    }
    static void delete(String name) { scripts.remove(name); runtimes.remove(name); ModuleSettings.binds.remove(name); }
    static List<RvScreen.Module> modules() {
        var result = new ArrayList<RvScreen.Module>();
        for (var script : scripts.values()) result.add(new RvScreen.Module(script.name(), "Lua скрипт", () -> scripts.get(script.name()).enabled(), enabled -> {
            var current = scripts.get(script.name());
            if (enabled) runtimes.put(current.name(), new LuaRuntime(current.code(), GAME)); else runtimes.remove(current.name());
            scripts.put(current.name(), new Script(current.name(), current.code(), enabled));
        }));
        return result;
    }
    static void tick(Minecraft client) {
        if (client.player == null || client.level == null) return;
        for (var entry : new ArrayList<>(scripts.values())) {
            if (!entry.enabled()) continue;
            try { runtimes.get(entry.name()).tick(); }
            catch (RuntimeException | StackOverflowError e) {
                scripts.put(entry.name(), new Script(entry.name(), entry.code(), false)); runtimes.remove(entry.name());
                String error = Objects.toString(e.getMessage(), "ошибка");
                client.player.displayClientMessage(Component.literal("Lua " + entry.name() + ": " + error.substring(0, Math.min(120, error.length()))), false);
                RvSteal.save();
            }
        }
    }
    static void store(Properties p) {
        int i = 0;
        for (var s : scripts.values()) { String k = "script." + i++ + "."; p.setProperty(k + "name", s.name()); p.setProperty(k + "code", s.code()); }
    }
    static void load(Properties p) {
        scripts.clear(); runtimes.clear();
        for (int i = 0; i < MAX_SCRIPTS; i++) { String k = "script." + i + "."; if (!p.containsKey(k + "name")) continue; try { create(p.getProperty(k + "name"), p.getProperty(k + "code", "")); } catch (RuntimeException ignored) {} }
    }
}
