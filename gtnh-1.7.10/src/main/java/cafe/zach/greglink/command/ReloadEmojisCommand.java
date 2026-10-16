package cafe.zach.greglink.command;

import java.util.Collections;
import java.util.List;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import cafe.zach.discord.api.action.format.EmojiRegistry;

public class ReloadEmojisCommand extends CommandBase {

    private volatile long lastReloadTime = 0;

    @Override
    public String getCommandName() {
        return "emoji_reload";
    }

    @Override
    public List<String> getCommandAliases() {
        return Collections.singletonList("reload_emoji");
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/emoji_reload";
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return true;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        long now = System.currentTimeMillis();
        if (now - lastReloadTime < 5000) {
            sender.addChatMessage(
                new ChatComponentText(
                    EnumChatFormatting.RED + "[GregLink] Please wait a few seconds before refreshing again."));
            return;
        }
        lastReloadTime = now;

        sender.addChatMessage(
            new ChatComponentText(EnumChatFormatting.GRAY + "[GregLink] Fetching updated emojis from Discord..."));

        EmojiRegistry.refreshEmojis(result -> {
            try {
                sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN + "[GregLink] " + result));
            } catch (Throwable ignored) {}
        });
    }
}
