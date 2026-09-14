package xiaoshi2022.corpseorigin.client.skin.clone;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.world.entity.player.PlayerSkin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 客户端玩家皮肤缓存。
 * <p>
 * 克隆体/克隆仓假人渲染时，如果主人已经离线，
 * {@code connection.getPlayerInfo(uuid)} 会返回 null，导致皮肤丢失。
 * 这里把每次成功解析到的 PlayerSkin 按 UUID 缓存下来，
 * 离线时直接读缓存，避免回退到默认皮肤。
 */
public final class ClientSkinCache {

    private static final Map<UUID, PlayerSkin> CACHE = new ConcurrentHashMap<>();

    private ClientSkinCache() {
    }

    /**
     * 解析玩家皮肤：优先从连接取，取到就缓存；取不到就读缓存；再没有就返回默认皮肤。
     */
    public static PlayerSkin resolve(UUID uuid) {
        if (uuid == null) {
            return DefaultPlayerSkin.getDefaultSkin();
        }

        var conn = Minecraft.getInstance().getConnection();
        if (conn != null) {
            var info = conn.getPlayerInfo(uuid);
            if (info != null) {
                PlayerSkin skin = info.getSkin();
                CACHE.put(uuid, skin);
                return skin;
            }
        }

        PlayerSkin cached = CACHE.get(uuid);
        return cached != null ? cached : DefaultPlayerSkin.getDefaultSkin();
    }

    /** 主动写入缓存（例如从网络包收到皮肤数据时）。 */
    public static void put(UUID uuid, PlayerSkin skin) {
        if (uuid != null && skin != null) {
            CACHE.put(uuid, skin);
        }
    }

    /** 玩家真正退出、确定不再需要时清理，避免长期占用。 */
    public static void invalidate(UUID uuid) {
        if (uuid != null) {
            CACHE.remove(uuid);
        }
    }

    public static void clear() {
        CACHE.clear();
    }
}