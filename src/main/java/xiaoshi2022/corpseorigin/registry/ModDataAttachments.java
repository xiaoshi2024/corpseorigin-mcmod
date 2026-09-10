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
                    .persistent(CompoundTag.CODEC)   // 自动保存到玩家 NBT
                    .initializer(() -> new CompoundTag())
                    .copyOnDeath()  // 死亡后保留
                    .buildAndRegister(Identifier.fromNamespaceAndPath(
                            CorpseOrigin.MOD_ID, "player_corpse"));

    public static void init() {
        CorpseOrigin.LOGGER.info("CorpseOrigin data attachments registered");
    }
}