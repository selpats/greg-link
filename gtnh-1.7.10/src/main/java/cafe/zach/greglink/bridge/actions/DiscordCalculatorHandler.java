package cafe.zach.greglink.bridge.actions;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import cafe.zach.discord.DiscordBridge;
import cafe.zach.discord.api.action.context.DiscordContext;
import cafe.zach.discord.api.config.ChannelMapping;
import cafe.zach.discord.api.config.ConfigHandler;
import cafe.zach.greglink.GregLink;
import cpw.mods.fml.common.Loader;

public final class DiscordCalculatorHandler {

    private static final String OPENMODS_MOD_ID = "OpenMods";
    private static final int MAX_EXPR_LENGTH = 256;
    private static final int MAX_ERROR_LENGTH = 200;

    private static volatile boolean initialized = false;
    private static volatile boolean available = false;

    private static Method createDefaultMethod;
    private static Method compileExecuteAndPrintMethod;
    private static Object exprTypeInfix;

    private DiscordCalculatorHandler() {}

    public static void handleMessage(DiscordContext context) {
        if (context == null || context.isBot || context.content == null) return;

        if (!context.directMessage && !isListeningChannel(context.channelId)) return;

        String trimmed = context.content.trim();
        if (trimmed.length() < 2 || trimmed.charAt(0) != '=' || !Character.isWhitespace(trimmed.charAt(1))) return;

        String expr = trimmed.substring(2)
            .trim();
        if (expr.isEmpty() || expr.length() > MAX_EXPR_LENGTH) return;
        if (!ensureInitialized()) return;

        try {
            Object calculator = createDefaultMethod.invoke(null);
            Object rawResult = compileExecuteAndPrintMethod.invoke(calculator, exprTypeInfix, expr);
            if (rawResult instanceof String) {
                String result = ((String) rawResult).trim();
                if (!result.isEmpty()) {
                    sendReply(context, result);
                }
            }
        } catch (InvocationTargetException e) {
            Throwable cause = e.getTargetException();
            String errorMsg = cause != null ? sanitizeError(cause.getMessage()) : null;
            sendReply(context, errorMsg != null ? "Error: " + errorMsg : "Error: invalid expression");
        } catch (Throwable t) {
            GregLink.LOG.warn("Failed to evaluate Discord calculator expression.", t);
        }
    }

    private static boolean isListeningChannel(String channelId) {
        if (channelId == null) return false;
        ConfigHandler config = ConfigHandler.getInstance();
        if (config == null) return false;
        for (ChannelMapping mapping : config.getListenChannels()) {
            if (channelId.equals(mapping.discordChannelId)) {
                return true;
            }
        }
        return false;
    }

    private static String sanitizeError(String msg) {
        if (msg == null) return null;
        String cleaned = msg.replace('\r', ' ')
            .replace('\n', ' ')
            .trim();
        if (cleaned.isEmpty()) return null;
        if (cleaned.length() > MAX_ERROR_LENGTH) {
            cleaned = cleaned.substring(0, MAX_ERROR_LENGTH) + "...";
        }
        return cleaned;
    }

    private static void sendReply(DiscordContext context, String text) {
        if (context.directMessage) {
            DiscordBridge.sendDirectMessage(context.userId, text);
        } else {
            DiscordBridge.replyToMessage(context.channelId, context.messageId, text);
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static boolean ensureInitialized() {
        if (initialized) return available;
        synchronized (DiscordCalculatorHandler.class) {
            if (initialized) return available;
            try {
                if (!Loader.isModLoaded(OPENMODS_MOD_ID)) {
                    available = false;
                    initialized = true;
                    return false;
                }

                Class<?> factoryClass = Class.forName("openmods.calc.types.fp.DoubleCalculatorFactory");
                createDefaultMethod = factoryClass.getMethod("createDefault");

                Class<?> exprTypeClass = Class.forName("openmods.calc.ExprType");
                exprTypeInfix = Enum.valueOf((Class<Enum>) exprTypeClass, "INFIX");

                Class<?> calculatorClass = Class.forName("openmods.calc.Calculator");
                compileExecuteAndPrintMethod = calculatorClass
                    .getMethod("compileExecuteAndPrint", Object.class, String.class);

                available = true;
            } catch (Throwable t) {
                available = false;
                GregLink.LOG.warn("OpenModsLib calculator not available for Discord expressions.", t);
            } finally {
                initialized = true;
            }
            return available;
        }
    }
}
