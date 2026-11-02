package ru.marketbot.gui;

import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/** Универсальная форма: несколько полей + «Сохранить / Отмена». */
public class FormScreen extends BaseScreen {
    public record Field(String label, String value, int max) {}

    private final List<Field> fields;
    /** Возвращает текст ошибки или null/пустую строку, если всё хорошо. */
    private final Function<List<String>, String> onSubmit;
    private final List<EditBox> boxes = new ArrayList<>();
    private StringWidget errorLabel;

    public FormScreen(String title, Screen parent, List<Field> fields, Function<List<String>, String> onSubmit) {
        super(title, parent);
        this.fields = fields;
        this.onSubmit = onSubmit;
    }

    @Override
    protected void init() {
        boxes.clear();
        int w = Math.min(this.width - 40, 320);
        int x = (this.width - w) / 2;
        int total = 20 + fields.size() * 32 + 30;
        int y = Math.max(6, (this.height - total) / 2);

        label(x, y, w, this.title.getString());
        y += 18;
        for (Field f : fields) {
            label(x, y, w, f.label());
            boxes.add(field(x, y + 11, w, f.value(), f.max()));
            y += 32;
        }
        errorLabel = addRenderableWidget(new StringWidget(x, y, w, 12, Component.literal(""), this.font));
        y += 12;
        int half = (w - 6) / 2;
        btn(x, y, half, "Сохранить", this::submit);
        btn(x + half + 6, y, half, "Отмена", this::onClose);
    }

    private void submit() {
        List<String> values = new ArrayList<>();
        for (EditBox b : boxes) values.add(b.getValue());
        String err = onSubmit.apply(values);
        if (err == null || err.isEmpty()) {
            onClose();
        } else {
            errorLabel.setMessage(Component.literal(err));
        }
    }
}
