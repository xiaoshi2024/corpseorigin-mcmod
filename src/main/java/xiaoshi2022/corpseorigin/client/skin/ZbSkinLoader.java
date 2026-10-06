package xiaoshi2022.corpseorigin.client.skin;

import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import xiaoshi2022.corpseorigin.entity.LowerLevelZbEntity;

import java.util.UUID;

@Environment(EnvType.CLIENT)
public class ZbSkinLoader {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static void loadSkinAsync(LowerLevelZbEntity entity, String username) {
        if (username == null || username.isEmpty()) {
            setDefaultSkin(entity, username);
            return;
        }

        // ✅ 本地皮肤优先（config/corpseorigin/skins/<folder>/<名>.png）：
        //    命中就不查 Mojang、不做染色，直接用制作者放好的最终效果（漫展尸兄用）
        Identifier local = LocalSkinStore.getTexture(username);
        if (local != null) {
            entity.setSkinTexture(local);
            entity.setSkinState(ZbSkinState.LOADED);
            ZbSkinCache.put(username, local);
            LOGGER.debug("📁 使用本地皮肤: {} -> {}", username, local);
            return;
        }

        // ✅ 缓存命中
        Identifier cached = ZbSkinCache.get(username);
        if (cached != null) {
            entity.setSkinTexture(cached);
            entity.setSkinState(ZbSkinState.LOADED);
            LOGGER.debug("♻️ 从缓存加载皮肤: {} -> {}", username, cached);  // ← debug
            return;
        }

        entity.setSkinState(ZbSkinState.LOADING);
        LOGGER.debug("⏳ 开始异步加载皮肤: {}", username);  // ← debug

        ZbSkinIntegration.getPlayerSkinAsync(username)
                .thenAcceptAsync(skin -> {
                    if (skin != null) {
                        entity.setSkinTexture(skin);
                        entity.setSkinState(ZbSkinState.LOADED);
                        ZbSkinCache.put(username, skin);
//                        LOGGER.info("✅ 皮肤加载成功: {} -> {}", username, skin);  // ← info（首次）
                    } else {
                        LOGGER.warn("⚠️ 皮肤加载失败，使用默认: {}", username);
                        setDefaultSkin(entity, username);
                    }
                }, Minecraft.getInstance())
                .exceptionally(throwable -> {
                    LOGGER.error("❌ 皮肤加载异常: {}", throwable.getMessage());
                    setDefaultSkin(entity, username);
                    return null;
                });
    }

    private static void setDefaultSkin(LowerLevelZbEntity entity, String username) {
        UUID uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes());
        Identifier defaultSkin = DefaultPlayerSkin.get(uuid).body().texturePath();
        entity.setSkinTexture(defaultSkin);
        entity.setSkinState(ZbSkinState.LOADED);
        ZbSkinCache.put(username, defaultSkin);
        LOGGER.debug("⚠️ 使用默认皮肤: {} -> {}", username, defaultSkin);  // ← debug
    }
}