package xiaoshi2022.corpseorigin.event;

import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import xiaoshi2022.corpseorigin.entity.FlyingGreatSwordEntity;
import xiaoshi2022.corpseorigin.registry.ModDataAttachments;
import xiaoshi2022.corpseorigin.skill.baixiaofei.AncientPoetrySwordSkill;
import xiaoshi2022.corpseorigin.skill.baixiaofei.aps.APSTerrainManager;

/**
 * APS 剑意展开期间：Shift + 右键 → 发射剑意核心
 * 对所有武器/空手都生效，不依赖 JuQue。
 */
public final class APSGreatSwordInterceptor {

    private static final int APS_SWORD_COOLDOWN = 40;

    private APSGreatSwordInterceptor() {}

    public static void register() {
        UseItemCallback.EVENT.register((player, level, hand) -> {
            if (level.isClientSide()) {
                return InteractionResult.PASS;
            }
            if (!(player instanceof ServerPlayer sp)) {
                return InteractionResult.PASS;
            }
            if (!player.isShiftKeyDown()) {
                return InteractionResult.PASS;
            }
            if (hand != InteractionHand.MAIN_HAND) {
                return InteractionResult.PASS;
            }
            if (!APSTerrainManager.hasActiveAPS(player)) {
                return InteractionResult.PASS;
            }

            ItemStack stack = player.getItemInHand(hand);
            if (sp.getCooldowns().isOnCooldown(stack)) {
                return InteractionResult.SUCCESS;
            }

            ServerLevel sl = (ServerLevel) level;

            if (stack.isDamageableItem()) {
                stack.hurtAndBreak(1, sp, EquipmentSlot.MAINHAND);
            }

            sl.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.TRIDENT_RIPTIDE_1, SoundSource.PLAYERS, 1.0F, 1.4F);

            int stage = player.getAttachedOrCreate(ModDataAttachments.APS_STATE)
                    .getInt(AncientPoetrySwordSkill.STAGE_KEY)
                    .orElse(0);
            float scale = 2.5F + stage * 0.5F;

            // ✅ 统一工厂
            FlyingGreatSwordEntity.spawnDirected(sl, sp, stack, scale, 0f);

            sp.getCooldowns().addCooldown(stack, APS_SWORD_COOLDOWN);
            sp.swing(hand, true);

            return InteractionResult.SUCCESS;
        });
    }
}