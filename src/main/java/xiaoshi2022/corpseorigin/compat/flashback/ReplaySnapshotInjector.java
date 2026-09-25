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
import xiaoshi2022.corpseorigin.client.CorpseOriginClient;
import xiaoshi2022.corpseorigin.client.OrganClient;
import xiaoshi2022.corpseorigin.character.CharacterManager;
import xiaoshi2022.corpseorigin.growth.OrganLibrary;
import xiaoshi2022.corpseorigin.growth.SurvivalGrowth;
import xiaoshi2022.corpseorigin.network.CorpsePayloads;
import xiaoshi2022.corpseorigin.network.OrganEditorPayload;

import java.util.function.Consumer;

/**
 * Flashback 录制端快照状态注入。
 * <p>
 * Flashback 在录制开始及每个回放块边界都会写一次世界快照，供初始加载 / seek 时重建。
 * 快照里只有原版实体数据（AddEntity + SynchedEntityData + 属性 + 装备），
 * <b>没有</b> Fabric 附件，也没有第三方模组的登录同步包；本模组驱动渲染的三类数据因此在回放中全丢：
 * <ul>
 *   <li>{@code corpseDataCache}（尸兄形态 / 变种，门控外骨骼、进化身体渲染）；</li>
 *   <li>{@code corpseorigin:evolution_parts} 附件（器官装配方案、器官阶段、wings/gills、葫芦状态）；</li>
 *   <li>{@code OrganClient.catalog}（器官目录，进回放时连接断开还会被清空）。</li>
 * </ul>
 * Recorder#writeCustomSnapshot 是 Flashback 预留给模组的快照扩展点（0.43.4 中为空实现），
 * 把当前客户端已有的渲染状态包成普通 S2C 包交给 consumer，就会随快照一起写入录像；
 * 回放端 viewer 的 sendLevelInfo 阶段这些包会经 FlashbackRawCustomPayload 还原并重新走
 * 模组自己的客户端接收器，数据即被重建。
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

            // 3. 与快照相同的实体集合：尸兄缓存（含分身等非玩家实体）+ 玩家的 evolution_parts 附件
            for (Entity entity : level.entitiesForRendering()) {
                CorpseOriginClient.ClientCorpseData corpse =
                        CorpseOriginClient.corpseDataCache.get(entity.getUUID());
                if (corpse != null) {
                    out.accept(new ClientboundCustomPayloadPacket(new CorpsePayloads.PlayerCorpseSyncS2C(
                            entity.getUUID(), corpse.isCorpse, corpse.corpseType, corpse.data)));
                }
                if (entity instanceof Player player) {
                    CompoundTag body = player.getAttachedOrCreate(SurvivalGrowth.BODY);
                    if (!body.isEmpty()) {
                        out.accept(new ClientboundCustomPayloadPacket(
                                new CorpsePayloads.ReplayPlayerBodyS2C(player.getUUID(), body.copy())));
                    }
                }
            }
        } catch (Exception e) {
            // 快照注入失败只影响回放外观，绝不能让录制流程崩掉
            CorpseOrigin.LOGGER.warn("Flashback 快照注入模组渲染状态失败", e);
        }
    }
}
