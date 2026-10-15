package cafe.zach.discord.listeners;

import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import org.jetbrains.annotations.NotNull;

import cafe.zach.discord.api.action.context.DiscordContext;
import cafe.zach.discord.api.action.format.EmojiRegistry;
import cafe.zach.discord.api.action.registry.ActionRegistry;

public class ReadyListener extends ListenerAdapter {

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        EmojiRegistry.loadEmojis(event.getJDA());

        DiscordContext context = DiscordContext.forReady(event.getGuildTotalCount());
        ActionRegistry.fireDiscord(ActionRegistry.ON_DISCORD_READY, context);
    }
}
