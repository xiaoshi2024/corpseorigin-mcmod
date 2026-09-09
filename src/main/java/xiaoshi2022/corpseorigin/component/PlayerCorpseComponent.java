package xiaoshi2022.corpseorigin.component;

import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import xiaoshi2022.corpseorigin.network.CorpseNetwork;
import xiaoshi2022.corpseorigin.registry.ModDataComponents;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class PlayerCorpseComponent {

    private static final String KEY_ORIGINAL_NAME = "original_name";
    private static final String KEY_SKIN_UUID = "skin_uuid";
    private static final String KEY_EVOLUTION_LEVEL = "evolution_level";
    private static final String KEY_KILLS = "kills";
    private static final String KEY_HUNGER = "hunger";
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

    private final Player player;

    // 回退缓存（当 DataComponent 不可用时使用）
    private static final Map<java.util.UUID, CompoundTag> fallbackCache = new ConcurrentHashMap<>();

    public PlayerCorpseComponent(Player player) {
        this.player = player;
    }

    // ==================== 数据读写 ====================

    @SuppressWarnings("unchecked")
    private CompoundTag getData() {
        // 方式1：尝试从 DataComponent 获取
        if (player instanceof DataComponentHolder holder) {
            CompoundTag tag = holder.get(ModDataComponents.PLAYER_CORPSE);
            if (tag != null) {
                return tag;
            }
        }
        // 方式2：从回退缓存获取
        java.util.UUID uuid = player.getUUID();
        CompoundTag tag = fallbackCache.get(uuid);
        if (tag == null) {
            tag = new CompoundTag();
            fallbackCache.put(uuid, tag);
        }
        return tag;
    }

    // ✅ 添加公共方法供网络同步使用
    public CompoundTag getDataPublic() {
        return getData();
    }

    @SuppressWarnings("unchecked")
    private void setData(CompoundTag tag) {
        // 方式1：尝试设置到 DataComponent
        try {
            if (player instanceof net.minecraft.world.entity.Entity) {
                try {
                    java.lang.reflect.Method setMethod = player.getClass().getMethod("setDataComponent", DataComponentType.class, Object.class);
                    setMethod.invoke(player, ModDataComponents.PLAYER_CORPSE, tag);
                    return;
                } catch (Exception e) {
                    // 反射失败，使用回退
                }
            }
        } catch (Exception e) {
            // 忽略
        }

        // 方式2：回退到缓存
        fallbackCache.put(player.getUUID(), tag);
    }

    // ==================== 核心状态 ====================

    public boolean isCorpse() {
        Optional<Boolean> value = getData().getBoolean(KEY_IS_CORPSE);
        return value.orElse(false);
    }

    public void setCorpse(boolean isCorpse) {
        CompoundTag tag = getData();
        tag.putBoolean(KEY_IS_CORPSE, isCorpse);
        setData(tag);
    }

    public int getCorpseType() {
        Optional<Integer> value = getData().getInt(KEY_CORPSE_TYPE);
        return value.orElse(0);
    }

    public void setCorpseType(int type) {
        CompoundTag tag = getData();
        tag.putInt(KEY_CORPSE_TYPE, type);
        setData(tag);
    }

    // ==================== 基础信息 ====================

    public String getOriginalName() {
        Optional<String> value = getData().getString(KEY_ORIGINAL_NAME);
        return value.orElse("");
    }

    public void setOriginalName(String name) {
        CompoundTag tag = getData();
        tag.putString(KEY_ORIGINAL_NAME, name);
        setData(tag);
    }

    public String getSkinUuid() {
        Optional<String> value = getData().getString(KEY_SKIN_UUID);
        return value.orElse("");
    }

    public void setSkinUuid(String uuid) {
        CompoundTag tag = getData();
        tag.putString(KEY_SKIN_UUID, uuid);
        setData(tag);
    }

    // ==================== 进化系统 ====================

    public int getEvolutionLevel() {
        Optional<Integer> value = getData().getInt(KEY_EVOLUTION_LEVEL);
        return value.orElse(1);
    }

    public void setEvolutionLevel(int level) {
        CompoundTag tag = getData();
        tag.putInt(KEY_EVOLUTION_LEVEL, Math.max(1, Math.min(5, level)));
        setData(tag);
    }

    // ==================== 击杀 ====================

    public int getKills() {
        Optional<Integer> value = getData().getInt(KEY_KILLS);
        return value.orElse(0);
    }

    public void addKill() {
        CompoundTag tag = getData();
        int kills = getKills();
        tag.putInt(KEY_KILLS, kills + 1);
        setData(tag);
    }

    // ==================== 饥饿值 ====================

    public int getHunger() {
        Optional<Integer> value = getData().getInt(KEY_HUNGER);
        return value.orElse(100);
    }

    public void setHunger(int hunger) {
        CompoundTag tag = getData();
        tag.putInt(KEY_HUNGER, Math.max(0, Math.min(100, hunger)));
        setData(tag);
    }

    // ==================== 特性 ====================

    public boolean isGreedy() {
        Optional<Boolean> value = getData().getBoolean(KEY_IS_GREEDY);
        return value.orElse(false);
    }

    public void setGreedy(boolean greedy) {
        CompoundTag tag = getData();
        tag.putBoolean(KEY_IS_GREEDY, greedy);
        setData(tag);
    }

    public int getVariant() {
        Optional<Integer> value = getData().getInt(KEY_VARIANT);
        return value.orElse(0);
    }

    public void setVariant(int variant) {
        CompoundTag tag = getData();
        tag.putInt(KEY_VARIANT, variant);
        setData(tag);
    }

    // ==================== 身体部件 ====================

    public boolean hasWing() {
        Optional<Boolean> value = getData().getBoolean(KEY_HAS_WING);
        return value.orElse(false);
    }

    public void setHasWing(boolean hasWing) {
        CompoundTag tag = getData();
        tag.putBoolean(KEY_HAS_WING, hasWing);
        setData(tag);
    }

    public boolean hasTail() {
        Optional<Boolean> value = getData().getBoolean(KEY_HAS_TAIL);
        return value.orElse(false);
    }

    public void setHasTail(boolean hasTail) {
        CompoundTag tag = getData();
        tag.putBoolean(KEY_HAS_TAIL, hasTail);
        setData(tag);
    }

    public boolean isDisguised() {
        Optional<Boolean> value = getData().getBoolean(KEY_IS_DISGUISED);
        return value.orElse(false);
    }

    public void setDisguised(boolean disguised) {
        CompoundTag tag = getData();
        tag.putBoolean(KEY_IS_DISGUISED, disguised);
        setData(tag);
    }

    // ==================== 多眼系统 ====================

    public int getExtraEyeCount() {
        Optional<Integer> value = getData().getInt(KEY_EXTRA_EYE_COUNT);
        return value.orElse(0);
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
        Optional<Boolean> restored = tag.getBoolean(KEY_CONSCIOUSNESS_RESTORED);
        if (restored.orElse(false)) {
            return true;
        }
        Optional<Boolean> innate = tag.getBoolean(KEY_HAS_CONSCIOUSNESS);
        return innate.orElse(false);
    }

    public boolean hasInnateConsciousness() {
        Optional<Boolean> value = getData().getBoolean(KEY_HAS_CONSCIOUSNESS);
        return value.orElse(false);
    }

    public boolean isConsciousnessRestored() {
        Optional<Boolean> value = getData().getBoolean(KEY_CONSCIOUSNESS_RESTORED);
        return value.orElse(false);
    }

    public void restoreConsciousness() {
        CompoundTag tag = getData();
        tag.putBoolean(KEY_CONSCIOUSNESS_RESTORED, true);
        setData(tag);
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

    public static void setPlayerAsCorpse(Player player, int corpseType) {
        PlayerCorpseComponent comp = get(player);
        comp.setCorpse(true);
        comp.setCorpseType(corpseType);
        comp.setOriginalName(player.getName().getString());
        comp.setSkinUuid(player.getUUID().toString());
        comp.setEvolutionLevel(1);
        comp.setHunger(100);
        comp.setGreedy(player.getRandom().nextFloat() < 0.5f);
        comp.setVariant(player.getRandom().nextFloat() < 0.3f ? 1 : 0);
        comp.setHasWing(false);
        comp.setHasTail(false);
        comp.setDisguised(false);
        comp.setExtraEyeCount(0);

        boolean hasConsciousness = player.getRandom().nextFloat() < CONSCIOUSNESS_RETAIN_CHANCE;
        CompoundTag tag = comp.getData();
        tag.putBoolean(KEY_HAS_CONSCIOUSNESS, hasConsciousness);
        tag.putBoolean(KEY_CONSCIOUSNESS_RESTORED, false);
        comp.setData(tag);

        syncToClient(player);
    }

    public static void removeCorpseState(Player player) {
        PlayerCorpseComponent comp = get(player);
        comp.setCorpse(false);
        comp.setCorpseType(0);
        comp.setData(new CompoundTag());
        syncToClient(player);
    }

    private static void syncToClient(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            CorpseNetwork.sendPlayerCorpseSync(serverPlayer);
        }
    }
}