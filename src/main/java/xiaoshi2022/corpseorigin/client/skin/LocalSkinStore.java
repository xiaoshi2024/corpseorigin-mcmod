package xiaoshi2022.corpseorigin.client.skin;

import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import xiaoshi2022.corpseorigin.skin.LocalSkinNames;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 本地皮肤纹理加载（仅客户端）：把 {@code config/corpseorigin/skins/<folder>/<名>.png}
 * 读成 {@link DynamicTexture} 注册进纹理管理器，返回可用的 {@link Identifier}。
 * <p>
 * 命中本地皮肤（{@link LocalSkinNames#isLocal}）时由 {@code ZbSkinLoader} 优先走这里，
 * <b>不再查 Mojang API、不再触发皮肤染色</b>——本地皮肤就是制作者预定的最终效果。
 * <p>
 * 支持 64×32 旧版皮肤（自动转 64×64，与 ZbSkinIntegration 同一套 {@link SkinProcessor}）。
 */
@Environment(EnvType.CLIENT)
public final class LocalSkinStore {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** 皮肤名 → 已注册纹理。进程级缓存（文件改了重启游戏生效，与 Java 代码同约定）。 */
    private static final Map<String, Identifier> CACHE = new ConcurrentHashMap<>();

    private LocalSkinStore() {
    }

    /**
     * 取本地皮肤纹理；名字不在本地名单或读取失败返回 {@code null}（调用方回退正常流程）。
     */
    public static Identifier getTexture(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        Identifier cached = CACHE.get(name);
        if (cached != null) {
            return cached;
        }
        Path file = LocalSkinNames.resolveFile(name);
        if (file == null) {
            return null;
        }
        try (InputStream in = Files.newInputStream(file)) {
            com.mojang.blaze3d.platform.NativeImage image = com.mojang.blaze3d.platform.NativeImage.read(in);
            if (image.getWidth() == 64 && image.getHeight() == 32) {
                image = SkinProcessor.convertLegacySkin(image);
            }

            Identifier location = Identifier.fromNamespaceAndPath(
                    "corpseorigin", "localskin/" + sanitize(name));
            // ⚠️ 不要先 getTexture(location) 判空：原版语义是"没注册就按资源加载并注册一张
            //    加载失败的 SimpleTexture（missing 棋盘）并返回非 null"，会把真注册永久挡住。
            //    直接 register（同 id 会替换旧纹理）；重复注册已由 CACHE 挡住。
            Minecraft.getInstance().getTextureManager()
                    .register(location, new DynamicTexture(() -> location.toString(), image));
            CACHE.put(name, location);
            LOGGER.info("✅ 本地皮肤已注册: {} -> {}", name, location);
            return location;
        } catch (Exception e) {
            LOGGER.warn("⚠️ 本地皮肤读取失败: {} ({}): {}", name, file, e.toString());
            return null;
        }
    }

    /** 注册用的路径名：只保留 a-z 0-9 _ - .，其余换成 _，再挂到 localskin/ 下。 */
    private static String sanitize(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        StringBuilder sb = new StringBuilder();
        for (char c : lower.toCharArray()) {
            sb.append((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')
                    || c == '_' || c == '-' || c == '.' ? c : '_');
        }
        return sb.toString();
    }
}
