package com.phagens.corpseorigin.skill;

import com.phagens.corpseorigin.CorpseOrigin;
import com.phagens.corpseorigin.player.PlayerCorpseData;
import com.phagens.corpseorigin.skill.special.CorpseKingPowerSkill;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * 技能事件处理器 - 处理被动技能效果
 *
 * 【功能说明】
 * 1. 处理被动技能的持续效果（每tick/每秒）
 * 2. 处理攻击相关的被动技能（毒液、重击、吞噬强化）
 * 3. 处理防御相关的被动技能（闪避）
 * 4. 处理玩家登录/重生/克隆事件，确保数据同步
 * 5. 管理技能冷却时间的更新
 *
 * 【被动技能处理】
 * - 进化感知：每20tick刷新夜视效果
 * - 快速再生：每20tick恢复生命值
 * - 毒液：攻击时给目标添加中毒效果
 * - 重击：攻击时添加击退效果
 * - 吞噬强化：攻击活物时恢复生命和饥饿
 * - 闪避：受到伤害时25%几率闪避
 * - 尸王之力：攻击时触发控制效果
 *
 * 【事件处理】
 * - PlayerTickEvent: 更新冷却、处理持续被动效果
 * - LivingDamageEvent.Pre: 处理攻击者效果
 * - LivingDamageEvent.Post: 处理防御者效果（闪避）
 * - PlayerRespawnEvent: 重生后同步数据
 * - PlayerLoggedInEvent: 登录后同步数据
 * - PlayerCloneEvent: 克隆/死亡后恢复数据
 *
 * 【关联系统】
 * - CorpseSkills: 技能定义
 * - SkillAttachment: 数据获取
 * - CorpseKingPowerSkill: 尸王之力特殊处理
 *
 * @author Phagens
 * @version 1.0
 */
@EventBusSubscriber(modid = CorpseOrigin.MODID)
public class SkillEventHandler {

    /**
     * 玩家tick事件 - 处理持续性的被动技能效果
     */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();

        ISkillHandler handler = SkillAttachment.getSkillHandler(player);
        if (handler == null) return;

        // 更新技能冷却
        if (handler instanceof SkillHandler skillHandler) {
            skillHandler.updateCooldowns();
        }

        if (!player.level().isClientSide) {
            // 每 20tick（1 秒）处理一次被动效果
            if (player.tickCount % 20 == 0) {
                // 进化感知 - 夜视效果
                if (handler.hasLearned(CorpseSkills.EVOLUTION_SENSE.getId())) {
                    player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 400, 0, false, false));
                }

                // 快速再生 - 生命恢复
                if (handler.hasLearned(CorpseSkills.REGENERATION.getId())) {
                    if (player.getHealth() < player.getMaxHealth()) {
                        player.heal(0.5f);
                    }
                }
            }
        }

        // 每20tick（1秒）处理一次被动效果
        if (player.tickCount % 20 == 0) {
            // 进化感知 - 夜视效果
            if (handler.hasLearned(CorpseSkills.EVOLUTION_SENSE.getId())) {
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 400, 0, false, false));
            }

            // 快速再生 - 生命恢复
            if (handler.hasLearned(CorpseSkills.REGENERATION.getId())) {
                if (player.getHealth() < player.getMaxHealth()) {
                    player.heal(0.5f);
                }
            }
        }
    }

    /**
     * 玩家攻击事件 - 处理攻击相关的被动技能，并追踪伤害
     * 使用 Pre 事件来确保能捕获所有伤害
     */
    @SubscribeEvent
    public static void onLivingDamagePre(LivingDamageEvent.Pre event) {
        // 处理攻击者相关的效果
        if (event.getSource().getEntity() instanceof Player player) {
            if (!PlayerCorpseData.isCorpse(player)) {
                return;
            }

            ISkillHandler handler = SkillAttachment.getSkillHandler(player);
            if (handler == null) return;

            LivingEntity target = event.getEntity();

            // 尸王之力 - 攻击时控制玩家或收服尸兄
            if (handler.hasLearned(CorpseSkills.CORPSE_KING_POWER.getId())) {
                CorpseKingPowerSkill.onAttackTarget(player, target);
            }

            // 毒液 - 攻击带毒
            if (handler.hasLearned(CorpseSkills.VENOM.getId())) {
                target.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0));
            }

            // 重击 - 击退效果
            if (handler.hasLearned(CorpseSkills.HEAVY_STRIKE.getId())) {
                double knockbackX = target.getX() - player.getX();
                double knockbackZ = target.getZ() - player.getZ();
                double knockbackStrength = 0.8;

                target.knockback(knockbackStrength, knockbackX, knockbackZ);
            }

            // 吞噬强化 - 额外恢复
            if (handler.hasLearned(CorpseSkills.DEVOUR_ENHANCEMENT.getId())) {
                if (isLivingCreature(target)) {
                    float healAmount = 1.5f;
                    player.heal(healAmount);

                    // 增加饥饿度
                    int currentHunger = PlayerCorpseData.getHunger(player);
                    PlayerCorpseData.setHunger(player, Math.min(100, currentHunger + 8));
                }
            }
        }
    }

    /**
     * 处理受伤者的效果
     */
    @SubscribeEvent
    public static void onLivingDamagePost(LivingDamageEvent.Post event) {
        // 闪避 - 受到伤害时有几率闪避
        if (event.getEntity() instanceof Player player) {
            if (!PlayerCorpseData.isCorpse(player)) {
                return;
            }

            ISkillHandler handler = SkillAttachment.getSkillHandler(player);
            if (handler == null) return;

            if (handler.hasLearned(CorpseSkills.EVASION.getId())) {
                // 25%几率闪避
                if (player.getRandom().nextFloat() < 0.25f) {
                    // 取消伤害（通过恢复生命值）
                    player.heal(event.getNewDamage());
                    // 播放闪避效果
                    player.level().broadcastEntityEvent(player, (byte) 29); // 盾牌阻挡效果
                    CorpseOrigin.LOGGER.debug("玩家 {} 闪避了 {} 伤害", player.getName().getString(), event.getNewDamage());
                }
            }
        }
    }

    /**
     * 玩家重生事件 - 确保技能数据同步到客户端
     */
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        Player player = event.getEntity();

        if (player instanceof ServerPlayer serverPlayer) {
            CorpseOrigin.LOGGER.info("玩家 {} 重生，准备同步技能数据", player.getName().getString());

            // 延迟执行，确保所有数据加载完成
            serverPlayer.server.execute(() -> {
                try {
                    Thread.sleep(100); // 等待100ms确保附件完全加载
                } catch (InterruptedException e) {
                    // ignore
                }

                ISkillHandler handler = SkillAttachment.getSkillHandler(player);

                if (handler != null) {
//                    CorpseOrigin.LOGGER.info("重生后技能数据: {} 个技能, {} 进化点",
//                            handler.getLearnedSkills().size(),
//                            handler.getEvolutionPoints());

                    // 重新应用被动技能
                    if (handler instanceof SkillHandler skillHandler) {
                        skillHandler.reapplyPassiveSkills();

                        // 强制标记为脏数据并同步到客户端
                        skillHandler.markDirty();
                        skillHandler.syncToClient();

//                        CorpseOrigin.LOGGER.info("技能数据已同步到客户端");
                    }
                } else {
//                    CorpseOrigin.LOGGER.warn("玩家 {} 重生后没有技能处理器", player.getName().getString());
                }
            });
        }
    }

    /**
     * 玩家登录事件 - 确保技能数据正确加载
     */
    /**
     * 玩家登录事件 - 确保技能数据正确同步到客户端
     */
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();

        if (player instanceof ServerPlayer serverPlayer) {
            ISkillHandler handler = SkillAttachment.getSkillHandler(player);

            if (handler != null) {
//                CorpseOrigin.LOGGER.info("玩家 {} 登录，技能数据: {} 个技能, {} 进化点",
//                        player.getName().getString(),
//                        handler.getLearnedSkills().size(),
//                        handler.getEvolutionPoints());

                // 重新应用被动技能
                if (handler instanceof SkillHandler skillHandler) {
                    skillHandler.reapplyPassiveSkills();

                    // 延迟同步，确保客户端已完全加载
                    serverPlayer.server.execute(() -> {
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            // ignore
                        }
                        skillHandler.forceSyncToClient();
                    });
                }
            }
        }
    }

    /**
     * 玩家克隆事件 - 简化版，让附件系统自动处理
     */
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        Player original = event.getOriginal();
        Player newPlayer = event.getEntity();

//        CorpseOrigin.LOGGER.info("玩家克隆事件 - 原玩家: {}, 新玩家: {}, 是否死亡: {}",
//                original.getName().getString(),
//                newPlayer.getName().getString(),
//                event.isWasDeath());

        // 如果只是数据同步，不需要做任何事，copyOnDeath() 已经处理

        // 但我们需要确保技能效果被重新应用
        if (event.isWasDeath() && newPlayer instanceof ServerPlayer serverPlayer) {
            serverPlayer.server.execute(() -> {
                try {
                    Thread.sleep(100); // 等待附件完成反序列化
                } catch (InterruptedException e) {
                    // ignore
                }

                ISkillHandler handler = SkillAttachment.getSkillHandler(newPlayer);
                if (handler != null) {
//                    CorpseOrigin.LOGGER.info("重生后技能数据: {} 个技能, {} 进化点",
//                            handler.getLearnedSkills().size(),
//                            handler.getEvolutionPoints());

                    // 重新应用被动技能
                    if (handler instanceof SkillHandler skillHandler) {
                        skillHandler.reapplyPassiveSkills();
                        skillHandler.syncToClient();
                    }
                }
            });
        }
    }



    /**
     * 判断目标是否是活物（可被吞噬）
     */
    private static boolean isLivingCreature(LivingEntity entity) {
        // 排除已死亡的生物
        if (!entity.isAlive()) {
            return false;
        }

        // 排除玩家自己
        if (entity instanceof Player) {
            return true; // 玩家可以被吞噬（PVP）
        }

        MobCategory category = entity.getType().getCategory();

        // 可被吞噬的生物类别
        return category == MobCategory.CREATURE ||
                category == MobCategory.AMBIENT ||
                category == MobCategory.MONSTER ||
                category == MobCategory.AXOLOTLS ||
                category == MobCategory.UNDERGROUND_WATER_CREATURE ||
                category == MobCategory.WATER_CREATURE ||
                category == MobCategory.WATER_AMBIENT ||
                entity instanceof net.minecraft.world.entity.npc.AbstractVillager;
    }
}