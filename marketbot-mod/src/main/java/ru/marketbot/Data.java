package ru.marketbot;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Данные бота (config/marketbot/data.json). */
public class Data {
    /** Внутренние балансы игроков. Ключ — ник в нижнем регистре. */
    public Map<String, Double> balances = new LinkedHashMap<>();
    public List<Market> markets = new ArrayList<>();
    public List<Product> products = new ArrayList<>();
    public List<Order> orders = new ArrayList<>();
    public Set<String> banned = new LinkedHashSet<>();
    public int nextProductId = 1;
    public int nextOrderId = 1;

    public static class Market {
        public String name;
        public boolean enabled = true;

        public Market() {}

        public Market(String name) {
            this.name = name;
        }
    }

    public static class Product {
        public int id;
        public String market = "";
        public String name = "";
        public double price;
        /** -1 = без ограничений. */
        public int stock = -1;
        public String desc = "";
    }

    public static class Order {
        public int id;
        public String player = "";
        public int productId;
        public String productName = "";
        public int qty;
        public double total;
        public long time;
        /** PENDING, DONE, CANCELLED */
        public String status = "PENDING";
    }
}
