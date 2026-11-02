package ru.marketbot;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class PendingDepositsTest {
    @Test void acceptsOnlyPositiveAmountsAtCentPrecision() {
        for (String value : new String[]{"0", "-1", "NaN", "0.001", "1000000000001"})
            assertNull(PendingDeposits.amount(value), value);
        assertEquals(0, new BigDecimal("100.50").compareTo(PendingDeposits.amount("100,50")));
    }

    @Test void consumesOneMatchingRequestOnly() {
        PendingDeposits deposits = new PendingDeposits();
        deposits.request("Player", new BigDecimal("100"), 1000);
        assertFalse(deposits.consume("other", new BigDecimal("100"), 1100));
        assertFalse(deposits.consume("PLAYER", new BigDecimal("99"), 1100));
        assertTrue(deposits.consume("player", new BigDecimal("100.00"), 1100));
        assertFalse(deposits.consume("player", new BigDecimal("100"), 1100));
    }

    @Test void expiresAndClearsRequests() {
        PendingDeposits deposits = new PendingDeposits();
        deposits.request("Player", new BigDecimal("100"), 1000);
        assertFalse(deposits.consume("Player", new BigDecimal("100"), 601001));
        deposits.request("Player", new BigDecimal("100"), 1000);
        deposits.clear();
        assertFalse(deposits.consume("Player", new BigDecimal("100"), 1001));
    }
}
