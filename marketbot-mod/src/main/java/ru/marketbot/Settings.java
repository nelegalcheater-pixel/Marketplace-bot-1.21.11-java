package ru.marketbot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Настройки бота (config/marketbot/settings.json). */
public class Settings {
    public boolean enabled = true;
    public String currency = "$";

    /** Префиксы команд в общем чате. В ЛС префикс не нужен. */
    public List<String> prefixes = new ArrayList<>(List.of("$$$"));

    /** Как бот отвечает в ЛС. {player} и {text} подставляются автоматически. */
    public String dmTemplate = "/msg {player} {text}";

    public int cooldownSec = 3;
    public int sendDelayTicks = 30;
    public int maxLines = 6;
    public double minTransfer = 1;
    public boolean answerUnknownInDm = true;

    /** Показывать в вашем чате сырые сообщения сервера (чтобы подобрать регулярки). */
    public boolean debug = false;

    /** Авто-пополнение баланса по сообщению сервера о переводе. Нужна своя регулярка. */
    public boolean paymentParser = false;
    public String paymentRegex =
            "^(?<player>\\w{3,16}) (?:перевел|перевёл|sent you|paid you) (?:вам )?\\$?(?<amount>[\\d.,]+)";

    /** Регулярки общего чата. Обязательные группы: player и message. */
    public List<String> publicRegex = new ArrayList<>(Arrays.asList(
            "^<(?<player>\\w{3,16})>\\s*(?<message>.+)$",
            "^(?:\\[[^\\]]+\\]\\s*)*(?<player>\\w{3,16})\\s*[»:›>]\\s*(?<message>.+)$"
    ));

    /** Регулярки входящих ЛС. Обязательные группы: player и message. */
    public List<String> dmRegex = new ArrayList<>(Arrays.asList(
            "^\\[?(?<player>\\w{3,16})\\s*(?:->|→|»|>)\\s*(?:me|я|вам|Вы|You|you)\\s*\\]?\\s*[:»>-]?\\s*(?<message>.+)$",
            "^(?<player>\\w{3,16}) whispers to you:\\s*(?<message>.+)$",
            "^From (?<player>\\w{3,16}):\\s*(?<message>.+)$"
    ));
}
