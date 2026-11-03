package ru.rvsteal;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import java.util.Locale;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

final class RvScreen extends Screen {
    static record Module(String name, String detail, BooleanSupplier get, Consumer<Boolean> set) {}
    private int category;
    private int scroll;
    private String query = "";
    private EditBox search;

    RvScreen() { super(Component.literal("KillerKC2 client")); }

    private List<Module> modules() {
        return (query.isBlank() ? categoryModules(category) : allModules()).stream().filter(m -> m.name().toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT))).toList();
    }
    private static List<Module> base;
    static List<Module> baseModules() {
        if (base == null) {
            var list = new java.util.ArrayList<Module>();
            for (int i = 0; i < 6; i++) list.addAll(categoryModules(i));
            base = List.copyOf(list);
        }
        return base;
    }
    static List<Module> allModules() {
        var list = new java.util.ArrayList<Module>(baseModules());
        list.addAll(CustomFunctions.modules()); return list;
    }
    private static List<Module> categoryModules(int category) {
        return switch (category) {
            case 0 -> List.of(
                new Module("Hitboxes", "Увеличение области прицеливания игроков", () -> ModuleSettings.hitboxes, v -> ModuleSettings.hitboxes = v),
                new Module("Auto Totem", "Тотем из инвентаря во вторую руку", () -> ModuleSettings.autoTotem, v -> ModuleSettings.autoTotem = v),
                new Module("Player Aura", "Атака ближайшего игрока", () -> RvSteal.playerAura, v -> RvSteal.playerAura = v),
                new Module("Trigger PvP", "Атака игрока под прицелом", () -> RvSteal.attackPlayers, v -> RvSteal.attackPlayers = v));
            case 1 -> List.of(
                new Module("Mob Aura", "Атака ближайшего монстра", () -> RvSteal.mobAura, v -> RvSteal.mobAura = v),
                new Module("Trigger PvE", "Атака монстра под прицелом", () -> RvSteal.attackMobs, v -> RvSteal.attackMobs = v),
                new Module("Auto Tool", "Выбор инструмента из хотбара", () -> RvSteal.autoTool, v -> RvSteal.autoTool = v),
                new Module("Auto Respawn", "Возрождение после смерти", () -> RvSteal.autoRespawn, v -> RvSteal.autoRespawn = v));
            case 2 -> List.of(
                new Module("Auto Sprint", "Спринт при движении вперёд", () -> RvSteal.autoSprint, v -> RvSteal.autoSprint = v),
                new Module("Bunny Hop", "Прыжки при движении вперёд", () -> RvSteal.bunnyHop, v -> RvSteal.bunnyHop = v));
            case 3 -> List.of(
                new Module("Player ESP", "Подсветка загруженных игроков", () -> RvSteal.playerEsp, v -> RvSteal.playerEsp = v),
                new Module("Mob ESP", "Подсветка враждебных мобов", () -> RvSteal.mobEsp, v -> RvSteal.mobEsp = v),
                new Module("Item ESP", "Название и количество дропа", () -> ModuleSettings.itemEsp, v -> ModuleSettings.itemEsp = v),
                new Module("Block ESP", "Выбранные блоки сквозь стены", () -> ModuleSettings.blockEsp, v -> ModuleSettings.blockEsp = v),
                new Module("Throw Trails", "Следы жемчуга, зелий и других бросков", () -> ModuleSettings.pearlTrails, v -> ModuleSettings.pearlTrails = v));
            case 4 -> List.of(
                new Module("World Theme", "Цвета неба, облаков и тумана", () -> ModuleSettings.worldTheme, v -> ModuleSettings.worldTheme = v),
                new Module("Ambient Particles", "Частицы вокруг игрока", () -> ModuleSettings.ambientParticles, v -> ModuleSettings.ambientParticles = v),
                new Module("Night Vision", "Клиентское ночное зрение", () -> ModuleSettings.nightVision, v -> ModuleSettings.nightVision = v),
                new Module("Brightness", "Яркость игры на максимум", () -> RvSteal.bright, v -> RvSteal.bright = v),
                new Module("No Hurt Cam", "Отключает качание камеры при уроне", () -> ModuleSettings.noHurtCam, v -> ModuleSettings.noHurtCam = v),
                new Module("Item Physics", "Предметы лежат на земле", () -> ModuleSettings.itemPhysics, v -> ModuleSettings.itemPhysics = v));
            case 5 -> List.of(
                new Module("Watermark", "Имя клиента, FPS и ping", () -> ModuleSettings.watermark, v -> ModuleSettings.watermark = v),
                new Module("Hotkeys HUD", "Включённые модули с биндом", () -> ModuleSettings.hotkeysHud, v -> ModuleSettings.hotkeysHud = v),
                new Module("Potions HUD", "Активные эффекты и время", () -> ModuleSettings.potionsHud, v -> ModuleSettings.potionsHud = v),
                new Module("Staffs HUD", "Ранги staff/admin/mod из TAB", () -> ModuleSettings.staffHud, v -> ModuleSettings.staffHud = v),
                new Module("Target HUD", "Здоровье, броня и эффекты цели", () -> ModuleSettings.targetHud, v -> ModuleSettings.targetHud = v),
                new Module("Better Near", "Список игроков рядом", () -> RvSteal.betterNear, v -> RvSteal.betterNear = v),
                new Module("Armor HUD", "Броня и прочность", () -> RvSteal.armorHud, v -> RvSteal.armorHud = v),
                new Module("XYZ / FPS", "Координаты и частота кадров", () -> RvSteal.coordinatesHud, v -> RvSteal.coordinatesHud = v),
                new Module("Health HUD", "Здоровье и голод", () -> RvSteal.healthHud, v -> RvSteal.healthHud = v));
            case 6 -> CustomFunctions.modules();
            default -> List.of(new Module("Configs", "Профили и обмен", () -> false, v -> {}),
                new Module("HUD editor", "Расположение виджетов", () -> false, v -> {}),
                new Module("Function builder", "Создание функций", () -> false, v -> {}));
        };
    }
    private int panelWidth() { return Math.min(540, width - 8); }
    private int panelHeight() { return Math.min(340, height - 8); }
    private int left() { return (width - panelWidth()) / 2; }
    private int top() { return (height - panelHeight()) / 2; }
    private int sidebar() { return Math.min(108, panelWidth() / 3); }
    private int columns() { return panelWidth() >= 400 ? 2 : 1; }
    private int cardWidth() { return (panelWidth() - sidebar() - 24) / columns() - 4; }
    private int visibleRows() { return Math.max(1, (panelHeight() - 66) / 65); }
    private int maxScroll() { return Math.max(0, (modules().size() + columns() - 1) / columns() - visibleRows()); }
    @Override protected void init() {
        if (!AccessKey.unlocked()) { minecraft.setScreen(new ActivationScreen()); return; }
        search = new EditBox(font, left() + sidebar() + 12, top() + 10, panelWidth() - sidebar() - 24, 18, Component.literal("Search modules"));
        search.setBordered(false); search.setHint(Component.literal("Search...")); search.setMaxLength(64); search.setValue(query);
        search.setResponder(value -> { query = value; scroll = 0; });
        addRenderableWidget(search);
    }
    @Override public void render(GuiGraphics g, int mx, int my, float delta) {
        int x = left(), y = top(), w = panelWidth(), h = panelHeight(), side = sidebar();
        g.fill(0, 0, width, height, 0x40090b10);
        Ui.round(g, x - 2, y - 2, w + 4, h + 4, 11, 0x552b253f);
        Ui.round(g, x, y, w, h, 10, 0xf020262c);
        Ui.round(g, x, y, side, h, 10, 0xf014191e);
        g.drawString(font, "K", x + 16, y + 16, 0xffbcb3ff);
        g.drawString(font, "CLIENT", x + 31, y + 16, 0xffdadce5);
        String[] names = {"Combat", "PvE", "Movement", "ESP", "World", "HUD", "Custom", "Tools"};
        for (int i = 0; i < names.length; i++) {
            int ty = y + 47 + i * 18;
            if (category == i) Ui.round(g, x + 7, ty - 5, side - 14, 17, 5, 0xff0f1419);
            g.drawString(font, names[i], x + 15, ty + 1, category == i ? 0xfff1f1fa : 0xff929aa6);
        }
        g.drawString(font, "Hide client", x + 12, y + h - 27, 0xffffa5b1);
        g.drawString(font, "5x F7", x + 12, y + h - 14, 0xff8c83bd);
        Ui.round(g, x + side + 7, y + 6, w - side - 14, 25, 7, 0xff171d23);
        var list = modules();
        for (int row = 0; row < visibleRows(); row++) for (int col = 0; col < columns(); col++) {
            int index = (scroll + row) * columns() + col;
            if (index >= list.size()) continue;
            var m = list.get(index);
            int rx = x + side + 10 + col * (cardWidth() + 8), ry = y + 42 + row * 65, cw = cardWidth();
            Ui.round(g, rx, ry, cw, 58, 6, 0xff191f25);
            g.drawString(font, font.plainSubstrByWidth(m.name(), cw - 12), rx + 6, ry + 6, 0xffb9bdc8);
            boolean tool = category == 7 && query.isBlank();
            g.drawString(font, tool ? "Открыть" : "Active", rx + 6, ry + 24, 0xfff0f0f6);
            if (!tool) {
            boolean enabled = m.get().getAsBoolean();
            Ui.round(g, rx + cw - 30, ry + 22, 23, 12, 6, enabled ? 0xffa69bf1 : 0xff515862);
            Ui.round(g, rx + cw - (enabled ? 17 : 28), ry + 23, 10, 10, 5, 0xffececf8);
            }
            g.drawString(font, tool ? "ЛКМ: открыть" : "ПКМ: настройки", rx + 6, ry + 43, 0xff686f7e);
        }
        if (list.isEmpty()) g.drawString(font, "Ничего не найдено", x + side + 14, y + 48, 0xff989fac);
        if (maxScroll() > 0) g.drawString(font, (scroll + 1) + " / " + (maxScroll() + 1) + "  scroll", x + side + 14, y + h - 12, 0xff727b8a);
        super.render(g, mx, my, delta);
    }
    @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) return true;
        if (event.button() != 0 && event.button() != 1) return false;
        int x = left(), y = top();
        if (event.button() == 0 && event.x() >= x + 7 && event.x() < x + sidebar() - 7) {
            if (event.y() >= y + panelHeight() - 32 && event.y() < y + panelHeight() - 16) { RvSteal.hide(); return true; }
            for (int i = 0; i < 8; i++) if (event.y() >= y + 42 + i * 18 && event.y() < y + 59 + i * 18) {
                category = i; scroll = 0; search.setValue(""); return true;
            }
        }
        var list = modules();
        for (int row = 0; row < visibleRows(); row++) for (int col = 0; col < columns(); col++) {
            int index = (scroll + row) * columns() + col;
            if (index >= list.size()) continue;
            int rx = x + sidebar() + 10 + col * (cardWidth() + 8), ry = y + 42 + row * 65;
            if (event.x() < rx || event.x() >= rx + cardWidth() || event.y() < ry || event.y() >= ry + 58) continue;
            var module = list.get(index);
            if (module.name().equals("Configs")) { minecraft.setScreen(new ConfigShareScreen(this)); return true; }
            if (module.name().equals("HUD editor")) { minecraft.setScreen(new HudEditorScreen(this)); return true; }
            if (module.name().equals("Function builder")) { minecraft.setScreen(new FunctionBuilderScreen(this)); return true; }
            if (event.button() == 1) minecraft.setScreen(new SettingsScreen(this, module));
            else { module.set().accept(!module.get().getAsBoolean()); RvSteal.save(); }
            return true;
        }
        return false;
    }
    @Override public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        scroll = Math.clamp(scroll - (int)Math.signum(vertical), 0, maxScroll()); return true;
    }
    @Override public boolean isPauseScreen() { return false; }
}
