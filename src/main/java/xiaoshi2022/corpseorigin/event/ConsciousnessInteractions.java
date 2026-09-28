package xiaoshi2022.corpseorigin.event;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntitySpawnReason;
import xiaoshi2022.corpseorigin.entity.DamoEntity;
import xiaoshi2022.corpseorigin.registry.ModEntities;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.component.PlayerCorpseComponent;
import xiaoshi2022.corpseorigin.network.CorpseNetwork;
import xiaoshi2022.corpseorigin.registry.ModItems;

public final class ConsciousnessInteractions {
    /** Only ordinary corpse roles use the random consciousness/recovery progression. */
    public static boolean requiresRecovery(String role) {
        return "corpse_brother".equals(role);
    }
    private static boolean restricted(net.minecraft.world.entity.player.Player player) {
        return player instanceof ServerPlayer sp
                && requiresRecovery(xiaoshi2022.corpseorigin.character.CharacterManager.getInstance().getPlayerCharacterId(sp))
                && PlayerCorpseComponent.get(player).isMindless();
    }
    public static final AttachmentType<Integer> FLESH = AttachmentRegistry.create(CorpseOrigin.id("consciousness_flesh"),
            b -> b.initializer(() -> 0).persistent(com.mojang.serialization.Codec.INT));
    public static void register() {
        UseBlockCallback.EVENT.register((p, level, hand, hit) -> {
            var pos = hit.getBlockPos();
            var stack = p.getItemInHand(hand);
            if (level.getBlockState(pos).is(Blocks.BEACON) && stack.is(Items.COOKED_CHICKEN)) {
                if (level.isClientSide()) return InteractionResult.SUCCESS;
                if (!(level instanceof net.minecraft.server.level.ServerLevel serverLevel)) return InteractionResult.PASS;
                boolean nearbyDamo = !serverLevel.getEntitiesOfClass(DamoEntity.class,
                        new net.minecraft.world.phys.AABB(pos).inflate(128), DamoEntity::isAlive).isEmpty();
                if (nearbyDamo) {
                    p.sendSystemMessage(Component.translatable("message.corpseorigin.damo.beacon_exists"));
                    return InteractionResult.SUCCESS;
                }
                DamoEntity damo = ModEntities.DAMO.create(serverLevel, EntitySpawnReason.TRIGGERED);
                if (damo == null) return InteractionResult.PASS;
                BlockPos spawnPos = pos.above();
                damo.setPos(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
                damo.setYRot(p.getYRot());
                if (!serverLevel.addFreshEntity(damo)) return InteractionResult.PASS;
                if (!p.getAbilities().instabuild) stack.shrink(1);
                serverLevel.levelEvent(2001, pos, net.minecraft.world.level.block.Block.getId(Blocks.BEACON.defaultBlockState()));
                return InteractionResult.SUCCESS;
            }
            if (level.isClientSide() || !restricted(p)) return InteractionResult.PASS;
            p.sendOverlayMessage(Component.translatable("message.corpseorigin.consciousness_interactions.text_01"));
            return InteractionResult.FAIL;
        });
        UseItemCallback.EVENT.register((p, level, hand) -> {
            var stack = p.getItemInHand(hand);
            boolean eye = stack.is(ModItems.DR_MU_EYE);
            if (!eye && !stack.is(ModItems.ZBR_FLESH)) return InteractionResult.PASS;
            if (level.isClientSide()) return InteractionResult.PASS;
            var comp = PlayerCorpseComponent.get(p);
            if (!restricted(p)) return InteractionResult.PASS;
            stack.shrink(1);
            int eaten = p.getAttachedOrCreate(FLESH) + 1;
            p.setAttached(FLESH, eaten);
            if (eye || eaten >= 20) {
                comp.restoreConsciousness();
                p.sendOverlayMessage(Component.translatable("message.corpseorigin.consciousness_interactions.text_02"));
                if (p instanceof ServerPlayer sp) CorpseNetwork.broadcastPlayerCorpseSync(sp);
            } else p.sendOverlayMessage(Component.translatable("message.corpseorigin.consciousness_interactions.text_03", eaten));
            return InteractionResult.SUCCESS;
        });
    }
}
