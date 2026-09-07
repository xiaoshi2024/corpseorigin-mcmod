package xiaoshi2022.corpseorigin.client.skin;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ZbSkinCache {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<String, CacheEntry> CACHE = new ConcurrentHashMap<>();
    private static final long CACHE_EXPIRE_TIME = 30 * 60 * 1000;

    private static class CacheEntry {
        final Identifier texture;
        final long timestamp;

        CacheEntry(Identifier texture) {
            this.texture = texture;
            this.timestamp = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_EXPIRE_TIME;
        }
    }

    public static Identifier get(String username) {
        if (username == null) return null;
        CacheEntry entry = CACHE.get(username);
        if (entry != null && !entry.isExpired()) {
            return entry.texture;
        }
        if (entry != null) {
            CACHE.remove(username);
        }
        return null;
    }

    public static void put(String username, Identifier texture) {
        if (username != null && texture != null) {
            CACHE.put(username, new CacheEntry(texture));
        }
    }

    public static void clear(String username) {
        CACHE.remove(username);
    }

    public static void clearAll() {
        CACHE.clear();
    }

    public static int getCacheSize() {
        return CACHE.size();
    }
}