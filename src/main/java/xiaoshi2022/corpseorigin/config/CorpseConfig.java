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
    /** 七星棺：沉棺事件 / 尸水污染 / 自动开馆参数 */
    public QiXingGuan qiXingGuan = new QiXingGuan();
    /**
     * 配置文件版本号：每次新增需要"老配置文件迁移"的字段时把它 +1。
     * <p>
     * 字段默认值是当前最新版本；老文件里没有这一项时 Gson 反序列化为 0，触发 sanitize 里的迁移分支。
     */
    public int configVersion = 1;
    public static final class GourdInheritance {
        public boolean enabled = true;
        public double passiveChance = .40, neutralChance = .30, hostileChance = .25;
        public double eliteChance = .12, bossChance = .05, automaticMultiplier = .5;
    }
    public static final class CharacterBooks {
        public List<String> disabledCharacters = new ArrayList<>();
    }

    /** 七星棺参数（沉棺事件 / 自动开馆） */
    public static final class QiXingGuan {
        /**
         * 落水后多少秒自动开馆（贴合原著"运输途中掉河里，没人喂怪也能开"）。
         * 0 = 禁用自动开馆，只能靠"周围 ≥3 只尸兄"或指令触发。
         */
        public int autoOpenSeconds = 300;
        /** 沉棺事件把周边多少格半径内的水源染成尸水。 */
        public int infectRadius = 8;
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
        /** 尸兄·尔多兽王事件型召唤参数（见 {@code EldorKingSpawns}）—— GUI 里可改 */
        public EldorKing eldorKing = new EldorKing();

        /** 穆博士事件型召唤参数（见 {@code MuDoctorSpawns}）—— GUI 里可改 */
        public MuDoctor muDoctor = new MuDoctor();

        /** 蚊子尸兄事件型召唤参数（见 {@code MosquitoZbrSpawns}）—— GUI 里可改 */
        public Mosquito mosquito = new Mosquito();

        /** 青蛙奇葩尸兄事件型召唤参数（见 {@code FrogZbrMcSpawns}）—— GUI 里可改 */
        public FrogZbrMc frogZbrMc = new FrogZbrMc();

        /** 壁虎奇葩尸兄事件型召唤参数（见 {@code GeckoZbrSpawns}）—— GUI 里可改 */
        public GeckoZbr geckoZbr = new GeckoZbr();

        /** 世界威胁等级：尸兄强度随服务器在线玩家最高境界缩放（见 {@code WorldThreatManager}）—— GUI 里可改 */
        public WorldThreat worldThreat = new WorldThreat();

        /** 漫展尸兄事件型召唤参数（见 {@code ManzhanSpawns}）—— GUI 里可改 */
        public Manzhan manzhan = new Manzhan();

        public static final class WorldThreat {
            /** 总闸。关闭后尸兄强度固定为面板基础值，不随玩家境界变化。 */
            public boolean enabled = true;
            /** 世界威胁每 1 级（玩家最高境界每升 1 级）给尸兄增加的最大生命比例（乘基础值）。0.15 = +15%/级。 */
            public float hpPerLevel = 0.15F;
            /** 每 1 级威胁增加的攻击伤害比例（乘基础值）。 */
            public float damagePerLevel = 0.12F;
            /** 每 1 级威胁增加的护甲点数（绝对值）。 */
            public float armorPerLevel = 0.3F;
            /** 计入威胁的等级上限（1~20）：神上 20 级 ×0.08 = +152% 生命，超出部分不再加成。 */
            public int maxLevelsCounted = 20;
        }

        public static final class Mosquito {
            /** 首次现身的游戏日（第几天）。0 = 关闭事件召唤，只能靠指令/刷怪蛋。 */
            public int firstDay = 18;
            /** 两次现身之间的间隔天数。 */
            public int intervalDays = 14;
            /** 距目标玩家最近召唤距离（格）。 */
            public int minRadius = 24;
            /** 距目标玩家最远召唤距离（格）。 */
            public int maxRadius = 40;
            /** 该半径内已有存活的蚊子尸兄就跳过本次（防堆叠）。 */
            public int nearbyBossCheck = 128;
        }

        public static final class FrogZbrMc {
            /** 首次现身的游戏日（第几天）。0 = 关闭事件召唤，只能靠指令/刷怪蛋。 */
            public int firstDay = 10;
            /** 两次现身之间的间隔天数。 */
            public int intervalDays = 18;
            /** 距目标玩家最近召唤距离（格）。 */
            public int minRadius = 24;
            /** 距目标玩家最远召唤距离（格）。 */
            public int maxRadius = 40;
            /** 该半径内已有存活的青蛙奇葩尸兄就跳过本次（防堆叠）。 */
            public int nearbyBossCheck = 128;
        }

        public static final class GeckoZbr {
            /** 首次现身的游戏日（第几天）。0 = 关闭事件召唤，只能靠指令/刷怪蛋。 */
            public int firstDay = 12;
            /** 两次现身之间的间隔天数。 */
            public int intervalDays = 20;
            /** 距目标玩家最近召唤距离（格）。 */
            public int minRadius = 24;
            /** 距目标玩家最远召唤距离（格）。 */
            public int maxRadius = 40;
            /** 该半径内已有存活的壁虎奇葩尸兄就跳过本次（防堆叠）。 */
            public int nearbyBossCheck = 128;
        }

        public static final class MuDoctor {
            /** 首次降临的世界日（第几天）。0 = 关闭事件召唤，只能靠指令/刷怪蛋。 */
            public int firstDay = 14;
            /** 两次降临之间的间隔天数。 */
            public int intervalDays = 16;
            /** 距目标玩家最近召唤距离（格）。 */
            public int minRadius = 24;
            /** 距目标玩家最远召唤距离（格）。 */
            public int maxRadius = 40;
            /** 该半径内已有存活的穆博士就跳过本次（防堆叠）。 */
            public int nearbyBossCheck = 128;
        }

        public static final class EldorKing {
            /** 首次降临的世界日（第几天）。0 = 关闭事件召唤，只能靠指令/刷怪蛋。 */
            public int firstDay = 8;
            /** 两次降临之间的间隔天数。 */
            public int intervalDays = 12;
            /** 距目标玩家最近召唤距离（格）。 */
            public int minRadius = 32;
            /** 距目标玩家最远召唤距离（格）。 */
            public int maxRadius = 48;
            /** 该半径内已有存活的多尔兽王就跳过本次（防堆叠）。 */
            public int nearbyBossCheck = 128;
        }

        /** 漫展尸兄事件召唤参数（见 {@code ManzhanSpawns}）—— GUI 里可改 */
        public static final class Manzhan {
            /** 皮肤文件夹名：{@code config/corpseorigin/skins/<folder>/*.png}，文件名 = 皮肤名。 */
            public String folder = "manzhan";
            /** 事件总闸。false = 事件召唤关闭（/summonmanzhan 指令不受影响）。 */
            public boolean enabled = true;
            /** 首次现身的游戏日（第几天）。0 = 关闭事件召唤，只能靠指令。 */
            public int firstDay = 3;
            /** 两次现身之间的间隔天数。 */
            public int intervalDays = 15;
            /** 每次事件召唤的数量。 */
            public int count = 16;
            /** 距目标玩家的最小散布半径（格）。 */
            public int minRadius = 16;
            /** 距目标玩家的最大散布半径（格）。 */
            public int maxRadius = 32;
            /** 附近多少格内已有"漫展皮肤"尸兄就跳过本次事件（防堆叠）。 */
            public int nearbyCheck = 128;
        }

        /** World day of the first scripted corpse worm encounter. Set to 0 to disable it. */
        public int corpseWormFirstDay = 4;
        /** Days after the first encounter before the next one. */
        public int corpseWormBaseIntervalDays = 4;
        /** Extra days added to each successive interval. */
        public int corpseWormIntervalIncreaseDays = 1;
        /**
         * 是否实时禁用<b>原版</b>僵尸（含尸壳、村民僵尸）的自然生成。
         * <p>
         * 默认 false（原版僵尸照常刷）。改完之后跑 {@code /corpseconfig reload} 立刻生效，
         * 或者用 {@code /corpseconfig zombies on|off} 直接开关（会自动写回文件）。
         * <b>只拦 NATURAL 自然生成</b>：刷怪蛋、刷怪笼、{@code /summon} 不受影响。
         */
        public boolean disableVanillaZombieSpawns = false;
        /** 尸兄三兄弟 + CoCo 尸兄的生成权重，默认与原版僵尸持平（100）—— 想更稀有就调小 */
        public int lowerLevelZbWeight = 100;
        public int aotumanZbWeight = 100;
        public int mikuZbWeight = 100;
        public int cocoZombieWeight = 100;
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

        // ---- 吸食进化（ZbEvolution）：尸兄自己吃血肉升级的那条线，GUI 第 2 页可改 ----
        /** 每级进化所需的血肉能量（击杀所得：动物 1 / 村民 3 / 同类 4 / 玩家 5 点）。 */
        public int zbEvolutionEnergyPerLevel = 10;
        /**
         * 正常进化的等级上限（1~9）：过了这条线就要靠"超脱临界"概率突破，
         * 每次突破成功随机突变一个器官 —— 也就是"外骨骼"的来源。
         */
        public int zbEvolutionBreakthroughLevel = 5;
        /** 临界突破基础成功率（0~1）。 */
        public float zbEvolutionBreakthroughChance = 0.15F;
        /** 每次突破失败后叠加的成功率（0~0.5），直到突破为止。 */
        public float zbEvolutionBreakthroughBonus = 0.10F;

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

    /** 配置整体替换后的回调：供启动时快照了配置值的模块刷新缓存（如 ModSpawns 的远湖几率）。 */
    public static Runnable onReplaceCallback = null;

    /**
     * 用编辑态对象整体替换当前实例并落盘（配置 GUI 用）。
     * GUI 先用 {@link #snapshot()} 拿一份深拷贝当编辑态，确认保存时整体写回 —— 取消则直接丢弃，内存对象不脏。
     * 写回后触发 {@link #onReplaceCallback}，保证"启动时快照"的配置项（生成规则等）保存即刷新。
     */
    public static void replace(CorpseConfig newConfig) {
        instance = newConfig;
        save();
        if (onReplaceCallback != null) onReplaceCallback.run();
    }

    /** 当前配置的深拷贝（GSON 走一圈），给配置 GUI 当编辑态。 */
    public static CorpseConfig snapshot() {
        return GSON.fromJson(GSON.toJson(get()), CorpseConfig.class);
    }

    /**
     * 重新读盘载入配置（运行时修改 config/corpseorigin.json 后跑 {@code /corpseconfig reload} 触发）。
     * <p>
     * 不重启服务器就能让 {@code disableVanillaZombieSpawns}、刷怪权重等"实时"字段生效；
     * 写坏了会兜底成默认值，不会把现有 instance 顶掉。
     */
    public static void reload() {
        CorpseConfig fresh = load();
        instance = fresh;
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
        // 七星棺：老配置文件里没有这一节
        if (qiXingGuan == null) qiXingGuan = new QiXingGuan();
        qiXingGuan.autoOpenSeconds = (int) clamp(qiXingGuan.autoOpenSeconds, 0, 86400);
        qiXingGuan.infectRadius = (int) clamp(qiXingGuan.infectRadius, 1, 32);
        if (characterBooks == null) characterBooks = new CharacterBooks();
        if (characterBooks.disabledCharacters == null) characterBooks.disabledCharacters = new ArrayList<>();
        if (growth == null) growth = new xiaoshi2022.corpseorigin.growth.GrowthConfig();
        if (growth.exploration == null) growth.exploration = new ArrayList<>();
        if (growth.teachings == null) growth.teachings = new ArrayList<>();
        if (spawn == null) {
            spawn = new Spawn();
        }
        // 老配置文件里还没有 muDoctor 这一节：Gson 会反序列化成 null，
        // 不补的话 MuDoctorSpawns 每 200 tick 读 cfg 时会 NPE。
        if (spawn.muDoctor == null) {
            spawn.muDoctor = new Spawn.MuDoctor();
        }
        // 同理：蚊子尸兄的事件召唤参数（老文件里没有这一节）
        if (spawn.mosquito == null) {
            spawn.mosquito = new Spawn.Mosquito();
        }
        // 同理：青蛙奇葩尸兄的事件召唤参数（老文件里没有这一节）
        if (spawn.frogZbrMc == null) {
            spawn.frogZbrMc = new Spawn.FrogZbrMc();
        }
        // 同理：壁虎奇葩尸兄的事件召唤参数（老文件里没有这一节）
        if (spawn.geckoZbr == null) {
            spawn.geckoZbr = new Spawn.GeckoZbr();
        }
        // 同理：世界威胁等级参数（老文件里没有这一节）
        if (spawn.worldThreat == null) {
            spawn.worldThreat = new Spawn.WorldThreat();
        }
        // 同理：漫展尸兄事件召唤参数（老文件里没有这一节）
        if (spawn.manzhan == null) {
            spawn.manzhan = new Spawn.Manzhan();
        }
        spawn.manzhan.folder = orDefault(spawn.manzhan.folder, "manzhan");
        spawn.manzhan.firstDay = (int) clamp(spawn.manzhan.firstDay, 0, 3650);
        spawn.manzhan.intervalDays = (int) clamp(spawn.manzhan.intervalDays, 1, 365);
        spawn.manzhan.count = (int) clamp(spawn.manzhan.count, 1, 128);
        spawn.manzhan.minRadius = (int) clamp(spawn.manzhan.minRadius, 8, 96);
        spawn.manzhan.maxRadius = Math.max(spawn.manzhan.minRadius,
                (int) clamp(spawn.manzhan.maxRadius, 16, 128));
        spawn.manzhan.nearbyCheck = (int) clamp(spawn.manzhan.nearbyCheck, 16, 256);
        spawn.worldThreat.hpPerLevel = clamp(spawn.worldThreat.hpPerLevel, 0F, 0.5F);
        spawn.worldThreat.damagePerLevel = clamp(spawn.worldThreat.damagePerLevel, 0F, 0.5F);
        spawn.worldThreat.armorPerLevel = clamp(spawn.worldThreat.armorPerLevel, 0F, 3F);
        spawn.worldThreat.maxLevelsCounted = (int)clamp(spawn.worldThreat.maxLevelsCounted, 1, 20);
        // 配置文件迁移：老文件里尸兄权重默认 2（远低于"和原版僵尸持平"的 100），
        // 老用户升级上来后只有把它们顶到新默认，才符合"尸兄生成权重和僵尸持平"的预期。
        // 只动恰好等于 2 的字段 —— 用户如果手动改过（≠2）一律尊重。
        if (configVersion < 1) {
            if (spawn.lowerLevelZbWeight == 2) spawn.lowerLevelZbWeight = 100;
            if (spawn.aotumanZbWeight == 2) spawn.aotumanZbWeight = 100;
            if (spawn.mikuZbWeight == 2) spawn.mikuZbWeight = 100;
            if (spawn.cocoZombieWeight == 2) spawn.cocoZombieWeight = 100;
            configVersion = 1;
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
