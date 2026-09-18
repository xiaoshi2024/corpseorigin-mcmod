package xiaoshi2022.corpseorigin.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import xiaoshi2022.corpseorigin.CorpseOrigin;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 模组配置文件：{@code config/corpseorigin.json}。
 * <p>
 * 第一次启动时自动生成一份带默认值的文件，改完<b>重启游戏 / 服务器</b>生效。
 * 服务端读的是服务端那份（刷怪权重等），客户端读的是客户端那份（皮肤染色），各读各的。
 * <p>
 * 写坏了（字段缺失、类型不对）不会崩：读失败就整体退回默认值，缺的字段单独补默认值。
 */
public final class CorpseConfig {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();
    private static final String FILE_NAME = "corpseorigin.json";

    private static CorpseConfig instance;

    /** 自然生成 */
    public Spawn spawn = new Spawn();
    /** 尸兄的"玩家 ID"来源 */
    public Names names = new Names();
    /** 皮肤表现 */
    public Skin skin = new Skin();

    /** 自然生成的权重与"尸水泉聚集"参数。权重参照原版僵尸 = 100 */
    public static final class Spawn {
        public int lowerLevelZbWeight = 2;
        public int aotumanZbWeight = 2;
        public int mikuZbWeight = 2;
        public int cocoZombieWeight = 2;
        public int cocoZombieXWeight = 1;
        public int cocoPenguinWeight = 8;
        public int uncleWeight = 3;
        /**
         * 不在尸水泉附近时的额外通过几率（0~1）。
         * <p>
         * 权重是全局的、没法按位置变，所以"湖边更密"是靠这道概率门实现的：
         * 湖边直接放行、别处只有这个几率通过。设 1.0 = 取消聚集。
         */
        public float nearBywaterChance = 0.2F;
    }

    /** 名字来源 */
    public static final class Names {
        /**
         * 作者与已同意的朋友：原样使用，因此查得到真皮肤、不染色。
         * <p>
         * ⚠️ 只放同意过的人 —— 等于"让这个人以尸兄形态出现在别人的游戏里"。
         */
        public List<String> consentedIds = List.of("UFO_by_shi", "Ti_tian_kuang");
        /** 直接给 {@link #consentedIds} 的几率（1/N） */
        public int consentedChance = 8;
        /**
         * 大名单：真实存在的账号 ID。
         * <p>
         * 配了它就<b>只用它</b>（不再用内置的音节重组）—— 想用一大堆真实账号的皮肤就填这里，
         * 名字从哪来由你决定（模组不会去任何网站抓名字）。
         * 只认英文 / 数字 / 下划线，3~16 位；中文查不到皮肤会被忽略。
         */
        public List<String> ids = List.of();
    }

    /** 皮肤表现 */
    public static final class Skin {
        /** 查不到皮肤时，是否按 ID 哈希给基础皮肤染色（这是"每只尸兄长得不一样"的来源） */
        public boolean tintUnresolvedSkins = true;
        /** 染色强度：1 = 默认，0 = 不染，2 = 更夸张 */
        public float tintStrength = 1.0F;
    }

    private CorpseConfig() {
    }

    /** 取配置（首次调用时读盘；文件不存在就写一份默认的出来） */
    public static CorpseConfig get() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    private static CorpseConfig load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
        try {
            if (Files.exists(path)) {
                CorpseConfig config = GSON.fromJson(Files.readString(path, StandardCharsets.UTF_8), CorpseConfig.class);
                if (config != null) {
                    config.sanitize();
                    CorpseOrigin.LOGGER.info("已加载配置文件：{}", path);
                    return config;
                }
            }
            CorpseConfig config = new CorpseConfig();
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(config), StandardCharsets.UTF_8);
            CorpseOrigin.LOGGER.info("已生成配置文件：{}", path);
            return config;
        } catch (Exception e) {
            CorpseOrigin.LOGGER.warn("读取配置文件失败，本次使用默认值: {}", e.getMessage());
            return new CorpseConfig();
        }
    }

    /**
     * 兜底：文件里缺字段、或被手改坏了（null / 越界）时补上安全值。
     * <p>
     * Gson 反序列化时不一定走构造器，所以不能只靠字段初始值。
     */
    private void sanitize() {
        if (spawn == null) {
            spawn = new Spawn();
        }
        if (names == null) {
            names = new Names();
        }
        if (skin == null) {
            skin = new Skin();
        }
        if (names.consentedIds == null) {
            names.consentedIds = new ArrayList<>();
        }
        if (names.ids == null) {
            names.ids = new ArrayList<>();
        }
        if (names.consentedChance < 1) {
            names.consentedChance = 1;
        }
        spawn.nearBywaterChance = clamp(spawn.nearBywaterChance, 0.0F, 1.0F);
        skin.tintStrength = Math.max(0.0F, skin.tintStrength);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
