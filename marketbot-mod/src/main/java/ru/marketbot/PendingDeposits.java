package ru.marketbot;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Заявки действуют только в текущем подключении и не переживают перезапуск. */
public final class PendingDeposits {
    private static final long TIMEOUT_MS = 10 * 60 * 1000L;
    private final Map<String, Request> requests = new HashMap<>();
    private record Request(BigDecimal amount, long time) {}

    public static BigDecimal amount(String raw) {
        try {
            BigDecimal value = new BigDecimal(raw.replace(',', '.'));
            if (value.signum() <= 0 || value.scale() > 2 || value.compareTo(new BigDecimal("1000000000000")) > 0) return null;
            return value;
        } catch (NumberFormatException | NullPointerException e) {
            return null;
        }
    }
    public void request(String player, BigDecimal amount, long now) {
        requests.put(player.toLowerCase(Locale.ROOT), new Request(amount, now));
    }
    public boolean consume(String player, BigDecimal amount, long now) {
        String key = player.toLowerCase(Locale.ROOT);
        Request request = requests.get(key);
        if (request == null) return false;
        if (now < request.time || now - request.time > TIMEOUT_MS) {
            requests.remove(key);
            return false;
        }
        if (request.amount.compareTo(amount) != 0) return false;
        requests.remove(key);
        return true;
    }
    public void clear() { requests.clear(); }
}
