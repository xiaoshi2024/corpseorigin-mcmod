package xiaoshi2022.corpseorigin.component;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import xiaoshi2022.corpseorigin.registry.ModDataAttachments;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 玩家持有的「器官 / 收藏品」记录。
 * <p>
 * 这些获取物<b>不占背包</b>，直接挂在玩家身上（见 {@link ModDataAttachments#PLAYER_RELICS}），
 * 由事件或指令授予 —— 例如把「黑金心脏」授给玩家，就会满足 {@code black_gold_heart}
 * 技能的获取式解锁条件（见 {@link xiaoshi2022.corpseorigin.skill.unlock.SkillUnlockManager}）。
 * <p>
 * 判定用的是「拥有」而不是「拾取瞬间」，所以掉落、存入箱子、被别人拿走这些情况都天然正确。
 */
public final class PlayerRelicComponent {

    private static final String KEY_RELICS = "relics";

    private PlayerRelicComponent() {
    }

    /** 已获得的全部器官 / 收藏品 id */
    public static Set<String> all(Player player) {
        CompoundTag relics = relics(player);
        return Collections.unmodifiableSet(new LinkedHashSet<>(relics.keySet()));
    }

    /** 是否持有某个器官 / 收藏品 */
    public static boolean has(Player player, String relicId) {
        return relics(player).getBoolean(relicId).orElse(false);
    }

    /**
     * 授予一个器官 / 收藏品。
     *
     * @return true 表示本次真的新增了（之前没有）
     */
    public static boolean grant(Player player, String relicId) {
        if (has(player, relicId)) {
            return false;
        }
        CompoundTag tag = getData(player);
        CompoundTag relics = tag.getCompound(KEY_RELICS).orElseGet(CompoundTag::new);
        relics.putBoolean(relicId, true);
        tag.put(KEY_RELICS, relics);
        setData(player, tag);
        return true;
    }

    /**
     * 收回一个器官 / 收藏品。
     *
     * @return true 表示本次真的移除了
     */
    public static boolean revoke(Player player, String relicId) {
        if (!has(player, relicId)) {
            return false;
        }
        CompoundTag tag = getData(player);
        CompoundTag relics = tag.getCompound(KEY_RELICS).orElseGet(CompoundTag::new);
        relics.remove(relicId);
        tag.put(KEY_RELICS, relics);
        setData(player, tag);
        return true;
    }

    /** 已获得的器官里，是不是所有 id 都拿到了（进度/成就类判定用） */
    public static boolean hasAll(Player player, Iterable<String> relicIds) {
        for (String id : relicIds) {
            if (!has(player, id)) {
                return false;
            }
        }
        return true;
    }

    private static CompoundTag relics(Player player) {
        return getData(player).getCompound(KEY_RELICS).orElseGet(CompoundTag::new);
    }

    private static CompoundTag getData(Player player) {
        return player.getAttachedOrCreate(ModDataAttachments.PLAYER_RELICS).copy();
    }

    private static void setData(Player player, CompoundTag tag) {
        player.setAttached(ModDataAttachments.PLAYER_RELICS, tag);
    }

    /** 便捷：给指定 UUID 的在线玩家授予（找不到就返回 false），指令里常用 */
    public static boolean grantIfOnline(net.minecraft.server.MinecraftServer server,
                                        UUID uuid, String relicId) {
        Player player = server.getPlayerList().getPlayer(uuid);
        return player != null && grant(player, relicId);
    }
}
