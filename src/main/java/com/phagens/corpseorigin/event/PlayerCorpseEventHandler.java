package com.phagens.corpseorigin.event;

import com.phagens.corpseorigin.CorpseOrigin;
import com.phagens.corpseorigin.network.PlayerCorpseSyncPacket;
import com.phagens.corpseorigin.player.CorpsePlayerAttachment;
import com.phagens.corpseorigin.player.PlayerCorpseData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 尸兄玩家事件处理器 - 处理尸兄玩家的特殊行为和属性
 *
 * 【功能说明】
 * 1. 尸兄玩家属性修改：增加护甲、攻击伤害、移动速度、最大生命值
 * 2. 攻击活物时恢复生命值和饥饿度（吞噬机制）
 * 3. 尸兄玩家死亡时掉落尸丹
 * 4. 击杀生物获得进化点
 *
 * 【属性修改】
 * - 护甲值：+4（Armor）
 * - 攻击伤害：+3（Attack Damage）
 * - 移动速度：+20%（Movement Speed）
 * - 最大生命值：+10（Max Health）
 *
 * 【吞噬机制】
 * - 攻击活物时恢复1.5生命值
 * - 恢复8点饥饿度
 * - 仅对尸兄玩家生效
 * - 攻击玩家时不触发
 *
 * 【尸丹掉落】
 * - 尸兄玩家死亡时掉落尸丹
 * - 尸丹可用于制作药剂或提升修为
 *
 * 【进化点获取】
 * - 击杀生物获得1-3点进化点
 * - 通过SkillEventHandler处理
 *
 * 【关联系统】
 * - PlayerCorpseData: 尸兄状态管理
 * - CorpseSkills: 技能系统
 * - Moditems.SHI_DAN: 尸丹物品
 * - SkillAttachment: 技能数据管理
 *
 * @author Phagens
 * @version 1.0
 */
@EventBusSubscriber(modid = CorpseOrigin.MODID)
public class PlayerCorpseEventHandler {

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            syncCorpseStateToClient(serverPlayer);
            // 恢复飞行能力
            restoreFlightAbility(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            syncCorpseStateToClient(serverPlayer);
            // 恢复飞行能力
            restoreFlightAbility(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            syncCorpseStateToClient(serverPlayer);
            // 恢复飞行能力
            restoreFlightAbility(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        Player original = event.getOriginal();
        Player newPlayer = event.getEntity();

        boolean isCorpse = original.getData(CorpsePlayerAttachment.IS_CORPSE);
        int corpseType = original.getData(CorpsePlayerAttachment.CORPSE_TYPE);
        CompoundTag corpseData = original.getData(CorpsePlayerAttachment.CORPSE_DATA);

        newPlayer.setData(CorpsePlayerAttachment.IS_CORPSE, isCorpse);
        newPlayer.setData(CorpsePlayerAttachment.CORPSE_TYPE, corpseType);
        newPlayer.setData(CorpsePlayerAttachment.CORPSE_DATA, corpseData.copy());

        if (newPlayer instanceof ServerPlayer serverPlayer) {
            syncCorpseStateToClient(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();

        if (player instanceof ServerPlayer serverPlayer) {
            if (PlayerCorpseData.isCorpse(player)) {
                updateCorpseBehavior(serverPlayer);
                updateCorpseMovement(serverPlayer); // 添加移动能力更新
            }
        }
    }

    private static void syncCorpseStateToClient(ServerPlayer player) {
        boolean isCorpse = player.getData(CorpsePlayerAttachment.IS_CORPSE);
        int corpseType = player.getData(CorpsePlayerAttachment.CORPSE_TYPE);
        CompoundTag corpseData = player.getData(CorpsePlayerAttachment.CORPSE_DATA);

        PlayerCorpseSyncPacket packet = new PlayerCorpseSyncPacket(
                player.getId(), isCorpse, corpseType, corpseData
        );

        // 发送给所有在线玩家，确保所有人都能看到尸兄状态
        PacketDistributor.sendToAllPlayers(packet);
    }

    /**
     * 恢复飞行能力（登录、重生、切换维度时调用）
     */
    private static void restoreFlightAbility(ServerPlayer player) {
        if (!PlayerCorpseData.isCorpse(player)) return;

        // 只对生存模式玩家应用飞行能力，创造模式玩家不受影响
        if (player.gameMode.getGameModeForPlayer().isSurvival()) {
            // 如果有翅膀且未伪装，给予飞行权限
            if (PlayerCorpseData.hasWing(player) && !PlayerCorpseData.isDisguised(player)) {
                if (!player.getAbilities().mayfly) {
                    player.getAbilities().mayfly = true;
                    player.onUpdateAbilities();
                }
            } else {
                // 没有翅膀或伪装时禁用飞行能力
                if (player.getAbilities().mayfly) {
                    player.getAbilities().mayfly = false;
                    player.getAbilities().flying = false;
                    player.onUpdateAbilities();
                }
            }
        }
    }

    private static void updateCorpseBehavior(ServerPlayer player) {
        // 每5秒减少1点饥饿度
        if (player.tickCount % 100 == 0) {
            int hunger = PlayerCorpseData.getHunger(player);
            if (hunger > 0) {
                PlayerCorpseData.setHunger(player, hunger - 1);
            }
        }

        // 每10秒：尸族被动恢复（吃活物回血特性）
        if (player.tickCount % 200 == 0) {
            // 基础生命恢复
            if (player.getHealth() < player.getMaxHealth()) {
                float healAmount = 0.5f; // 基础恢复

                // 根据进化等级增加恢复量
                int evolutionLevel = PlayerCorpseData.getEvolutionLevel(player);
                healAmount += evolutionLevel * 0.3f; // 每级增加0.3恢复

                player.heal(healAmount);
            }

            // 高等级尸族获得力量效果
            int evolutionLevel = PlayerCorpseData.getEvolutionLevel(player);
            if (evolutionLevel >= 3) {
                // 3级以上获得力量I
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 300, 0, false, false));
            }
            if (evolutionLevel >= 5) {
                // 5级获得力量II（覆盖力量I）
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 300, 1, false, false));
            }

            // 高等级尸族获得夜视能力（3级以上）
            if (evolutionLevel >= 3) {
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 400, 0, false, false));
            }
        }

    }

    /**
     * 更新尸族玩家的移动能力（翅膀飞行和鱼尾游泳）
     */
    private static void updateCorpseMovement(ServerPlayer player) {
        // 只对生存模式玩家应用飞行能力，创造模式玩家不受影响
        if (player.gameMode.getGameModeForPlayer().isSurvival()) {
            // 翅膀飞行能力 - 简化版，像创造模式一样直接飞行
            if (PlayerCorpseData.hasWing(player) && !PlayerCorpseData.isDisguised(player)) {
                handleWingFlight(player);
            } else {
                // 没有翅膀或伪装时禁用飞行能力
                if (player.getAbilities().mayfly) {
                    player.getAbilities().mayfly = false;
                    player.getAbilities().flying = false;
                    player.onUpdateAbilities();
                }
            }
        }

        // 鱼尾游泳功能
        if (PlayerCorpseData.hasTail(player) && !PlayerCorpseData.isDisguised(player)) {
            handleTailSwimming(player);
        }
    }

    /**
     * 处理翅膀飞行 - 简化版，直接给予飞行能力
     */
    private static void handleWingFlight(ServerPlayer player) {
        // 只对生存模式玩家应用飞行能力，创造模式玩家不受影响
        if (player.gameMode.getGameModeForPlayer().isSurvival()) {
            // 允许玩家飞行（像创造模式一样）
            if (!player.getAbilities().mayfly) {
                player.getAbilities().mayfly = true;
                player.onUpdateAbilities();
            }

            // 飞行时消耗饥饿度
            if (player.getAbilities().flying) {
                // 每20tick（1秒）消耗1点饥饿值
                if (player.tickCount % 20 == 0) {
                    int hunger = PlayerCorpseData.getHunger(player);
                    if (hunger > 0) {
                        PlayerCorpseData.setHunger(player, hunger - 1);
                    } else {
                        // 饥饿值为0时强制落地
                        player.getAbilities().flying = false;
                        player.onUpdateAbilities();
                        player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§c饥饿值耗尽，无法继续飞行！"));
                    }
                }
            }
        }
    }

    /**
     * 处理鱼尾游泳功能
     */
    private static void handleTailSwimming(ServerPlayer player) {
        // 检查玩家是否在水中
        if (player.isInFluidType()) {
            // 消耗尸兄饥饿值
            int hunger = PlayerCorpseData.getHunger(player);
            if (hunger > 0) {
                // 每20tick消耗1点饥饿值
                if (player.tickCount % 20 == 0) {
                    PlayerCorpseData.setHunger(player, hunger - 1);
                }

                // 给予水下呼吸效果
                player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 200, 0, false, false));

                // 给予游泳速度提升效果
                player.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 200, 1, false, false));
            }
        }
    }

    /**
     * 尸族攻击生物时恢复饱食度和生命值（吃活物特性）
     */
    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Post event) {
        if (event.getSource().getEntity() instanceof ServerPlayer player) {
            if (PlayerCorpseData.isCorpse(player)) {
                LivingEntity target = event.getEntity();

                // 只有攻击活物（非亡灵、非机械）才恢复
                if (isLivingCreature(target)) {
                    // 恢复饥饿度（饱食度）
                    int currentHunger = PlayerCorpseData.getHunger(player);
                    int newHunger = Math.min(100, currentHunger + 5); // 攻击一次恢复5点
                    PlayerCorpseData.setHunger(player, newHunger);

                    // 恢复生命值
                    float healAmount = 1.0f; // 基础恢复
                    int evolutionLevel = PlayerCorpseData.getEvolutionLevel(player);
                    healAmount += evolutionLevel * 0.5f; // 每级增加0.5恢复

                    player.heal(healAmount);

                    // 攻击村民时有概率获得进化点
                    if (target instanceof net.minecraft.world.entity.npc.AbstractVillager) {
                        // 0.05%的概率获得1点进化点
                        if (player.getRandom().nextFloat() < 0.0005f) {
                            com.phagens.corpseorigin.skill.ISkillHandler handler = com.phagens.corpseorigin.skill.SkillAttachment.getSkillHandler(player);
                            if (handler != null) {
                                handler.addEvolutionPoints(1);
                                player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.corpseorigin.evolution_point_gained"));
                                CorpseOrigin.LOGGER.info("尸族玩家 {} 攻击村民后获得 1 点进化点",
                                        player.getName().getString());
                            }
                        }
                    }

                    CorpseOrigin.LOGGER.debug("尸族玩家 {} 攻击 {} 恢复 {} 饥饿度和 {} 生命值",
                            player.getName().getString(), target.getName().getString(),
                            newHunger - currentHunger, healAmount);
                }
            }
        }
    }

    /**
     * 判断目标是否是活物（可以被"吃"）
     */
    private static boolean isLivingCreature(LivingEntity entity) {
        // 排除亡灵生物（僵尸、骷髅等）
        if (entity.getType().getCategory() == net.minecraft.world.entity.MobCategory.MONSTER) {
            // 尸兄可以吞噬其他怪物
            return true;
        }
        // 动物、水生生物、村民都是活物
        return entity.getType().getCategory() == net.minecraft.world.entity.MobCategory.CREATURE ||
                entity.getType().getCategory() == net.minecraft.world.entity.MobCategory.AMBIENT ||
                entity.getType().getCategory() == net.minecraft.world.entity.MobCategory.WATER_AMBIENT ||
                entity instanceof net.minecraft.world.entity.npc.AbstractVillager ||
                entity instanceof Player;
    }

    /**
     * 尸族免疫普通毒素
     */
    @SubscribeEvent
    public static void onEffectApplicable(MobEffectEvent.Applicable event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (PlayerCorpseData.isCorpse(player)) {
                // 检查是否是毒素效果
                if (isPoisonEffect(event.getEffectInstance())) {
                    // 尸族免疫普通毒素
                    event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
                    CorpseOrigin.LOGGER.debug("尸族玩家 {} 免疫毒素", player.getName().getString());
                }
            }
        }
    }

    /**
     * 判断是否是毒素效果
     */
    private static boolean isPoisonEffect(MobEffectInstance effect) {
        if (effect == null) return false;

        // 获取效果实例（解包 Holder）
        MobEffect mobEffect = effect.getEffect().value();

        // 原版毒素效果
        return mobEffect == MobEffects.POISON.value() ||
                mobEffect == MobEffects.HUNGER.value() ||
                mobEffect.getCategory() == net.minecraft.world.effect.MobEffectCategory.HARMFUL;
    }

    /**
     * 尸族玩家击杀生物时增加击杀数并检查进化等级
     */
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer player) {
            if (PlayerCorpseData.isCorpse(player)) {
                LivingEntity target = event.getEntity();

                // 只有击杀活物（非亡灵、非机械）才增加击杀数
                if (isLivingCreature(target)) {
                    // 增加击杀数
                    PlayerCorpseData.addKill(player);

                    // 检查是否是高阶生物
                    if (isHighLevelCreature(target)) {
                        int currentLevel = PlayerCorpseData.getEvolutionLevel(player);
                        int newLevel = currentLevel + 1;

                        // 确保进化等级不超过5级
                        if (newLevel <= 5) {
                            PlayerCorpseData.setEvolutionLevel(player, newLevel);
                            syncCorpseStateToClient(player);

                            // 发送进化等级提升提示
                            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                                    "§a你吞噬了高阶生物！进化等级提升至 " + newLevel + " 级！"
                            ));

                            CorpseOrigin.LOGGER.info("尸族玩家 {} 吞噬了高阶生物，进化等级提升至 {} 级",
                                    player.getName().getString(), newLevel);

                            // 检查是否达到3级，如果是且失去意识，则恢复意识
                            if (newLevel >= 3 && !PlayerCorpseData.hasConsciousness(player)) {
                                PlayerCorpseData.restoreConsciousness(player);
                                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                                        "§a§l你的意识从混沌中苏醒！§r\n" +
                                        "§7随着进化的提升，你重新获得了人类的智慧！"
                                ));
                                CorpseOrigin.LOGGER.info("尸族玩家 {} 进化到3级，恢复了意识！", player.getName().getString());
                            }
                        }
                    } else {
                        // 检查是否需要基于击杀数提升进化等级
                        checkEvolutionLevel(player);
                    }

                    // 检查是否击杀鸟类/鸡，有概率长出羽翼
                    if (isBirdOrChicken(target) && !PlayerCorpseData.hasWing(player)) {
                        if (player.getRandom().nextFloat() < 0.3f) { // 30%概率
                            PlayerCorpseData.setHasWing(player, true);
                            syncCorpseStateToClient(player);
                            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                                    "§a你吞噬了鸟类！长出了羽翼！"
                            ));
                            CorpseOrigin.LOGGER.info("尸族玩家 {} 击杀鸟类后长出了羽翼",
                                    player.getName().getString());
                        }
                    }

                    // 检查是否击杀鱼类，有概率长出鱼尾
                    if (isFish(target) && !PlayerCorpseData.hasTail(player)) {
                        if (player.getRandom().nextFloat() < 0.3f) { // 30%概率
                            PlayerCorpseData.setHasTail(player, true);
                            syncCorpseStateToClient(player);
                            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                                    "§a你吞噬了鱼类！长出了鱼尾！"
                            ));
                            CorpseOrigin.LOGGER.info("尸族玩家 {} 击杀鱼类后长出了鱼尾",
                                    player.getName().getString());
                        }
                    }

                    CorpseOrigin.LOGGER.debug("尸族玩家 {} 击杀 {}，当前击杀数: {}",
                            player.getName().getString(), target.getName().getString(),
                            PlayerCorpseData.getKills(player));
                }
            }
        }
    }

    /**
     * 判断是否是鸟类或鸡
     */
    private static boolean isBirdOrChicken(LivingEntity entity) {
        return entity instanceof net.minecraft.world.entity.animal.Chicken ||
                entity instanceof net.minecraft.world.entity.animal.Parrot;
    }

    /**
     * 判断是否是鱼类
     */
    private static boolean isFish(LivingEntity entity) {
        boolean isFish = entity.getType().getCategory() == net.minecraft.world.entity.MobCategory.WATER_AMBIENT;
        if (isFish) {
            CorpseOrigin.LOGGER.info("识别到鱼类生物: {}", entity.getName().getString());
        }
        return isFish;
    }

    /**
     * 检查并提升进化等级
     */
    private static void checkEvolutionLevel(ServerPlayer player) {
        int kills = PlayerCorpseData.getKills(player);
        int currentLevel = PlayerCorpseData.getEvolutionLevel(player);
        int newLevel = currentLevel;

        // 根据击杀数判断进化等级
        if (kills >= 400 && currentLevel < 5) {
            newLevel = 5;
        } else if (kills >= 200 && currentLevel < 4) {
            newLevel = 4;
        } else if (kills >= 100 && currentLevel < 3) {
            newLevel = 3;
        } else if (kills >= 50 && currentLevel < 2) {
            newLevel = 2;
        }

        // 如果进化等级发生变化，更新等级并发送提示
        if (newLevel > currentLevel) {
            PlayerCorpseData.setEvolutionLevel(player, newLevel);
            syncCorpseStateToClient(player);

            // 发送进化等级提升提示
            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                    "message.corpseorigin.evolution_level_up", newLevel
            ));

            CorpseOrigin.LOGGER.info("尸族玩家 {} 进化等级提升至 {} 级，当前击杀数: {}",
                    player.getName().getString(), newLevel, kills);

            // 检查是否达到3级，如果是且失去意识，则恢复意识
            if (newLevel >= 3 && !PlayerCorpseData.hasConsciousness(player)) {
                PlayerCorpseData.restoreConsciousness(player);
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§a§l你的意识从混沌中苏醒！§r\n" +
                        "§7随着进化的提升，你重新获得了人类的智慧！"
                ));
                CorpseOrigin.LOGGER.info("尸族玩家 {} 进化到3级，恢复了意识！", player.getName().getString());
            }
        }
    }

    /**
     * 判断是否是高阶生物
     */
    private static boolean isHighLevelCreature(LivingEntity entity) {
        // 检查是否是末影龙
        if (entity instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon) {
            return true;
        }

        // 检查是否是凋灵
        if (entity instanceof net.minecraft.world.entity.boss.wither.WitherBoss) {
            return true;
        }

        // 检查是否是监守者
        if (entity instanceof net.minecraft.world.entity.monster.warden.Warden) {
            return true;
        }

        // 检查是否是龙右（尸族高阶生物）
        if (entity instanceof com.phagens.corpseorigin.entity.LongyouEntity) {
            return true;
        }

        // 检查实体等级（如果有）
        if (entity instanceof net.minecraft.world.entity.monster.Enemy) {
            // 对于敌人实体，检查其生命值
            if (entity.getMaxHealth() >= 100.0f) {
                return true;
            }
        }

        return false;
    }
}