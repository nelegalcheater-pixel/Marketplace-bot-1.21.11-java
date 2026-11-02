package ru.marketbot;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

/**
 * Мозг бота: разбирает сообщения чата, выполняет команды игроков и
 * отправляет ответы ТОЛЬКО в личные сообщения (через очередь с задержкой).
 */
public final class BotLogic {
    private BotLogic() {}

    private static final ArrayDeque<String[]> QUEUE = new ArrayDeque<>();
    private static final Map<String, Long> LAST = new HashMap<>();
    private static final Map<String, Pattern> CACHE = new HashMap<>();
    private static int sinceSend = 1000;
    private static final PendingDeposits DEPOSITS = new PendingDeposits();
    private static Object connection;

    private static final Set<String> DEPOSIT = Set.of("пополнить", "deposit");
    private static final Set<String> BAL = Set.of("баланс", "б", "balance", "bal", "b");
    private static final Set<String> HELP = Set.of("помощь", "команды", "help", "?", "commands");
    private static final Set<String> SHOP = Set.of("магазин", "магазины", "товары", "каталог", "список", "shop", "shops", "market", "list");
    private static final Set<String> INFO = Set.of("инфо", "товар", "info", "item");
    private static final Set<String> BUY = Set.of("купить", "заказать", "buy");
    private static final Set<String> PAY = Set.of("перевод", "перевести", "pay", "send");
    private static final Set<String> TOP = Set.of("топ", "top");
    private static final Set<String> ORD = Set.of("заказы", "мои", "orders");

    public static int queueSize() {
        return QUEUE.size();
    }

    // =====================================================================
    //  Приём сообщений
    // =====================================================================

    public static void onMessage(String text, boolean systemMessage) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || text == null || text.isBlank()) return;
        Settings s = Store.settings;

        if (s.debug) mc.player.displayClientMessage(Component.literal("[MB debug] " + text), false);
        if (!s.enabled) return;

        String me = mc.player.getName().getString();

        String[] dm = parse(text, s.dmRegex);
        boolean isDm = dm != null;
        String[] p = isDm ? dm : parse(text, s.publicRegex);
        if (p == null) {
            if (systemMessage && s.paymentParser) tryPayment(text);
            return;
        }

        String player = p[0];
        String msg = p[1].trim();
        if (player.equalsIgnoreCase(me)) return;
        if (Store.data.banned.contains(Store.key(player))) return;

        String cmdLine = stripPrefix(msg, s.prefixes);
        if (cmdLine == null) {
            if (!isDm) return; // в общем чате отвечаем только на сообщения с префиксом
            cmdLine = msg;
        }
        cmdLine = cmdLine.trim();
        if (cmdLine.isEmpty()) return;

        long now = System.currentTimeMillis();
        Long last = LAST.get(Store.key(player));
        if (last != null && now - last < s.cooldownSec * 1000L) return;
        LAST.put(Store.key(player), now);

        handle(player, cmdLine, isDm);
    }

    private static String stripPrefix(String msg, List<String> prefixes) {
        for (String pr : prefixes) {
            if (!pr.isEmpty() && msg.regionMatches(true, 0, pr, 0, pr.length())) {
                return msg.substring(pr.length());
            }
        }
        return null;
    }

    private static Pattern pattern(String regex) {
        if (regex == null || regex.isBlank()) return null;
        if (CACHE.containsKey(regex)) return CACHE.get(regex);
        Pattern p = null;
        try {
            p = Pattern.compile(regex);
        } catch (PatternSyntaxException ignored) {
        }
        CACHE.put(regex, p);
        return p;
    }

    private static String[] parse(String text, List<String> regexes) {
        for (String r : regexes) {
            Pattern p = pattern(r);
            if (p == null) continue;
            Matcher m = p.matcher(text);
            if (m.matches()) {
                try {
                    return new String[]{m.group("player"), m.group("message")};
                } catch (IllegalArgumentException ignored) {
                    // в регулярке нет нужных групп
                }
            }
        }
        return null;
    }

    private static boolean tryPayment(String text) {
        Pattern p = pattern(Store.settings.paymentRegex);
        if (p == null) return false;
        Matcher m = p.matcher(text);
        if (!m.matches()) return false;
        try {
            String player = m.group("player");
            BigDecimal amount = PendingDeposits.amount(m.group("amount"));
            if (player == null || !player.matches("[A-Za-z0-9_]{1,16}") || amount == null
                    || Store.data.banned.contains(Store.key(player))
                    || !DEPOSITS.consume(player, amount, System.currentTimeMillis())) return false;
            Store.addBalance(player, amount.doubleValue());
            reply(player, "Спасибо за пополнение! +" + Store.money(amount.doubleValue())
                    + ". Ваш баланс: " + Store.money(Store.balance(player)));
            notifyOwner("Пополнение от " + player + ": +" + Store.money(amount.doubleValue()));
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static void deposit(String player, String[] args) {
        if (!Store.settings.paymentParser) {
            reply(player, "Автопополнение сейчас выключено.");
            return;
        }
        BigDecimal amount = args.length == 1 ? PendingDeposits.amount(args[0]) : null;
        if (amount == null) {
            reply(player, "Укажите сумму: пополнить 100");
            return;
        }
        DEPOSITS.request(player, amount, System.currentTimeMillis());
        notifyOwner("Заявка на пополнение от " + player + ": " + Store.money(amount.doubleValue()));
    }

    // =====================================================================
    //  Команды игроков
    // =====================================================================

    private static String t(boolean en, String ru, String eng) {
        return en ? eng : ru;
    }

    private static boolean isEn(String cmd) {
        return cmd.chars().allMatch(c -> c < 128);
    }

    private static void handle(String player, String cmdLine, boolean isDm) {
        String[] parts = cmdLine.split("\\s+");
        String cmd = parts[0].toLowerCase(Locale.ROOT);
        String[] args = Arrays.copyOfRange(parts, 1, parts.length);
        boolean en = isEn(cmd);

        if (DEPOSIT.contains(cmd)) {
            deposit(player, args);
        } else if (BAL.contains(cmd)) {
            reply(player, t(en, "Ваш баланс: ", "Your balance: ") + Store.money(Store.balance(player)));
        } else if (HELP.contains(cmd)) {
            help(player, en);
        } else if (SHOP.contains(cmd)) {
            shop(player, args, en);
        } else if (INFO.contains(cmd)) {
            info(player, args, en);
        } else if (BUY.contains(cmd)) {
            buy(player, args, en);
        } else if (PAY.contains(cmd)) {
            pay(player, args, en);
        } else if (TOP.contains(cmd)) {
            top(player, en);
        } else if (ORD.contains(cmd)) {
            myOrders(player, en);
        } else if (isDm && Store.settings.answerUnknownInDm) {
            reply(player, t(en, "Неизвестная команда. Напишите: помощь", "Unknown command. Type: help"));
        }
    }

    private static void help(String player, boolean en) {
        String pr = Store.settings.prefixes.get(0);
        if (en) {
            reply(player, List.of(
                    "Commands: balance (b) | shop [name] | info <id> | buy <id> [qty] | pay <nick> <sum> | orders | top",
                    "In public chat start with " + pr + " ; in DM no prefix needed."));
        } else {
            reply(player, List.of(
                    "Команды: баланс (б) | магазин [название] | инфо <id> | купить <id> [кол-во] | пополнить <сумма> | перевод <ник> <сумма> | заказы | топ",
                    "В общем чате пишите с " + pr + " , в ЛС префикс не нужен."));
        }
    }

    private static void shop(String player, String[] args, boolean en) {
        List<Data.Market> ms = Store.data.markets.stream().filter(m -> m.enabled).collect(Collectors.toList());
        if (ms.isEmpty()) {
            reply(player, t(en, "Магазинов пока нет.", "No shops yet."));
            return;
        }
        Data.Market chosen = null;
        if (args.length == 0) {
            if (ms.size() == 1) {
                chosen = ms.get(0);
            } else {
                List<String> items = new ArrayList<>();
                for (Data.Market m : ms) items.add(m.name + " (" + countProducts(m) + ")");
                List<String> lines = pack(t(en, "Магазины: ", "Shops: "), items, 180);
                lines.add(t(en, "Откройте: магазин <название>", "Open: shop <name>"));
                reply(player, lines);
                return;
            }
        } else {
            String q = String.join(" ", args);
            for (Data.Market m : ms) if (m.name.equalsIgnoreCase(q)) chosen = m;
            if (chosen == null) {
                for (Data.Market m : ms) if (m.name.toLowerCase(Locale.ROOT).startsWith(q.toLowerCase(Locale.ROOT))) { chosen = m; break; }
            }
            if (chosen == null) {
                reply(player, t(en, "Магазин не найден. Список: магазин", "Shop not found. List: shop"));
                return;
            }
        }
        List<String> items = new ArrayList<>();
        for (Data.Product p : Store.data.products) {
            if (!p.market.equalsIgnoreCase(chosen.name)) continue;
            if (p.stock == 0) continue;
            String stock = p.stock < 0 ? "" : " (" + p.stock + t(en, " шт.", " pcs") + ")";
            items.add("#" + p.id + " " + p.name + " — " + Store.money(p.price) + stock);
        }
        if (items.isEmpty()) {
            reply(player, t(en, "В магазине «" + chosen.name + "» пусто.", "Shop '" + chosen.name + "' is empty."));
            return;
        }
        List<String> lines = pack(chosen.name + ": ", items, 180);
        lines.add(t(en, "Купить: купить <id> [кол-во]", "Buy: buy <id> [qty]"));
        reply(player, lines);
    }

    private static long countProducts(Data.Market m) {
        return Store.data.products.stream().filter(p -> p.market.equalsIgnoreCase(m.name) && p.stock != 0).count();
    }

    private static void info(String player, String[] args, boolean en) {
        Integer id = args.length > 0 ? parseInt(args[0]) : null;
        Data.Product p = id == null ? null : Store.product(id);
        if (p == null) {
            reply(player, t(en, "Использование: инфо <id>", "Usage: info <id>"));
            return;
        }
        String stock = p.stock < 0 ? t(en, "без ограничений", "unlimited") : String.valueOf(p.stock);
        String desc = p.desc.isBlank() ? "" : " | " + p.desc;
        reply(player, "#" + p.id + " " + p.name + " — " + Store.money(p.price) + " | "
                + t(en, "магазин: ", "shop: ") + p.market + " | "
                + t(en, "в наличии: ", "stock: ") + stock + desc);
    }

    private static void buy(String player, String[] args, boolean en) {
        Integer id = args.length > 0 ? parseInt(args[0]) : null;
        if (id == null) {
            reply(player, t(en, "Использование: купить <id> [кол-во]", "Usage: buy <id> [qty]"));
            return;
        }
        int qty = 1;
        if (args.length > 1) {
            Integer q = parseInt(args[1]);
            if (q == null || q < 1 || q > 10000) {
                reply(player, t(en, "Неверное количество.", "Invalid quantity."));
                return;
            }
            qty = q;
        }
        Data.Product p = Store.product(id);
        Data.Market m = p == null ? null : Store.market(p.market);
        if (p == null || m == null || !m.enabled) {
            reply(player, t(en, "Товар не найден.", "Item not found."));
            return;
        }
        if (p.stock >= 0 && qty > p.stock) {
            reply(player, t(en, "Недостаточно на складе. Осталось: " + p.stock, "Not enough stock. Left: " + p.stock));
            return;
        }
        double total = Store.round(p.price * qty);
        double bal = Store.balance(player);
        if (bal + 0.0001 < total) {
            reply(player, t(en,
                    "Не хватает средств. Нужно " + Store.money(total) + ", у вас " + Store.money(bal),
                    "Not enough funds. Need " + Store.money(total) + ", you have " + Store.money(bal)));
            return;
        }
        Store.addBalance(player, -total);
        if (p.stock >= 0) p.stock -= qty;
        Data.Order o = new Data.Order();
        o.id = Store.data.nextOrderId++;
        o.player = player;
        o.productId = p.id;
        o.productName = p.name;
        o.qty = qty;
        o.total = total;
        o.time = System.currentTimeMillis();
        Store.data.orders.add(o);
        Store.save();

        reply(player, t(en,
                "Заказ #" + o.id + ": " + p.name + " x" + qty + " за " + Store.money(total) + ". Продавец скоро выдаст товар. Баланс: " + Store.money(Store.balance(player)),
                "Order #" + o.id + ": " + p.name + " x" + qty + " for " + Store.money(total) + ". The seller will deliver soon. Balance: " + Store.money(Store.balance(player))));
        notifyOwner("Новый заказ #" + o.id + ": " + player + " — " + p.name + " x" + qty + " = " + Store.money(total));
    }

    private static void pay(String player, String[] args, boolean en) {
        if (args.length < 2) {
            reply(player, t(en, "Использование: перевод <ник> <сумма>", "Usage: pay <nick> <sum>"));
            return;
        }
        String to = args[0];
        Double amt = Store.num(args[1]);
        if (!to.matches("\\w{3,16}") || amt == null) {
            reply(player, t(en, "Неверный ник или сумма.", "Invalid nick or amount."));
            return;
        }
        amt = Store.round(amt);
        if (to.equalsIgnoreCase(player)) {
            reply(player, t(en, "Нельзя переводить самому себе.", "You can't pay yourself."));
            return;
        }
        if (amt < Store.settings.minTransfer) {
            reply(player, t(en, "Минимальный перевод: " + Store.money(Store.settings.minTransfer),
                    "Minimum transfer: " + Store.money(Store.settings.minTransfer)));
            return;
        }
        if (Store.balance(player) + 0.0001 < amt) {
            reply(player, t(en, "Не хватает средств.", "Not enough funds."));
            return;
        }
        Store.addBalance(player, -amt);
        Store.addBalance(to, amt);
        reply(player, t(en, "Переведено " + Store.money(amt) + " игроку " + to + ". Баланс: " + Store.money(Store.balance(player)),
                "Sent " + Store.money(amt) + " to " + to + ". Balance: " + Store.money(Store.balance(player))));
        reply(to, "Вам перевод " + Store.money(amt) + " от " + player + ". Баланс: " + Store.money(Store.balance(to)));
    }

    private static void top(String player, boolean en) {
        List<Map.Entry<String, Double>> list = new ArrayList<>(Store.data.balances.entrySet());
        list.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        List<String> items = new ArrayList<>();
        for (int i = 0; i < Math.min(5, list.size()); i++) {
            Map.Entry<String, Double> e = list.get(i);
            if (e.getValue() <= 0) break;
            items.add((i + 1) + ". " + e.getKey() + " " + Store.money(e.getValue()));
        }
        if (items.isEmpty()) {
            reply(player, t(en, "Пока никого в топе.", "Nobody in the top yet."));
            return;
        }
        reply(player, pack(t(en, "Топ балансов: ", "Top balances: "), items, 180));
    }

    private static void myOrders(String player, boolean en) {
        List<String> items = new ArrayList<>();
        List<Data.Order> mine = Store.data.orders.stream()
                .filter(o -> o.player.equalsIgnoreCase(player)).collect(Collectors.toList());
        for (int i = Math.max(0, mine.size() - 5); i < mine.size(); i++) {
            Data.Order o = mine.get(i);
            items.add("#" + o.id + " " + o.productName + " x" + o.qty + " — " + Store.statusRu(o.status));
        }
        if (items.isEmpty()) {
            reply(player, t(en, "У вас пока нет заказов.", "You have no orders yet."));
            return;
        }
        reply(player, pack(t(en, "Ваши заказы: ", "Your orders: "), items, 180));
    }

    // =====================================================================
    //  Заказы (из меню / команд владельца)
    // =====================================================================

    public static boolean completeOrder(int id) {
        Data.Order o = Store.order(id);
        if (o == null || !"PENDING".equals(o.status)) return false;
        o.status = "DONE";
        Store.save();
        reply(o.player, "Ваш заказ #" + id + " (" + o.productName + " x" + o.qty + ") выдан. Спасибо за покупку!");
        return true;
    }

    public static boolean cancelOrder(int id) {
        Data.Order o = Store.order(id);
        if (o == null || !"PENDING".equals(o.status)) return false;
        o.status = "CANCELLED";
        Store.addBalance(o.player, o.total);
        Data.Product p = Store.product(o.productId);
        if (p != null && p.stock >= 0) p.stock += o.qty;
        Store.save();
        reply(o.player, "Заказ #" + id + " отменён, " + Store.money(o.total) + " возвращено на баланс.");
        return true;
    }

    // =====================================================================
    //  Отправка
    // =====================================================================

    private static void notifyOwner(String text) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.displayClientMessage(Component.literal("[MarketBot] " + text), false);
        }
    }

    public static void reply(String player, String line) {
        if (QUEUE.size() < 200) QUEUE.add(new String[]{player, line});
    }

    private static void reply(String player, List<String> lines) {
        int n = 0;
        for (String l : lines) {
            if (n++ >= Store.settings.maxLines) break;
            reply(player, l);
        }
    }

    public static void tick(Minecraft mc) {
        Object currentConnection = mc.player == null ? null : mc.player.connection;
        if (currentConnection != connection) {
            connection = currentConnection;
            DEPOSITS.clear();
            QUEUE.clear();
            LAST.clear();
        }
        if (mc.player == null || mc.player.connection == null) {
            QUEUE.clear();
            return;
        }
        if (sinceSend < 1000) sinceSend++;
        if (QUEUE.isEmpty() || sinceSend < Store.settings.sendDelayTicks) return;
        String[] e = QUEUE.poll();
        sendDm(mc, e[0], e[1]);
        sinceSend = 0;
    }

    private static void sendDm(Minecraft mc, String player, String text) {
        String tpl = Store.settings.dmTemplate;
        text = text.replaceAll("[\\p{Cntrl}§]", " ");
        String base = tpl.replace("{player}", player).replace("{text}", "");
        int allowed = Math.max(10, 250 - base.length());
        if (text.length() > allowed) text = text.substring(0, allowed);
        String full = tpl.replace("{player}", player).replace("{text}", text);
        if (full.startsWith("/")) {
            mc.player.connection.sendCommand(full.substring(1));
        } else {
            mc.player.connection.sendChat(full);
        }
    }

    // =====================================================================
    //  Помощники
    // =====================================================================

    private static Integer parseInt(String s) {
        try {
            return Integer.parseInt(s.trim().replace("#", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Склеивает элементы через " | " в строки не длиннее max символов. */
    private static List<String> pack(String head, List<String> items, int max) {
        List<String> out = new ArrayList<>();
        String cur = head;
        boolean empty = true;
        for (String it : items) {
            String add = empty ? it : " | " + it;
            if (!empty && cur.length() + add.length() > max) {
                out.add(cur);
                cur = it;
            } else {
                cur += add;
            }
            empty = false;
        }
        out.add(cur);
        return out;
    }
}
