package moe.haiiro.cakecommands.util;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class CommandUtils {
    public static void sendFeedback(CommandContext<ServerCommandSource> context, String message, int color) {
        context.getSource().sendFeedback(() -> MessageFormatter.formatMessage(message, color), false);
    }

    public static void sendFeedback(CommandContext<ServerCommandSource> context, Text message) {
        context.getSource().sendFeedback(() -> MessageFormatter.prefixMessage(message), false);
    }

    public static void broadcastMessage(CommandContext<ServerCommandSource> context, String message, int color) {
        context.getSource().getServer().getPlayerManager().broadcast(MessageFormatter.formatMessage(message, color), false);
    }

    public static void sendToPlayer(CommandContext<ServerCommandSource> context, String playerName, String message, int color) {
        var player = context.getSource().getServer().getPlayerManager().getPlayer(playerName);
        if (player != null) {
            player.sendMessage(MessageFormatter.formatMessage(message, color));
        } else {
            sendFeedback(context, "Player " + playerName + " not found!", MessageFormatter.ERROR_COLOR);
        }
    }

    public static void sendToPlayer(CommandContext<ServerCommandSource> context, String playerName, Text message) {
        var player = context.getSource().getServer().getPlayerManager().getPlayer(playerName);
        if (player != null) {
            player.sendMessage(MessageFormatter.prefixMessage(message));
        } else {
            sendFeedback(context, "Player " + playerName + " not found!", MessageFormatter.ERROR_COLOR);
        }
    }

    public static void sendToPlayer(CommandContext<ServerCommandSource> context, ServerPlayerEntity player, Text message) {
        if (player != null) {
            player.sendMessage(message);
        } else {
            sendFeedback(context, "Player not found!", MessageFormatter.ERROR_COLOR);
        }
    }
}
