package moe.haiiro.cakecommands.teams;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.server.command.ServerCommandSource;

import java.util.concurrent.CompletableFuture;

public class PlayerSuggestionProvider implements SuggestionProvider<ServerCommandSource> {
    @Override
    public CompletableFuture<Suggestions> getSuggestions(CommandContext<ServerCommandSource> commandSource, SuggestionsBuilder builder) {
        var players = commandSource.getSource().getServer().getPlayerManager().getPlayerList();
        if (players.isEmpty()) {
            return Suggestions.empty();
        }

        return CompletableFuture.supplyAsync(() -> {
            players.forEach(player -> builder.suggest(player.getName().getString()));
            return builder.build();
        });
    }
}
