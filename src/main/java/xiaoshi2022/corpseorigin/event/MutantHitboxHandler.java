package xiaoshi2022.corpseorigin.event;

import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import org.slf4j.Logger;
import xiaoshi2022.corpseorigin.component.MutantForm;
import xiaoshi2022.corpseorigin.config.CorpseConfig;
import xiaoshi2022.corpseorigin.entity.GuardianPartEntity;
import xiaoshi2022.corpseorigin.registry.ModEntities;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 左护法变异体的「蛟龙节碰撞箱」维护器（服务端每 tick）。
 * <p>
 * 干三件事：
 * <ol>
 *   <li><b>按需生成</b> —— 玩家进入变异体形态时，照配置里的节列表各生成一个
 *       {@link GuardianPartEntity}；退出形态 / 死亡 / 换维度 / 离线则全部销毁；</li>
 *   <li><b>每 tick 摆位</b> —— 顺手把配置里最新的一节参数刷进实体（这样用
 *       {@code /character hitbox} 现场调偏移能<b>立刻生效</b>，不用重启）；</li>
 *   <li><b>兜底清理</b> —— 节自己在 {@code tick} 里也会在主人失效时自毁，这里再扫一遍掉线的。</li>
 * </ol>
 * 节的数量与顺序完全跟着配置走：想少几节删条目、想多几节加条目，下一 tick 就会重建。
 */
public final class MutantHitboxHandler {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** 玩家 UUID → 他的几节碰撞箱（顺序与配置里的节列表一致） */
    private static final Map<UUID, List<GuardianPartEntity>> PARTS = new ConcurrentHashMap<>();

    private MutantHitboxHandler() {
    }

    public static void tick(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            try {
                tickPlayer(player);
            } catch (Exception e) {
                LOGGER.warn("维护蛟龙碰撞箱失败（{}）: {}", player.getName().getString(), e.getMessage());
            }
        }

        // 掉线的玩家：节跟着清掉（节自己也会自毁，这里保证 map 不残留）
        PARTS.entrySet().removeIf(entry -> {
            if (server.getPlayerList().getPlayer(entry.getKey()) != null) {
                return false;
            }
            discard(entry.getValue());
            return true;
        });
    }

    private static void tickPlayer(ServerPlayer player) {
        UUID uuid = player.getUUID();
        List<GuardianPartEntity> existing = PARTS.get(uuid);

        List<CorpseConfig.MutantBody.Hitboxes.Segment> segments =
                CorpseConfig.get().mutantBody.hitboxes.segments;

        boolean want = player.isAlive()
                && !player.isSpectator()
                && MutantForm.isMutantWithHitboxes(player)
                && !segments.isEmpty();

        if (!want) {
            if (existing != null) {
                discard(existing);
                PARTS.remove(uuid);
            }
            return;
        }

        // 数量对不上（配置改过）、换了维度、或某一节自己销毁了 → 整体重建
        if (existing != null && (!sameSize(existing, segments) || existing.stream()
                .anyMatch(part -> part.level() != player.level() || part.isRemoved()))) {
            discard(existing);
            existing = null;
            PARTS.remove(uuid);
        }

        if (existing == null) {
            existing = spawn(player, segments);
            PARTS.put(uuid, existing);
            if (existing.isEmpty()) {
                return;
            }
        }

        // 每 tick 刷参数 + 摆位：命令改了偏移这里立刻就能看到，不用重启
        for (int i = 0; i < existing.size() && i < segments.size(); i++) {
            GuardianPartEntity part = existing.get(i);
            part.applySegment(segments.get(i));
            part.followOwner(player);
        }
    }

    private static boolean sameSize(List<GuardianPartEntity> parts,
                                    List<CorpseConfig.MutantBody.Hitboxes.Segment> segments) {
        return parts.size() == segments.size();
    }

    /** 照配置生成一节节隐形实体；失败的节直接跳过（宁可少一节，也别把 tick 搞崩） */
    private static List<GuardianPartEntity> spawn(ServerPlayer player,
                                                  List<CorpseConfig.MutantBody.Hitboxes.Segment> segments) {
        List<GuardianPartEntity> parts = new ArrayList<>(segments.size());
        for (CorpseConfig.MutantBody.Hitboxes.Segment segment : segments) {
            GuardianPartEntity part = ModEntities.GUARDIAN_PART.create(player.level(), EntitySpawnReason.TRIGGERED);
            if (part == null) {
                continue;
            }
            part.setOwner(player);
            part.applySegment(segment);
            part.followOwner(player);
            if (!player.level().addFreshEntity(part)) {
                continue;
            }
            parts.add(part);
        }
        return parts;
    }

    private static void discard(List<GuardianPartEntity> parts) {
        if (parts == null) {
            return;
        }
        for (GuardianPartEntity part : parts) {
            if (part != null && !part.isRemoved()) {
                part.discard();
            }
        }
    }
}
