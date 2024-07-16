package moe.haiiro.cakecommands.teams;

import com.mojang.brigadier.context.CommandContext;
import moe.haiiro.cakecommands.CakeCommands;
import moe.haiiro.cakecommands.util.MessageFormatter;
import net.minecraft.scoreboard.ServerScoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static moe.haiiro.cakecommands.util.CommandUtils.sendFeedback;
import static moe.haiiro.cakecommands.util.CommandUtils.broadcastMessage;
import static moe.haiiro.cakecommands.util.CommandUtils.sendToPlayer;
import static net.minecraft.scoreboard.AbstractTeam.CollisionRule;
import static net.minecraft.scoreboard.AbstractTeam.VisibilityRule;
import static moe.haiiro.cakecommands.util.MessageFormatter.prefixMessage;

public class TeamManager {
    private static ArrayList<TeamInvite> teamInvites = new ArrayList<>();

    private static TeamState getTeam(CommandContext<ServerCommandSource> ctx) {
        String name = ctx.getArgument("name", String.class);
        String color = ctx.getArgument("color", String.class);
        var owner = ctx.getSource().getPlayer().getName();
        if (owner == null) {
            owner = Text.literal("Server");
        }

        System.out.println("Getting team with name " + name + " and color " + color + " for " + owner.getString());
        return new TeamState(name, color, owner.getString());
    }

    public static ServerScoreboard getScoreboard(CommandContext<ServerCommandSource> ctx) {
        return ctx.getSource().getServer().getScoreboard();
    }

    private static boolean isTeamOwner(CommandContext<ServerCommandSource> ctx) {
        ServerScoreboard scoreboard = getScoreboard(ctx);
        var player = ctx.getSource().getPlayer().getName();
        if (player == null) {
            return false;
        }
        Team ownerTeam = scoreboard.getTeams()
                .stream()
                .filter(t -> t.getName().startsWith("cteam") && t.getName().endsWith(player.getString()))
                .findFirst()
                .orElse(null);

        return ownerTeam != null;
    }

    private static boolean isValidColor(String color) {
        var colors = new String[]{
                "black",
                "dark_blue",
                "dark_green",
                "dark_aqua",
                "dark_red",
                "dark_purple",
                "gold",
                "gray",
                "dark_gray",
                "blue",
                "green",
                "aqua",
                "red",
                "light_purple",
                "yellow",
                "white"
        };

        return color.matches("^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$") || List.of(colors).contains(color);
    }

    private static ServerPlayerEntity getPlayer(CommandContext<ServerCommandSource> ctx) {
        return ctx.getSource().getPlayer();
    }

    private static boolean isPlayer(CommandContext<ServerCommandSource> ctx) {
        return ctx.getSource().getPlayer() != null;
    }

    public static int createTeam(CommandContext<ServerCommandSource> ctx) {
        var player = getPlayer(ctx);
        if (player == null) {
            sendFeedback(ctx, "You must be a player to create a team!", MessageFormatter.ERROR_COLOR);
            return 0;
        }

        TeamState team = getTeam(ctx);
        CakeCommands.LOGGER.info("Creating team with name {} and color {}", team.name(), team.color());

        String teamName = team.getTeamId();

        var prefix = Text.literal(team.getFormattedName());
        prefix.setStyle(Style.EMPTY.withColor(team.getMinecraftColor()));

        var displayName = Text.literal(team.getFormattedName().trim());
        displayName.setStyle(Style.EMPTY.withColor(team.getMinecraftColor()));

        ServerScoreboard scoreboard = getScoreboard(ctx);

        if (scoreboard.getTeam(teamName) != null) {
            sendFeedback(ctx, "Team already exists!", MessageFormatter.ERROR_COLOR);
            return 0;
        }

        if (isTeamOwner(ctx)) {
            sendFeedback(ctx, "You already own a team!", MessageFormatter.ERROR_COLOR);
            return 0;
        }

        Team t = scoreboard.addTeam(teamName);
        t.setPrefix(prefix);
        t.setDisplayName(displayName);
        t.setCollisionRule(CollisionRule.NEVER);
        t.setFriendlyFireAllowed(false);
        t.setShowFriendlyInvisibles(true);
        t.setDeathMessageVisibilityRule(VisibilityRule.ALWAYS);
        t.setNameTagVisibilityRule(VisibilityRule.HIDE_FOR_OTHER_TEAMS);

        scoreboard.addScoreHolderToTeam(player.getName().getString(), t);
        sendFeedback(ctx, "Team created successfully!", MessageFormatter.SUCCESS_COLOR);
        broadcastMessage(ctx, player.getName().getString() + " has created a new team!", MessageFormatter.INFO_COLOR);
        return 1;
    }

    public static int deleteTeam(CommandContext<ServerCommandSource> ctx) {
        if (!isPlayer(ctx)) {
            sendFeedback(ctx, "You must be a player to delete a team!", MessageFormatter.ERROR_COLOR);
            return 0;
        }

        Text owner = getPlayer(ctx).getName();

        if (!isTeamOwner(ctx)) {
            sendFeedback(ctx, "You don't own a team!", MessageFormatter.ERROR_COLOR);
            return 0;
        }

        ServerScoreboard scoreboard = getScoreboard(ctx);
        Optional<Team> ownerTeam = scoreboard.getTeams()
                .stream()
                .filter(t -> t.getName().startsWith("cteam") && t.getName().endsWith(owner.getString()))
                .findFirst();

        if (ownerTeam.isEmpty()) {
            sendFeedback(ctx, "Team not found!", MessageFormatter.ERROR_COLOR);
            return 0;
        }

        CakeCommands.LOGGER.info("Deleting team with name {} (Owner: {})", ownerTeam.get().getName(), owner.getString());

        ownerTeam.ifPresent(scoreboard::removeTeam);
        sendFeedback(ctx, "Team deleted successfully!", MessageFormatter.SUCCESS_COLOR);
        return 1;
    }

    public static int editTeamName(CommandContext<ServerCommandSource> ctx) {
        if (!isTeamOwner(ctx)) {
            sendFeedback(ctx, "You don't own a team!", MessageFormatter.ERROR_COLOR);
            return 0;
        }

        var name = ctx.getArgument("name", String.class);
        if (name.length() > 4) {
            sendFeedback(ctx, "Team name must be 4 characters or less!", MessageFormatter.ERROR_COLOR);
            return 0;
        }

        var scoreboard = getScoreboard(ctx);
        var ownerTeam = scoreboard.getTeams()
                .stream()
                .filter(t -> t.getName().startsWith("cteam") && t.getName().endsWith(getPlayer(ctx).getName().getString()))
                .findFirst();
        assert ownerTeam.isPresent();

        CakeCommands.LOGGER.info("Editing team name from {} to {} (Owner: {})", ownerTeam.get().getName(), name, getPlayer(ctx).getName().getString());

        var color = ownerTeam.get().getPrefix().getStyle();
        ownerTeam.get().setPrefix(Text.literal(name + " ").setStyle(color));

        sendFeedback(ctx, "Team name changed successfully!", MessageFormatter.SUCCESS_COLOR);
        return 1;
    }

    public static int editTeamColor(CommandContext<ServerCommandSource> ctx) {
        if (!isTeamOwner(ctx)) {
            sendFeedback(ctx, "You don't own a team!", MessageFormatter.ERROR_COLOR);
            return 0;
        }

        var color = ctx.getArgument("color", String.class);
        if (!isValidColor(color)) {
            sendFeedback(ctx, "Invalid color! Must be a valid minecraft color or hex color.", MessageFormatter.ERROR_COLOR);
            return 0;
        }

        var scoreboard = getScoreboard(ctx);
        var ownerTeam = scoreboard.getTeams()
                .stream()
                .filter(t -> t.getName().startsWith("cteam") && t.getName().endsWith(getPlayer(ctx).getName().getString()))
                .findFirst();

        ownerTeam.ifPresent(t -> {
            var name = t.getPrefix().getString();

            TextColor c = null;
            if (color.matches("^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$")) {
                c = TextColor.fromRgb(Integer.parseInt(color.substring(1), 16));
            }

            for (Formatting formatting : Formatting.values()) {
                if (formatting.getName().toLowerCase().equals(color)) {
                    c = TextColor.fromFormatting(formatting);
                }
            }

            if (c == null) {
                c = TextColor.fromFormatting(Formatting.WHITE);
            }

            t.setPrefix(Text.literal(name).setStyle(Style.EMPTY.withColor(c)));
        });

        sendFeedback(ctx, "Team color changed successfully!", MessageFormatter.SUCCESS_COLOR);
        return 1;
    }

    public static int inviteToTeam(CommandContext<ServerCommandSource> ctx) {
        var player = getPlayer(ctx);
        if (player == null) {
            sendFeedback(ctx, "You must be a player to invite someone to a team!", MessageFormatter.ERROR_COLOR);
            return 0;
        }

        String target = ctx.getArgument("player", String.class);
        if (target == null || target.isEmpty()) {
            sendFeedback(ctx, "Target player not found!", MessageFormatter.ERROR_COLOR);
            return 0;
        }

        ServerPlayerEntity targetPlayer;
        try {
            targetPlayer = ctx.getSource().getServer().getPlayerManager().getPlayer(target);
        } catch (Exception e) {
            sendFeedback(ctx, "Target player not found!", MessageFormatter.ERROR_COLOR);
            return 0;
        }

        if (targetPlayer == null) {
            sendFeedback(ctx, "Target player not found!", MessageFormatter.ERROR_COLOR);
            return 0;
        }

        var owner = player.getName();
        if (!isTeamOwner(ctx)) {
            sendFeedback(ctx, "You don't own a team!", MessageFormatter.ERROR_COLOR);
            return 0;
        }

        var team = getScoreboard(ctx).getTeams()
                .stream()
                .filter(t -> t.getName().startsWith("cteam") && t.getName().endsWith(owner.getString()))
                .findFirst()
                .orElse(null);

        if (team == null) {
            sendFeedback(ctx, "Team not found!", MessageFormatter.ERROR_COLOR);
            return 0;
        }

        if (team.getPlayerList().contains(targetPlayer.getName().getString())) {
            sendFeedback(ctx, "Player is already in the team!", MessageFormatter.ERROR_COLOR);
            return 0;
        }

        TeamInvite inv = new TeamInvite(team, targetPlayer.getName().getString(), LocalDateTime.now(), false);
        teamInvites.add(inv);

        Text inviteText = Text.literal(owner.getString() + " has invited you to join their team!\n")
                .setStyle(Style.EMPTY.withColor(MessageFormatter.INFO_COLOR))
                .append(
                        Text.literal("[Accept]")
                                .setStyle(Style.EMPTY
                                        .withColor(MessageFormatter.SUCCESS_COLOR)
                                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("Click to accept the invite!")))
                                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/cteam accept " + inv.getInviteId())))
                                .append(
                                        Text.literal(" [Decline]")
                                                .setStyle(Style.EMPTY
                                                        .withColor(MessageFormatter.ERROR_COLOR)
                                                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("Click to decline the invite!")))
                                                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/cteam decline " + inv.getInviteId())))
                                )
                );
        sendToPlayer(ctx, targetPlayer, inviteText);

        ctx.getSource().getServer().getWorld(targetPlayer.getEntityWorld().getRegistryKey())
                .playSound(null, targetPlayer.getBlockPos(), net.minecraft.sound.SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, net.minecraft.sound.SoundCategory.PLAYERS, 1.0f, 1.0f);

        sendFeedback(ctx, "Player invited to team!", MessageFormatter.SUCCESS_COLOR);
        return 1;
    }

    public static int kickFromTeam(CommandContext<ServerCommandSource> ctx) {
        var player = getPlayer(ctx);
        if (player == null) {
            sendFeedback(ctx, "You must be a player to kick someone from a team!", MessageFormatter.ERROR_COLOR);
            return 0;
        }

        String target = ctx.getArgument("player", String.class);
        if (target == null || target.isEmpty()) {
            sendFeedback(ctx, "Target player not found!", MessageFormatter.ERROR_COLOR);
            return 0;
        }

        ServerPlayerEntity targetPlayer;
        try {
            targetPlayer = ctx.getSource().getServer().getPlayerManager().getPlayer(target);
        } catch (Exception e) {
            sendFeedback(ctx, "Target player not found!", MessageFormatter.ERROR_COLOR);
            return 0;
        }

        if (targetPlayer == null) {
            sendFeedback(ctx, "Target player not found!", MessageFormatter.ERROR_COLOR);
            return 0;
        }

        var owner = player.getName();
        if (!isTeamOwner(ctx)) {
            sendFeedback(ctx, "You don't own a team!", MessageFormatter.ERROR_COLOR);
            return 0;
        }

        var team = getScoreboard(ctx).getTeams()
                .stream()
                .filter(t -> t.getName().startsWith("cteam") && t.getName().endsWith(owner.getString()))
                .findFirst()
                .orElse(null);

        if (team == null) {
            sendFeedback(ctx, "Team not found!", MessageFormatter.ERROR_COLOR);
            return 0;
        }

        if (!team.getPlayerList().contains(targetPlayer.getName().getString())) {
            sendFeedback(ctx, "Player is not in the team!", MessageFormatter.ERROR_COLOR);
            return 0;
        }

        getScoreboard(ctx).removeScoreHolderFromTeam(targetPlayer.getName().getString(), team);
        sendFeedback(ctx, "Player kicked from team!", MessageFormatter.SUCCESS_COLOR);

        Text kickText = Text.literal("You have been kicked from ")
                .setStyle(Style.EMPTY.withColor(MessageFormatter.WARNING_COLOR))
                .append(
                        Text.literal(team.getDisplayName().getString())
                                .setStyle(team.getPrefix().getStyle())
                ).append(
                        Text.literal("!")
                                .setStyle(Style.EMPTY.withColor(MessageFormatter.WARNING_COLOR))
                );
        sendToPlayer(ctx, targetPlayer, kickText);

        return 1;
    }

    public static int listTeams(CommandContext<ServerCommandSource> ctx) {
        var scoreboard = getScoreboard(ctx);

        var teams = scoreboard.getTeams()
                .stream()
                .filter(t -> t.getName().startsWith("cteam"))
                .toList();

        if (teams.isEmpty()) {
            sendFeedback(ctx, "There are no teams created yet!", MessageFormatter.WARNING_COLOR);
            return 0;
        } else {
            Text teamList = Text.literal("Teams:\n")
                    .setStyle(Style.EMPTY.withColor(MessageFormatter.INFO_COLOR));

            for (Team t : teams) {
                HoverEvent hoverEvent = new HoverEvent(
                        HoverEvent.Action.SHOW_TEXT,
                        Text.literal("This team has % out of 3 members:\n".replace("%", String.valueOf(t.getPlayerList().size())))
                                .setStyle(Style.EMPTY.withColor(MessageFormatter.WARNING_COLOR))
                                .append(
                                        Text.literal(t.getPlayerList().stream().reduce("", (a, b) -> "\n" + a + b))
                                                .setStyle(Style.EMPTY.withColor(MessageFormatter.INFO_COLOR))
                                )
                );
                teamList = teamList.copy().append(
                                Text.literal("\n" + t.getPrefix().getString())
                                        .setStyle(t.getPrefix().getStyle()
                                                .withHoverEvent(hoverEvent)
                                        )
                        )
                        .append(
                                Text.literal(" (Owner: ")
                                        .setStyle(Style.EMPTY
                                                .withColor(MessageFormatter.INFO_COLOR)
                                                .withHoverEvent(hoverEvent)
                                        )
                        )
                        .append(
                                Text.literal(t.getName().split("_")[t.getName().split("_").length - 1])
                                        .setStyle(Style.EMPTY
                                                .withColor(MessageFormatter.WARNING_COLOR)
                                                .withHoverEvent(hoverEvent)
                                        )
                        )
                        .append(
                                Text.literal(")")
                                        .setStyle(Style.EMPTY
                                                .withColor(MessageFormatter.INFO_COLOR)
                                                .withHoverEvent(hoverEvent)
                                        )
                        );
            }

            sendFeedback(ctx, teamList);
        }

        return 1;
    }

    public static int acceptInvite(CommandContext<ServerCommandSource> ctx) {
        var player = getPlayer(ctx);
        if (player == null) {
            sendFeedback(ctx, "You must be a player to accept an invite!", MessageFormatter.ERROR_COLOR);
            return 0;
        }

        long inviteId = ctx.getArgument("invite_id", Long.class);
        var invite = teamInvites.stream()
                .filter(i -> i.getInviteId() == inviteId)
                .findFirst()
                .orElse(null);

        if (invite == null) {
            sendFeedback(ctx, "Invite not found!", MessageFormatter.ERROR_COLOR);
            return 0;
        }

        if (invite.accepted()) {
            sendFeedback(ctx, "Invite already accepted!", MessageFormatter.ERROR_COLOR);
            return 0;
        }

        if (invite.inviteTime().isBefore(LocalDateTime.now().minusMinutes(15))) {
            sendFeedback(ctx, "Invite has expired!", MessageFormatter.ERROR_COLOR);
            teamInvites.remove(invite);
            return 0;
        }

        var team = invite.team();
        var target = player.getName();

        if (team.getPlayerList().contains(target.getString())) {
            sendFeedback(ctx, "Player is already in the team!", MessageFormatter.ERROR_COLOR);
            return 0;
        }

        // Replace the invite with an accepted one
        teamInvites.remove(invite);
        teamInvites.add(new TeamInvite(invite.team(), invite.playerName(), invite.inviteTime(), true));

        var scoreboard = getScoreboard(ctx);
        scoreboard.addScoreHolderToTeam(target.getString(), team);

        Text acceptText = Text.literal("You have accepted the invite to join ")
                .setStyle(Style.EMPTY.withColor(MessageFormatter.SUCCESS_COLOR))
                .append(
                        Text.literal(team.getDisplayName().getString())
                                .setStyle(team.getPrefix().getStyle())
                ).append(
                        Text.literal("!")
                                .setStyle(Style.EMPTY.withColor(MessageFormatter.SUCCESS_COLOR))
                );
        sendToPlayer(ctx, player, acceptText);

        for (String p : team.getPlayerList()) {
            ServerPlayerEntity playerEntity = ctx.getSource().getServer().getPlayerManager().getPlayer(p);
            if (playerEntity != null) {
                Text joinText = Text.literal(target.getString() + " has joined the team!")
                        .setStyle(Style.EMPTY.withColor(MessageFormatter.INFO_COLOR));
                sendToPlayer(ctx, playerEntity, joinText);
            }
        }

        return 1;
    }

    public static int declineInvite(CommandContext<ServerCommandSource> ctx) {
        var player = getPlayer(ctx);
        if (player == null) {
            sendFeedback(ctx, "You must be a player to decline an invite!", MessageFormatter.ERROR_COLOR);
            return 0;
        }

        long inviteId = ctx.getArgument("invite_id", Long.class);
        var invite = teamInvites.stream()
                .filter(i -> i.getInviteId() == inviteId)
                .findFirst()
                .orElse(null);

        if (invite == null) {
            sendFeedback(ctx, "Invite not found!", MessageFormatter.ERROR_COLOR);
            return 0;
        }

        if (invite.accepted()) {
            sendFeedback(ctx, "Invite already accepted!", MessageFormatter.ERROR_COLOR);
            return 0;
        }

        if (invite.inviteTime().isBefore(LocalDateTime.now().minusMinutes(15))) {
            sendFeedback(ctx, "Invite has expired!", MessageFormatter.ERROR_COLOR);
            teamInvites.remove(invite);
            return 0;
        }

        var team = invite.team();

        teamInvites.remove(invite);

        Text declineText = Text.literal("You have declined the invite to join ")
                .setStyle(Style.EMPTY.withColor(MessageFormatter.ERROR_COLOR))
                .append(
                        Text.literal(team.getDisplayName().getString())
                                .setStyle(team.getPrefix().getStyle())
                ).append(
                        Text.literal("!")
                                .setStyle(Style.EMPTY.withColor(MessageFormatter.ERROR_COLOR))
                );
        sendToPlayer(ctx, player, declineText);

        String ownerName = team.getName().split("_")[team.getName().split("_").length - 1];
        ServerPlayerEntity owner = ctx.getSource().getServer().getPlayerManager().getPlayer(ownerName);
        if (owner != null) {
            Text declineOwnerText = Text.literal(player.getName().getString() + " has declined the invite to join your team!")
                    .setStyle(Style.EMPTY.withColor(MessageFormatter.ERROR_COLOR));
            sendToPlayer(ctx, owner, declineOwnerText);
        }

        return 1;
    }

    public static int leaveTeam(CommandContext<ServerCommandSource> ctx) {
        var player = getPlayer(ctx);
        if (player == null) {
            sendFeedback(ctx, "You must be a player to leave a team!", MessageFormatter.ERROR_COLOR);
            return 0;
        }

        var teamStream = getScoreboard(ctx).getTeams()
                .stream()
                .filter(t -> t.getName().startsWith("cteam"));

        Team team = null;
        for (Team t : teamStream.toList()) {
            if (t.getPlayerList().contains(player.getName().getString())) {
                team = t;
                break;
            }
        }

        if (team == null) {
            sendFeedback(ctx, "You are not in a team!", MessageFormatter.ERROR_COLOR);
            return 0;
        }

        getScoreboard(ctx).removeScoreHolderFromTeam(player.getName().getString(), team);
        sendFeedback(ctx, "You have left the team!", MessageFormatter.SUCCESS_COLOR);

        String ownerName = team.getName().split("_")[team.getName().split("_").length - 1];
        if (ownerName.equals(player.getName().getString())) {
            sendFeedback(ctx, "You are the owner of the team! Use /cteam delete to delete the team.", MessageFormatter.WARNING_COLOR);
            return 1;
        }

        team.getPlayerList().forEach(p -> {
            ServerPlayerEntity playerEntity = ctx.getSource().getServer().getPlayerManager().getPlayer(p);
            if (playerEntity != null) {
                Text leaveText = Text.literal(player.getName().getString() + " has left the team!")
                        .setStyle(Style.EMPTY.withColor(MessageFormatter.INFO_COLOR));
                sendToPlayer(ctx, playerEntity, leaveText);
            }
        });

        return 1;
    }

    public static int teamInfo(CommandContext<ServerCommandSource> ctx) {
        var teamName = ctx.getArgument("team", String.class);
        var teams = getScoreboard(ctx).getTeams()
                .stream()
                .filter(t -> t.getName().startsWith("cteam"))
                .toList();

        var team = teams.stream()
                .filter(t -> t.getName().startsWith("cteam_" + teamName))
                .findFirst()
                .orElse(null);

        if (team == null) {
            sendFeedback(ctx, "Team not found!", MessageFormatter.ERROR_COLOR);
            return 0;
        }

        var owner = team.getName().split("_")[team.getName().split("_").length - 1];
        var ownerPlayer = ctx.getSource().getServer().getPlayerManager().getPlayer(owner);
        var ownerName = ownerPlayer != null ? ownerPlayer.getName().getString() : owner;

        var members = team.getPlayerList();
        var memberList = members.stream()
                .map(m -> {
                    var player = ctx.getSource().getServer().getPlayerManager().getPlayer(m);
                    return player != null ? player.getName().getString() : m;
                })
                .reduce("", (a, b) -> a + "\n" + b);

        var hoverEvent = new HoverEvent(
                HoverEvent.Action.SHOW_TEXT,
                Text.literal("This team has % out of 3 members:\n".replace("%", String.valueOf(members.size())))
                        .setStyle(Style.EMPTY.withColor(MessageFormatter.WARNING_COLOR))
                        .append(
                                Text.literal(memberList)
                                        .setStyle(Style.EMPTY.withColor(MessageFormatter.INFO_COLOR))
                        )
        );

        var teamInfo = Text.literal("Team Info:\n")
                .setStyle(Style.EMPTY.withColor(MessageFormatter.INFO_COLOR))
                .append(
                        Text.literal("Name: ")
                                .setStyle(Style.EMPTY.withColor(MessageFormatter.INFO_COLOR))
                )
                .append(
                        Text.literal(team.getDisplayName().getString())
                                .setStyle(team.getPrefix().getStyle()
                                        .withHoverEvent(hoverEvent)
                                )
                )
                .append(
                        Text.literal("\nOwner: ")
                                .setStyle(Style.EMPTY.withColor(MessageFormatter.INFO_COLOR))
                )
                .append(
                        Text.literal(ownerName)
                                .setStyle(Style.EMPTY.withColor(MessageFormatter.WARNING_COLOR))
                )
                .append(
                        Text.literal("\nMembers: ")
                                .setStyle(Style.EMPTY.withColor(MessageFormatter.INFO_COLOR))
                )
                .append(
                        Text.literal(memberList)
                                .setStyle(Style.EMPTY.withColor(MessageFormatter.INFO_COLOR))
                );

        sendFeedback(ctx, teamInfo);

        return 1;
    }

    public static int forceDeleteTeam(CommandContext<ServerCommandSource> ctx) {
        var teamName = ctx.getArgument("team", String.class);
        var teams = getScoreboard(ctx).getTeams()
                .stream()
                .filter(t -> t.getName().startsWith("cteam"))
                .toList();

        var team = teams.stream()
                .filter(t -> t.getName().startsWith("cteam_" + teamName))
                .findFirst()
                .orElse(null);

        if (team == null) {
            sendFeedback(ctx, "Team not found!", MessageFormatter.ERROR_COLOR);
            return 0;
        }

        CakeCommands.LOGGER.info("Force deleting team with name {}", teamName);
        getScoreboard(ctx).removeTeam(team);
        sendFeedback(ctx, "Team deleted successfully!", MessageFormatter.SUCCESS_COLOR);
        return 1;
    }

    public static int help(CommandContext<ServerCommandSource> ctx) {
        var helpText = Text.literal("Commands:\n")
                .setStyle(Style.EMPTY.withColor(MessageFormatter.INFO_COLOR))
                .append(
                        Text.literal("/cteam create <name> <color>")
                                .setStyle(Style.EMPTY.withColor(MessageFormatter.INFO_COLOR))
                                .append(
                                        Text.literal(" - Create a new team!")
                                                .setStyle(Style.EMPTY.withColor(MessageFormatter.WARNING_COLOR))
                                )
                )
                .append(
                        Text.literal("\n/cteam delete")
                                .setStyle(Style.EMPTY.withColor(MessageFormatter.INFO_COLOR))
                                .append(
                                        Text.literal(" - Delete your team!")
                                                .setStyle(Style.EMPTY.withColor(MessageFormatter.WARNING_COLOR))
                                )
                )
                .append(
                        Text.literal("\n/cteam edit color <color>")
                                .setStyle(Style.EMPTY.withColor(MessageFormatter.INFO_COLOR))
                                .append(
                                        Text.literal(" - Change your team's color!")
                                                .setStyle(Style.EMPTY.withColor(MessageFormatter.WARNING_COLOR))
                                )
                )
                .append(
                        Text.literal("\n/cteam edit name <name>")
                                .setStyle(Style.EMPTY.withColor(MessageFormatter.INFO_COLOR))
                                .append(
                                        Text.literal(" - Change your team's name!")
                                                .setStyle(Style.EMPTY.withColor(MessageFormatter.WARNING_COLOR))
                                )
                )
                .append(
                        Text.literal("\n/cteam invite <player>")
                                .setStyle(Style.EMPTY.withColor(MessageFormatter.INFO_COLOR))
                                .append(
                                        Text.literal(" - Invite a player to your team!")
                                                .setStyle(Style.EMPTY.withColor(MessageFormatter.WARNING_COLOR))
                                )
                )
                .append(
                        Text.literal("\n/cteam kick <player>")
                                .setStyle(Style.EMPTY.withColor(MessageFormatter.INFO_COLOR))
                                .append(
                                        Text.literal(" - Kick a player from your team!")
                                                .setStyle(Style.EMPTY.withColor(MessageFormatter.WARNING_COLOR))
                                )
                )
                .append(
                        Text.literal("\n/cteam list")
                                .setStyle(Style.EMPTY.withColor(MessageFormatter.INFO_COLOR))
                                .append(
                                        Text.literal(" - List all teams!")
                                                .setStyle(Style.EMPTY.withColor(MessageFormatter.WARNING_COLOR))
                                )
                )
                .append(
                        Text.literal("\n/cteam leave")
                                .setStyle(Style.EMPTY.withColor(MessageFormatter.INFO_COLOR))
                                .append(
                                        Text.literal(" - Leave your team!")
                                                .setStyle(Style.EMPTY.withColor(MessageFormatter.WARNING_COLOR))
                                )
                )
                .append(
                        Text.literal("\n/cteam info <team>")
                                .setStyle(Style.EMPTY.withColor(MessageFormatter.INFO_COLOR))
                                .append(
                                        Text.literal(" - Get info about a team!")
                                                .setStyle(Style.EMPTY.withColor(MessageFormatter.WARNING_COLOR))
                                )
                )
                .append(
                        Text.literal("\n\nCakeCommands by ")
                                .setStyle(Style.EMPTY.withColor(MessageFormatter.INFO_COLOR))
                                .append(
                                        Text.literal("HaiiroMajo")
                                                .setStyle(Style.EMPTY.withColor(MessageFormatter.WARNING_COLOR))
                                                .append(
                                                        Text.literal(" with ")
                                                                .setStyle(Style.EMPTY.withColor(MessageFormatter.INFO_COLOR))
                                                                .append(
                                                                        Text.literal("<3")
                                                                                .setStyle(Style.EMPTY.withColor(MessageFormatter.ERROR_COLOR))
                                                                )
                                                )

                                )
                );

        sendFeedback(ctx, helpText);

        return 1;
    }

    public static int privateMessageTeam(CommandContext<ServerCommandSource> ctx) {
        var player = getPlayer(ctx);
        if (player == null) {
            sendFeedback(ctx, "You must be a player to send a message to your team!", MessageFormatter.ERROR_COLOR);
            return 0;
        }

        CakeCommands.LOGGER.info("Sending message to team from player {}", player.getName().getString());

        var message = ctx.getArgument("message", String.class);
        CakeCommands.LOGGER.info("message: {}", message);
        if (message == null || message.isEmpty()) {
            sendFeedback(ctx, "Message cannot be empty!", MessageFormatter.ERROR_COLOR);
            return 0;
        }

        var teamStream = getScoreboard(ctx).getTeams()
                .stream()
                .filter(t -> t.getName().startsWith("cteam"));

        Team team = null;
        for (Team t : teamStream.toList()) {
            if (t.getPlayerList().contains(player.getName().getString())) {
                team = t;
                break;
            }
        }

        if (team == null) {
            sendFeedback(ctx, "You are not in a team!", MessageFormatter.ERROR_COLOR);
            return 0;
        }

        var members = team.getPlayerList();

        var teamColor = team.getPrefix().getStyle().getColor();
        var messageText = Text.literal("[" + team.getDisplayName().getString() + "] ")
                .setStyle(Style.EMPTY.withColor(teamColor))
                .append(Text.literal("<" + player.getName().getString() + "> " + message)
                        .setStyle(Style.EMPTY.withColor(MessageFormatter.WHITE)));

        for (String p : members) {
            ServerPlayerEntity playerEntity = ctx.getSource().getServer().getPlayerManager().getPlayer(p);
            if (playerEntity != null) {
                sendToPlayer(ctx, playerEntity, messageText);
            }
        }

        return 1;
    }
}
