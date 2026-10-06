package xiaoshi2022.corpseorigin.compat.flashback;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.character.CharacterManager;
import xiaoshi2022.corpseorigin.client.CorpseOriginClient;
import xiaoshi2022.corpseorigin.client.OrganClient;
import xiaoshi2022.corpseorigin.growth.OrganLibrary;
import xiaoshi2022.corpseorigin.growth.SurvivalGrowth;
import xiaoshi2022.corpseorigin.network.CorpsePayloads;
import xiaoshi2022.corpseorigin.network.OrganEditorPayload;

import java.util.function.Consumer;

/**
 * 将本模组的客户端状态写入 Flashback 录制快照。
 * <p>
 * Flashback 的世界快照只包含原版实体数据，不包含 Fabric 附件或模组登录同步包，
 * 因此回放时需要补发以下客户端数据：
 * <ul>
 *   <li>{@code corpseDataCache} 中的尸兄形态和变种；</li>
 *   <li>{@code corpseorigin:evolution_parts} 附件；</li>
 *   <li>{@code OrganClient.catalog} 器官目录。</li>
 * </ul>
 * 这些数据通过普通 S2C 包写入快照。回放时，Flashback 会在
 * {@code sendLevelInfo} 阶段还原数据包并交给本模组的客户端接收器。
 */
public final class ReplaySnapshotInjector {

    private ReplaySnapshotInjector() {
    }

    public static void inject(Consumer<Packet<? super ClientGamePacketListener>> out) {
        Minecraft client = Minecraft.getInstance();
        ClientLevel level = client.level;
        if (level == null) return;
        try {
            // 1. 器官目录是纯客户端元数据（服务端 JOIN 时下发），进入回放连接时会被 DISCONNECT 清空
            if (!OrganClient.catalog.isEmpty()) {
                out.accept(new ClientboundCustomPayloadPacket(
                        new OrganEditorPayload.Catalog(OrganLibrary.JSON.toJson(OrganClient.catalog))));
            }

            // 2. CharacterSyncS2C 只携带本地玩家自己的角色（客户端缓存是单值）
            String characterId = CharacterManager.getInstance().getClientCachedCharacterId();
            if (characterId != null && !characterId.isEmpty()) {
                out.accept(new ClientboundCustomPayloadPacket(new CorpsePayloads.CharacterSyncS2C(characterId)));
            }

            // 3. 与快照相同的实体集合：尸兄缓存（含分身等非玩家实体）+ 玩家evolution_parts 附件
            for (Entity entity : level.entitiesForRendering()) {
                xiaoshi2022.corpseorigin.client.ClientCorpseData corpse =
                        CorpseOriginClient.corpseDataCache.get(entity.getUUID());
                if (corpse != null) {
                    out.accept(new ClientboundCustomPayloadPacket(new CorpsePayloads.PlayerCorpseSyncS2C(
                            entity.getUUID(), corpse.isCorpse, corpse.corpseType, corpse.data)));
                }
                if (entity instanceof Player player) {
                    CompoundTag body = player.getAttachedOrCreate(SurvivalGrowth.BODY);
                    // 伪装渲染链路依赖的同步附件（GourdInheritance.disguised / ChameleonHeadLayer 都读它们）
                    String role = player.getAttachedOrCreate(
                            xiaoshi2022.corpseorigin.skill.chapter.ChapterActorState.ROLE);
                    boolean disguised = player.getAttachedOrCreate(
                            xiaoshi2022.corpseorigin.skill.chapter.GourdInheritance.DISGUISED);
                    String chameleonSkin = player.getAttachedOrCreate(
                            xiaoshi2022.corpseorigin.skill.chapter.ChapterActorState.DISGUISE);
                    var chameleonProfile = player.getAttached(
                            xiaoshi2022.corpseorigin.skill.chapter.ChapterActorState.DISGUISE_PROFILE);
                    if (!body.isEmpty() || !role.isEmpty() || disguised || !chameleonSkin.isEmpty()
                            || chameleonProfile != null) {
                        out.accept(new ClientboundCustomPayloadPacket(new CorpsePayloads.ReplayPlayerBodyS2C(
                                player.getUUID(), body.copy(), role, disguised, chameleonSkin, chameleonProfile)));
                    }
                }
            }
        } catch (Exception e) {
            // 快照注入失败只影响回放外观，绝不能让录制流程崩掉
            CorpseOrigin.LOGGER.warn("Flashback snapshot injection failed", e);
        }
    }
}
