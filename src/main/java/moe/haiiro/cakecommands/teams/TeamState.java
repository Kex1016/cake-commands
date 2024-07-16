package moe.haiiro.cakecommands.teams;

import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;

import java.util.List;

/**
 * Represents the state of a team.
 */
public record TeamState(String name, String color, String owner) {
    public TeamState {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Name cannot be null or empty!");
        }

        if (color == null || color.isEmpty()) {
            color = "white";
        }

        if (owner == null || owner.isEmpty()) {
            owner = "Server";
        }

        if (name.length() > 4) {
            throw new IllegalArgumentException("Name cannot be longer than 4 characters!");
        }

        // Test if name has special characters
        if (!name.matches("^[a-zA-Z0-9]*$")) {
            throw new IllegalArgumentException("Name cannot contain special characters!");
        }

        // Test for a valid color (either minecraft color or hex color)
        var colors = List.of("black",
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
                "white");
        if (!colors.contains(color) && !color.matches("^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$")) {
            throw new IllegalArgumentException("Invalid color! Must be a valid minecraft color or hex color.");
        }
    }

    public TeamState(String name, String color) {
        this(name, color, "Server");
    }

    public TeamState(String name) {
        this(name, "white", "Server");
    }

    public String getTeamId() {
        return "cteam_" + name() + "_" + owner();
    }

    public String getFormattedName() {
        return name + " ";
    }

    public int getRgbColor() {
        // If the color is a hex color, return it as "0x" + the hex color
        if (color.matches("^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$")) {
            return Integer.parseInt(color.substring(1), 16);
        }
        return 0;
    }

    public TextColor getMinecraftColor() {
        if (color.matches("^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$")) {
            return TextColor.fromRgb(getRgbColor());
        }

        for (Formatting formatting : Formatting.values()) {
            if (formatting.getName().toLowerCase().equals(color)) {
                return TextColor.fromFormatting(formatting);
            }
        }
        return TextColor.fromFormatting(Formatting.WHITE);
    }

    public String getRawColor() {
        return color;
    }

    @Override
    public String toString() {
        return "TeamState{" +
                "name='" + name + '\'' +
                ", color='" + color + '\'' +
                ", owner='" + owner + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        TeamState teamState = (TeamState) obj;
        return name.equals(teamState.name) && color.equals(teamState.color) && owner.equals(teamState.owner);
    }
}
