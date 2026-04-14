package com.phagens.corpseorigin.event.player;

import com.phagens.corpseorigin.CorpseOrigin;
import com.phagens.corpseorigin.Item.Organic.OrdinaryZbEyeItem;
import com.phagens.corpseorigin.player.PlayerCorpseData;
import com.phagens.corpseorigin.skill.*;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * 器官进化系统事件处理器
 * 处理玩家食用器官后的持续效果
 */
@EventBusSubscriber(modid = CorpseOrigin.MODID)
public class OrganEvolutionHandler {

    /**
     * 玩家tick事件 - 处理永久夜视等持续效果
     */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();

        // 只有尸兄玩家才有这些效果
        if (!PlayerCorpseData.isCorpse(player)) {
            return;
        }

        // 处理永久夜视
        handlePermanentNightVision(player);

        // 处理多眼形态效果
        handleMultiEyeEffect(player);

        // 更新多眼感知技能
        MultiEyePerceptionSkill.tick(player);
    }

    /**
     * 处理永久夜视
     * 如果玩家已经进化出永久夜视，持续给予夜视效果
     */
    private static void handlePermanentNightVision(Player player) {
        if (OrdinaryZbEyeItem.hasPermanentNightVision(player)) {
            // 给予长时间的夜视效果（10秒）
            // 使用较短的持续时间以避免效果闪烁
            if (!player.hasEffect(MobEffects.NIGHT_VISION) ||
                player.getEffect(MobEffects.NIGHT_VISION).getDuration() < 200) {
                player.addEffect(new MobEffectInstance(
                        MobEffects.NIGHT_VISION,
                        400, // 20秒持续时间
                        0,
                        false, // 不显示粒子
                        false, // 不显示图标
                        true   // 显示在物品栏
                ));
            }
        }
    }

    /**
     * 处理多眼形态的视觉效果
     * 进化后一次性获得全部9个眼睛的效果
     */
    private static void handleMultiEyeEffect(Player player) {
        // 检查是否进化出多眼形态
        boolean hasMultiEye = OrdinaryZbEyeItem.getExtraEyeCount(player) > 0;

        // 同步眼睛数量到PlayerCorpseData（用于渲染）
        int currentRenderEyeCount = PlayerCorpseData.getExtraEyeCount(player);
        int actualEyeCount = hasMultiEye ? 9 : 0;
        if (actualEyeCount != currentRenderEyeCount) {
            PlayerCorpseData.setExtraEyeCount(player, actualEyeCount);
        }

        // 如果进化出多眼形态，自动学习多眼感知技能
        if (hasMultiEye && !player.level().isClientSide) {
            ISkillHandler skillHandler = SkillAttachment.getSkillHandler(player);
            ISkill multiEyeSkill = CorpseSkills.MULTI_EYE_PERCEPTION;

            if (multiEyeSkill != null && !skillHandler.hasLearned(multiEyeSkill.getId())) {
                // 自动学习技能
                skillHandler.learnSkill(multiEyeSkill);
                CorpseOrigin.LOGGER.info("玩家 {} 进化出多眼形态，自动学习了多眼感知技能",
                        player.getName().getString());
                player.sendSystemMessage(
                    net.minecraft.network.chat.Component.translatable(
                        "skill.corpseorigin.multi_eye.auto_learned"
                    ).withStyle(net.minecraft.ChatFormatting.DARK_RED, net.minecraft.ChatFormatting.BOLD)
                );
            }
        }

        if (hasMultiEye) {
            // 多眼形态给予基础增益效果

            // 攻击伤害加成
            if (!player.hasEffect(MobEffects.DAMAGE_BOOST)) {
                player.addEffect(new MobEffectInstance(
                        MobEffects.DAMAGE_BOOST,
                        100,
                        0, // 1级攻击加成
                        false,
                        false,
                        false
                ));
            }

            // 移动速度加成
            if (!player.hasEffect(MobEffects.MOVEMENT_SPEED)) {
                player.addEffect(new MobEffectInstance(
                        MobEffects.MOVEMENT_SPEED,
                        100,
                        0,
                        false,
                        false,
                        false
                ));
            }

            // 抗性提升
            if (!player.hasEffect(MobEffects.DAMAGE_RESISTANCE)) {
                player.addEffect(new MobEffectInstance(
                        MobEffects.DAMAGE_RESISTANCE,
                        100,
                        0,
                        false,
                        false,
                        false
                ));
            }
        }
    }
}
