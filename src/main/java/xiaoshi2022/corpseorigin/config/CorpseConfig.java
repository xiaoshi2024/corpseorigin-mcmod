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
    /** 左护法变异体（玩家整身换成 zuo_guardian 模型） */
    public MutantBody mutantBody = new MutantBody();
    /** 与其他模组的软联动 */
    public Compat compat = new Compat();

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

    /**
     * 左护法变异体表现。
     * <p>
     * 左护法角色会把整具身体换成 {@code zuo_guardian}（巨蛇 + 骑手，骑手贴玩家皮肤）——
     * 那套模型是按 BOSS 体型做的，直接按原始尺寸套在玩家身上会大得离谱，
     * 所以这里给一组"看着舒服"的调节项。改完重启游戏生效。
     */
    public static final class MutantBody {
        /**
         * 是否启用变异体外观。
         * <p>
         * 关掉之后左护法仍然算尸兄（阵营、技能、饥饿都照旧），只是外观退回普通尸兄那一套。
         */
        public boolean enabled = true;
        /**
         * 整体缩放：1.0 = 模型的原始尺寸（约 3 格宽、5 格高的巨物），0.5 = 缩一半。
         * <p>
         * 缩放以玩家脚底为基准，缩小时模型会更贴近玩家体型。
         */
        public float scale = 0.5F;
        /**
         * 垂直微调（格）：模型整体悬空就往下压（填负数），埋进地里就往上抬（填正数）。
         * <p>
         * 模型是在 Blockbench 里按它自己的姿势摆的，不同动画的落点略有差异，这里只做整体对齐。
         */
        public float yOffset = 0.0F;
        /** 动画对应表：哪个状态播 zuo_guardian 的哪条动画（留空 = 用内置默认值） */
        public Animations animations = new Animations();
        /** 蛟龙多段碰撞箱 */
        public Hitboxes hitboxes = new Hitboxes();

        /**
         * 蛟龙多段碰撞箱。
         * <p>
         * 变异体形态下玩家自己的原版箱子会被设成"不可被射线选中"，改由这里列出的<b>节</b>来挨打：
         * 每节是一个隐形实体，近战 / 远程打到任意一节，伤害都转给玩家本体（自己打自己无效）。
         * <p>
         * ⚠️ 节的落点<b>没法自动算</b>：蛇身在哪，是客户端 GeckoLib 动画算出来的，服务端看不到。
         * 所以这里写的是"相对玩家、按玩家朝向旋转"的固定偏移，需要你对着模型调 ——
         * 进游戏开 {@code F3+B} 看箱子，配合 {@code /character hitbox <节名> <字段> <值>} 现场微调，
         * 调好之后把数值抄回本文件（命令改的是内存里的值，重启会丢）。
         */
        public static final class Hitboxes {
            /**
             * 总开关。
             * <p>
             * 关掉之后：不生成任何节，玩家自己的原版箱子也恢复成可被选中（等于回到"没有多段碰撞箱"）。
             */
            public boolean enabled = true;
            /** 节的列表；想少几节就把条目删掉，想多几节就照格式加。第一节默认是骑手（人形箱） */
            public List<Segment> segments = new ArrayList<>(List.of(
                    new Segment("rider", 0.0F, 1.6F, 0.0F, 0.6F, 1.8F),
                    new Segment("head", 3.0F, 1.0F, 0.0F, 1.4F, 1.4F),
                    new Segment("body1", 1.6F, 1.0F, 0.0F, 1.6F, 1.6F),
                    new Segment("body2", 0.3F, 1.0F, 0.0F, 1.8F, 1.8F)));

            /** 一节碰撞箱：相对玩家的前 / 上 / 右偏移 + 尺寸（单位格） */
            public static final class Segment {
                /** 名字，只用于命令和日志 */
                public String name = "segment";
                /** 前（+ = 玩家面朝方向） */
                public float forward = 0.0F;
                /** 上（箱子<b>底面</b>相对玩家脚底的高度） */
                public float up = 0.0F;
                /** 右（+ = 玩家右手方向） */
                public float right = 0.0F;
                public float width = 1.0F;
                public float height = 1.0F;

                public Segment() {
                }

                public Segment(String name, float forward, float up, float right, float width, float height) {
                    this.name = name;
                    this.forward = forward;
                    this.up = up;
                    this.right = right;
                    this.width = width;
                    this.height = height;
                }
            }
        }

        /**
         * 变异体动画对应表。
         * <p>
         * 填的是 {@code zuo_guardian.animation.json} 里的动画名，改完重启游戏生效；
         * 名字填错不会崩，但 GeckoLib 会在日志里报 {@code Unable to find animation: 'xxx'}，模型会僵住不动。
         * <p>
         * 这套模型自带的动画：{@code idle} 待机、{@code reptile} 爬行、{@code swim} 游动、
         * {@code raised} 抬头探身、{@code riderx_attack} 骑手攻击、{@code eat} 进食、{@code speak} 说话。
         */
        public static final class Animations {
            /** 站着不动 */
            public String idle = "idle";
            /** 陆地移动（这套模型没有 walk，用爬行代替） */
            public String move = "reptile";
            /** 在水里 */
            public String swim = "swim";
            /** 潜行（蹲下）= 探头，播一次停在末帧 */
            public String raised = "raised";
            /** 挥击，播一次 */
            public String attack = "riderx_attack";
        }
    }

    /**
     * 与其他模组的软联动。
     * <p>
     * 全是"装了才生效"的：没装对应模组时各自退回本模组的内置表现，不会有硬依赖。
     */
    public static final class Compat {
        /**
         * 「嘴里吐蛇」是否借用 <b>Snakes Alive</b> 的蛇。
         * <p>
         * 开着且装了那个模组时：技能会真的吐出一条它家的蛇（认得你为主人，
         * 它自家的"跟随主人 / 护主"AI 会接管，不用我们再建模）。
         * 关掉 / 没装 → 只有内置的贯穿打击与粒子表现。
         */
        public boolean mouthSnakeUsesSnakesAlive = true;
        /**
         * 吐哪一条蛇：填 Snakes Alive 的物种 id（不带命名空间），例如
         * {@code king_snake} / {@code cobra} / {@code inland_taipan} / {@code corn_snake} …
         * <p>
         * 完整名单见它家的语言文件（{@code assets/snakesalive/lang/en_us.json} 里的
         * {@code entity.snakesalive.*}）。填错名字不会崩，只是那一次不吐蛇。
         */
        public String mouthSnakeSpecies = "king_snake";
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
        if (mutantBody == null) {
            mutantBody = new MutantBody();
        }
        if (compat == null) {
            compat = new Compat();
        }
        compat.mouthSnakeSpecies = orDefault(compat.mouthSnakeSpecies, "king_snake");
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
        // 缩到 0 或负数会把模型压成一张纸、甚至翻面，给个下限；放太大也没意义（模型本来就有 5 格高）
        mutantBody.scale = clamp(mutantBody.scale, 0.05F, 4.0F);
        mutantBody.yOffset = clamp(mutantBody.yOffset, -4.0F, 4.0F);
        if (mutantBody.animations == null) {
            mutantBody.animations = new MutantBody.Animations();
        }
        if (mutantBody.hitboxes == null) {
            mutantBody.hitboxes = new MutantBody.Hitboxes();
        }
        if (mutantBody.hitboxes.segments == null) {
            mutantBody.hitboxes.segments = new ArrayList<>(
                    new MutantBody.Hitboxes().segments);
        }
        for (MutantBody.Hitboxes.Segment segment : mutantBody.hitboxes.segments) {
            if (segment == null) {
                continue;
            }
            segment.name = orDefault(segment.name, "segment");
            // 偏移给宽一点的范围（模型落点本来就靠手调）；尺寸必须为正，否则箱子会翻面
            segment.forward = clamp(segment.forward, -16.0F, 16.0F);
            segment.up = clamp(segment.up, -8.0F, 16.0F);
            segment.right = clamp(segment.right, -16.0F, 16.0F);
            segment.width = clamp(segment.width, 0.1F, 12.0F);
            segment.height = clamp(segment.height, 0.1F, 12.0F);
        }
        // 动画名留空 = 用内置默认值（填错了 GeckoLib 会在日志里报，不用在这里猜）
        mutantBody.animations.idle = orDefault(mutantBody.animations.idle, "idle");
        mutantBody.animations.move = orDefault(mutantBody.animations.move, "reptile");
        mutantBody.animations.swim = orDefault(mutantBody.animations.swim, "swim");
        mutantBody.animations.raised = orDefault(mutantBody.animations.raised, "raised");
        mutantBody.animations.attack = orDefault(mutantBody.animations.attack, "riderx_attack");
    }

    private static String orDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
