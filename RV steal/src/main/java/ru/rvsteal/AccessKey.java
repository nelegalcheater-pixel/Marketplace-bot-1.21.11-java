package ru.rvsteal;

import java.util.Properties;

/** Offline activation only; this is not a tamper-resistant licensing service. */
final class AccessKey {
    private static String acceptedKey = "";
    static boolean accepts(String key) { return "KillerKC2".equals(key); }
    static boolean unlocked() { return accepts(acceptedKey); }
    static boolean activate(String key) {
        if (!accepts(key)) return false;
        acceptedKey = key; return true;
    }
    static void load(Properties properties) {
        String key = properties.getProperty("access.key", "");
        acceptedKey = accepts(key) ? key : "";
    }
    static void store(Properties properties) { properties.setProperty("access.key", acceptedKey); }
}
