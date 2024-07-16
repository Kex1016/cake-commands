package moe.haiiro.cakecommands.teams;

import net.minecraft.scoreboard.Team;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

public record TeamInvite(Team team, String playerName, LocalDateTime inviteTime, boolean accepted) {
    public TeamInvite {
        if (team == null) {
            throw new IllegalArgumentException("Team cannot be null or empty!");
        }

        if (playerName == null || playerName.isEmpty()) {
            throw new IllegalArgumentException("Player name cannot be null or empty!");
        }

        if (inviteTime == null) {
            throw new IllegalArgumentException("Invite time cannot be null!");
        }
    }

    public long getInviteId() {
        return inviteTime.toEpochSecond(ZoneOffset.UTC);
    }
}
