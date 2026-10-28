package cafe.zach.greglink.command;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import cafe.zach.discord.api.action.format.EmojiRegistry;
import cafe.zach.greglink.links.AccountLinkManager;

public class GregLinkCommand extends CommandBase {

    private volatile long lastEmojiReloadTime;

    @Override
    public String getCommandName() {
        return "greglink";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/greglink <link|unlink>";
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return true;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (!(sender instanceof EntityPlayerMP)) {
            send(sender, EnumChatFormatting.RED, "This command can only be used by a player.");
            return;
        }
        if (args.length != 1) {
            send(sender, EnumChatFormatting.YELLOW, getCommandUsage(sender));
            return;
        }

        EntityPlayerMP player = (EntityPlayerMP) sender;
        UUID minecraftUuid = player.getUniqueID();
        if ("link".equalsIgnoreCase(args[0])) {
            String code = AccountLinkManager.createCode(minecraftUuid, player.getCommandSenderName());
            if (code == null) {
                send(sender, EnumChatFormatting.RED, "Please wait before requesting another link code.");
                return;
            }
            send(
                sender,
                EnumChatFormatting.GREEN,
                "DM the GregLink bot this one-time code: " + EnumChatFormatting.GOLD
                    + code
                    + EnumChatFormatting.GREEN
                    + ". It expires in 10 minutes.");
            return;
        }

        if ("unlink".equalsIgnoreCase(args[0])) {
            if (!AccountLinkManager.isLinked(minecraftUuid)) {
                send(sender, EnumChatFormatting.YELLOW, "Your Minecraft account is not linked.");
            } else if (AccountLinkManager.unlink(minecraftUuid)) {
                send(sender, EnumChatFormatting.GREEN, "Your Minecraft and Discord accounts are unlinked.");
            } else {
                send(sender, EnumChatFormatting.RED, "The unlink could not be saved. Check the GregLink log.");
            }
            return;
        }

        if ("emoji_reload".equalsIgnoreCase(args[0])) {
            long now = System.currentTimeMillis();
            if (now - lastEmojiReloadTime < 5000) {
                send(sender, EnumChatFormatting.RED, "Please wait a few seconds before refreshing again.");
                return;
            }
            lastEmojiReloadTime = now;
            send(sender, EnumChatFormatting.GRAY, "Fetching updated emojis from Discord...");
            EmojiRegistry.refreshEmojis(result -> {
                try {
                    send(sender, EnumChatFormatting.GREEN, result);
                } catch (Throwable ignored) {}
            });
            return;
        }

        send(sender, EnumChatFormatting.YELLOW, getCommandUsage(sender));
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "link", "unlink", "emoji_reload");
        }
        return Collections.emptyList();
    }

    private static void send(ICommandSender sender, EnumChatFormatting color, String message) {
        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GRAY + "[GregLink] " + color + message));
    }
}
