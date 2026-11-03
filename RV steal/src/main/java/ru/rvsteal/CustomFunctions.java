package ru.rvsteal;

import java.util.*;

final class CustomFunctions {
    record Function(String name, List<String> actions, boolean enabled) {}
    static final LinkedHashMap<String, Function> functions = new LinkedHashMap<>();
    private static final Map<String, String> ALIASES = Map.ofEntries(
        Map.entry("ночное зрение", "Night Vision"), Map.entry("автоспринт", "Auto Sprint"),
        Map.entry("координаты", "XYZ / FPS"), Map.entry("броня", "Armor HUD"),
        Map.entry("игроки есп", "Player ESP"), Map.entry("мобы есп", "Mob ESP"),
        Map.entry("блоки есп", "Block ESP"), Map.entry("следы бросков", "Throw Trails"),
        Map.entry("физика предметов", "Item Physics"), Map.entry("частицы", "Ambient Particles"),
        Map.entry("тема мира", "World Theme"), Map.entry("автоинструмент", "Auto Tool"));
    static String supported() { return String.join("; ", new TreeSet<>(ALIASES.keySet())); }
    static Function parse(String name, String text) {
        name = name.trim();
        if (name.isEmpty() || name.length() > 32 || name.contains(".") || name.contains("\n")) throw new IllegalArgumentException("Название: 1–32 символа без точки");
        if (Set.of("Configs", "HUD editor", "Function builder").contains(name)) throw new IllegalArgumentException("Название занято");
        var actions = new ArrayList<String>();
        for (String part : text.split(";", -1)) {
            String action = part.trim().toLowerCase(Locale.ROOT).replaceFirst("^(включить|переключать|управлять)\\s+", "");
            String canonical = ALIASES.get(action);
            if (canonical == null) canonical = ALIASES.values().stream().filter(v -> v.equalsIgnoreCase(action)).findFirst().orElse(null);
            if (canonical == null) throw new IllegalArgumentException("Неизвестное действие: " + part.trim());
            if (!actions.contains(canonical)) actions.add(canonical);
        }
        if (actions.isEmpty() || actions.size() > 12) throw new IllegalArgumentException("Нужно от 1 до 12 действий");
        return new Function(name, List.copyOf(actions), false);
    }
    static void create(String name, String text) {
        Function f = parse(name, text);
        if (functions.size() >= 24 && !functions.containsKey(f.name())) throw new IllegalArgumentException("Не более 24 функций");
        if (RvScreen.baseModules().stream().anyMatch(m -> m.name().equalsIgnoreCase(f.name()))) throw new IllegalArgumentException("Название занято модулем");
        functions.put(f.name(), f);
    }
    static List<RvScreen.Module> modules() {
        var list = new ArrayList<RvScreen.Module>();
        for (Function f : functions.values()) list.add(new RvScreen.Module(f.name(), String.join("; ", f.actions()),
            () -> functions.get(f.name()).enabled(), enabled -> {
                functions.put(f.name(), new Function(f.name(), f.actions(), enabled));
                for (var m : RvScreen.baseModules()) if (f.actions().contains(m.name())) m.set().accept(enabled);
            }));
        return list;
    }
    static void store(Properties p) {
        int i = 0;
        for (var f : functions.values()) {
            String k = "custom." + i++ + ".";
            p.setProperty(k + "name", f.name()); p.setProperty(k + "actions", String.join(";", f.actions())); p.setProperty(k + "enabled", "" + f.enabled());
        }
    }
    static void load(Properties p) {
        functions.clear();
        for (int i = 0; i < 24; i++) {
            String k = "custom." + i + ".";
            if (!p.containsKey(k + "name")) continue;
            try {
                Function f = parse(p.getProperty(k + "name"), p.getProperty(k + "actions", ""));
                if (RvScreen.baseModules().stream().anyMatch(m -> m.name().equalsIgnoreCase(f.name()))) continue;
                functions.put(f.name(), new Function(f.name(), f.actions(), Boolean.parseBoolean(p.getProperty(k + "enabled", "false"))));
            } catch (IllegalArgumentException ignored) { }
        }
    }
}
