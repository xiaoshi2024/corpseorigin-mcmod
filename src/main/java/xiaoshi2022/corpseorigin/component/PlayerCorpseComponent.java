package xiaoshi2022.corpseorigin.component;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import xiaoshi2022.corpseorigin.network.CorpseNetwork;
import xiaoshi2022.corpseorigin.registry.ModDataAttachments;

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

    public static final float CONSCIOUSNESS_RETAIN_CHANCE = 0.05f;

    private static final String KEY_INFECTION = "infection";

    // ==================== 尸兄类型常量 ====================
    /** 普通尸兄 */
    public static final int TYPE_NORMAL = 0;
    /** 精英尸兄 */
    public static final int TYPE_ELITE = 1;
    /** 尸王（龙右） */
    public static final int TYPE_KING = 2;

    /** 进化等级上限 */
    public static final int MAX_EVOLUTION_LEVEL = 5;

    private final Player player;

    public PlayerCorpseComponent(Player player) {
        this.player = player;
    }

    public int getInfection() {
        return getData().getInt(KEY_INFECTION).orElse(0);
    }

    public void setInfection(int value) {
        CompoundTag tag = getData();
        tag.putInt(KEY_INFECTION, Math.max(0, Math.min(100, value)));
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
        CompoundTag tag = getData();
        tag.putInt(KEY_KILLS, getKills() + 1);
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
        if (player instanceof ServerPlayer sp) {
            CorpseNetwork.sendPlayerCorpseSync(sp);
        }
    }

    public boolean isMindless() {
        return isCorpse() && !hasConsciousness();
    }

    // ==================== 静态工具方法 ====================

    public static PlayerCorpseComponent get(Player player) {
        return new PlayerCorpseComponent(player);
    }

    public static boolean isCorpse(Player player) {
        return get(player).isCorpse();
    }

    /**
     * ✅ 一次性写入所有数据（避免多次 setData）
     */
    public static void setPlayerAsCorpse(Player player, int corpseType) {
        CompoundTag tag = new CompoundTag();

        // ✅ 一次性构建所有数据
        tag.putBoolean(KEY_IS_CORPSE, true);
        tag.putInt(KEY_CORPSE_TYPE, corpseType);
        tag.putString(KEY_ORIGINAL_NAME, player.getName().getString());
        tag.putString(KEY_SKIN_UUID, player.getUUID().toString());
        tag.putInt(KEY_EVOLUTION_LEVEL, 1);
        tag.putBoolean(KEY_IS_GREEDY, player.getRandom().nextFloat() < 0.5f);
        tag.putInt(KEY_VARIANT, player.getRandom().nextFloat() < 0.3f ? 1 : 0);
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
    }

    public static void removeCorpseState(Player player) {
        player.setAttached(ModDataAttachments.PLAYER_CORPSE, new CompoundTag());
        syncToClient(player);
    }

    private static void syncToClient(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            CorpseNetwork.sendPlayerCorpseSync(serverPlayer);
        }
    }
}