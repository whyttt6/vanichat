package whytt.vanichat.client;

import net.minecraft.text.*;
import net.minecraft.util.Formatting;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class PaintDirectMessage {
    public static MutableText PaintText(boolean to_or_from, String name, String text, int color1, int color2, boolean is2colors) {
        // --- 1. PREPARE DATA ---
        String cleanName = name.trim().split("\\s+")[0];
        ClickEvent replyClickEvent = new ClickEvent.SuggestCommand(String.format("/msg %s ", cleanName));
        String senderDisplay = to_or_from ? name : "Вы";
        String receiverDisplay = to_or_from ? "Вы" : name;

        MutableText hoverText = createHoverText(name, to_or_from);
        HoverEvent hoverEvent = new HoverEvent.ShowText(hoverText);

        // --- 2. BUILD MESSAGE ---
        MutableText message = Text.literal("").formatted(Formatting.RESET);

        if (is2colors) {
            // --- 2a. Two-Color Mode (New Approach) ---
            // Build each component with its full style individually.

            // "PM " part
            message.append(Text.literal("PM ").setStyle(Style.EMPTY.withColor(color1).withHoverEvent(hoverEvent)));

            // Sender part (e.g., the player's name)
            Style senderStyle = Style.EMPTY.withColor(color2).withBold(true).withHoverEvent(hoverEvent);
            if (to_or_from) { // If this is a received message, the sender is a player we can reply to.
                senderStyle = senderStyle.withClickEvent(replyClickEvent);
            }
            message.append(Text.literal(senderDisplay).setStyle(senderStyle));

            // "→" part
            message.append(Text.literal(" → ").setStyle(Style.EMPTY.withColor(color1).withHoverEvent(hoverEvent)));

            // Receiver part (e.g., "Вы" or a player's name)
            Style receiverStyle = Style.EMPTY.withColor(color2).withBold(true).withHoverEvent(hoverEvent);
            if (!to_or_from) { // If this is a sent message, the receiver is a player we can reply to.
                receiverStyle = receiverStyle.withClickEvent(replyClickEvent);
            }
            message.append(Text.literal(receiverDisplay).setStyle(receiverStyle));

            // "●" part
            message.append(Text.literal(" ● ").setStyle(Style.EMPTY.withColor(color1).withHoverEvent(hoverEvent)));

        } else {
            // --- 2b. Gradient Mode ---
            String fullPrefix = "PM " + senderDisplay + " → " + receiverDisplay + " ● ";
            int senderStart = "PM ".length();
            int senderEnd = senderStart + senderDisplay.length();
            int receiverStart = senderEnd + " → ".length();
            int receiverEnd = receiverStart + receiverDisplay.length();

            for (int i = 0; i < fullPrefix.length(); i++) {
                char c = fullPrefix.charAt(i);
                float ratio = (float) i / (fullPrefix.length() - 1);
                int color = interpolateColor(color1, color2, ratio);

                // Start with a style that includes the hover event for every character in the prefix
                Style style = Style.EMPTY.withColor(color).withHoverEvent(hoverEvent);

                boolean isSenderPart = i >= senderStart && i < senderEnd;
                boolean isReceiverPart = i >= receiverStart && i < receiverEnd;

                if (isSenderPart || isReceiverPart) {
                    style = style.withBold(true);
                }

                // Add the click event only for the characters that are part of the actual player's name
                if ((to_or_from && isSenderPart) || (!to_or_from && isReceiverPart)) {
                    style = style.withClickEvent(replyClickEvent);
                }

                message.append(Text.literal(String.valueOf(c)).setStyle(style));
            }
        }

        // --- 3. FINALIZE MESSAGE ---
        // Append the actual message text, which has no special events
        message.append(Text.literal(text).setStyle(Style.EMPTY.withColor(Formatting.WHITE)));

        return message;
    }

    private static MutableText createHoverText(String name, boolean to_or_from) {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
        String formattedDateTime = now.format(formatter);
        String receivedOrSent = to_or_from ? "Получено " : "Отправлено ";

        return Text.literal("Личное сообщение\n")
            .formatted(Formatting.WHITE)
            .append(Text.literal(receivedOrSent)
                .formatted(Formatting.DARK_AQUA)
                .append(Text.literal(formattedDateTime)
                    .formatted(Formatting.WHITE)))
            .append("\n\n")
            .append(Text.literal("Нажмите для ответа (/msg)")
                .formatted(Formatting.GRAY));
    }

    private static int interpolateColor(int color1, int color2, float ratio) {
        int r1 = (color1 >> 16) & 0xFF, g1 = (color1 >> 8) & 0xFF, b1 = color1 & 0xFF;
        int r2 = (color2 >> 16) & 0xFF, g2 = (color2 >> 8) & 0xFF, b2 = color2 & 0xFF;

        int r = (int) (r1 + (r2 - r1) * ratio);
        int g = (int) (g1 + (g2 - g1) * ratio);
        int b = (int) (b1 + (b2 - b1) * ratio);

        return (r << 16) | (g << 8) | b;
    }
}
