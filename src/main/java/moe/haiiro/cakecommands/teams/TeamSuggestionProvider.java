package moe.haiiro.cakecommands.teams;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.server.command.ServerCommandSource;

import java.util.concurrent.CompletableFuture;

public class TeamSuggestionProvider implements SuggestionProvider<ServerCommandSource> {
    @Override
    public CompletableFuture<Suggestions> getSuggestions(CommandContext<ServerCommandSource> commandSource, SuggestionsBuilder builder) {
        var teams = TeamManager.getScoreboard(commandSource).getTeams();
        if (teams.isEmpty()) {
            return Suggestions.empty();
        }

        return CompletableFuture.supplyAsync(() -> {
            teams.forEach(team -> {
                var fullName = team.getName();
                if (fullName.startsWith("cteam_")) {
                    var name = fullName.split("_")[1];
                    builder.suggest(name);
                }
            });
            return builder.build();
        });
    }
}
