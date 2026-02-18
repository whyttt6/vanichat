package whytt.vanichat.client;

import whytt.vanichat.client.config.VaniChatConfig;

public class ChatSendHandler {

    /**
     * Adds a specific prefix to a message based on the color set in the config,
     * but only if prefixes are enabled.
     *
     * @param message The original chat message.
     * @return The message with the appropriate prefix, or the original message if disabled.
     */
    public static String addPrefix(String message) {
        // First, check if prefixes are enabled at all.
        if (!VaniChatConfig.getInstance().chatPrefixEnabled) {
            return message; // Return the original message without any prefix.
        }

        int mainColor = VaniChatConfig.getInstance().prefixColor;
        
        // Convert the integer color to a lowercase HEX string for the prefix
        String prefixWithExclamation = String.format("&#%06x", mainColor);
        
        // Create a dimmer version of the color for local chat
        int dimmedColor = dimColor(mainColor, 0.75f); // 75% brightness
        String prefixWithoutExclamation = String.format("&#%06x", dimmedColor);

        // Check if the message STARTS with an exclamation mark for global chat.
        if (message.startsWith("!")) {
            // Insert the prefix after the first character.
            return "!" + prefixWithExclamation + message.substring(1);
        } else {
            // No exclamation mark at the start. Add the prefix to the beginning for local chat.
            return prefixWithoutExclamation + message;
        }
    }

    /**
     * Dims a color by a given factor.
     * @param color The original color in 0xRRGGBB format.
     * @param factor The factor to dim by (e.g., 0.75 for 75% brightness).
     * @return The dimmed color.
     */
    private static int dimColor(int color, float factor) {
        int r = (int) (((color >> 16) & 0xFF) * factor);
        int g = (int) (((color >> 8) & 0xFF) * factor);
        int b = (int) ((color & 0xFF) * factor);

        // Ensure components are within the valid range [0, 255]
        r = Math.min(255, Math.max(0, r));
        g = Math.min(255, Math.max(0, g));
        b = Math.min(255, Math.max(0, b));

        return (r << 16) | (g << 8) | b;
    }
}
