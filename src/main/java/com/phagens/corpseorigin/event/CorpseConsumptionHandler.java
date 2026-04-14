package com.phagens.corpseorigin.event;

import com.phagens.corpseorigin.CorpseOrigin;
import com.phagens.corpseorigin.entity.CorpseGibEntity;
import com.phagens.corpseorigin.player.PlayerCorpseData;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * 尸兄食用尸体处理器
 *
 * 【功能说明】
 * 尸兄玩家可以 Shift+右键食用残肢来恢复尸兄饥饿值（蓝条）
 * 普通玩家无法食用残肢
 *
 * 【食用效果】
 * - 恢复尸兄饥饿值（根据残肢类型不同）
 * - 可能获得短暂的力量效果
 * - 播放食用音效和粒子效果
 *
 * 【参照 Mob-Dismemberment】
 * 合并尸体和残肢后，统一使用 CorpseGibEntity
 */
@EventBusSubscriber(modid = CorpseOrigin.MODID)
public class CorpseConsumptionHandler {

    /**
     * 处理玩家与残肢的交互
     */
    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        // 只处理主手（右手）交互，防止触发两次
        if (event.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND) {
            return;
        }

        Player player = event.getEntity();

        // 检查是否是残肢
        if (event.getTarget() instanceof CorpseGibEntity gib) {
            handleGibInteraction(player, gib, event);
        }
    }

    /**
     * 处理与残肢的交互
     */
    private static void handleGibInteraction(Player player, CorpseGibEntity gib, PlayerInteractEvent.EntityInteract event) {
        // 只有尸兄可以食用残肢
        if (!PlayerCorpseData.isCorpseBrother(player)) {
            return;
        }

        // 需要 Shift+右键
        if (!player.isShiftKeyDown()) {
            return;
        }

        event.setCanceled(true);

        if (player.level().isClientSide) {
            return;
        }

        // 检查尸兄饥饿值是否已满（尸兄饥饿值上限是100）
        int currentHunger = PlayerCorpseData.getHunger(player);
        if (currentHunger >= 100) {
            player.sendSystemMessage(Component.literal(
                    "§c§l你已经吃饱了，无法再吃下更多..."
            ));
            return;
        }

        // 食用残肢
        int nutrition = gib.consume(player);

        if (nutrition > 0) {
            // 恢复尸兄饥饿值（蓝条）而非普通饥饿值
            PlayerCorpseData.setHunger(player, Math.min(100, currentHunger + nutrition * 2));

            // 播放音效
            player.playSound(SoundEvents.PLAYER_BURP, 0.5f, 0.8f + player.getRandom().nextFloat() * 0.4f);

            // 身体部位可能获得力量效果（30%几率）
            if (gib.isBody() && player.getRandom().nextFloat() < 0.3f) {
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 0));
                player.sendSystemMessage(Component.literal(
                        "§4§l血肉的力量在你体内涌动..."
                ));
            }

            // 发送消息
            player.sendSystemMessage(Component.literal(
                    "§c§l你吞噬了 " + gib.getPartTypeName() + "，恢复了 " + (nutrition * 2) + " 点尸兄饥饿值"
            ));

            CorpseOrigin.LOGGER.info("尸兄玩家 {} 食用了残肢 {} (营养值: {}, 尸兄饥饿值: {})",
                    player.getName().getString(), gib.getPartTypeName(), nutrition, PlayerCorpseData.getHunger(player));
        }
    }
}