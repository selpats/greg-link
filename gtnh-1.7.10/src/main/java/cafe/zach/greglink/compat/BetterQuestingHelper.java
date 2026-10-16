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

    private static final Pattern SHARE_PATTERN = Pattern
        .compile("betterquesting\\.msg\\.sharequest:([A-Za-z0-9_=-]++)");
    private static final Pattern FORMATTING_PATTERN = Pattern.compile("(?i)\\u00a7[0-9a-fk-or]");
    private static volatile Boolean bqAvailable = null;
    private static volatile boolean reflectionInitialized = false;
    private static Object qdbInstance;
    private static Method qdbGetMethod;
    private static Method translateQuestNameMethod;
    private static Object nativePropsName;
    private static Method iquestGetPropertyMethod;

    public static String formatQuestShare(String message) {
        if (message == null || !message.contains("betterquesting.msg.sharequest:")) {
            return message;
        }

        try {
            Matcher matcher = SHARE_PATTERN.matcher(message);
            StringBuffer sb = null;
            while (matcher.find()) {
                if (sb == null) {
                    sb = new StringBuffer(message.length() + 16);
                }
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
            if (sb == null) {
                return message;
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
            if (bytes.length != 16) return null;
            ByteBuffer bb = ByteBuffer.wrap(bytes);
            return new UUID(bb.getLong(), bb.getLong());
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static synchronized void initReflection() {
        if (reflectionInitialized) return;
        try {
            Class<?> qdbClass = Class.forName("betterquesting.questing.QuestDatabase");
            Field instanceField = qdbClass.getField("INSTANCE");
            qdbInstance = instanceField.get(null);
            try {
                qdbGetMethod = qdbClass.getMethod("get", UUID.class);
            } catch (NoSuchMethodException e) {
                qdbGetMethod = qdbClass.getMethod("get", Object.class);
            }

            Class<?> iquestClass = Class.forName("betterquesting.api.questing.IQuest");
            try {
                Class<?> qtClass = Class.forName("betterquesting.api2.utils.QuestTranslation");
                translateQuestNameMethod = qtClass.getMethod("translateQuestName", UUID.class, iquestClass);
            } catch (Throwable ignored) {}

            try {
                Class<?> npClass = Class.forName("betterquesting.api.properties.NativeProps");
                Field nameField = npClass.getField("NAME");
                nativePropsName = nameField.get(null);
                Class<?> propTypeClass = Class.forName("betterquesting.api.properties.IPropertyType");
                iquestGetPropertyMethod = iquestClass.getMethod("getProperty", propTypeClass);
            } catch (Throwable ignored) {}
        } catch (Throwable ignored) {} finally {
            reflectionInitialized = true;
        }
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

        if (!reflectionInitialized) {
            initReflection();
        }
        if (qdbInstance == null || qdbGetMethod == null) return null;

        try {
            Object quest = qdbGetMethod.invoke(qdbInstance, questId);
            if (quest == null) return null;

            if (translateQuestNameMethod != null) {
                try {
                    String translated = (String) translateQuestNameMethod.invoke(null, questId, quest);
                    if (translated != null && !translated.trim()
                        .isEmpty()) {
                        return translated;
                    }
                } catch (Throwable ignored) {}
            }

            if (iquestGetPropertyMethod != null && nativePropsName != null) {
                try {
                    String rawName = (String) iquestGetPropertyMethod.invoke(quest, nativePropsName);
                    if (rawName != null && !rawName.trim()
                        .isEmpty()) {
                        return StatCollector.translateToLocal(rawName);
                    }
                } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}

        return null;
    }

    public static String cleanFormatting(String text) {
        if (text == null || text.isEmpty()) return "";
        if (text.indexOf('\u00a7') == -1) return text.trim();
        return FORMATTING_PATTERN.matcher(text)
            .replaceAll("")
            .trim();
    }
}
