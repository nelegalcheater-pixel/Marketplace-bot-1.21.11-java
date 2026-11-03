package ru.rvsteal;
import java.io.*;
import java.util.Base64;
import java.util.Properties;

final class ConfigCodec {
    private static final String PREFIX = "KILLERKC2:1:";
    static String encode(Properties settings) throws IOException {
        Properties copy = new Properties(); copy.putAll(settings); copy.remove("access.key");
        ByteArrayOutputStream output = new ByteArrayOutputStream(); copy.store(output, "KillerKC2");
        return PREFIX + Base64.getEncoder().encodeToString(output.toByteArray());
    }
    static Properties decode(String input) throws IOException {
        String value = input.trim();
        if (!value.startsWith(PREFIX) || value.length() > 65536) throw new IllegalArgumentException("Invalid config");
        byte[] bytes = Base64.getDecoder().decode(value.substring(PREFIX.length()));
        Properties p = new Properties(); p.load(new ByteArrayInputStream(bytes));
        p.remove("access.key");
        if (p.isEmpty()) throw new IllegalArgumentException("Empty config");
        return p;
    }
}
