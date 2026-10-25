package cafe.zach.discord.api.action.registry;

import java.util.*;

import cafe.zach.discord.api.action.IDiscordAction;
import cafe.zach.discord.api.action.IMinecraftAction;
import cafe.zach.discord.api.action.context.DiscordContext;
import cafe.zach.discord.api.action.context.MinecraftContext;

public class ActionRegistry {

    // Built-in event keys
    public static final String ON_DISCORD_READY = "on_discord_ready";
    public static final String ON_DISCORD_MESSAGE = "on_discord_message";
    public static final String ON_DISCORD_MEMBER_JOIN = "on_discord_member_join";
    public static final String ON_DISCORD_MEMBER_LEAVE = "on_discord_member_leave";

    public static final String ON_MINECRAFT_CHAT = "on_minecraft_chat";
    public static final String ON_MINECRAFT_COMMAND = "on_minecraft_command";
    public static final String ON_MINECRAFT_PLAYER_JOIN = "on_minecraft_player_join";
    public static final String ON_MINECRAFT_PLAYER_LEAVE = "on_minecraft_player_leave";
    public static final String ON_MINECRAFT_SERVER_STARTING = "on_minecraft_server_starting";
    public static final String ON_MINECRAFT_SERVER_STARTED = "on_minecraft_server_started";
    public static final String ON_MINECRAFT_SERVER_STOPPING = "on_minecraft_server_stopping";
    public static final String ON_MINECRAFT_SERVER_STOPPED = "on_minecraft_server_stopped";
    public static final String ON_MINECRAFT_ACHIEVEMENT = "on_minecraft_achievement";
    public static final String ON_MINECRAFT_DEATH = "on_minecraft_death";
    public static final String ON_MINECRAFT_AFK = "on_minecraft_afk";

    private static final Map<String, List<IDiscordAction>> discordActions = new LinkedHashMap<>();
    private static final Map<String, List<IMinecraftAction>> minecraftActions = new LinkedHashMap<>();

    // Discord -> Minecraft
    public static void register(String event, IDiscordAction action) {
        discordActions.computeIfAbsent(event, k -> new ArrayList<>())
            .add(action);
    }

    public static List<IDiscordAction> getDiscordActions(String event) {
        return discordActions.getOrDefault(event, Collections.emptyList());
    }

    // Minecraft -> Discord
    public static void register(String event, IMinecraftAction action) {
        minecraftActions.computeIfAbsent(event, k -> new ArrayList<>())
            .add(action);
    }

    public static List<IMinecraftAction> getMinecraftActions(String event) {
        return minecraftActions.getOrDefault(event, Collections.emptyList());
    }

    public static void fireMinecraft(String event, MinecraftContext context) {
        for (IMinecraftAction action : getMinecraftActions(event)) {
            action.execute(context);
        }
    }

    public static void fireDiscord(String event, DiscordContext context) {
        for (IDiscordAction action : getDiscordActions(event)) {
            action.execute(context);
        }
    }

    public static void clear() {
        discordActions.clear();
        minecraftActions.clear();
    }
}
