package ru.marketbot;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/** Загрузка/сохранение настроек и данных + мелкие помощники. */
public final class Store {
    private Store() {}

    public static Settings settings = new Settings();
    public static Data data = new Data();

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static Path dir() {
        return FabricLoader.getInstance().getConfigDir().resolve("marketbot");
    }

    public static void load() {
        try {
            Files.createDirectories(dir());
            Path sp = dir().resolve("settings.json");
            Path dp = dir().resolve("data.json");
            if (Files.exists(sp)) {
                Settings s = GSON.fromJson(Files.readString(sp), Settings.class);
                if (s != null) settings = s;
            }
            if (Files.exists(dp)) {
                Data d = GSON.fromJson(Files.readString(dp), Data.class);
                if (d != null) data = d;
            }
        } catch (Exception e) {
            MarketBot.LOG.error("MarketBot: не удалось загрузить файлы", e);
        }
        if (settings.prefixes == null || settings.prefixes.isEmpty()) settings.prefixes = new Settings().prefixes;
        if (settings.publicRegex == null) settings.publicRegex = new Settings().publicRegex;
        if (settings.dmRegex == null) settings.dmRegex = new Settings().dmRegex;
        if (data.markets == null || data.markets.isEmpty()) {
            data.markets = new java.util.ArrayList<>();
            data.markets.add(new Data.Market("Основной"));
        }
        if (data.products == null) data.products = new java.util.ArrayList<>();
        if (data.orders == null) data.orders = new java.util.ArrayList<>();
        if (data.balances == null) data.balances = new java.util.LinkedHashMap<>();
        if (data.banned == null) data.banned = new java.util.LinkedHashSet<>();
        for (Data.Product p : data.products) {
            if (p.market == null) p.market = data.markets.get(0).name;
            if (p.name == null) p.name = "?";
            if (p.desc == null) p.desc = "";
        }
        save();
    }

    public static void save() {
        try {
            Files.createDirectories(dir());
            Files.writeString(dir().resolve("settings.json"), GSON.toJson(settings));
            Files.writeString(dir().resolve("data.json"), GSON.toJson(data));
        } catch (Exception e) {
            MarketBot.LOG.error("MarketBot: не удалось сохранить файлы", e);
        }
    }

    // ---------- балансы ----------

    public static String key(String nick) {
        return nick.toLowerCase(Locale.ROOT);
    }

    public static double balance(String nick) {
        return data.balances.getOrDefault(key(nick), 0.0);
    }

    public static void setBalance(String nick, double value) {
        data.balances.put(key(nick), round(Math.max(0, value)));
        save();
    }

    public static void addBalance(String nick, double delta) {
        setBalance(nick, balance(nick) + delta);
    }

    public static double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    /** Число без валюты: 100 или 12.50 */
    public static String plain(double v) {
        if (v == Math.rint(v)) return String.valueOf((long) v);
        return String.format(Locale.ROOT, "%.2f", v);
    }

    /** Число с валютой: 100$ */
    public static String money(double v) {
        return plain(v) + settings.currency;
    }

    /** Разбор числа: принимает запятую и пробелы. null, если не число или не конечное. */
    public static Double num(String s) {
        if (s == null) return null;
        try {
            double d = Double.parseDouble(s.trim().replace(" ", "").replace(',', '.'));
            if (Double.isNaN(d) || Double.isInfinite(d)) return null;
            return d;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ---------- поиск ----------

    public static Data.Market market(String name) {
        if (name == null) return null;
        for (Data.Market m : data.markets) if (m.name.equalsIgnoreCase(name.trim())) return m;
        return null;
    }

    public static Data.Product product(int id) {
        for (Data.Product p : data.products) if (p.id == id) return p;
        return null;
    }

    public static Data.Order order(int id) {
        for (Data.Order o : data.orders) if (o.id == id) return o;
        return null;
    }

    public static String statusRu(String status) {
        return switch (status) {
            case "PENDING" -> "ожидает выдачи";
            case "DONE" -> "выдан";
            case "CANCELLED" -> "отменён";
            default -> status;
        };
    }
}
