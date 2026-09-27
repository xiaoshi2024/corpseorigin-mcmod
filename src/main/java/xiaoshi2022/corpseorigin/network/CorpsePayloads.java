package xiaoshi2022.corpseorigin.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;
import xiaoshi2022.corpseorigin.CorpseOrigin;

import java.util.UUID;

public final class CorpsePayloads {

    private CorpsePayloads() {
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(CorpseOrigin.MOD_ID, path);
    }

    public record CorpseNestTeleportC2S(BlockPos targetPos) implements CustomPacketPayload {
        public static final Type<CorpseNestTeleportC2S> TYPE = new Type<>(id("corpse_nest_teleport"));
        public static final StreamCodec<ByteBuf, CorpseNestTeleportC2S> CODEC =
                CustomPacketPayload.codec((p, buf) -> buf.writeLong(p.targetPos().asLong()),
                        buf -> new CorpseNestTeleportC2S(BlockPos.of(buf.readLong())));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    // ==================== 角色选择 ====================

    public record SelectCharacterC2S(String characterId) implements CustomPacketPayload {
        public static final Type<SelectCharacterC2S> TYPE = new Type<>(id("select_character"));
        public static final StreamCodec<ByteBuf, SelectCharacterC2S> CODEC =
                CustomPacketPayload.codec(
                        (payload, buf) -> ByteBufCodecs.STRING_UTF8.encode(buf, payload.characterId()),
                        buf -> new SelectCharacterC2S(ByteBufCodecs.STRING_UTF8.decode(buf)));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /**
     * 「统一角色书」界面里选定角色（C2S）。
     * <p>
     * 和 {@link SelectCharacterC2S} 分开，是因为这条<b>要求手上真的拿着角色书</b>才生效：
     * 服务端校验通过后才应用角色并消耗一本（创造模式除外）。界面只管发包，
     * "换人 + 扣书"两件事都留在服务端判定。
     */
    public record CharacterBookSelectC2S(String characterId) implements CustomPacketPayload {
        public static final Type<CharacterBookSelectC2S> TYPE = new Type<>(id("character_book_select"));
        public static final StreamCodec<ByteBuf, CharacterBookSelectC2S> CODEC =
                CustomPacketPayload.codec(
                        (payload, buf) -> ByteBufCodecs.STRING_UTF8.encode(buf, payload.characterId()),
                        buf -> new CharacterBookSelectC2S(ByteBufCodecs.STRING_UTF8.decode(buf)));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record CharacterSyncS2C(String characterId) implements CustomPacketPayload {
        public static final Type<CharacterSyncS2C> TYPE = new Type<>(id("character_sync"));
        public static final StreamCodec<ByteBuf, CharacterSyncS2C> CODEC =
                CustomPacketPayload.codec(
                        (payload, buf) -> ByteBufCodecs.STRING_UTF8.encode(buf, payload.characterId()),
                        buf -> new CharacterSyncS2C(ByteBufCodecs.STRING_UTF8.decode(buf)));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record InfectionSyncS2C(int infection) implements CustomPacketPayload {
        public static final Type<InfectionSyncS2C> TYPE = new Type<>(id("infection_sync"));

        public static final StreamCodec<ByteBuf, InfectionSyncS2C> CODEC = StreamCodec.composite(
                ByteBufCodecs.INT,
                InfectionSyncS2C::infection,
                InfectionSyncS2C::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record TempRedEyeSyncS2C(UUID playerUuid, int durationTicks) implements CustomPacketPayload {
        public static final Type<TempRedEyeSyncS2C> TYPE = new Type<>(id("temp_red_eye_sync"));
        public static final StreamCodec<ByteBuf, TempRedEyeSyncS2C> CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, p -> p.playerUuid().toString(),
                ByteBufCodecs.INT, TempRedEyeSyncS2C::durationTicks,
                (s, t) -> new TempRedEyeSyncS2C(UUID.fromString(s), t));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /**
     * 天线宝宝尸兄的「吸食」状态（S2C）。
     * <p>
     * 驱动穿在身上的天线宝宝盔甲播 {@code absorb} 动画，并让触手转向
     * {@code targetEntityId} 这具目标（动画里的 {@code query.target_*_rotation}）；
     * {@code durationTicks <= 0} 表示立刻结束（松手 / 被打断），客户端收到后直接清掉状态。
     */
    public record AntennaSuckSyncS2C(UUID playerUuid, int targetEntityId, int durationTicks)
            implements CustomPacketPayload {
        public static final Type<AntennaSuckSyncS2C> TYPE = new Type<>(id("antenna_suck_sync"));
        public static final StreamCodec<ByteBuf, AntennaSuckSyncS2C> CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, p -> p.playerUuid().toString(),
                ByteBufCodecs.INT, AntennaSuckSyncS2C::targetEntityId,
                ByteBufCodecs.INT, AntennaSuckSyncS2C::durationTicks,
                (s, targetId, t) -> new AntennaSuckSyncS2C(UUID.fromString(s), targetId, t));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /**
     * 天线宝宝尸兄的「格挡」动画信号（S2C）。
     * <p>
     * 被动那 30% 概率挡下一击、或者主动格挡窗口开启时都会发这条，盔甲据此重播
     * {@code special_attack}（动画文件里没有专门的格挡 clip，所以借用挥击那条）；
     * {@code durationTicks <= 0} 表示立刻结束。与吸食一样，这条只管表现，判定全在服务端。
     */
    public record AntennaBlockSyncS2C(UUID playerUuid, int durationTicks) implements CustomPacketPayload {
        public static final Type<AntennaBlockSyncS2C> TYPE = new Type<>(id("antenna_block_sync"));
        public static final StreamCodec<ByteBuf, AntennaBlockSyncS2C> CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, p -> p.playerUuid().toString(),
                ByteBufCodecs.INT, AntennaBlockSyncS2C::durationTicks,
                (s, t) -> new AntennaBlockSyncS2C(UUID.fromString(s), t));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /**
     * 开胃奶「菊花盾」的格挡窗口（S2C）。
     * <p>
     * 主动开盾时发一条覆盖整个持续时间的窗口，客户端据此让背后那套 {@code niunaix} 背挂
     * 播 {@code parry}（花瓣张开成盾）；{@code durationTicks <= 0} 表示立刻结束。
     * 与天线格挡一样，这条只管表现，反弹判定全在服务端。
     */
    public record NiunaiParrySyncS2C(UUID playerUuid, int durationTicks) implements CustomPacketPayload {
        public static final Type<NiunaiParrySyncS2C> TYPE = new Type<>(id("niunai_parry_sync"));
        public static final StreamCodec<ByteBuf, NiunaiParrySyncS2C> CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, p -> p.playerUuid().toString(),
                ByteBufCodecs.INT, NiunaiParrySyncS2C::durationTicks,
                (s, t) -> new NiunaiParrySyncS2C(UUID.fromString(s), t));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /**
     * 开胃奶「拦腰斩断」的表现窗口（S2C）。
     * <p>
     * 受到致命一击时，服务端不让他死、改为拦腰斩断，并广播一条覆盖整个过程的窗口
     * （断开的 {@code broken_off} + 保持 + 接回的 {@code link}，总时长见
     * {@code KaiWeiNai.NIUNAI_LINK_TOTAL_TICKS}）。客户端据此把玩家模型整身换成
     * {@code niunai_link_player} 并播这两条动画；{@code durationTicks <= 0} 表示立刻结束。
     * 与菊花盾一样，这条只管表现，不死判定全在服务端。
     */
    public record NiunaiLinkSyncS2C(UUID playerUuid, int durationTicks) implements CustomPacketPayload {
        public static final Type<NiunaiLinkSyncS2C> TYPE = new Type<>(id("niunai_link_sync"));
        public static final StreamCodec<ByteBuf, NiunaiLinkSyncS2C> CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, p -> p.playerUuid().toString(),
                ByteBufCodecs.INT, NiunaiLinkSyncS2C::durationTicks,
                (s, t) -> new NiunaiLinkSyncS2C(UUID.fromString(s), t));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /**
     * 尸巢之子「千眼万目」的凝视窗口（S2C）。
     * <p>
     * 施展时发一条覆盖整段 {@code special} 动画（3.2 秒）的窗口，客户端据此让整具尸巢之子身体
     * 改播 {@code special}；{@code durationTicks <= 0} 表示立刻结束。
     * 与其它表现类包一样，这条只管动画，定身判定全在服务端（{@code ThousandEyesHandler}）。
     */
    public record ShiChaoSpecialSyncS2C(UUID playerUuid, int durationTicks) implements CustomPacketPayload {
        public static final Type<ShiChaoSpecialSyncS2C> TYPE = new Type<>(id("shichao_special_sync"));
        public static final StreamCodec<ByteBuf, ShiChaoSpecialSyncS2C> CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, p -> p.playerUuid().toString(),
                ByteBufCodecs.INT, ShiChaoSpecialSyncS2C::durationTicks,
                (s, t) -> new ShiChaoSpecialSyncS2C(UUID.fromString(s), t));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    // ==================== ✅ 玩家尸兄数据同步（用 UUID） ====================
    public record PlayerCorpseSyncS2C(
            UUID playerUuid,       // ✅ 改为 UUID
            boolean isCorpse,
            int corpseType,
            CompoundTag corpseData
    ) implements CustomPacketPayload {
        public static final Type<PlayerCorpseSyncS2C> TYPE = new Type<>(id("player_corpse_sync"));

        public static final StreamCodec<ByteBuf, PlayerCorpseSyncS2C> CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8,
                p -> p.playerUuid().toString(),
                ByteBufCodecs.BOOL,
                PlayerCorpseSyncS2C::isCorpse,
                ByteBufCodecs.INT,
                PlayerCorpseSyncS2C::corpseType,
                ByteBufCodecs.COMPOUND_TAG,
                PlayerCorpseSyncS2C::corpseData,
                (uuidStr, isCorpse, corpseType, corpseData) ->
                        new PlayerCorpseSyncS2C(UUID.fromString(uuidStr), isCorpse, corpseType, corpseData)
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /**
     * 克隆身体的"角色外观"同步（S2C，按身体 UUID 索引）。
     * <p>
     * 尸兄 NBT（外骨骼/红眼/皮肤）走 {@link PlayerCorpseSyncS2C}；本包负责"角色相关的附加骨骼"：
     * 角色 id（复制层里的角色服装层靠 ROLE 附件，这里只给渲染层做兜底判断）、
     * 整份进化部件（翅膀/鱼鳃）、金刚婴儿与巨熊臂标记。
     * <p>
     * 发送端有两个：克隆仓 BE（键为仓的身体稳定 UUID）和苏醒后的克隆分身实体（键为分身 UUID）。
     */
    public record CloneBodySyncS2C(
            UUID bodyUuid,
            String characterId,
            CompoundTag evolutionParts,
            boolean infant,
            int bearArms
    ) implements CustomPacketPayload {
        public static final Type<CloneBodySyncS2C> TYPE = new Type<>(id("clone_body_sync"));

        public static final StreamCodec<ByteBuf, CloneBodySyncS2C> CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8,
                p -> p.bodyUuid().toString(),
                ByteBufCodecs.STRING_UTF8,
                CloneBodySyncS2C::characterId,
                ByteBufCodecs.COMPOUND_TAG,
                CloneBodySyncS2C::evolutionParts,
                ByteBufCodecs.BOOL,
                CloneBodySyncS2C::infant,
                ByteBufCodecs.VAR_INT,
                CloneBodySyncS2C::bearArms,
                (uuidStr, characterId, evolutionParts, infant, bearArms) ->
                        new CloneBodySyncS2C(UUID.fromString(uuidStr), characterId, evolutionParts, infant, bearArms)
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /**
     * Flashback 回放专用：把某位玩家的 {@code corpseorigin:evolution_parts} 附件整份带回放端（S2C）。
     * <p>
     * Flashback 的快照（初始加载 / seek）只重建原版实体数据，不含 Fabric 附件；而进化器官装配方案、
     * 器官阶段、wings/gills、葫芦状态全部存在这个附件里。录制端把快照时刻的附件 NBT 经此通道补发，
     * 回放客户端按 UUID 找到重建出的玩家实体后直接回填。正常服务端不会发送此通道。
     */
    public record ReplayPlayerBodyS2C(UUID playerUuid, CompoundTag body) implements CustomPacketPayload {
        public static final Type<ReplayPlayerBodyS2C> TYPE = new Type<>(id("replay_player_body"));

        public static final StreamCodec<ByteBuf, ReplayPlayerBodyS2C> CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8,
                p -> p.playerUuid().toString(),
                ByteBufCodecs.COMPOUND_TAG,
                ReplayPlayerBodyS2C::body,
                (uuidStr, body) -> new ReplayPlayerBodyS2C(UUID.fromString(uuidStr), body)
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    // ==================== ✅ 学习技能（C2S） ====================

    public record LearnSkillC2S(String skillPath) implements CustomPacketPayload {
        public static final Type<LearnSkillC2S> TYPE = new Type<>(id("learn_skill"));

        public static final StreamCodec<ByteBuf, LearnSkillC2S> CODEC =
                CustomPacketPayload.codec(
                        (payload, buf) -> ByteBufCodecs.STRING_UTF8.encode(buf, payload.skillPath()),
                        buf -> new LearnSkillC2S(ByteBufCodecs.STRING_UTF8.decode(buf)));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    // ==================== ✅ 技能激活（C2S） ====================

    public record ActivateSkillC2S(String skillPath) implements CustomPacketPayload {
        public static final Type<ActivateSkillC2S> TYPE = new Type<>(id("activate_skill"));

        public static final StreamCodec<ByteBuf, ActivateSkillC2S> CODEC =
                CustomPacketPayload.codec(
                        (payload, buf) -> ByteBufCodecs.STRING_UTF8.encode(buf, payload.skillPath()),
                        buf -> new ActivateSkillC2S(ByteBufCodecs.STRING_UTF8.decode(buf)));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    // ==================== ✅ 进化/技能同步（S2C） ====================

    public record EvolutionSyncS2C(int earnedPoints, int availablePoints, int kills, byte[] learnedSkills) implements CustomPacketPayload {
        public static final Type<EvolutionSyncS2C> TYPE = new Type<>(id("evolution_sync"));

        public static final StreamCodec<ByteBuf, EvolutionSyncS2C> CODEC = StreamCodec.composite(
                ByteBufCodecs.INT,
                EvolutionSyncS2C::earnedPoints,
                ByteBufCodecs.INT,
                EvolutionSyncS2C::availablePoints,
                ByteBufCodecs.INT,
                EvolutionSyncS2C::kills,
                ByteBufCodecs.BYTE_ARRAY,
                EvolutionSyncS2C::learnedSkills,
                EvolutionSyncS2C::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    // ==================== ✅ 技能冷却同步（S2C） ====================

    public record CooldownSyncS2C(String skillPath, int ticks) implements CustomPacketPayload {
        public static final Type<CooldownSyncS2C> TYPE = new Type<>(id("cooldown_sync"));

        public static final StreamCodec<ByteBuf, CooldownSyncS2C> CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8,
                CooldownSyncS2C::skillPath,
                ByteBufCodecs.INT,
                CooldownSyncS2C::ticks,
                CooldownSyncS2C::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    // ==================== ✅ 内力同步（S2C） ====================

    public record InnerPowerSyncS2C(int current, int max) implements CustomPacketPayload {

        public static final Type<InnerPowerSyncS2C> TYPE = new Type<>(id("inner_power_sync"));

        public static final StreamCodec<ByteBuf, InnerPowerSyncS2C> CODEC = StreamCodec.composite(
                ByteBufCodecs.INT,
                InnerPowerSyncS2C::current,
                ByteBufCodecs.INT,
                InnerPowerSyncS2C::max,
                InnerPowerSyncS2C::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    // ==================== ✅ 尸王雷电特效（S2C） ====================

    /**
     * 一道紫色雷电：从 from 劈到 to，客户端渲染 durationTicks 后消散。
     * <p>
     * 几何和血莲宝灯那条链子共用同一套（赫兹波形 + 双层发光），只是换成紫白配色 ——
     * 落雷、球状闪电炸开时的电弧都用这个包。
     */
    public record ThunderBoltFxS2C(
            double fromX, double fromY, double fromZ,
            double toX, double toY, double toZ,
            int durationTicks,
            float width
    ) implements CustomPacketPayload {

        public static final Type<ThunderBoltFxS2C> TYPE = new Type<>(id("thunder_bolt_fx"));

        public static final StreamCodec<ByteBuf, ThunderBoltFxS2C> CODEC =
                StreamCodec.ofMember(ThunderBoltFxS2C::write, ThunderBoltFxS2C::read);

        private static ThunderBoltFxS2C read(ByteBuf buf) {
            return new ThunderBoltFxS2C(
                    buf.readDouble(), buf.readDouble(), buf.readDouble(),
                    buf.readDouble(), buf.readDouble(), buf.readDouble(),
                    buf.readInt(), buf.readFloat());
        }

        private void write(ByteBuf buf) {
            buf.writeDouble(fromX);
            buf.writeDouble(fromY);
            buf.writeDouble(fromZ);
            buf.writeDouble(toX);
            buf.writeDouble(toY);
            buf.writeDouble(toZ);
            buf.writeInt(durationTicks);
            buf.writeFloat(width);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static ThunderBoltFxS2C create(Vec3 from, Vec3 to, int durationTicks, float width) {
            return new ThunderBoltFxS2C(from.x, from.y, from.z, to.x, to.y, to.z,
                    durationTicks, width);
        }

        public Vec3 getFrom() {
            return new Vec3(fromX, fromY, fromZ);
        }

        public Vec3 getTo() {
            return new Vec3(toX, toY, toZ);
        }
    }
}
