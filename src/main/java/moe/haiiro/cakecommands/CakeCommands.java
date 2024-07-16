package moe.haiiro.cakecommands;

import moe.haiiro.cakecommands.teams.PlayerSuggestionProvider;
import moe.haiiro.cakecommands.teams.TeamManager;
import moe.haiiro.cakecommands.teams.TeamSuggestionProvider;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.arguments.LongArgumentType.longArg;
import static com.mojang.brigadier.arguments.StringArgumentType.string;
import static com.mojang.brigadier.arguments.StringArgumentType.word;
import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class CakeCommands implements ModInitializer {
    public static final String MODID = "cakecommands";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing CakeCommands");
        loadCommands();
    }

    private void loadCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                    literal("cteam")
                            .then(literal("help")
                                    .executes(TeamManager::help)
                            )
                            .then(literal("create")
                                    .then(argument("name", word())
                                            .then(argument("color", string())
                                                    .executes(TeamManager::createTeam)
                                            )
                                    )
                            )
                            .then(literal("delete")
                                    .executes(TeamManager::deleteTeam)
                            )
                            .then(literal("edit")
                                    .then(literal("color")
                                            .then(argument("color", string())
                                                    .executes(TeamManager::editTeamColor)
                                            )
                                    )
                                    .then(literal("name")
                                            .then(argument("name", word())
                                                    .executes(TeamManager::editTeamName)
                                            )
                                    )
                            )
                            .then(literal("invite")
                                    .then(argument("player", word())
                                            .suggests(new PlayerSuggestionProvider())
                                            .executes(TeamManager::inviteToTeam)
                                    )
                            )
                            .then(literal("kick")
                                    .then(argument("player", word())
                                            .suggests(new PlayerSuggestionProvider())
                                            .executes(TeamManager::kickFromTeam)
                                    )
                            )
                            .then(literal("list")
                                    .executes(TeamManager::listTeams)
                            )
                            .then(literal("accept")
                                    .then(argument("invite_id", longArg())
                                            .executes(TeamManager::acceptInvite)
                                    )
                            )
                            .then(literal("decline")
                                    .then(argument("invite_id", longArg())
                                            .executes(TeamManager::declineInvite)
                                    )
                            )
                            .then(literal("leave")
                                    .executes(TeamManager::leaveTeam)
                            )
                            .then(literal("info")
                                    .then(argument("team", word())
                                            .suggests(new TeamSuggestionProvider())
                                            .executes(TeamManager::teamInfo)
                                    )
                            )
                            .then(literal("forcedelete")
                                    .requires(source -> source.hasPermissionLevel(4))
                                    .then(argument("team", word())
                                            .suggests(new TeamSuggestionProvider())
                                            .executes(TeamManager::forceDeleteTeam)
                                    )
                            )
                            .then(literal("msg")
                                    .then(argument("message", string())
                                            .executes(TeamManager::privateMessageTeam)
                                    ))
            );

            dispatcher.register(
                    literal("ctm")
                            .then(argument("message", greedyString())
                                    .executes(TeamManager::privateMessageTeam)
                            )
            );
        });
    }
}