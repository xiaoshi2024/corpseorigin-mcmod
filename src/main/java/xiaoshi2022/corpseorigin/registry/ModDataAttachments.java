package xiaoshi2022.corpseorigin.registry;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import xiaoshi2022.corpseorigin.CorpseOrigin;

public class ModDataAttachments {

    // ✅ 玩家尸兄数据（持久化）
    public static final AttachmentType<CompoundTag> PLAYER_CORPSE =
            AttachmentRegistry.<CompoundTag>builder()
                    .persistent(CompoundTag.CODEC)
                    .initializer(() -> new CompoundTag())
                    .copyOnDeath()
                    .buildAndRegister(Identifier.fromNamespaceAndPath(
                            CorpseOrigin.MOD_ID, "player_corpse"));

    // ✅ APS 技能状态（持久化）
    public static final AttachmentType<CompoundTag> APS_STATE =
            AttachmentRegistry.<CompoundTag>builder()
                    .persistent(CompoundTag.CODEC)
                    .initializer(() -> new CompoundTag())
                    .copyOnDeath()
                    .buildAndRegister(Identifier.fromNamespaceAndPath(
                            CorpseOrigin.MOD_ID, "aps_state"));

    // ✅ 可夺舍身体索引（持久化：每具身体所在维度 + 坐标，区块没加载也能找到）
    public static final AttachmentType<CompoundTag> SHELL_BODIES =
            AttachmentRegistry.<CompoundTag>builder()
                    .persistent(CompoundTag.CODEC)
                    .initializer(() -> new CompoundTag())
                    .copyOnDeath()
                    .buildAndRegister(Identifier.fromNamespaceAndPath(
                            CorpseOrigin.MOD_ID, "shell_bodies"));

    public static void init() {
        CorpseOrigin.LOGGER.info("CorpseOrigin data attachments registered");
    }
}