package ru.rvsteal;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

final class SettingsScreen extends Screen {
    private final Screen parent;
    private final RvScreen.Module module;
    private boolean binding;
    private int page;
    private String error = "";
    private final List<Runnable> rows = new ArrayList<>();
    private int x, y, row;
    SettingsScreen(Screen parent, RvScreen.Module module) {
        super(Component.literal(module.name() + " — настройки")); this.parent = parent; this.module = module;
    }
    private void button(String label, Runnable action) {
        int index = rows.size();
        rows.add(() -> addRenderableWidget(Button.builder(Component.literal(label), b -> { action.run(); RvSteal.save(); rebuildWidgets(); })
                .bounds(x, y + 52 + (index % 4) * 24, 260, 20).build()));
    }
    private void toggle(String label, BooleanSupplier get, Consumer<Boolean> set) {
        button(label + ": " + (get.getAsBoolean() ? "Вкл" : "Выкл"), () -> set.accept(!get.getAsBoolean()));
    }
    private void colorInput(String label, int color, IntConsumer set) {
        int index = rows.size();
        rows.add(() -> {
            int yy = y + 52 + (index % 4) * 24;
            EditBox input = new EditBox(font, x, yy, 260, 20, Component.literal(label + " #RRGGBB"));
            input.setMaxLength(48); input.setValue(label + ": #" + ModuleSettings.hex(color));
            input.setResponder(value -> {
                String hex = value.substring(value.lastIndexOf('#') + 1).trim();
                if (!hex.matches("[0-9a-fA-F]{6}")) { error = "Цвет: 6 символов после #"; return; }
                set.accept(Integer.parseInt(hex, 16)); error = ""; RvSteal.save();
            });
            addRenderableWidget(input);
        });
    }
    @Override protected void init() {
        x = (width - 260) / 2; y = Math.max(2, (height - 210) / 2); row = 0; rows.clear();
        addRenderableWidget(Button.builder(Component.literal(binding ? "Нажми клавишу; Esc — отмена" : "Бинд: " + keyName()), b -> { binding = true; rebuildWidgets(); })
                .bounds(x, y + 24, 260, 20).build());
        String name = module.name();
        if (name.equals("Hitboxes")) {
            button("Добавить к каждой стороне: " + String.format(Locale.ROOT, "%.1f", ModuleSettings.hitboxSize / 100.0) + " блоков", () -> ModuleSettings.hitboxSize = ModuleSettings.hitboxSize >= 300 ? 0 : ModuleSettings.hitboxSize + 10);
        }
        if (name.equals("Player Aura") || name.equals("Mob Aura")) {
            boolean player = name.equals("Player Aura");
            boolean legit = player ? ModuleSettings.playerLegit : ModuleSettings.mobLegit;
            button("Режим: " + (legit ? "Legit" : "Rage"), () -> {
                if (player) ModuleSettings.playerLegit = !ModuleSettings.playerLegit; else ModuleSettings.mobLegit = !ModuleSettings.mobLegit;
            });
        }
        if (name.equals("Player ESP") || name.equals("Mob ESP")) {
            var e = name.equals("Player ESP") ? ModuleSettings.players : ModuleSettings.mobs;
            toggle("Рамка", () -> e.box, v -> e.box = v);
            toggle("Имя", () -> e.name, v -> e.name = v);
            toggle("HP", () -> e.hp, v -> e.hp = v);
            toggle("Экипировка", () -> e.gear, v -> e.gear = v);
            toggle("Чары", () -> e.enchants, v -> e.enchants = v);
            button("Дальность: " + e.range, () -> e.range = e.range >= 128 ? 16 : e.range + 16);
            colorInput("Рамка", e.color, v -> e.color = v);
            colorInput("Текст", e.textColor, v -> e.textColor = v);
            colorInput("Обводка", e.outlineColor, v -> e.outlineColor = v);
            button("Толщина: " + e.thickness + " px", () -> e.thickness = e.thickness % 4 + 1);
            button("Заливка: " + e.fill, () -> e.fill = e.fill >= 150 ? 0 : e.fill + 30);
            button("Масштаб текста/вещей: " + e.scale + "%", () -> e.scale = e.scale >= 200 ? 50 : e.scale + 25);
            button("Отступ рамки: " + e.padding + " px", () -> e.padding = e.padding >= 12 ? 0 : e.padding + 2);
            toggle("Угловая рамка", () -> e.corners, v -> e.corners = v);
            toggle("Полоска здоровья", () -> e.healthBar, v -> e.healthBar = v);
            toggle("Дистанция", () -> e.distance, v -> e.distance = v);
            toggle("Линии к целям", () -> e.tracers, v -> e.tracers = v);
            toggle("Внешняя обводка", () -> e.shadow, v -> e.shadow = v);
            toggle("Фон подписей", () -> e.background, v -> e.background = v);
        }
        if (name.equals("Block ESP")) {
            button("Радиус: " + ModuleSettings.blockRange, () -> { ModuleSettings.blockRange = ModuleSettings.blockRange >= 32 ? 8 : ModuleSettings.blockRange + 8; EspOverlay.resetBlocks(); });
            button("Сундуки и шалкеры", () -> setBlocks("minecraft:chest,minecraft:trapped_chest,minecraft:ender_chest,minecraft:shulker_box"));
            button("Алмазы и древние обломки", () -> setBlocks("minecraft:diamond_ore,minecraft:deepslate_diamond_ore,minecraft:ancient_debris"));
            button("Спавнеры", () -> setBlocks("minecraft:spawner,minecraft:trial_spawner"));
            colorInput("Цвет блоков", ModuleSettings.blockColor, v -> ModuleSettings.blockColor = v);
            int index = rows.size();
            rows.add(() -> {
                EditBox input = new EditBox(font, x, y + 52 + (index % 4) * 24, 260, 20, Component.literal("ID блоков через запятую"));
                input.setMaxLength(2048); input.setValue(ModuleSettings.blocks);
                input.setResponder(value -> {
                    if (!validBlocks(value)) { error = "Неизвестный ID блока"; return; }
                    error = ""; setBlocks(value); RvSteal.save();
                });
                addRenderableWidget(input);
            });
        }
        if (name.equals("World Theme")) {
            button("Тема: Lavender", () -> { ModuleSettings.skyColor = 0x9385da; ModuleSettings.fogColor = 0xc4adeb; ModuleSettings.cloudColor = 0xffd5ed; });
            button("Тема: Sunset", () -> { ModuleSettings.skyColor = 0xc171a7; ModuleSettings.fogColor = 0xf3b49d; ModuleSettings.cloudColor = 0xffd4b0; });
            button("Тема: Arctic", () -> { ModuleSettings.skyColor = 0x428ba8; ModuleSettings.fogColor = 0xa4dce8; ModuleSettings.cloudColor = 0xd5f5ff; });
            colorInput("Небо", ModuleSettings.skyColor, v -> ModuleSettings.skyColor = v);
            colorInput("Туман", ModuleSettings.fogColor, v -> ModuleSettings.fogColor = v);
            colorInput("Облака", ModuleSettings.cloudColor, v -> ModuleSettings.cloudColor = v);
        }
        if (name.equals("Ambient Particles")) {
            button("Частицы: " + new String[]{"End Rod", "Cherry", "Firefly"}[ModuleSettings.particleStyle], () -> ModuleSettings.particleStyle = (ModuleSettings.particleStyle + 1) % 3);
            button("Плотность: " + ModuleSettings.particleDensity, () -> ModuleSettings.particleDensity = ModuleSettings.particleDensity % 5 + 1);
        }
        int pages = Math.max(1, (rows.size() + 3) / 4); page = Math.min(page, pages - 1);
        for (int i = page * 4; i < Math.min(rows.size(), (page + 1) * 4); i++) rows.get(i).run();
        addRenderableWidget(Button.builder(Component.literal("<"), b -> { page = Math.max(0, page - 1); rebuildWidgets(); }).bounds(x, y + 154, 30, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Назад  " + (page + 1) + "/" + pages), b -> onClose()).bounds(x + 34, y + 154, 192, 20).build());
        addRenderableWidget(Button.builder(Component.literal(">"), b -> { page = Math.min(pages - 1, page + 1); rebuildWidgets(); }).bounds(x + 230, y + 154, 30, 20).build());
    }
    private static boolean validBlocks(String value) {
        if (value.isBlank()) return false;
        for (String part : value.split(",", -1)) {
            Identifier id = Identifier.tryParse(part.trim());
            if (id == null || !BuiltInRegistries.BLOCK.containsKey(id)) return false;
        }
        return true;
    }
    private static void setBlocks(String value) { ModuleSettings.blocks = value; EspOverlay.resetBlocks(); }
    private String keyName() {
        int key = ModuleSettings.binds.getOrDefault(module.name(), -1);
        if (key < 0) return "нет";
        String name = GLFW.glfwGetKeyName(key, 0);
        return (name == null ? "KEY " + key : name.toUpperCase(Locale.ROOT)) + " (Del — убрать)";
    }
    @Override public boolean keyPressed(KeyEvent event) {
        if (!binding) return super.keyPressed(event);
        int key = event.key();
        if (key == GLFW.GLFW_KEY_ESCAPE) { binding = false; rebuildWidgets(); return true; }
        if (key == GLFW.GLFW_KEY_DELETE || key == GLFW.GLFW_KEY_BACKSPACE) ModuleSettings.binds.remove(module.name());
        else if (key >= 32 && key <= GLFW.GLFW_KEY_LAST && key != GLFW.GLFW_KEY_RIGHT_SHIFT && key != GLFW.GLFW_KEY_F7) {
            ModuleSettings.binds.put(module.name(), key);
        } else return true;
        binding = false; RvSteal.save(); rebuildWidgets(); return true;
    }
    @Override public void render(GuiGraphics g, int mx, int my, float delta) {
        g.fill(0, 0, width, height, 0xe811131c);
        g.drawString(font, title, x, y + 7, 0xffd9bdff);
        super.render(g, mx, my, delta);
        if (!error.isEmpty()) g.drawString(font, error, x, y + 182, 0xffff7777);
    }
    @Override public void onClose() { RvSteal.save(); minecraft.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
}
