package moe.haiiro.cakecommands.util;

import net.minecraft.text.Style;
import net.minecraft.text.Text;

public class MessageFormatter {
    private static final String PREFIX = "GakkouCraft";
    public static final int ERROR_COLOR = 0xf7676a;
    public static final int SUCCESS_COLOR = 0xa3f767;
    public static final int INFO_COLOR = 0x67a3f7;
    public static final int WARNING_COLOR = 0xf7d767;
    public static final int WHITE = 0xffffff;
    public static final int BLACK = 0x000000;

    public static Text formatMessage(String message, int messageColor) {
        Text formattedPrefix = Text.literal("[")
                .setStyle(Style.EMPTY.withColor(0xd167f7))
                .append(Text.literal(PREFIX)
                        .setStyle(Style.EMPTY.withColor(0xdb7dd5))
                        .append(Text.literal("] ")
                                .setStyle(Style.EMPTY.withColor(0xd167f7))));
        Text formattedMessage = Text.literal(message)
                .setStyle(Style.EMPTY.withColor(messageColor));
        return formattedPrefix.copy().append(formattedMessage);
    }

    public static Text prefixMessage(Text message) {
        Text formattedPrefix = Text.literal("[")
                .setStyle(Style.EMPTY.withColor(0xd167f7))
                .append(Text.literal(PREFIX)
                        .setStyle(Style.EMPTY.withColor(0xdb7dd5))
                        .append(Text.literal("] ")
                                .setStyle(Style.EMPTY.withColor(0xd167f7))));
        return formattedPrefix.copy().append(message);
    }
}
