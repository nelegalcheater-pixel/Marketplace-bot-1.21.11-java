package ru.rvsteal;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

final class RvScreen extends Screen {
    RvScreen() { super(Component.literal("KillerKC2 client")); }

    @Override protected void init() {
        int x = (width - 200) / 2;
        int y = Math.max(24, height / 2 - 100);
        addRenderableWidget(new StringWidget(x, y - 18, 200, 12, title, font));
        toggle(x, y, "Better Near", () -> RvSteal.betterNear, value -> RvSteal.betterNear = value);
        toggle(x, y + 24, "Armor HUD", () -> RvSteal.armorHud, value -> RvSteal.armorHud = value);
        toggle(x, y + 48, "XYZ / FPS", () -> RvSteal.coordinatesHud, value -> RvSteal.coordinatesHud = value);
        toggle(x, y + 72, "PvP: атака под прицелом", () -> RvSteal.attackPlayers, value -> RvSteal.attackPlayers = value);
        toggle(x, y + 96, "PvE: атака монстров", () -> RvSteal.attackMobs, value -> RvSteal.attackMobs = value);
        toggle(x, y + 120, "Автоспринт", () -> RvSteal.autoSprint, value -> RvSteal.autoSprint = value);
        toggle(x, y + 144, "Автовозрождение", () -> RvSteal.autoRespawn, value -> RvSteal.autoRespawn = value);
        addRenderableWidget(Button.builder(Component.literal("Готово"), button -> onClose())
                .bounds(x, y + 172, 200, 20).build());
    }
    private void toggle(int x, int y, String name, BooleanSupplier get, Consumer<Boolean> set) {
        addRenderableWidget(Button.builder(label(name, get.getAsBoolean()), button -> {
            set.accept(!get.getAsBoolean());
            RvSteal.save();
            button.setMessage(label(name, get.getAsBoolean()));
        }).bounds(x, y, 200, 20).build());
    }
    private static Component label(String name, boolean enabled) {
        return Component.literal(name + ": " + (enabled ? "Вкл" : "Выкл"));
    }
    @Override public boolean isPauseScreen() { return false; }
}
