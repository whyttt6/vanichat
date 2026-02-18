package whytt.vanichat.client;

import whytt.vanichat.client.config.VaniChatConfig;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

public class VaniChatClient implements ClientModInitializer {
    // Предохранители от бесконечных циклов (рекурсии)
    private static boolean isHandlingSend = false;
    private static boolean isHandlingReceive = false;

    @Override
    public void onInitializeClient() {
        // Регистрация команд
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("vanichat")
                    .executes(context -> {
                        sendHelpMessage(context.getSource());
                        return 1;
                    })
                    .then(ClientCommandManager.literal("help")
                            .executes(context -> {
                                sendHelpMessage(context.getSource());
                                return 1;
                            }))
                    .then(ClientCommandManager.literal("color")
                            .then(ClientCommandManager.argument("hex_color", StringArgumentType.word())
                                    .executes(context -> {
                                        String hexColor = StringArgumentType.getString(context, "hex_color");
                                        try {
                                            int color = parseHexColor(hexColor);
                                            VaniChatConfig.getInstance().prefixColor = color;
                                            VaniChatConfig.getInstance().save();
                                            context.getSource().sendFeedback(Text.literal("Основной цвет чата изменен на ")
                                                    .append(Text.literal(hexColor.toLowerCase())
                                                            .setStyle(Style.EMPTY.withColor(color).withBold(true)))
                                                    .append("!"));
                                        } catch (NumberFormatException e) {
                                            context.getSource().sendError(Text.literal("Неверный формат цвета. Используйте HEX, например: #ffcff1"));
                                        }
                                        return 1;
                                    })))
                    .then(ClientCommandManager.literal("pmcolor")
                            .then(ClientCommandManager.argument("hex1", StringArgumentType.word())
                                    .then(ClientCommandManager.argument("hex2", StringArgumentType.word())
                                            .executes(context -> {
                                                String hex1 = StringArgumentType.getString(context, "hex1");
                                                String hex2 = StringArgumentType.getString(context, "hex2");
                                                try {
                                                    int color1 = parseHexColor(hex1);
                                                    int color2 = parseHexColor(hex2);
                                                    VaniChatConfig.getInstance().customColor1 = color1;
                                                    VaniChatConfig.getInstance().customColor2 = color2;
                                                    VaniChatConfig.getInstance().save();
                                                    context.getSource().sendFeedback(Text.literal("Цвета ЛС изменены на ")
                                                            .append(Text.literal(hex1.toLowerCase()).setStyle(Style.EMPTY.withColor(color1)))
                                                            .append(" и ")
                                                            .append(Text.literal(hex2.toLowerCase()).setStyle(Style.EMPTY.withColor(color2)))
                                                            .append("!"));
                                                } catch (NumberFormatException e) {
                                                    context.getSource().sendError(Text.literal("Неверный формат цвета. Используйте два HEX цвета, например: #67e8f9 #22d3ee"));
                                                }
                                                return 1;
                                            }))))
                    .then(ClientCommandManager.literal("chatcolor")
                            .then(ClientCommandManager.literal("on")
                                    .executes(context -> {
                                        VaniChatConfig.getInstance().chatPrefixEnabled = true;
                                        VaniChatConfig.getInstance().save();
                                        context.getSource().sendFeedback(Text.literal("Цвет чата включен.").formatted(Formatting.GREEN));
                                        return 1;
                                    }))
                            .then(ClientCommandManager.literal("off")
                                    .executes(context -> {
                                        VaniChatConfig.getInstance().chatPrefixEnabled = false;
                                        VaniChatConfig.getInstance().save();
                                        context.getSource().sendFeedback(Text.literal("Цвет чата отключен.").formatted(Formatting.YELLOW));
                                        return 1;
                                    }))));
        });

        // Проверка версии
        VersionChecker.VersionResponse response = VersionChecker.checkForUpdate("0.5-mc1.21.3");
        if (response != null && response.has_update) {
            AtomicBoolean updateScreenShown = new AtomicBoolean(false);
            ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
                if (screen instanceof TitleScreen && !updateScreenShown.get()) {
                    updateScreenShown.set(true);
                    client.setScreen(new UpdateScreen(screen, response));
                }
            });
        }

        // Исправленный блок отправки сообщений 🚀
        ClientSendMessageEvents.ALLOW_CHAT.register((message) -> {
            if (isHandlingSend) return true; // Пропускаем, если сообщение отправлено нашим же модом

            if (message.startsWith("/")) return true;
            if (message.contains("&#") && message.length() > 8) return true;

            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null) {
                isHandlingSend = true; // Включаем защиту
                try {
                    String modifiedMessage = ChatSendHandler.addPrefix(message);
                    client.player.networkHandler.sendChatMessage(modifiedMessage);
                } finally {
                    isHandlingSend = false; // Выключаем защиту в любом случае (даже при ошибке)
                }
                return false; // Отменяем оригинальное сообщение
            }
            return true;
        });

        // Исправленный блок получения сообщений 📥
        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
            if (isHandlingReceive) return true; // Пропускаем, если мы сами создали это сообщение

            String rawMessage = getRawMessageContent(message);
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null) return true;

            if (rawMessage.startsWith("PM ") && rawMessage.contains(" → ") && rawMessage.contains(" ● ")) {
                try {
                    String content = rawMessage.substring(3);
                    String[] senderPart = content.split(" → ", 2);
                    if (senderPart.length < 2) return true;
                    String sender = senderPart[0].trim();

                    String[] receiverPart = senderPart[1].split(" ● ", 2);
                    if (receiverPart.length < 2) return true;
                    String receiver = receiverPart[0].trim();
                    String messageText = receiverPart[1].trim();

                    String myName = client.player.getName().getString(); // Используем getString для надежности
                    String cleanSender = sender.trim().split("\\s+")[0];
                    String cleanReceiver = receiver.trim().split("\\s+")[0];

                    boolean isMessageForMe = cleanReceiver.equals(myName);
                    boolean isMessageFromMe = cleanSender.equals(myName);

                    if (!isMessageForMe && !isMessageFromMe) return true;

                    String otherPlayerName = isMessageForMe ? sender : receiver;

                    MutableText text = PaintDirectMessage.PaintText(
                            isMessageForMe, otherPlayerName, messageText,
                            VaniChatConfig.getInstance().customColor1, VaniChatConfig.getInstance().customColor2,
                            VaniChatConfig.getInstance().selectedOption.equals("vanichat.config.option.color_scheme.2_colors")
                    );

                    isHandlingReceive = true; // Включаем защиту
                    try {
                        client.player.sendMessage(text, false);
                    } finally {
                        isHandlingReceive = false; // Выключаем защиту
                    }
                    return false; // Скрываем оригинальное системное сообщение
                } catch (Exception e) {
                    return true;
                }
            }

            return true;
        });
    }

    private void sendHelpMessage(net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource source) {
        MutableText helpMessage = Text.literal("\n")
                .append(Text.literal("--- Справка по VaniChat ---").formatted(Formatting.GOLD, Formatting.BOLD))
                .append("\n\n")
                .append(Text.literal("/vanichat color <#hex>").formatted(Formatting.AQUA))
                .append(Text.literal(" - Устанавливает основной цвет чата.").formatted(Formatting.GRAY))
                .append("\n   Пример: ").append(Text.literal("/vanichat color #ffcff1").formatted(Formatting.YELLOW))
                .append("\n\n")
                .append(Text.literal("/vanichat pmcolor <#hex1> <#hex2>").formatted(Formatting.AQUA))
                .append(Text.literal(" - Устанавливает цвета для личных сообщений.").formatted(Formatting.GRAY))
                .append("\n   Пример: ").append(Text.literal("/vanichat pmcolor #67e8f9 #22d3ee").formatted(Formatting.YELLOW))
                .append("\n\n")
                .append(Text.literal("/vanichat chatcolor <on|off>").formatted(Formatting.AQUA))
                .append(Text.literal(" - Включает или отключает цвет чата.").formatted(Formatting.GRAY))
                .append("\n\n")
                .append(Text.literal("/vanichat help").formatted(Formatting.AQUA))
                .append(Text.literal(" - Показывает это сообщение.").formatted(Formatting.GRAY))
                .append("\n");

        source.sendFeedback(helpMessage);
    }

    private int parseHexColor(String hex) throws NumberFormatException {
        if (hex.startsWith("#")) {
            hex = hex.substring(1);
        }
        return Integer.parseInt(hex, 16);
    }

    private String getRawMessageContent(Text message) {
        final StringBuilder builder = new StringBuilder();
        message.visit(s -> {
            builder.append(s);
            return Optional.empty();
        });
        return builder.toString();
    }
}