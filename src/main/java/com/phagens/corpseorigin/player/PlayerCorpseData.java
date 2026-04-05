package com.phagens.corpseorigin.player;

import com.phagens.corpseorigin.network.PlayerCorpseSyncPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

public class PlayerCorpseData {
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
    private static final String KEY_HAS_CONSCIOUSNESS = "has_consciousness"; // 是否有智慧/意识
    private static final String KEY_CONSCIOUSNESS_RESTORED = "consciousness_restored"; // 意识是否已恢复（通过道具或进化）

    // 保留意识的概率（极低）
    public static final float CONSCIOUSNESS_RETAIN_CHANCE = 0.05f; // 5%概率保留意识

    public static void setPlayerAsCorpse(Player player, int corpseType) {
        player.setData(CorpsePlayerAttachment.IS_CORPSE.get(), true);
        player.setData(CorpsePlayerAttachment.CORPSE_TYPE.get(), corpseType);

        CompoundTag data = new CompoundTag();
        data.putString(KEY_ORIGINAL_NAME, player.getName().getString());
        data.putString(KEY_SKIN_UUID, player.getUUID().toString());
        data.putInt(KEY_EVOLUTION_LEVEL, 1);
        data.putInt(KEY_KILLS, 0);
        data.putInt(KEY_HUNGER, 100);
        data.putBoolean(KEY_IS_GREEDY, player.getRandom().nextFloat() < 0.5f);
        data.putInt(KEY_VARIANT, player.getRandom().nextFloat() < 0.3f ? 1 : 0);
        data.putBoolean(KEY_HAS_WING, false);
        data.putBoolean(KEY_HAS_TAIL, false);
        data.putBoolean(KEY_IS_DISGUISED, false);
        data.putInt(KEY_EXTRA_EYE_COUNT, 0);

        // 意识判定：极低概率保留意识（5%），否则失去意识
        boolean hasConsciousness = player.getRandom().nextFloat() < CONSCIOUSNESS_RETAIN_CHANCE;
        data.putBoolean(KEY_HAS_CONSCIOUSNESS, hasConsciousness);
        data.putBoolean(KEY_CONSCIOUSNESS_RESTORED, false);

        player.setData(CorpsePlayerAttachment.CORPSE_DATA.get(), data);
        syncToClient(player);
    }

    public static void removeCorpseState(Player player) {
        player.setData(CorpsePlayerAttachment.IS_CORPSE.get(), false);
        player.setData(CorpsePlayerAttachment.CORPSE_TYPE.get(), 0);
        player.setData(CorpsePlayerAttachment.CORPSE_DATA.get(), new CompoundTag());
        syncToClient(player);
    }

    public static boolean isCorpse(Player player) {
        return player.getData(CorpsePlayerAttachment.IS_CORPSE.get());
    }

    public static int getCorpseType(Player player) {
        return player.getData(CorpsePlayerAttachment.CORPSE_TYPE.get());
    }

    public static CompoundTag getCorpseData(Player player) {
        return player.getData(CorpsePlayerAttachment.CORPSE_DATA.get());
    }

    public static String getOriginalName(Player player) {
        return getCorpseData(player).getString(KEY_ORIGINAL_NAME);
    }

    public static String getSkinUuid(Player player) {
        return getCorpseData(player).getString(KEY_SKIN_UUID);
    }

    public static int getEvolutionLevel(Player player) {
        return getCorpseData(player).getInt(KEY_EVOLUTION_LEVEL);
    }

    public static void setEvolutionLevel(Player player, int level) {
        CompoundTag data = getCorpseData(player);
        data.putInt(KEY_EVOLUTION_LEVEL, Math.max(1, Math.min(5, level)));
        player.setData(CorpsePlayerAttachment.CORPSE_DATA.get(), data);
        syncToClient(player);
    }

    public static int getKills(Player player) {
        return getCorpseData(player).getInt(KEY_KILLS);
    }

    public static void addKill(Player player) {
        CompoundTag data = getCorpseData(player);
        data.putInt(KEY_KILLS, data.getInt(KEY_KILLS) + 1);
        player.setData(CorpsePlayerAttachment.CORPSE_DATA.get(), data);
        syncToClient(player);
    }

    public static int getHunger(Player player) {
        return getCorpseData(player).getInt(KEY_HUNGER);
    }

    public static void setHunger(Player player, int hunger) {
        CompoundTag data = getCorpseData(player);
        data.putInt(KEY_HUNGER, Math.max(0, Math.min(100, hunger)));
        player.setData(CorpsePlayerAttachment.CORPSE_DATA.get(), data);
        syncToClient(player);
    }

    public static boolean isGreedy(Player player) {
        return getCorpseData(player).getBoolean(KEY_IS_GREEDY);
    }

    public static int getVariant(Player player) {
        return getCorpseData(player).getInt(KEY_VARIANT);
    }

    public static boolean hasWing(Player player) {
        return getCorpseData(player).getBoolean(KEY_HAS_WING);
    }

    public static void setHasWing(Player player, boolean hasWing) {
        CompoundTag data = getCorpseData(player);
        data.putBoolean(KEY_HAS_WING, hasWing);
        player.setData(CorpsePlayerAttachment.CORPSE_DATA.get(), data);
        syncToClient(player);
    }

    public static boolean hasTail(Player player) {
        return getCorpseData(player).getBoolean(KEY_HAS_TAIL);
    }

    public static void setHasTail(Player player, boolean hasTail) {
        CompoundTag data = getCorpseData(player);
        data.putBoolean(KEY_HAS_TAIL, hasTail);
        player.setData(CorpsePlayerAttachment.CORPSE_DATA.get(), data);
        syncToClient(player);
    }

    public static boolean isDisguised(Player player) {
        return getCorpseData(player).getBoolean(KEY_IS_DISGUISED);
    }

    public static void setDisguised(Player player, boolean isDisguised) {
        CompoundTag data = getCorpseData(player);
        data.putBoolean(KEY_IS_DISGUISED, isDisguised);
        player.setData(CorpsePlayerAttachment.CORPSE_DATA.get(), data);
        syncToClient(player);
    }

    public static int getExtraEyeCount(Player player) {
        return getCorpseData(player).getInt(KEY_EXTRA_EYE_COUNT);
    }

    public static void setExtraEyeCount(Player player, int count) {
        CompoundTag data = getCorpseData(player);
        data.putInt(KEY_EXTRA_EYE_COUNT, Math.max(0, Math.min(9, count)));
        player.setData(CorpsePlayerAttachment.CORPSE_DATA.get(), data);
        syncToClient(player);
    }

    public static void addExtraEye(Player player) {
        int currentCount = getExtraEyeCount(player);
        if (currentCount < 9) {
            setExtraEyeCount(player, currentCount + 1);
        }
    }

    /**
     * 检查是否进化出多眼形态
     * 当额外眼睛数量大于0时返回true
     */
    public static boolean hasMultiEye(Player player) {
        return getExtraEyeCount(player) > 0;
    }

    /**
     * 检查玩家是否有智慧/意识
     * 包括：天生保留意识 或 通过道具/进化恢复意识
     */
    public static boolean hasConsciousness(Player player) {
        CompoundTag data = getCorpseData(player);
        // 如果意识已恢复，返回true
        if (data.getBoolean(KEY_CONSCIOUSNESS_RESTORED)) {
            return true;
        }
        // 否则返回天生的意识状态
        return data.getBoolean(KEY_HAS_CONSCIOUSNESS);
    }

    /**
     * 检查玩家是否天生保留意识（初始判定）
     */
    public static boolean hasInnateConsciousness(Player player) {
        return getCorpseData(player).getBoolean(KEY_HAS_CONSCIOUSNESS);
    }

    /**
     * 检查意识是否已通过道具或进化恢复
     */
    public static boolean isConsciousnessRestored(Player player) {
        return getCorpseData(player).getBoolean(KEY_CONSCIOUSNESS_RESTORED);
    }

    /**
     * 恢复玩家的意识（通过吃穆博士眼睛等道具）
     */
    public static void restoreConsciousness(Player player) {
        CompoundTag data = getCorpseData(player);
        data.putBoolean(KEY_CONSCIOUSNESS_RESTORED, true);
        player.setData(CorpsePlayerAttachment.CORPSE_DATA.get(), data);
        syncToClient(player);
    }

    /**
     * 检查玩家是否因失去意识而无法进行复杂交互
     * 返回true表示玩家失去意识，无法交互
     */
    public static boolean isMindless(Player player) {
        return isCorpse(player) && !hasConsciousness(player);
    }

    /**
     * 检查玩家是否是尸兄（包括有意识和无意识的）
     * 用于判断是否可以食用尸体
     */
    public static boolean isCorpseBrother(Player player) {
        return isCorpse(player);
    }

    private static void syncToClient(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            PlayerCorpseSyncPacket packet = new PlayerCorpseSyncPacket(
                    serverPlayer.getId(),
                    isCorpse(serverPlayer),
                    getCorpseType(serverPlayer),
                    getCorpseData(serverPlayer).copy()
            );
            PacketDistributor.sendToPlayer(serverPlayer, packet);
        }
    }
}