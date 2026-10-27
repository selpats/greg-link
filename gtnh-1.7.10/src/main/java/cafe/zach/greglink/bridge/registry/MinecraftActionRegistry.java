package cafe.zach.greglink.bridge.registry;

import cafe.zach.discord.api.action.common.minecraft.*;
import cafe.zach.discord.api.action.registry.ActionRegistry;

public class MinecraftActionRegistry {

    public static void register() {
        ActionRegistry.register(ActionRegistry.ON_MINECRAFT_CHAT, RelayChatToDiscord.create());
        ActionRegistry.register(ActionRegistry.ON_MINECRAFT_PLAYER_JOIN, RelayJoinToDiscord.create());
        ActionRegistry.register(ActionRegistry.ON_MINECRAFT_PLAYER_LEAVE, RelayLeaveToDiscord.create());
        ActionRegistry.register(ActionRegistry.ON_MINECRAFT_SERVER_STARTING, RelayServerStartingToDiscord.create());
        ActionRegistry.register(ActionRegistry.ON_MINECRAFT_SERVER_STARTED, RelayServerStartedToDiscord.create());
        ActionRegistry.register(ActionRegistry.ON_MINECRAFT_SERVER_STOPPING, RelayServerStoppingToDiscord.create());
        ActionRegistry.register(ActionRegistry.ON_MINECRAFT_SERVER_STOPPED, RelayServerStoppedToDiscord.create());
        ActionRegistry.register(ActionRegistry.ON_MINECRAFT_ACHIEVEMENT, RelayAchievementToDiscord.create());
        ActionRegistry.register(ActionRegistry.ON_MINECRAFT_DEATH, RelayDeathToDiscord.create());
        ActionRegistry.register(ActionRegistry.ON_MINECRAFT_AFK, RelayAfkToDiscord.create());
        ActionRegistry.register(ActionRegistry.ON_MINECRAFT_AE2_CRAFT, RelayAe2CraftToDiscord.create());
    }
}
