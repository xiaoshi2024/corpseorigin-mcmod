package xiaoshi2022.corpseorigin.component;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import xiaoshi2022.corpseorigin.limb.LimbSlots;
import xiaoshi2022.corpseorigin.limb.LimbState;
import xiaoshi2022.corpseorigin.network.CorpseNetwork;
import xiaoshi2022.corpseorigin.registry.ModDataAttachments;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerCorpseComponent {

    private static final String KEY_ORIGINAL_NAME = "original_name";
    private static final String KEY_SKIN_UUID = "skin_uuid";
    private static final String KEY_EVOLUTION_LEVEL = "evolution_level";
    private static final String KEY_KILLS = "kills";
    private static final String KEY_IS_GREEDY = "is_greedy";
    private static final String KEY_VARIANT = "variant";
    private static final String KEY_HAS_WING = "has_wing";
    private static final String KEY_HAS_TAIL = "has_tail";
    private static final String KEY_IS_DISGUISED = "is_disguised";
    private static final String KEY_EXTRA_EYE_COUNT = "extra_eye_count";
    private static final String KEY_HAS_CONSCIOUSNESS = "has_consciousness";
    private static final String KEY_CONSCIOUSNESS_RESTORED = "consciousness_restored";
    private static final String KEY_IS_CORPSE = "is_corpse";
    private static final String KEY_CORPSE_TYPE = "corpse_type";

    // ==================== 断肢 / 再生 ====================
    private static final String KEY_LIMB_MASK = "limb_mask";
    private static final String KEY_LIMB_REGROW = "limb_regrow_ticks";
    private static final String KEY_LIMB_TOTALS = "limb_regrow_totals";
    private static final String KEY_LIMB_COOLDOWNS = "limb_cooldowns";

    public static final float CONSCIOUSNESS_RETAIN_CHANCE = 0.05f;

    private static final String KEY_INFECTION = "infection";

    // ==================== 尸兄类型常量 ====================
    /** 普通尸兄 */
    public static final int TYPE_NORMAL = 0;
    /** 精英尸兄 */
    public static final int TYPE_ELITE = 1;
    /** 尸王（龙右） */
    public static final int TYPE_KING = 2;

    // ==================== 尸兄变种常量 ====================
    /** 无外骨骼通用变种：算尸兄，但不长尸眼骨骼，外观完全交给盔甲/模型自己表现 */
    public static final int VARIANT_NO_EXOSKELETON = 2;
    /**
     * 左护法变异体变种：算尸兄，但整具外观换成 {@code zuo_guardian} 变异体模型
     * （见 {@code ZuoGuardianBodyRenderer}），原版玩家模型、盔甲一律不画，也不长尸眼骨骼。
     */
    public static final int VARIANT_ZUO_GUARDIAN = 3;
    /**
     * 开胃奶「背挂」变种：算尸兄，但不长通用尸眼骨骼，改在背后挂 {@code niunaix} 那套
     * 触角 / 捆仙索 / 菊花盾（见 {@code NiunaiXRenderer}）。原版玩家模型、盔甲照常渲染。
     */
    public static final int VARIANT_NIUNAIX = 4;
    /** 尸巢之子吸收千名尸兄后主动开启的第二形态。 */
    public static final int VARIANT_SHICHAOZHIZI = 5;

    /** 进化等级上限 */
    public static final int MAX_EVOLUTION_LEVEL = 5;

    private final Player player;

    public PlayerCorpseComponent(Player player) {
        this.player = player;
    }

    public int getInfection() {
        CompoundTag data = getData();
        // 已转化的身体始终是完全感染；兼容旧存档中未写入 infection 的尸兄。
        return data.getBoolean(KEY_IS_CORPSE).orElse(false)
                ? 100 : data.getInt(KEY_INFECTION).orElse(0);
    }

    public void setInfection(int value) {
        CompoundTag tag = getData();
        tag.putInt(KEY_INFECTION, tag.getBoolean(KEY_IS_CORPSE).orElse(false)
                ? 100 : Math.max(0, Math.min(100, value)));
        setData(tag);
    }

    // ==================== ✅ 数据读写（每次都返回副本） ====================

    /**
     * ✅ 获取数据副本，避免外部修改内部引用
     */
    private CompoundTag getData() {
        CompoundTag tag = player.getAttachedOrCreate(ModDataAttachments.PLAYER_CORPSE);
        // ✅ 返回副本，防止外部直接修改引用
        return tag.copy();
    }

    /**
     * ✅ 公共方法，返回副本
     */
    public CompoundTag getDataPublic() {
        return getData();
    }

    /**
     * ✅ 写入数据（保证是干净的副本）
     */
    private void setData(CompoundTag tag) {
        // ✅ 存入时也复制，避免外部引用污染
        player.setAttached(ModDataAttachments.PLAYER_CORPSE, tag.copy());
        // 标记"这具身体的尸兄数据变了"，由服务端 tick 末尾统一广播一次
        markDirty(player);
    }

    // ==================== 同步：标脏 + 合并广播 ====================

    /**
     * 本 tick 内尸兄数据改动过的玩家。
     * <p>
     * 所有 setter 都汇到 {@link #setData}，那里只做标记、不立刻发包，等到 tick 末尾由
     * {@link #flushPendingSync} 统一广播一次。这样做的原因有两个：
     * <ul>
     *   <li><b>合并连写</b> —— 进化时经常连着改等级、多眼、体型，逐个发包会连发好几个同样大的包；</li>
     *   <li><b>兜住高频</b> —— 万一某个 setter 被放进 tick 之类的循环里，每 tick 一包也不会刷爆网络。</li>
     * </ul>
     * 用 Set 保证同一玩家一个 tick 只会收到一包。
     */
    private static final Set<UUID> PENDING_SYNC = ConcurrentHashMap.newKeySet();

    /** 标记这位玩家的尸兄数据已变化，tick 末尾统一广播 */
    private static void markDirty(Player player) {
        if (player instanceof ServerPlayer sp) {
            PENDING_SYNC.add(sp.getUUID());
        }
    }

    /**
     * 由服务端 tick 末尾调用：把本 tick 标记过的玩家各广播一次。
     * <p>
     * 必须是<b>广播</b>而不是只发给本人 —— 尸兄外观（多眼 / 外骨骼 / 皮肤 / 伪装）是
     * 别的玩家看你时才渲染的，只发本人会出现"自己看得见、别人眼里还是普通人"。
     */
    public static void flushPendingSync(MinecraftServer server) {
        if (PENDING_SYNC.isEmpty()) {
            return;
        }
        for (UUID uuid : PENDING_SYNC) {
            ServerPlayer target = server.getPlayerList().getPlayer(uuid);
            if (target != null) {
                CorpseNetwork.broadcastPlayerCorpseSync(target);
            }
        }
        PENDING_SYNC.clear();
    }

    public boolean hasData() {
        CompoundTag tag = player.getAttached(ModDataAttachments.PLAYER_CORPSE);
        return tag != null && !tag.isEmpty();
    }

    // ==================== 核心状态 ====================

    public boolean isCorpse() {
        return getData().getBoolean(KEY_IS_CORPSE).orElse(false);
    }

    public void setCorpse(boolean isCorpse) {
        CompoundTag tag = getData();  // 已经是副本
        tag.putBoolean(KEY_IS_CORPSE, isCorpse);
        setData(tag);  // setData 会再复制一次
    }

    public int getCorpseType() {
        return getData().getInt(KEY_CORPSE_TYPE).orElse(0);
    }

    public void setCorpseType(int type) {
        CompoundTag tag = getData();
        tag.putInt(KEY_CORPSE_TYPE, type);
        setData(tag);
    }

    // ==================== 基础信息 ====================

    public String getOriginalName() {
        return getData().getString(KEY_ORIGINAL_NAME).orElse("");
    }

    public void setOriginalName(String name) {
        CompoundTag tag = getData();
        tag.putString(KEY_ORIGINAL_NAME, name);
        setData(tag);
    }

    public String getSkinUuid() {
        return getData().getString(KEY_SKIN_UUID).orElse("");
    }

    public void setSkinUuid(String uuid) {
        CompoundTag tag = getData();
        tag.putString(KEY_SKIN_UUID, uuid);
        setData(tag);
    }

    // ==================== 进化系统 ====================

    public int getEvolutionLevel() {
        return getData().getInt(KEY_EVOLUTION_LEVEL).orElse(1);
    }

    public void setEvolutionLevel(int level) {
        CompoundTag tag = getData();
        tag.putInt(KEY_EVOLUTION_LEVEL, Math.max(1, Math.min(5, level)));
        setData(tag);
    }

    // ==================== 击杀 ====================

    public int getKills() {
        return getData().getInt(KEY_KILLS).orElse(0);
    }

    public void addKill() {
        addKills(1);
    }

    public void addKills(int amount) {
        if (amount <= 0) return;
        CompoundTag tag = getData();
        tag.putInt(KEY_KILLS, getKills() + amount);
        setData(tag);
    }

    // ==================== 饥饿值 ====================

    /**
     * ✅ 尸兄饥饿值（0-100），映射到原版饥饿值（0-20）
     * <p>
     * 映射关系：
     * - 原版 foodLevel 0 → 尸兄 0（极度饥饿）
     * - 原版 foodLevel 20 → 尸兄 100（饱腹）
     * - 比例：1 foodLevel = 5 尸兄饥饿值
     */
    public int getHunger() {
        // 原版 0-20 → 尸兄 0-100
        return player.getFoodData().getFoodLevel() * 5;
    }

    public void setHunger(int hunger) {
        // 尸兄 0-100 → 原版 0-20
        int foodLevel = Math.max(0, Math.min(20, hunger / 5));
        player.getFoodData().setFoodLevel(foodLevel);
    }

    /**
     * ✅ 是否饥饿（用于同类相食逻辑）
     * <p>
     * 30 / 5 = foodLevel 6，即原版饥饿值低于 6 时算饥饿
     */
    public boolean isHungry() {
        return getHunger() < 30;
    }

    // ==================== 特性 ====================

    public boolean isGreedy() {
        return getData().getBoolean(KEY_IS_GREEDY).orElse(false);
    }

    public void setGreedy(boolean greedy) {
        CompoundTag tag = getData();
        tag.putBoolean(KEY_IS_GREEDY, greedy);
        setData(tag);
    }

    public int getVariant() {
        return getData().getInt(KEY_VARIANT).orElse(0);
    }

    public void setVariant(int variant) {
        CompoundTag tag = getData();
        tag.putInt(KEY_VARIANT, variant);
        setData(tag);
    }

    // ==================== 身体部件 ====================

    public boolean hasWing() {
        return getData().getBoolean(KEY_HAS_WING).orElse(false);
    }

    public void setHasWing(boolean hasWing) {
        CompoundTag tag = getData();
        tag.putBoolean(KEY_HAS_WING, hasWing);
        setData(tag);
    }

    public boolean hasTail() {
        return getData().getBoolean(KEY_HAS_TAIL).orElse(false);
    }

    public void setHasTail(boolean hasTail) {
        CompoundTag tag = getData();
        tag.putBoolean(KEY_HAS_TAIL, hasTail);
        setData(tag);
    }

    public boolean isDisguised() {
        return getData().getBoolean(KEY_IS_DISGUISED).orElse(false);
    }

    public void setDisguised(boolean disguised) {
        CompoundTag tag = getData();
        tag.putBoolean(KEY_IS_DISGUISED, disguised);
        setData(tag);
    }

    // ==================== 多眼系统 ====================

    public int getExtraEyeCount() {
        return getData().getInt(KEY_EXTRA_EYE_COUNT).orElse(0);
    }

    public void setExtraEyeCount(int count) {
        CompoundTag tag = getData();
        tag.putInt(KEY_EXTRA_EYE_COUNT, Math.max(0, Math.min(9, count)));
        setData(tag);
    }

    public void addExtraEye() {
        int current = getExtraEyeCount();
        if (current < 9) {
            setExtraEyeCount(current + 1);
        }
    }

    public boolean hasMultiEye() {
        return getExtraEyeCount() > 0;
    }

    // ==================== 意识系统 ====================

    public boolean hasConsciousness() {
        CompoundTag tag = getData();
        if (tag.getBoolean(KEY_CONSCIOUSNESS_RESTORED).orElse(false)) {
            return true;
        }
        return tag.getBoolean(KEY_HAS_CONSCIOUSNESS).orElse(false);
    }

    public boolean hasInnateConsciousness() {
        return getData().getBoolean(KEY_HAS_CONSCIOUSNESS).orElse(false);
    }

    public boolean isConsciousnessRestored() {
        return getData().getBoolean(KEY_CONSCIOUSNESS_RESTORED).orElse(false);
    }

    public void restoreConsciousness() {
        CompoundTag tag = getData();
        tag.putBoolean(KEY_CONSCIOUSNESS_RESTORED, true);
        setData(tag);
    }

    public void readNbt(CompoundTag tag) {
        if (tag == null) tag = new CompoundTag();
        player.setAttached(ModDataAttachments.PLAYER_CORPSE, tag.copy());
        // 外部整份写入（存档恢复等）也走广播，保持和其他路径一致
        if (player instanceof ServerPlayer sp) {
            CorpseNetwork.broadcastPlayerCorpseSync(sp);
        }
    }

    public boolean isMindless() {
        return isCorpse() && !hasConsciousness();
    }

    // ==================== 断肢 / 再生 ====================

    /**
     * 一次性读出断肢状态（只复制一次 NBT）。
     * <p>
     * 客户端读这份数据的路径是 {@code CorpseOriginClient.corpseDataCache}（同样由
     * PlayerCorpseSyncS2C 携带的整份 tag 驱动），所以断肢状态跟着尸兄数据一起走，
     * 不需要单独的网络包。
     */
    public LimbState readLimbs() {
        CompoundTag tag = getData();
        return new LimbState(
                tag.getByteOr(KEY_LIMB_MASK, LimbSlots.NONE),
                toSlots(tag.getIntArray(KEY_LIMB_REGROW).orElse(null), LimbSlots.REGROW_PERMANENT),
                toSlots(tag.getIntArray(KEY_LIMB_TOTALS).orElse(null), 0),
                toSlots(tag.getIntArray(KEY_LIMB_COOLDOWNS).orElse(null), 0));
    }

    /** 一次性写入断肢状态（只复制一次 NBT） */
    public void writeLimbs(LimbState state) {
        CompoundTag tag = getData();
        tag.putByte(KEY_LIMB_MASK, state.mask());
        tag.putIntArray(KEY_LIMB_REGROW, state.regrowTicks());
        tag.putIntArray(KEY_LIMB_TOTALS, state.totals());
        tag.putIntArray(KEY_LIMB_COOLDOWNS, state.cooldowns());
        setData(tag);
    }

    /** 清掉断肢状态（失去黑小飞身份时用），返回是否原本有断肢 */
    public boolean clearLimbs() {
        CompoundTag tag = getData();
        if (!tag.getByte(KEY_LIMB_MASK).isPresent()) {
            return false;
        }
        tag.remove(KEY_LIMB_MASK);
        tag.remove(KEY_LIMB_REGROW);
        tag.remove(KEY_LIMB_TOTALS);
        tag.remove(KEY_LIMB_COOLDOWNS);
        setData(tag);
        return true;
    }

    /** NBT 里的数组补齐成 4 长，缺失部分用 fill */
    private static int[] toSlots(int[] raw, int fill) {
        int[] result = new int[LimbSlots.COUNT];
        java.util.Arrays.fill(result, fill);
        if (raw != null) {
            System.arraycopy(raw, 0, result, 0, Math.min(raw.length, result.length));
        }
        return result;
    }

    // ==================== 静态工具方法 ====================

    public static PlayerCorpseComponent get(Player player) {
        return new PlayerCorpseComponent(player);
    }

    public static boolean isCorpse(Player player) {
        return get(player).isCorpse();
    }

    /**
     * 轻量判定：这具身体现在是不是「左护法变异体」形态（尸兄 + 非伪装 + 变种 3）。
     * <p>
     * 直接读附件、<b>不做 NBT 副本</b> —— 这个方法会被每 tick 的碰撞箱 / 拾取逻辑反复调用，
     * 而 {@link #getData()} 每次都会复制整份 tag。判定条件与客户端那份缓存（
     * {@code MutantBodyRenderData}）保持一致。
     */
    public static boolean isMutantVariant(Player player) {
        CompoundTag tag = player.getAttached(ModDataAttachments.PLAYER_CORPSE);
        if (tag == null) {
            return false;
        }
        return tag.getBoolean(KEY_IS_CORPSE).orElse(false)
                && !tag.getBoolean(KEY_IS_DISGUISED).orElse(false)
                && tag.getInt(KEY_VARIANT).orElse(0) == VARIANT_ZUO_GUARDIAN;
    }

    /**
     * 这个变种要不要长那根尸眼骨骼。
     * <p>
     * 自带整套外观的变种（天线宝宝尸兄盔甲 {@link #VARIANT_NO_EXOSKELETON}、左护法变异体
     * {@link #VARIANT_ZUO_GUARDIAN}、开胃奶背挂 {@link #VARIANT_NIUNAIX}）都返回 {@code false}
     * —— 它们算尸兄，但外观不靠这根骨骼。
     */
    public static boolean hasExoskeleton(int variant) {
        return variant != VARIANT_NO_EXOSKELETON
                && variant != VARIANT_ZUO_GUARDIAN
                && variant != VARIANT_NIUNAIX
                && variant != VARIANT_SHICHAOZHIZI;
    }

    /**
     * 轻量判定：这具身体现在是不是「开胃奶背挂」形态（尸兄 + 非伪装 + 变种 4）。
     * <p>
     * 与 {@link #isMutantVariant} 同样直接读附件、不做 NBT 副本 —— 判定条件与客户端那份缓存
     * （{@code NiunaiXRenderData}）保持一致。
     */
    public static boolean isNiunaiVariant(Player player) {
        CompoundTag tag = player.getAttached(ModDataAttachments.PLAYER_CORPSE);
        if (tag == null) {
            return false;
        }
        return tag.getBoolean(KEY_IS_CORPSE).orElse(false)
                && !tag.getBoolean(KEY_IS_DISGUISED).orElse(false)
                && tag.getInt(KEY_VARIANT).orElse(0) == VARIANT_NIUNAIX;
    }

    /**
     * 轻量判定：这具身体现在是不是「尸巢之子第二形态」（尸兄 + 非伪装 + 变种 5）。
     * <p>
     * 与 {@link #isMutantVariant} 同样直接读附件、不做 NBT 副本 —— 这个方法会被
     * {@code PlayerDimensionsMixin} 每次取碰撞箱时调用。判定条件与客户端那份缓存
     * （{@code ShiChaoBodyRenderData}）保持一致。
     */
    public static boolean isShiChaoVariant(Player player) {
        CompoundTag tag = player.getAttached(ModDataAttachments.PLAYER_CORPSE);
        if (tag == null) {
            return false;
        }
        return tag.getBoolean(KEY_IS_CORPSE).orElse(false)
                && !tag.getBoolean(KEY_IS_DISGUISED).orElse(false)
                && tag.getInt(KEY_VARIANT).orElse(0) == VARIANT_SHICHAOZHIZI;
    }

    /**
     * ✅ 一次性写入所有数据（避免多次 setData）
     */
    public static void setPlayerAsCorpse(Player player, int corpseType) {
        setPlayerAsCorpse(player, corpseType, player.getRandom().nextFloat() < 0.3f ? 1 : 0);
    }

    /**
     * ✅ 同上，但显式指定变种。
     * <p>
     * 变种用来给"通用尸兄"分外观流派，目前：
     * <ul>
     *   <li>0 / 1 —— 随机分的普通流派，照旧长尸眼骨骼；</li>
     *   <li>{@link #VARIANT_NO_EXOSKELETON} —— 算尸兄但<b>不长</b>尸眼骨骼，
     *       给自带整套外观（例如天线宝宝尸兄盔甲）的角色用。</li>
     * </ul>
     */
    public static void setPlayerAsCorpse(Player player, int corpseType, int variant) {
        CompoundTag tag = new CompoundTag();

        // ✅ 一次性构建所有数据
        tag.putBoolean(KEY_IS_CORPSE, true);
        tag.putInt(KEY_INFECTION, 100);
        tag.putInt(KEY_CORPSE_TYPE, corpseType);
        tag.putString(KEY_ORIGINAL_NAME, player.getName().getString());
        tag.putString(KEY_SKIN_UUID, player.getUUID().toString());
        tag.putInt(KEY_EVOLUTION_LEVEL, 1);
        tag.putBoolean(KEY_IS_GREEDY, player.getRandom().nextFloat() < 0.5f);
        tag.putInt(KEY_VARIANT, variant);
        tag.putBoolean(KEY_HAS_WING, false);
        tag.putBoolean(KEY_HAS_TAIL, false);
        tag.putBoolean(KEY_IS_DISGUISED, false);
        tag.putInt(KEY_EXTRA_EYE_COUNT, 0);

        boolean hasConsciousness = player.getRandom().nextFloat() < CONSCIOUSNESS_RETAIN_CHANCE;
        tag.putBoolean(KEY_HAS_CONSCIOUSNESS, hasConsciousness);
        tag.putBoolean(KEY_CONSCIOUSNESS_RESTORED, false);

        // ✅ 一次性写入
        player.setAttached(ModDataAttachments.PLAYER_CORPSE, tag);

        // ✅ 只同步一次
        syncToClient(player);

        // ★ 成就：成为尸兄。尸水感染、选到尸兄角色、被夺舍成尸兄……全都汇到这一个写入口，
        //   所以挂在这里就够了
        if (player instanceof ServerPlayer serverPlayer) {
            xiaoshi2022.corpseorigin.advancement.CorpseAdvancements.BECOME_CORPSE.trigger(serverPlayer);
        }
    }

    public static void removeCorpseState(Player player) {
        player.setAttached(ModDataAttachments.PLAYER_CORPSE, new CompoundTag());
        syncToClient(player);
    }

    private static void syncToClient(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            CorpseNetwork.sendInfectionSync(serverPlayer);
            // ★ 必须广播而不是只发给自己：尸兄外观是"别人看你"时才渲染的，
            //   只发本人会导致切换角色后自己看得见、其他玩家眼里还是普通人。
            //   这个方法只在"变成/失去尸兄"这种低频且影响外观的操作里调用，广播开销可以忽略。
            CorpseNetwork.broadcastPlayerCorpseSync(serverPlayer);
        }
    }
}
