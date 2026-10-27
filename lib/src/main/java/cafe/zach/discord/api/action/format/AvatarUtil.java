package cafe.zach.discord.api.action.format;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class AvatarUtil {

    private static final float HEAD_SCALE = 0.75f;
    private static final int CANVAS_SIZE = 64;
    private static final int MAX_RESPONSE_BYTES = 8 * 1024;
    private static final int MAX_SOURCE_DIMENSION = 512;
    private static final long MAX_SOURCE_PIXELS = 512L * 512L;
    private static final int MAX_CACHE_SIZE = 256;
    private static final long FAILURE_COOLDOWN_MS = 5 * 60 * 1000L;
    private static final Pattern VALID_IDENTIFIER = Pattern.compile("^[A-Za-z0-9_-]{1,36}+$");

    private static final OkHttpClient CLIENT = new OkHttpClient.Builder().connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.SECONDS)
        .callTimeout(5, TimeUnit.SECONDS)
        .build();

    private static final Map<String, byte[]> CACHE = Collections
        .synchronizedMap(new LinkedHashMap<String, byte[]>(64, 0.75f, true) {

            @Override
            protected boolean removeEldestEntry(Map.Entry<String, byte[]> eldest) {
                return size() > MAX_CACHE_SIZE;
            }
        });
    private static final ConcurrentHashMap<String, Long> FAILED_CACHE = new ConcurrentHashMap<>();

    private AvatarUtil() {}

    private static boolean isValidIdentifier(String identifier) {
        if (identifier == null || identifier.isEmpty()) return false;
        if ("Server".equalsIgnoreCase(identifier) || "Rcon".equalsIgnoreCase(identifier)) return false;
        return VALID_IDENTIFIER.matcher(identifier)
            .matches();
    }

    public static byte[] getCachedAvatar(String identifier) {
        if (!isValidIdentifier(identifier)) return null;
        return CACHE.get(identifier);
    }

    public static byte[] fetchPaddedAvatar(String identifier) {
        if (!isValidIdentifier(identifier)) return null;

        byte[] cached = CACHE.get(identifier);
        if (cached != null) return cached;

        Long failedAt = FAILED_CACHE.get(identifier);
        if (failedAt != null && (System.currentTimeMillis() - failedAt) < FAILURE_COOLDOWN_MS) {
            return null;
        }

        byte[] result = fetch(identifier);
        if (result != null) {
            CACHE.put(identifier, result);
            FAILED_CACHE.remove(identifier);
        } else {
            if (FAILED_CACHE.size() >= MAX_CACHE_SIZE) {
                FAILED_CACHE.clear();
            }
            FAILED_CACHE.put(identifier, System.currentTimeMillis());
        }
        return result;
    }

    public static void invalidate(String identifier) {
        if (identifier == null) return;
        CACHE.remove(identifier);
        FAILED_CACHE.remove(identifier);
    }

    public static void clearCache() {
        CACHE.clear();
        FAILED_CACHE.clear();
    }

    private static byte[] fetch(String identifier) {
        int headSize = Math.round(CANVAS_SIZE * HEAD_SCALE);
        String url = String.format("https://mc-heads.net/avatar/%s/%d", identifier, headSize);

        Request request = new Request.Builder().url(url)
            .header("User-Agent", "GregLink-DiscordBot/1.0")
            .build();

        try (Response response = CLIENT.newCall(request)
            .execute()) {
            if (!response.isSuccessful() || response.body() == null) return null;

            long contentLength = response.body()
                .contentLength();
            if (contentLength > MAX_RESPONSE_BYTES) return null;

            byte[] imageBytes = readBounded(response.body().byteStream());
            if (imageBytes == null) return null;

            BufferedImage head = readBoundedImage(imageBytes);
            if (head == null) return null;

            int offset = (CANVAS_SIZE - headSize) / 2;
            BufferedImage canvas = new BufferedImage(CANVAS_SIZE, CANVAS_SIZE, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = canvas.createGraphics();
            g.drawImage(head, offset, offset, headSize, headSize, null);
            g.dispose();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(canvas, "png", out);
            return out.toByteArray();

        } catch (Exception e) {
            return null;
        }
    }

    private static byte[] readBounded(InputStream stream) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int total = 0;
        int read;
        while ((read = stream.read(buffer)) != -1) {
            total += read;
            if (total > MAX_RESPONSE_BYTES) return null;
            out.write(buffer, 0, read);
        }
        return out.toByteArray();
    }

    private static BufferedImage readBoundedImage(byte[] imageBytes) throws IOException {
        try (ImageInputStream imageInput = ImageIO.createImageInputStream(new ByteArrayInputStream(imageBytes))) {
            if (imageInput == null) return null;

            Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInput);
            if (!readers.hasNext()) return null;

            ImageReader reader = readers.next();
            try {
                reader.setInput(imageInput, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (
                    width <= 0 || height <= 0 || width > MAX_SOURCE_DIMENSION || height > MAX_SOURCE_DIMENSION
                        || (long) width * height > MAX_SOURCE_PIXELS
                ) return null;

                return reader.read(0);
            } finally {
                reader.dispose();
            }
        }
    }
}
