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
    /** Infected-water behavior. */
    public InfectedWater infectedWater = new InfectedWater();
    /** 与其他模组的软联动 */
    public Compat compat = new Compat();
    /** HUD 自定义（位置/大小/缩放） */
    public Hud hud = new Hud();
    /** 技能热键栏自定义（位置/间距/缩放） */
    public SkillHud skillHud = new SkillHud();
    public SwordVisuals swordVisuals = new SwordVisuals();
    public static final class SwordVisuals {
        public boolean hitStop = true, screenEffects = true;
        public float cameraShake = 1, flashIntensity = .65f;
        public int maxImpactEffects = 32;
    }
    public xiaoshi2022.corpseorigin.growth.GrowthConfig growth = new xiaoshi2022.corpseorigin.growth.GrowthConfig();
    public xiaoshi2022.corpseorigin.growth.RealmConfig realm = new xiaoshi2022.corpseorigin.growth.RealmConfig();
    public xiaoshi2022.corpseorigin.growth.CorpseHorrorConfig corpseHorror = new xiaoshi2022.corpseorigin.growth.CorpseHorrorConfig();
    public xiaoshi2022.corpseorigin.growth.RuinLoot.Config ruinLoot = new xiaoshi2022.corpseorigin.growth.RuinLoot.Config();
    /** Server-side restrictions on both bound and universal character books. Restart to apply. */
    public CharacterBooks characterBooks = new CharacterBooks();
    public GourdInheritance gourdInheritance = new GourdInheritance();
    public static final class GourdInheritance {
        public boolean enabled = true;
        public double passiveChance = .40, neutralChance = .30, hostileChance = .25;
        public double eliteChance = .12, bossChance = .05, automaticMultiplier = .5;
    }
    public static final class CharacterBooks {
        public List<String> disabledCharacters = new ArrayList<>();
    }

    /** 自然生成的权重与"尸水泉聚集"参数。权重参照原版僵尸 = 100 */
    public static final class InfectedWater {
        /**
         * Ticks before infected water touching seawater is diluted back into water.
         * 24000 ticks is one Minecraft day; 0 disables ocean dilution.
         */
        public int oceanDilutionTicks = 24000;
    }

    public static final class Spawn {
        /** World day of the first scripted corpse worm encounter. Set to 0 to disable it. */
        public int corpseWormFirstDay = 4;
        /** Days after the first encounter before the next one. */
        public int corpseWormBaseIntervalDays = 4;
        /** Extra days added to each successive interval. */
        public int corpseWormIntervalIncreaseDays = 1;
        public int lowerLevelZbWeight = 2;
        public int aotumanZbWeight = 2;
        public int mikuZbWeight = 2;
        public int cocoZombieWeight = 2;
        /**
         * CoCo 尸兄·二阶段（合体形态）。<b>目前不参与自然生成</b> —— 它按设定是企鹅与大叔的合体产物，
         * 只应由合体流程产生（见 {@code ModSpawns}）。这个值保留着，方便你想改回去时直接用。
         */
        public int cocoZombieXWeight = 1;
        public int cocoPenguinWeight = 3;
        public int uncleWeight = 3;
        /**
         * 哈姆（ham）—— 只在村庄附近刷的宠物犬，判据抄原版猫（见 {@code ModSpawns#corpseorigin$hamSpawnRules}）。
         * 权重对着原版狼（8）来定，想更少见就调小。
         */
        public int hamWeight = 2;
        /**
         * 不在尸水泉附近时的额外通过几率（0~1）。
         * <p>
         * 权重是全局的、没法按位置变，所以"湖边更密"是靠这道概率门实现的：
         * 湖边直接放行、别处只有这个几率通过。设 1.0 = 取消聚集。
         */
        public float nearBywaterChance = 0.2F;

        /**
         * 自然生成的尸兄是否按"游戏日"提升进化等级（越后期越强）。
         * <p>
         * 关掉就一律人1 —— 想回到"高阶只能靠吃血肉突破"的老手感时用。
         */
        public boolean evolutionLevelRamp = true;
        /**
         * 每多少个游戏日把自然生成的等级上限提高 1 档。
         * <p>
         * 默认 8：第 0~7 天全是人1，第 8~15 天到人2……约 56 天摸到默认上限（地4）。
         */
        public int daysPerEvolutionLevel = 8;
        /**
         * 自然生成的最高进化等级（1~10）。
         * <p>
         * 默认 8（地4）。注意 6 级以上会顺带带上突变器官 —— 那是"突破"过才有的东西，
         * 想让它更稀罕就把这个值压低、或把 {@link #levelDecay} 调大。
         */
        public int maxEvolutionLevel = 8;
        /**
         * 等级分布朝"当前上限"集中的程度：离上限每远一级，权重除以它。
         * <p>
         * 默认 2.0 —— 上限附近最多、往下逐级减半；调到 1.1 接近均摊，调到 4 就几乎只剩上限那一级。
         */
        public float levelDecay = 2.0F;
        /** Natural Corpse Brother spawn chance outside city chunks in a Lost Cities dimension. */
        public float lostCitiesOutsideSpawnChance = 0.025F;
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

    /**
     * HUD 自定义配置。
     * <p>
     * 位置：x/y 填 -1 表示自动靠右上（默认行为），否则为绝对像素坐标（原点左上角）。
     * 尺寸：batteryWidth/batteryHeight 控制电池条大小，rowGap 控制行间距，整体缩放 scale 可以放大/缩小整个 HUD。
     */
    public static final class Hud {
        /** 0..1920 绝对像素 X（-1 = 自动靠右上） */
        public int x = -1;
        /** 0..1080 绝对像素 Y（-1 = 自动靠右上） */
        public int y = -1;
        /** 电池条宽度（像素） */
        public int batteryWidth = 116;
        /** 电池条高度（像素） */
        public int batteryHeight = 13;
        /** 行间距（像素） */
        public int rowGap = 3;
        /** 整体缩放倍数（支持 0.5 / 0.75 / 1.0 / 1.25 / 1.5 / 2.0） */
        public float scale = 1.0F;
    }

    /**
     * 技能热键栏（左侧 3 个技能槽）自定义配置。
     * <p>
     * x/y 填 -1 表示自动（x=8 贴左边、y 垂直居中），否则为绝对像素坐标（原点左上角）。
     * spacing 是槽位之间的垂直间距，scale 是整个热键栏的缩放倍数。
     */
    public static final class SkillHud {
        /** 0..1920 绝对像素 X（-1 = 自动靠左） */
        public int x = -1;
        /** 0..1080 绝对像素 Y（-1 = 垂直居中） */
        public int y = -1;
        /** 槽位之间的垂直间距（像素） */
        public int spacing = 4;
        /** 整体缩放倍数（支持 0.5 / 0.75 / 1.0 / 1.25 / 1.5 / 2.0） */
        public float scale = 1.0F;
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

    /** 强制把当前配置写回文件（HUD 设置界面等运行时修改后调用） */
    public static void save() {
        if (instance == null) return;
        try {
            Path path = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(instance), StandardCharsets.UTF_8);
            CorpseOrigin.LOGGER.debug("已保存配置文件：{}", path);
        } catch (Exception e) {
            CorpseOrigin.LOGGER.warn("保存配置文件失败: {}", e.getMessage());
        }
    }

    private static CorpseConfig load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
        try {
            if (Files.exists(path)) {
                CorpseConfig config = GSON.fromJson(Files.readString(path, StandardCharsets.UTF_8), CorpseConfig.class);
                if (config != null) {
                    config.sanitize();
                    // Keep existing config files discoverable when new options are introduced.
                    Files.writeString(path, GSON.toJson(config), StandardCharsets.UTF_8);
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
        if(swordVisuals==null)swordVisuals=new SwordVisuals();
        swordVisuals.cameraShake=Float.isFinite(swordVisuals.cameraShake)?Math.clamp(swordVisuals.cameraShake,0,2):1;
        swordVisuals.flashIntensity=Float.isFinite(swordVisuals.flashIntensity)?Math.clamp(swordVisuals.flashIntensity,0,1):.65f;
        swordVisuals.maxImpactEffects=Math.clamp(swordVisuals.maxImpactEffects,4,64);
        if (realm == null) realm = new xiaoshi2022.corpseorigin.growth.RealmConfig();
        realm.sanitize();
        if (corpseHorror == null) corpseHorror = new xiaoshi2022.corpseorigin.growth.CorpseHorrorConfig();
        corpseHorror.sanitize();
        if(ruinLoot==null)ruinLoot=new xiaoshi2022.corpseorigin.growth.RuinLoot.Config();
        ruinLoot.sanitize();
        if (gourdInheritance == null) gourdInheritance = new GourdInheritance();
        if (characterBooks == null) characterBooks = new CharacterBooks();
        if (characterBooks.disabledCharacters == null) characterBooks.disabledCharacters = new ArrayList<>();
        if (growth == null) growth = new xiaoshi2022.corpseorigin.growth.GrowthConfig();
        if (growth.exploration == null) growth.exploration = new ArrayList<>();
        if (growth.teachings == null) growth.teachings = new ArrayList<>();
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
        if (infectedWater == null) {
            infectedWater = new InfectedWater();
        }
        infectedWater.oceanDilutionTicks = Math.max(0, infectedWater.oceanDilutionTicks);
        if (compat == null) {
            compat = new Compat();
        }
        compat.mouthSnakeSpecies = orDefault(compat.mouthSnakeSpecies, "king_snake");
        if (hud == null) {
            hud = new Hud();
        }
        // x/y：-1 = 自动位置；>= 0 = 绝对像素。超过合理范围时兜底成默认 -1
        if (hud.x < -1 || hud.x > 4096) hud.x = -1;
        if (hud.y < -1 || hud.y > 2160) hud.y = -1;
        hud.batteryWidth = Math.max(40, hud.batteryWidth);
        hud.batteryHeight = Math.max(6, hud.batteryHeight);
        hud.rowGap = Math.max(0, hud.rowGap);
        hud.scale = clamp(hud.scale, 0.25F, 4.0F);
        if (skillHud == null) {
            skillHud = new SkillHud();
        }
        if (skillHud.x < -1 || skillHud.x > 4096) skillHud.x = -1;
        if (skillHud.y < -1 || skillHud.y > 2160) skillHud.y = -1;
        skillHud.spacing = Math.max(0, skillHud.spacing);
        skillHud.scale = clamp(skillHud.scale, 0.25F, 4.0F);
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
        spawn.lostCitiesOutsideSpawnChance = clamp(spawn.lostCitiesOutsideSpawnChance, 0.0F, 1.0F);
        spawn.daysPerEvolutionLevel = Math.max(1, spawn.daysPerEvolutionLevel);
        spawn.maxEvolutionLevel = Math.clamp(spawn.maxEvolutionLevel, 1, 10);
        spawn.levelDecay = clamp(spawn.levelDecay, 1.1F, 10.0F);
        spawn.corpseWormFirstDay = Math.max(0, spawn.corpseWormFirstDay);
        spawn.corpseWormBaseIntervalDays = Math.max(1, spawn.corpseWormBaseIntervalDays);
        spawn.corpseWormIntervalIncreaseDays = Math.max(0, spawn.corpseWormIntervalIncreaseDays);
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
