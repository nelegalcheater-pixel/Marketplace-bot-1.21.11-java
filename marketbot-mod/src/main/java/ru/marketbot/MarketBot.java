package ru.marketbot;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.marketbot.gui.MainScreen;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class MarketBot implements ClientModInitializer {
    public static final String MOD_ID = "marketbot";
    public static final Logger LOG = LoggerFactory.getLogger(MOD_ID);

    private static KeyMapping openKey;
    private static boolean openMenu = false;

    @Override
    public void onInitializeClient() {
        Store.load();

        // --- клавиша меню (по умолчанию Right Shift, меняется в настройках управления)
        KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));
        openKey = KeyBindingHelper.registerKeyBinding(
                new KeyMapping("key.marketbot.open", InputConstants.Type.KEYSYM, InputConstants.KEY_RSHIFT, category));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openKey.consumeClick()) {
                if (client.screen == null) client.setScreen(new MainScreen());
            }
            if (openMenu) {
                openMenu = false;
                client.setScreen(new MainScreen());
            }
            BotLogic.tick(client);
        });

        // --- чат: системные сообщения (большинство плагинов) и подписанный чат
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!overlay) BotLogic.onMessage(message.getString(), true);
        });
        ClientReceiveMessageEvents.CHAT.register((message, signed, sender, params, time) ->
                BotLogic.onMessage(message.getString(), false));

        // --- клиентские команды /mb
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
                literal("mb")
                        .executes(c -> {
                            openMenu = true;
                            return 1;
                        })
                        .then(literal("on").executes(c -> {
                            Store.settings.enabled = true;
                            Store.save();
                            fb(c, "Бот включён");
                            return 1;
                        }))
                        .then(literal("off").executes(c -> {
                            Store.settings.enabled = false;
                            Store.save();
                            fb(c, "Бот выключен");
                            return 1;
                        }))
                        .then(literal("reload").executes(c -> {
                            Store.load();
                            fb(c, "Файлы перезагружены");
                            return 1;
                        }))
                        .then(literal("bal").then(argument("nick", StringArgumentType.word()).executes(c -> {
                            String nick = StringArgumentType.getString(c, "nick");
                            fb(c, nick + ": " + Store.money(Store.balance(nick)));
                            return 1;
                        })))
                        .then(literal("add").then(argument("nick", StringArgumentType.word())
                                .then(argument("amount", DoubleArgumentType.doubleArg(0)).executes(c -> bal(c, 1)))))
                        .then(literal("take").then(argument("nick", StringArgumentType.word())
                                .then(argument("amount", DoubleArgumentType.doubleArg(0)).executes(c -> bal(c, -1)))))
                        .then(literal("set").then(argument("nick", StringArgumentType.word())
                                .then(argument("amount", DoubleArgumentType.doubleArg(0)).executes(c -> bal(c, 0)))))
                        .then(literal("ban").then(argument("nick", StringArgumentType.word()).executes(c -> {
                            String nick = StringArgumentType.getString(c, "nick");
                            Store.data.banned.add(Store.key(nick));
                            Store.save();
                            fb(c, nick + " в чёрном списке");
                            return 1;
                        })))
                        .then(literal("unban").then(argument("nick", StringArgumentType.word()).executes(c -> {
                            String nick = StringArgumentType.getString(c, "nick");
                            Store.data.banned.remove(Store.key(nick));
                            Store.save();
                            fb(c, nick + " убран из чёрного списка");
                            return 1;
                        })))
                        .then(literal("orders").executes(c -> {
                            int n = 0;
                            for (Data.Order o : Store.data.orders) {
                                if (!"PENDING".equals(o.status)) continue;
                                fb(c, "#" + o.id + " " + o.player + ": " + o.productName + " x" + o.qty + " = " + Store.money(o.total));
                                n++;
                            }
                            if (n == 0) fb(c, "Нет заказов в ожидании");
                            return 1;
                        }))
                        .then(literal("done").then(argument("id", IntegerArgumentType.integer(1)).executes(c -> {
                            int id = IntegerArgumentType.getInteger(c, "id");
                            fb(c, BotLogic.completeOrder(id) ? "Заказ #" + id + " выдан" : "Заказ не найден или уже закрыт");
                            return 1;
                        })))
                        .then(literal("cancel").then(argument("id", IntegerArgumentType.integer(1)).executes(c -> {
                            int id = IntegerArgumentType.getInteger(c, "id");
                            fb(c, BotLogic.cancelOrder(id) ? "Заказ #" + id + " отменён, деньги возвращены" : "Заказ не найден или уже закрыт");
                            return 1;
                        })))
        ));
    }

    private static int bal(CommandContext<FabricClientCommandSource> c, int mode) {
        String nick = StringArgumentType.getString(c, "nick");
        double amount = DoubleArgumentType.getDouble(c, "amount");
        if (mode > 0) Store.addBalance(nick, amount);
        else if (mode < 0) Store.addBalance(nick, -amount);
        else Store.setBalance(nick, amount);
        fb(c, nick + ": " + Store.money(Store.balance(nick)));
        return 1;
    }

    private static void fb(CommandContext<FabricClientCommandSource> c, String text) {
        c.getSource().sendFeedback(Component.literal("[MarketBot] " + text));
    }
}
