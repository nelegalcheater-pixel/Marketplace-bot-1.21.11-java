package ru.marketbot.gui;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import ru.marketbot.BotLogic;
import ru.marketbot.Data;
import ru.marketbot.Settings;
import ru.marketbot.Store;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/** Главное меню: вкладки Главная / Магазины / Товары / Заказы / Балансы / Настройки. */
public class MainScreen extends BaseScreen {

    private enum Tab {
        HOME("Главная"), MARKETS("Магазины"), PRODUCTS("Товары"),
        ORDERS("Заказы"), BALANCES("Игроки"), DEPOSITS("Пополн."), SETTINGS("Настройки");

        final String title;

        Tab(String title) {
            this.title = title;
        }
    }

    private static Tab tab = Tab.HOME;
    private static int page = 0;
    private static String marketFilter = null;
    private static boolean showHistory = false;
    private static String balFilter = "";
    private static int settingsPage = 0;

    private static final int TOP = 62;
    private static final int ROW = 24;

    private int cw;
    private int x0;
    private int per;

    public MainScreen() {
        super("MarketBot", null);
    }

    // =====================================================================

    @Override
    protected void init() {
        cw = Math.min(this.width - 16, 480);
        x0 = (this.width - cw) / 2;
        per = Math.max(3, (this.height - TOP - 36) / ROW);

        Tab[] tabs = Tab.values();
        int tw = cw / tabs.length;
        for (int i = 0; i < tabs.length; i++) {
            final Tab t = tabs[i];
            Button b = btn(x0 + i * tw, 8, tw - 2, t.title, () -> {
                tab = t;
                page = 0;
                status = "";
                rebuildWidgets();
            });
            b.active = t != tab;
        }

        switch (tab) {
            case HOME -> homeTab();
            case MARKETS -> marketsTab();
            case PRODUCTS -> productsTab();
            case ORDERS -> ordersTab();
            case BALANCES -> balancesTab();
            case DEPOSITS -> depositsTab();
            case SETTINGS -> settingsTab();
        }

        if (!status.isEmpty()) label(x0, this.height - 11, cw, status);
    }

    /** Рисует кнопки «<» «>» и возвращает диапазон [от, до) элементов текущей страницы. */
    private int[] pager(int total) {
        int pages = Math.max(1, (total + per - 1) / per);
        if (page >= pages) page = pages - 1;
        if (page < 0) page = 0;
        int from = page * per;
        int to = Math.min(total, from + per);
        if (pages > 1) {
            int y = this.height - 32;
            btn(x0, y, 24, "<", () -> {
                page--;
                rebuildWidgets();
            }).active = page > 0;
            label(x0 + 30, y + 6, 60, (page + 1) + " / " + pages);
            btn(x0 + 70, y, 24, ">", () -> {
                page++;
                rebuildWidgets();
            }).active = page < pages - 1;
        }
        return new int[]{from, to};
    }

    private void open(net.minecraft.client.gui.screens.Screen s) {
        this.minecraft.setScreen(s);
    }

    private static Integer parseInt(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // =====================================================================
    //  Главная
    // =====================================================================

    private void homeTab() {
        Settings s = Store.settings;
        btn(x0, 36, 140, "Бот: " + (s.enabled ? "ВКЛ" : "ВЫКЛ"), () -> {
            s.enabled = !s.enabled;
            Store.save();
            rebuildWidgets();
        });
        btn(x0 + 148, 36, 160, "Перезагрузить файлы", () -> {
            Store.load();
            status = "Файлы перезагружены";
            rebuildWidgets();
        });

        long pending = Store.data.orders.stream().filter(o -> "PENDING".equals(o.status)).count();
        String[] lines = {
                "Магазинов: " + Store.data.markets.size() + "   Товаров: " + Store.data.products.size()
                        + "   Игроков с балансом: " + Store.data.balances.size(),
                "Заказов в ожидании: " + pending + "   Сообщений в очереди: " + BotLogic.queueSize()
                        + "   В чёрном списке: " + Store.data.banned.size(),
                "Игроки пишут в общий чат: " + s.prefixes.get(0) + "баланс, " + s.prefixes.get(0) + "магазин, "
                        + s.prefixes.get(0) + "купить <id>",
                "Бот отвечает ТОЛЬКО в ЛС. Меню: Right Shift или /mb"
        };
        int y = TOP + 4;
        for (String l : lines) {
            label(x0, y, cw, cut(l, 90));
            y += 14;
        }

        y += 8;
        label(x0, y, cw, "Чёрный список (игнорируются в чате и ЛС):");
        EditBox nick = field(x0, y + 14, 150, "", 16);
        btn(x0 + 156, y + 14, 70, "В бан", () -> {
            String n = nick.getValue().trim();
            if (!n.matches("\\w{1,16}")) {
                status = "Неверный ник";
            } else {
                Store.data.banned.add(Store.key(n));
                Store.save();
                status = n + " в чёрном списке";
            }
            rebuildWidgets();
        });
        btn(x0 + 232, y + 14, 80, "Разбан", () -> {
            String n = nick.getValue().trim();
            Store.data.banned.remove(Store.key(n));
            Store.save();
            status = n + " убран из чёрного списка";
            rebuildWidgets();
        });
    }

    // =====================================================================
    //  Магазины
    // =====================================================================

    private void marketsTab() {
        btn(x0, 36, 110, "+ Магазин", () -> open(new FormScreen("Новый магазин", this,
                List.of(new FormScreen.Field("Название", "", 32)), v -> {
            String n = v.get(0).trim();
            if (n.isEmpty()) return "Введите название";
            if (Store.market(n) != null) return "Такой магазин уже есть";
            Store.data.markets.add(new Data.Market(n));
            Store.save();
            return null;
        })));

        List<Data.Market> all = Store.data.markets;
        int[] r = pager(all.size());
        for (int i = r[0]; i < r[1]; i++) {
            Data.Market m = all.get(i);
            int y = TOP + (i - r[0]) * ROW;
            long cnt = Store.data.products.stream().filter(p -> p.market.equalsIgnoreCase(m.name)).count();
            label(x0, y + 6, cw - 220, cut(m.name + " (" + cnt + ")", 40));
            btn(x0 + cw - 216, y, 70, m.enabled ? "Вкл" : "Выкл", () -> {
                m.enabled = !m.enabled;
                Store.save();
                rebuildWidgets();
            });
            btn(x0 + cw - 142, y, 70, "Имя", () -> open(new FormScreen("Переименовать магазин", this,
                    List.of(new FormScreen.Field("Название", m.name, 32)), v -> {
                String n = v.get(0).trim();
                if (n.isEmpty()) return "Введите название";
                Data.Market other = Store.market(n);
                if (other != null && other != m) return "Такой магазин уже есть";
                for (Data.Product p : Store.data.products) {
                    if (p.market.equalsIgnoreCase(m.name)) p.market = n;
                }
                m.name = n;
                Store.save();
                return null;
            })));
            btn(x0 + cw - 68, y, 68, "Удал.", () -> {
                if (cnt > 0) status = "Сначала удалите товары из магазина";
                else if (Store.data.markets.size() <= 1) status = "Должен остаться хотя бы один магазин";
                else {
                    Store.data.markets.remove(m);
                    Store.save();
                    status = "";
                }
                rebuildWidgets();
            });
        }
    }

    // =====================================================================
    //  Товары
    // =====================================================================

    private void productsTab() {
        btn(x0, 36, 90, "+ Товар", () -> productForm(null));
        btn(x0 + 96, 36, 230, "Магазин: " + (marketFilter == null ? "Все" : cut(marketFilter, 24)), () -> {
            List<Data.Market> ms = Store.data.markets;
            if (marketFilter == null) {
                marketFilter = ms.get(0).name;
            } else {
                int idx = -1;
                for (int k = 0; k < ms.size(); k++) if (ms.get(k).name.equalsIgnoreCase(marketFilter)) idx = k;
                marketFilter = (idx >= 0 && idx + 1 < ms.size()) ? ms.get(idx + 1).name : null;
            }
            page = 0;
            rebuildWidgets();
        });

        List<Data.Product> all = Store.data.products.stream()
                .filter(p -> marketFilter == null || p.market.equalsIgnoreCase(marketFilter))
                .collect(Collectors.toList());
        int[] r = pager(all.size());
        for (int i = r[0]; i < r[1]; i++) {
            Data.Product p = all.get(i);
            int y = TOP + (i - r[0]) * ROW;
            String stock = p.stock < 0 ? "безл." : p.stock + " шт.";
            String text = "#" + p.id + " " + p.name + " — " + Store.money(p.price) + " (" + stock + ")"
                    + (marketFilter == null ? " · " + p.market : "");
            label(x0, y + 6, cw - 130, cut(text, 70));
            btn(x0 + cw - 124, y, 60, "Изм.", () -> productForm(p));
            btn(x0 + cw - 60, y, 60, "Удал.", () -> {
                Store.data.products.remove(p);
                Store.save();
                rebuildWidgets();
            });
        }
    }

    private void productForm(Data.Product edit) {
        String defMarket = edit != null ? edit.market
                : (marketFilter != null ? marketFilter : Store.data.markets.get(0).name);
        List<FormScreen.Field> f = List.of(
                new FormScreen.Field("Название", edit == null ? "" : edit.name, 40),
                new FormScreen.Field("Цена", edit == null ? "1" : Store.plain(edit.price), 12),
                new FormScreen.Field("Количество (-1 = без ограничений)", edit == null ? "-1" : String.valueOf(edit.stock), 7),
                new FormScreen.Field("Описание", edit == null ? "" : edit.desc, 120),
                new FormScreen.Field("Магазин (должен существовать)", defMarket, 32));
        open(new FormScreen(edit == null ? "Новый товар" : "Товар #" + edit.id, this, f, v -> {
            String name = v.get(0).trim();
            Double price = Store.num(v.get(1));
            Integer stock = parseInt(v.get(2));
            Data.Market m = Store.market(v.get(4));
            if (name.isEmpty()) return "Введите название";
            if (price == null || price < 0) return "Цена: неверное число";
            if (stock == null || stock < -1) return "Количество: целое число, -1 = безлимит";
            if (m == null) return "Такого магазина нет (см. вкладку «Магазины»)";
            Data.Product p = edit;
            if (p == null) {
                p = new Data.Product();
                p.id = Store.data.nextProductId++;
                Store.data.products.add(p);
            }
            p.name = name;
            p.price = Store.round(price);
            p.stock = stock;
            p.desc = v.get(3).trim();
            p.market = m.name;
            Store.save();
            return null;
        }));
    }

    // =====================================================================
    //  Заказы
    // =====================================================================

    private void ordersTab() {
        btn(x0, 36, 170, "История: " + (showHistory ? "показана" : "скрыта"), () -> {
            showHistory = !showHistory;
            page = 0;
            rebuildWidgets();
        });

        List<Data.Order> all = new ArrayList<>();
        for (int i = Store.data.orders.size() - 1; i >= 0; i--) {
            Data.Order o = Store.data.orders.get(i);
            if (showHistory || "PENDING".equals(o.status)) all.add(o);
        }
        int[] r = pager(all.size());
        for (int i = r[0]; i < r[1]; i++) {
            Data.Order o = all.get(i);
            int y = TOP + (i - r[0]) * ROW;
            boolean pend = "PENDING".equals(o.status);
            String text = "#" + o.id + " " + o.player + ": " + o.productName + " x" + o.qty + " = "
                    + Store.money(o.total) + (pend ? "" : " [" + Store.statusRu(o.status) + "]");
            label(x0, y + 6, cw - 140, cut(text, 64));
            if (pend) {
                btn(x0 + cw - 134, y, 66, "Выдано", () -> {
                    BotLogic.completeOrder(o.id);
                    rebuildWidgets();
                });
                btn(x0 + cw - 64, y, 64, "Отмена", () -> {
                    BotLogic.cancelOrder(o.id);
                    rebuildWidgets();
                });
            }
        }
        if (all.isEmpty()) label(x0, TOP + 6, cw, "Заказов нет.");
    }

    // =====================================================================
    //  Балансы
    // =====================================================================

    private void balancesTab() {
        EditBox search = field(x0, 36, 150, balFilter, 16);
        btn(x0 + 156, 36, 60, "Найти", () -> {
            balFilter = search.getValue().trim().toLowerCase(Locale.ROOT);
            page = 0;
            rebuildWidgets();
        });
        btn(x0 + 222, 36, 100, "+ Игрок", () -> open(new FormScreen("Задать баланс игроку", this, List.of(
                new FormScreen.Field("Ник", "", 16),
                new FormScreen.Field("Сумма", "0", 12)), v -> {
            String n = v.get(0).trim();
            Double a = Store.num(v.get(1));
            if (!n.matches("\\w{1,16}")) return "Неверный ник";
            if (a == null || a < 0) return "Неверная сумма";
            Store.setBalance(n, a);
            return null;
        })));

        // Список в TAB и уже известные боту игроки, без повторов по регистру.
        Map<String, String> players = new java.util.TreeMap<>();
        for (String nick : Store.data.balances.keySet()) players.put(Store.key(nick), nick);
        for (String nick : Store.data.banned) players.put(Store.key(nick), nick);
        for (Data.Order order : Store.data.orders) {
            if (order.player != null && !order.player.isBlank()) players.put(Store.key(order.player), order.player);
        }
        if (this.minecraft.getConnection() != null) {
            this.minecraft.getConnection().getOnlinePlayers().forEach(info -> {
                String nick = info.getProfile().name();
                players.put(Store.key(nick), nick);
            });
        }
        List<String> all = players.values().stream()
                .filter(nick -> Store.key(nick).contains(balFilter)).collect(Collectors.toList());
        int[] r = pager(all.size());
        for (int i = r[0]; i < r[1]; i++) {
            String nick = all.get(i);
            int y = TOP + (i - r[0]) * ROW;
            boolean banned = Store.data.banned.contains(Store.key(nick));
            label(x0, y + 6, cw - 158, cut(nick + " — " + Store.money(Store.balance(nick)), 34));
            btn(x0 + cw - 154, y, 62, banned ? "Разбан" : "Бан", () -> {
                if (banned) Store.data.banned.remove(Store.key(nick));
                else Store.data.banned.add(Store.key(nick));
                Store.save();
                rebuildWidgets();
            });
            btn(x0 + cw - 90, y, 28, "+", () -> amountForm(nick, 1));
            btn(x0 + cw - 60, y, 28, "-", () -> amountForm(nick, -1));
            btn(x0 + cw - 30, y, 30, "=", () -> amountForm(nick, 0));
        }
        if (all.isEmpty()) label(x0, TOP + 6, cw, "Игроки не найдены.");
    }

    private void depositsTab() {
        Settings s = Store.settings;
        int y = 38;
        label(x0, y, cw, "Игрок пишет $$$пополнить 100 и переводит вам 100.");
        label(x0, y + 14, cw, "Заявка действует 10 минут до отключения от сервера.");
        label(x0, y + 28, cw, "Шаблон сообщения сервера (группы player и amount):");
        EditBox pattern = field(x0, y + 42, cw, s.paymentRegex, 500);
        final boolean[] enabled = {s.paymentParser};
        Button[] toggle = new Button[1];
        toggle[0] = btn(x0, y + 68, (cw - 6) / 2, "Автопополнение: " + (enabled[0] ? "ВКЛ" : "ВЫКЛ"), () -> {
            enabled[0] = !enabled[0];
            toggle[0].setMessage(net.minecraft.network.chat.Component.literal("Автопополнение: " + (enabled[0] ? "ВКЛ" : "ВЫКЛ")));
        });
        btn(x0 + (cw - 6) / 2 + 6, y + 68, (cw - 6) / 2, "Сохранить", () -> {
            String regex = pattern.getValue().trim();
            String err = regex.isEmpty() ? "Укажите шаблон сообщения сервера" : checkRegex(List.of(regex), "player", "amount");
            if (err != null) status = err;
            else {
                s.paymentRegex = regex;
                s.paymentParser = enabled[0];
                Store.save();
                status = "Сохранено";
            }
            rebuildWidgets();
        });
        label(x0, y + 96, cw, "Настройте шаблон по точному сообщению сервера о переводе.");
    }

    private void amountForm(String nick, int mode) {
        String title = (mode > 0 ? "Пополнить: " : mode < 0 ? "Списать: " : "Установить баланс: ") + nick;
        open(new FormScreen(title, this, List.of(new FormScreen.Field("Сумма", "", 12)), v -> {
            Double a = Store.num(v.get(0));
            if (a == null || a < 0) return "Неверная сумма";
            if (mode > 0) Store.addBalance(nick, a);
            else if (mode < 0) Store.addBalance(nick, -a);
            else Store.setBalance(nick, a);
            return null;
        }));
    }

    // =====================================================================
    //  Настройки
    // =====================================================================

    private void settingsTab() {
        btn(x0, 36, 110, "Основные", () -> {
            settingsPage = 0;
            rebuildWidgets();
        }).active = settingsPage != 0;
        btn(x0 + 116, 36, 110, "Парсинг чата", () -> {
            settingsPage = 1;
            rebuildWidgets();
        }).active = settingsPage != 1;
        if (settingsPage == 0) settingsMain();
        else settingsParsing();
    }

    private void settingsMain() {
        Settings s = Store.settings;
        int colW = (cw - 10) / 2;
        int xl = x0;
        int xr = x0 + colW + 10;
        int y = 60;

        label(xl, y, colW, "Префикс(ы) команд, через запятую");
        EditBox fPrefix = field(xl, y + 11, colW, String.join(",", s.prefixes), 60);
        label(xr, y, colW, "Кулдаун на игрока, сек");
        EditBox fCd = field(xr, y + 11, colW, String.valueOf(s.cooldownSec), 4);
        y += 32;

        label(xl, y, colW, "Шаблон ЛС: {player} {text}");
        EditBox fDm = field(xl, y + 11, colW, s.dmTemplate, 100);
        label(xr, y, colW, "Пауза между ответами, тиков (20 = 1 с)");
        EditBox fDelay = field(xr, y + 11, colW, String.valueOf(s.sendDelayTicks), 5);
        y += 32;

        label(xl, y, colW, "Символ валюты");
        EditBox fCur = field(xl, y + 11, colW, s.currency, 6);
        label(xr, y, colW, "Макс. строк в одном ответе");
        EditBox fLines = field(xr, y + 11, colW, String.valueOf(s.maxLines), 3);
        y += 32;

        label(xl, y, colW, "Мин. сумма перевода");
        EditBox fMin = field(xl, y + 11, colW, Store.plain(s.minTransfer), 10);
        final boolean[] dbg = {s.debug};
        Button[] dbgBtn = new Button[1];
        dbgBtn[0] = btn(xr, y + 11, colW, "Отладка чата: " + (dbg[0] ? "ВКЛ" : "ВЫКЛ"), () -> {
            dbg[0] = !dbg[0];
            dbgBtn[0].setMessage(net.minecraft.network.chat.Component.literal("Отладка чата: " + (dbg[0] ? "ВКЛ" : "ВЫКЛ")));
        });
        y += 32;

        final boolean[] unk = {s.answerUnknownInDm};
        Button[] unkBtn = new Button[1];
        unkBtn[0] = btn(xl, y + 11, colW, "Неизвестное в ЛС: " + (unk[0] ? "отвечать" : "молчать"), () -> {
            unk[0] = !unk[0];
            unkBtn[0].setMessage(net.minecraft.network.chat.Component.literal("Неизвестное в ЛС: " + (unk[0] ? "отвечать" : "молчать")));
        });
        btn(xr, y + 11, colW, "Сохранить", () -> {
            Integer cd = parseInt(fCd.getValue());
            Integer delay = parseInt(fDelay.getValue());
            Integer lines = parseInt(fLines.getValue());
            Double min = Store.num(fMin.getValue());
            List<String> prefixes = new ArrayList<>();
            for (String p : fPrefix.getValue().split(",")) if (!p.trim().isEmpty()) prefixes.add(p.trim());
            if (prefixes.isEmpty()) status = "Нужен хотя бы один префикс";
            else if (cd == null || cd < 0) status = "Кулдаун: неверное число";
            else if (delay == null || delay < 5) status = "Пауза: минимум 5 тиков";
            else if (lines == null || lines < 1 || lines > 20) status = "Строк в ответе: от 1 до 20";
            else if (min == null || min < 0) status = "Мин. перевод: неверное число";
            else if (!fDm.getValue().contains("{player}") || !fDm.getValue().contains("{text}"))
                status = "В шаблоне ЛС нужны {player} и {text}";
            else {
                s.prefixes = prefixes;
                s.cooldownSec = cd;
                s.sendDelayTicks = delay;
                s.maxLines = lines;
                s.minTransfer = min;
                s.dmTemplate = fDm.getValue().trim();
                s.currency = fCur.getValue();
                s.debug = dbg[0];
                s.answerUnknownInDm = unk[0];
                Store.save();
                status = "Сохранено";
            }
            rebuildWidgets();
        });
    }

    private void settingsParsing() {
        Settings s = Store.settings;
        int y = 60;
        label(x0, y, cw, "Регулярки общего чата (группы player и message), разделитель |||");
        EditBox fPub = field(x0, y + 11, cw, String.join(" ||| ", s.publicRegex), 1500);
        y += 34;
        label(x0, y, cw, "Регулярки входящих ЛС (группы player и message), разделитель |||");
        EditBox fDm = field(x0, y + 11, cw, String.join(" ||| ", s.dmRegex), 1500);
        y += 34;
        label(x0, y, cw, "Регулярка пополнения (группы player и amount)");
        EditBox fPay = field(x0, y + 11, cw, s.paymentRegex, 500);
        y += 34;

        final boolean[] pay = {s.paymentParser};
        Button[] payBtn = new Button[1];
        int half = (cw - 6) / 2;
        payBtn[0] = btn(x0, y, half, "Авто-пополнение: " + (pay[0] ? "ВКЛ" : "ВЫКЛ"), () -> {
            pay[0] = !pay[0];
            payBtn[0].setMessage(net.minecraft.network.chat.Component.literal("Авто-пополнение: " + (pay[0] ? "ВКЛ" : "ВЫКЛ")));
        });
        btn(x0 + half + 6, y, half, "Сохранить", () -> {
            List<String> pub = splitRegex(fPub.getValue());
            List<String> dm = splitRegex(fDm.getValue());
            String err = checkRegex(pub, "player", "message");
            if (err == null) err = checkRegex(dm, "player", "message");
            if (err == null && !fPay.getValue().isBlank()) err = checkRegex(List.of(fPay.getValue()), "player", "amount");
            if (err != null) {
                status = err;
            } else {
                s.publicRegex = pub;
                s.dmRegex = dm;
                s.paymentRegex = fPay.getValue();
                s.paymentParser = pay[0];
                Store.save();
                status = "Сохранено";
            }
            rebuildWidgets();
        });
        label(x0, y + 26, cw, "Совет: включите «Отладка чата» на странице «Основные» и посмотрите,");
        label(x0, y + 38, cw, "в каком виде сервер присылает сообщения — под это подправьте регулярки.");
    }

    private static List<String> splitRegex(String raw) {
        List<String> out = new ArrayList<>();
        for (String p : raw.split("\\|\\|\\|")) if (!p.trim().isEmpty()) out.add(p.trim());
        return out;
    }

    private static String checkRegex(List<String> regexes, String... groups) {
        for (String r : regexes) {
            try {
                java.util.regex.Pattern p = java.util.regex.Pattern.compile(r);
                for (String g : groups) {
                    if (!r.contains("(?<" + g + ">")) return "В регулярке нет группы " + g + ": " + cut(r, 30);
                }
                p.matcher("");
            } catch (java.util.regex.PatternSyntaxException e) {
                return "Ошибка в регулярке: " + cut(r, 30);
            }
        }
        return null;
    }
}
