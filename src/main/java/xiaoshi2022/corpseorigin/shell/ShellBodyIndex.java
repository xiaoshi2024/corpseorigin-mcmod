package xiaoshi2022.corpseorigin.shell;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;
import xiaoshi2022.corpseorigin.registry.ModDataAttachments;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * 持久化的"可夺舍身体"索引：记录每具身体所在的维度 + 坐标。
 * <p>
 * 克隆仓/分身所在区块平时是没加载的（别的维度尤其如此），只靠内存注册表找不到它们。
 * 有了这份索引，死亡夺舍时可以按需加载对应区块，再把意识送过去。
 */
public final class ShellBodyIndex {

    private static final String KEY_BODIES = "Bodies";
    private static final String KEY_DIM = "Dim";
    private static final String KEY_POS = "Pos";
    private static final String KEY_AVATAR = "Avatar";

    /** 一条身体记录：avatarId 为空 = 克隆仓里的身体；非空 = 走动的分身 */
    public record Entry(Identifier dim, BlockPos pos, @Nullable UUID avatarId) {

        public boolean isAvatar() {
            return this.avatarId != null;
        }

        public Entry withPos(BlockPos newPos) {
            return new Entry(this.dim, newPos, this.avatarId);
        }
    }

    private ShellBodyIndex() {
    }

    public static List<Entry> read(Player player) {
        List<Entry> list = new ArrayList<>();
        CompoundTag root = player.getAttached(ModDataAttachments.SHELL_BODIES);
        if (root == null) {
            return list;
        }
        root.getList(KEY_BODIES).ifPresent(tags -> {
            for (Tag element : tags) {
                if (!(element instanceof CompoundTag tag)) continue;
                Identifier dim = tag.getString(KEY_DIM).map(Identifier::tryParse).orElse(null);
                if (dim == null) continue;
                BlockPos pos = BlockPos.of(tag.getLong(KEY_POS).orElse(0L));
                UUID avatar = tag.getString(KEY_AVATAR).map(s -> {
                    try {
                        return UUID.fromString(s);
                    } catch (IllegalArgumentException e) {
                        return null;
                    }
                }).orElse(null);
                list.add(new Entry(dim, pos, avatar));
            }
        });
        return list;
    }

    /** 登记 / 更新一具身体（克隆仓按坐标去重，分身按 UUID 去重） */
    public static void upsert(ServerPlayer owner, Entry entry) {
        List<Entry> list = read(owner);
        list.removeIf(exist -> sameTarget(exist, entry));
        list.add(entry);
        write(owner, list);
    }

    /** 移除记录（仓传坐标，分身传 UUID 即可） */
    public static void remove(ServerPlayer owner, Entry key) {
        List<Entry> list = read(owner);
        if (list.removeIf(exist -> sameTarget(exist, key))) {
            write(owner, list);
        }
    }

    /** 批量移除（夺舍时清掉已经不存在的身體） */
    public static void removeAll(ServerPlayer owner, Collection<Entry> keys) {
        if (keys.isEmpty()) return;
        List<Entry> list = read(owner);
        boolean changed = false;
        for (Entry key : keys) {
            changed |= list.removeIf(exist -> sameTarget(exist, key));
        }
        if (changed) {
            write(owner, list);
        }
    }

    private static void write(Player player, List<Entry> entries) {
        CompoundTag root = new CompoundTag();
        ListTag tags = new ListTag();
        for (Entry entry : entries) {
            CompoundTag tag = new CompoundTag();
            tag.putString(KEY_DIM, entry.dim().toString());
            tag.putLong(KEY_POS, entry.pos().asLong());
            if (entry.avatarId() != null) {
                tag.putString(KEY_AVATAR, entry.avatarId().toString());
            }
            tags.add(tag);
        }
        root.put(KEY_BODIES, tags);
        player.setAttached(ModDataAttachments.SHELL_BODIES, root);
    }

    /** 同一个目标：双方都是分身就比 UUID，否则比维度 + 坐标 */
    private static boolean sameTarget(Entry a, Entry b) {
        if (a.isAvatar() && b.isAvatar()) {
            return a.avatarId().equals(b.avatarId());
        }
        if (a.isAvatar() != b.isAvatar()) {
            return false;
        }
        return a.dim().equals(b.dim()) && a.pos().equals(b.pos());
    }
}
