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

        // 检查缓存
        Identifier cached = ZbSkinCache.get(username);
        if (cached != null) {
            entity.setSkinTexture(cached);
            entity.setSkinState(ZbSkinState.LOADED);
            LOGGER.debug("✅ 从缓存加载皮肤: {}", username);
            return;
        }

        entity.setSkinState(ZbSkinState.LOADING);

        // ✅ 使用异步方法加载
        ZbSkinIntegration.getPlayerSkinAsync(username).thenAcceptAsync(skin -> {
            if (skin != null) {
                entity.setSkinTexture(skin);
                entity.setSkinState(ZbSkinState.LOADED);
                ZbSkinCache.put(username, skin);
                LOGGER.info("✅ 皮肤加载成功: {}", username);
            } else {
                setDefaultSkin(entity, username);
            }
        }, Minecraft.getInstance());
    }

    private static void setDefaultSkin(LowerLevelZbEntity entity, String username) {
        UUID uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes());
        Identifier defaultSkin = DefaultPlayerSkin.get(uuid).body().texturePath();
        entity.setSkinTexture(defaultSkin);
        entity.setSkinState(ZbSkinState.LOADED);
        ZbSkinCache.put(username, defaultSkin);
        LOGGER.info("⚠️ 使用默认皮肤: {}", username);
    }
}