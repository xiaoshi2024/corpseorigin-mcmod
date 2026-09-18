package xiaoshi2022.corpseorigin.client.skin;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.SkinManager;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerSkin;
import org.slf4j.Logger;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Environment(EnvType.CLIENT)
public class ZbSkinIntegration {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean cslLoaded = false;

    /** Mojang 公开接口：名字 → 真实 UUID */
    private static final String MOJANG_NAME_TO_UUID =
            "https://api.mojang.com/users/profiles/minecraft/";
    /** Mojang 公开接口：UUID → 玩家档案（properties 里有 base64 的 textures，含皮肤地址） */
    private static final String MOJANG_PROFILE =
            "https://sessionserver.mojang.com/session/minecraft/profile/";

    static {
        try {
            cslLoaded = FabricLoader.getInstance().isModLoaded("customskinloader");
            if (cslLoaded) {
                LOGGER.info("✅ CustomSkinLoader 已安装（将自动拦截皮肤加载）");
            }
        } catch (Exception e) {
            cslLoaded = false;
            LOGGER.warn("CustomSkinLoader 检测失败: {}", e.getMessage());
        }
    }

    /**
     * 通过 Minecraft 的 SkinManager 触发 CSL 加载皮肤
     */
    public static CompletableFuture<Identifier> getPlayerSkinAsync(String username) {
        if (username == null || username.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                UUID fakeUuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes());
                GameProfile profile = new GameProfile(fakeUuid, username);

                SkinManager skinManager = Minecraft.getInstance().getSkinManager();

                // ✅ 方式1：使用 get() 方法获取皮肤（异步）
                CompletableFuture<Optional<PlayerSkin>> skinFuture = skinManager.get(profile);

                // 等待加载完成
                Optional<PlayerSkin> skinOptional = skinFuture.join();

                if (skinOptional.isPresent()) {
                    PlayerSkin skin = skinOptional.get();
                    Identifier texturePath = skin.body().texturePath();
                    LOGGER.info("📥 SkinManager 加载皮肤: {} -> {}", username, texturePath);
                    return texturePath;
                }

                // 方式2：如果上面的方法失败，尝试直接调用 CSL API
                if (cslLoaded) {
                    try {
                        Class<?> cslClass = Class.forName("customskinloader.CustomSkinLoader");
                        Object userProfile = cslClass.getMethod("loadProfile", GameProfile.class)
                                .invoke(null, profile);

                        if (userProfile != null) {
                            String skinUrl = (String) userProfile.getClass().getField("skinUrl").get(userProfile);
                            if (skinUrl != null && !skinUrl.isEmpty()) {
                                LOGGER.info("📥 CSL API 找到皮肤: {} -> {}", username, skinUrl);
                                return downloadAndRegisterSkin(skinUrl, username);
                            }
                        }
                    } catch (Exception e) {
                        LOGGER.warn("CSL API 调用失败: {}", e.getMessage());
                    }
                }

                // 方式3：自己按名字查 —— 不依赖 CSL。
                //   原版 SkinManager.get() 是<b>按 UUID</b> 去 sessionserver 查的（内部
                //   sessionService.getPackedTextures(profile)，只用 profile.getId()），
                //   而这里手里只有名字、离线 UUID 又对不上任何账号 —— 所以必须先拿真实 UUID 再取皮肤。
                Identifier resolved = resolveSkinFromMojang(username);
                if (resolved != null) {
                    return resolved;
                }

                // 方式4：回退到默认皮肤
                LOGGER.info("⚠️ 使用默认皮肤: {}", username);
                return DefaultPlayerSkin.get(fakeUuid).body().texturePath();

            } catch (Exception e) {
                LOGGER.warn("获取皮肤失败: {}", e.getMessage());
                return null;
            }
        });
    }

    /**
     * 按名字自己解析皮肤，不依赖 CustomSkinLoader。
     * <p>
     * 两步：{@code api.mojang.com} 把名字换成<b>真实 UUID</b> —— 这一步是关键，
     * 因为原版 {@link SkinManager#get(GameProfile)} 内部只认 {@code profile.getId()}，
     * 拿"离线 UUID"去问永远查不到任何账号；再问 {@code sessionserver.mojang.com} 拿档案里的
     * {@code textures} 属性，解出皮肤地址后下载注册成动态纹理。
     * <p>
     * ⚠️ 这两步都要能连上 Mojang：网络不通（例如国内直连被墙）会返回 {@code null} 退回默认皮肤 ——
     * 那种环境请改用 CustomSkinLoader + 可达的皮肤源（就是上面"方式2"那条路）。
     */
    private static Identifier resolveSkinFromMojang(String username) {
        try {
            String uuidJson = fetchText(MOJANG_NAME_TO_UUID + URLEncoder.encode(username, StandardCharsets.UTF_8));
            if (uuidJson == null) {
                return null;
            }
            String uuid = JsonParser.parseString(uuidJson).getAsJsonObject().get("id").getAsString();

            String profileJson = fetchText(MOJANG_PROFILE + uuid);
            if (profileJson == null) {
                return null;
            }
            return registerSkinFromProfile(profileJson, username);
        } catch (Exception e) {
            LOGGER.warn("按名字解析皮肤失败（{}）: {}", username, e.getMessage());
            return null;
        }
    }

    /** 从档案 JSON 里挖出 {@code textures.SKIN.url}，交给 {@link #downloadAndRegisterSkin} */
    private static Identifier registerSkinFromProfile(String profileJson, String username) {
        JsonObject root = JsonParser.parseString(profileJson).getAsJsonObject();
        if (!root.has("properties")) {
            return null;
        }
        for (JsonElement element : root.getAsJsonArray("properties")) {
            JsonObject property = element.getAsJsonObject();
            if (!property.has("name") || !"textures".equals(property.get("name").getAsString())) {
                continue;
            }
            byte[] decoded = Base64.getDecoder().decode(property.get("value").getAsString());
            JsonObject textures = JsonParser.parseString(new String(decoded, StandardCharsets.UTF_8))
                    .getAsJsonObject().getAsJsonObject("textures");
            if (textures == null || !textures.has("SKIN")) {
                return null;
            }
            String skinUrl = textures.getAsJsonObject("SKIN").get("url").getAsString();
            LOGGER.info("📥 Mojang 接口查到皮肤: {} -> {}", username, skinUrl);
            return downloadAndRegisterSkin(skinUrl, username);
        }
        return null;
    }

    /** 取一段文本；超时 / 非 200 / 断网一律返回 {@code null}，由调用方退回默认皮肤 */
    private static String fetchText(String url) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
            connection.setRequestProperty("User-Agent", "CorpseOrigin");
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(8000);
            if (connection.getResponseCode() != 200) {
                return null;
            }
            try (InputStream stream = connection.getInputStream()) {
                return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * 下载并注册皮肤纹理
     */
    public static Identifier downloadAndRegisterSkin(String skinUrl, String username) {
        try {
            String hash = com.google.common.hash.Hashing.sha1()
                    .hashUnencodedChars(skinUrl)
                    .toString();

            Identifier location = Identifier.fromNamespaceAndPath(
                    "corpseorigin",
                    "skins/" + hash
            );

            TextureManager textureManager = Minecraft.getInstance().getTextureManager();

            if (textureManager.getTexture(location) != null) {
                return location;
            }

            HttpURLConnection connection = (HttpURLConnection) URI.create(skinUrl).toURL().openConnection();
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            connection.setInstanceFollowRedirects(true);
            connection.connect();

            int responseCode = connection.getResponseCode();
            if (responseCode >= 200 && responseCode < 300) {
                try (InputStream inputStream = connection.getInputStream()) {
                    NativeImage image = NativeImage.read(inputStream);

                    if (image.getWidth() == 64 && image.getHeight() == 32) {
                        image = SkinProcessor.convertLegacySkin(image);
                    }

                    final NativeImage finalImage = image;
                    DynamicTexture texture = new DynamicTexture(
                            () -> location.toString(),
                            finalImage
                    );

                    textureManager.register(location, texture);
                    LOGGER.info("✅ 皮肤已注册: {}", location);
                    return location;
                }
            }
            connection.disconnect();

        } catch (Exception e) {
            LOGGER.error("注册皮肤失败: {}", e.getMessage(), e);
        }

        return null;
    }

    public static boolean isCslAvailable() {
        return cslLoaded;
    }
}