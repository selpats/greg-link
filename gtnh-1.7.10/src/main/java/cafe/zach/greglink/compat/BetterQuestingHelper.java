package cafe.zach.greglink.compat;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.util.StatCollector;

import cpw.mods.fml.common.Loader;

public class BetterQuestingHelper {

    private static final Pattern SHARE_PATTERN = Pattern.compile("betterquesting\\.msg\\.sharequest:([A-Za-z0-9_=-]+)");
    private static Boolean bqAvailable = null;

    public static String formatQuestShare(String message) {
        if (message == null || !message.contains("betterquesting.msg.sharequest:")) {
            return message;
        }

        try {
            Matcher matcher = SHARE_PATTERN.matcher(message);
            StringBuffer sb = new StringBuffer();
            while (matcher.find()) {
                String rawId = matcher.group(1);
                UUID questId = decodeUuid(rawId);
                String questName = null;
                if (questId != null) {
                    questName = getQuestName(questId);
                }

                String replacement;
                String cleanName = cleanFormatting(questName);
                if (!cleanName.isEmpty()) {
                    replacement = "[Quest: " + cleanName + "]";
                } else if (questId != null && questId.getMostSignificantBits() == 0L) {
                    replacement = "[Quest: #" + questId.getLeastSignificantBits() + "]";
                } else {
                    replacement = "[Quest: #" + rawId + "]";
                }

                matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
            }
            matcher.appendTail(sb);
            return sb.toString();
        } catch (Throwable t) {
            return message;
        }
    }

    public static UUID decodeUuid(String string) {
        if (string == null || string.isEmpty()) return null;

        try {
            byte[] bytes = Base64.getUrlDecoder()
                .decode(string);
            if (bytes.length >= 16) {
                ByteBuffer bb = ByteBuffer.wrap(bytes);
                return new UUID(bb.getLong(), bb.getLong());
            }
        } catch (Throwable ignored) {}

        try {
            byte[] bytes = Base64.getDecoder()
                .decode(string);
            if (bytes.length >= 16) {
                ByteBuffer bb = ByteBuffer.wrap(bytes);
                return new UUID(bb.getLong(), bb.getLong());
            }
        } catch (Throwable ignored) {}

        return null;
    }

    public static String getQuestName(UUID questId) {
        if (questId == null) return null;

        if (bqAvailable == null) {
            try {
                bqAvailable = Loader.isModLoaded("betterquesting");
            } catch (Throwable t) {
                bqAvailable = false;
            }
        }
        if (!bqAvailable) return null;

        try {
            // betterquesting.questing.QuestDatabase.INSTANCE
            Class<?> qdbClass = Class.forName("betterquesting.questing.QuestDatabase");
            Field instanceField = qdbClass.getField("INSTANCE");
            Object qdb = instanceField.get(null);
            Method getMethod = qdbClass.getMethod("get", Object.class);
            Object quest = getMethod.invoke(qdb, questId);
            if (quest == null) return null;

            // Try QuestTranslation.translateQuestName(UUID, IQuest)
            try {
                Class<?> qtClass = Class.forName("betterquesting.api2.utils.QuestTranslation");
                Class<?> iquestClass = Class.forName("betterquesting.api.questing.IQuest");
                Method translateMethod = qtClass.getMethod("translateQuestName", UUID.class, iquestClass);
                String translated = (String) translateMethod.invoke(null, questId, quest);
                if (translated != null && !translated.trim()
                    .isEmpty()) {
                    return translated;
                }
            } catch (Throwable ignored) {}

            // Fallback: quest.getProperty(NativeProps.NAME)
            try {
                Class<?> npClass = Class.forName("betterquesting.api.properties.NativeProps");
                Field nameField = npClass.getField("NAME");
                Object nameProp = nameField.get(null);
                Class<?> propTypeClass = Class.forName("betterquesting.api.properties.IPropertyType");
                Method getPropMethod = quest.getClass()
                    .getMethod("getProperty", propTypeClass);
                String rawName = (String) getPropMethod.invoke(quest, nameProp);
                if (rawName != null && !rawName.trim()
                    .isEmpty()) {
                    return StatCollector.translateToLocal(rawName);
                }
            } catch (Throwable ignored) {}

        } catch (Throwable ignored) {}

        return null;
    }

    public static String cleanFormatting(String text) {
        if (text == null) return "";
        return text.replaceAll("(?i)\\u00a7[0-9a-fk-or]", "")
            .trim();
    }
}
