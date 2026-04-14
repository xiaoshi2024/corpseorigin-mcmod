package com.phagens.corpseorigin.skill;

import com.phagens.corpseorigin.CorpseOrigin;
import com.phagens.corpseorigin.player.PlayerCorpseData;
import com.phagens.corpseorigin.skill.special.CorpseKingPowerSkill;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

/**
 * 尸兄技能定义类 - 基于《尸兄》原作中的各种能力
 *
 * 【功能说明】
 * 1. 定义所有尸兄可学习的技能
 * 2. 按类型分类：基础进化、力量型、敏捷型、特殊型、神级、超神级
 * 3. 每个技能都有独特的效果和激活逻辑
 * 4. 支持被动技能（自动生效）和主动技能（手动激活）
 *
 * 【技能体系】
 * 第1层 - 基础进化：硬化皮肤、利爪、吞噬强化、进化感知
 * 第2层 - 初级变异：巨力、疾行、再生、毒液
 * 第3层 - 进阶技能：狂暴、重击、跳跃、闪避、恐惧光环
 * 第4层 - 神级能力：不死之身
 * 第5层 - 终极能力：尸王之力、影袭
 *
 * 【技能类型】
 * - 被动技能：通过属性修饰符或事件监听实现
 * - 主动技能：通过onActivate方法实现即时效果
 * - 持续技能：激活后在持续时间内生效
 *
 * 【关联系统】
 * - BaseSkill: 技能基类
 * - CorpseSkillTree: 技能树组织
 * - SkillEventHandler: 被动技能事件处理
 * - CorpseKingPowerSkill: 特殊技能实现
 *
 * @author Phagens
 * @version 1.0
 */
public class CorpseSkills {

    /** 技能注册表，存储所有定义的技能 */
    private static final Map<ResourceLocation, ISkill> SKILLS = new HashMap<>();

    // ==================== 基础进化技能 ====================

    /**
     * 硬化皮肤 - 增加护甲值
     * 类型：被动技能
     * 效果：+4护甲值
     * 消耗：1进化点
     */
    public static final ISkill HARDENED_SKIN = register(new SimpleSkill(
            new BaseSkill.Builder(id("hardened_skin"))
                    .name(Component.translatable("skill.corpseorigin.hardened_skin"))
                    .description(Component.translatable("skill.corpseorigin.hardened_skin.desc"))
                    .cost(1)
                    .skillType(ISkill.SkillType.BASIC_EVOLUTION)
                    .requiredLevel(1)
                    .passive(true)
                    .attributeModifier(Attributes.ARMOR, 
                            new AttributeModifier(id("hardened_skin"), 4.0, AttributeModifier.Operation.ADD_VALUE))
    ) {
        @Override
        public void onActivate(Player player) {
            // 被动技能，无需激活
        }
    });
    
    /**
     * 利爪 - 增加攻击伤害
     * 类型：被动技能
     * 效果：+3攻击伤害
     * 消耗：1进化点
     */
    public static final ISkill SHARP_CLAWS = register(new SimpleSkill(
            new BaseSkill.Builder(id("sharp_claws"))
                    .name(Component.translatable("skill.corpseorigin.sharp_claws"))
                    .description(Component.translatable("skill.corpseorigin.sharp_claws.desc"))
                    .cost(1)
                    .skillType(ISkill.SkillType.BASIC_EVOLUTION)
                    .requiredLevel(1)
                    .passive(true)
                    .attributeModifier(Attributes.ATTACK_DAMAGE,
                            new AttributeModifier(id("sharp_claws"), 3.0, AttributeModifier.Operation.ADD_VALUE))
    ) {
        @Override
        public void onActivate(Player player) {
            // 被动技能，无需激活逻辑
        }
    });

    /**
     * 吞噬强化 - 攻击时恢复更多生命值
     * 类型：被动技能
     * 效果：攻击活物时恢复1.5生命值，+8饥饿度
     * 消耗：2进化点
     * 触发：攻击事件（SkillEventHandler处理）
     */
    public static final ISkill DEVOUR_ENHANCEMENT = register(new SimpleSkill(
            new BaseSkill.Builder(id("devour_enhancement"))
                    .name(Component.translatable("skill.corpseorigin.devour_enhancement"))
                    .description(Component.translatable("skill.corpseorigin.devour_enhancement.desc"))
                    .cost(2)
                    .skillType(ISkill.SkillType.BASIC_EVOLUTION)
                    .requiredLevel(2)
                    .passive(true)
    ) {
        @Override
        public void onActivate(Player player) {
            // 被动效果在攻击事件处理（SkillEventHandler）
        }
    });

    /**
     * 进化感知 - 获得夜视能力
     * 类型：被动技能
     * 效果：永久夜视效果
     * 消耗：2进化点
     * 触发：每20tick自动刷新夜视效果
     */
    public static final ISkill EVOLUTION_SENSE = register(new SimpleSkill(
            new BaseSkill.Builder(id("evolution_sense"))
                    .name(Component.translatable("skill.corpseorigin.evolution_sense"))
                    .description(Component.translatable("skill.corpseorigin.evolution_sense.desc"))
                    .cost(2)
                    .skillType(ISkill.SkillType.BASIC_EVOLUTION)
                    .requiredLevel(2)
                    .passive(true)
    ) {
        @Override
        public void onActivate(Player player) {
            // 被动效果在tick事件中处理（SkillEventHandler）
        }
    });
    
    // ==================== 力量型变异技能 ====================
    
    /**
     * 巨力 - 大幅增加攻击伤害
     */
    public static final ISkill GIANT_STRENGTH = register(new SimpleSkill(
            new BaseSkill.Builder(id("giant_strength"))
                    .name(Component.translatable("skill.corpseorigin.giant_strength"))
                    .description(Component.translatable("skill.corpseorigin.giant_strength.desc"))
                    .cost(3)
                    .skillType(ISkill.SkillType.POWER_MUTATION)
                    .requiredLevel(3)
                    .prerequisite(id("sharp_claws"))
                    .passive(true)
                    .attributeModifier(Attributes.ATTACK_DAMAGE, 
                            new AttributeModifier(id("giant_strength"), 0.5, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL))
    ) {
        @Override
        public void onActivate(Player player) {
            // 被动技能
        }
    });
    
    /**
     * 狂暴 - 临时增加攻击速度
     */
    public static final ISkill BERSERK = register(new SimpleSkill(
            new BaseSkill.Builder(id("berserk"))
                    .name(Component.translatable("skill.corpseorigin.berserk"))
                    .description(Component.translatable("skill.corpseorigin.berserk.desc"))
                    .cost(3)
                    .skillType(ISkill.SkillType.POWER_MUTATION)
                    .requiredLevel(3)
                    .prerequisite(id("giant_strength"))
                    .cooldown(600) // 30秒
                    .duration(200) // 10秒
    ) {
        @Override
        public void onActivate(Player player) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 1));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 0));
        }
    });
    
    /**
     * 重击 - 攻击带有击退效果
     */
    public static final ISkill HEAVY_STRIKE = register(new SimpleSkill(
            new BaseSkill.Builder(id("heavy_strike"))
                    .name(Component.translatable("skill.corpseorigin.heavy_strike"))
                    .description(Component.translatable("skill.corpseorigin.heavy_strike.desc"))
                    .cost(2)
                    .skillType(ISkill.SkillType.POWER_MUTATION)
                    .requiredLevel(4)
                    .prerequisite(id("giant_strength"))
                    .passive(true)
    ) {
        @Override
        public void onActivate(Player player) {
            // 被动效果在攻击事件中处理
        }
    });
    
    // ==================== 敏捷型变异技能 ====================
    
    /**
     * 疾行 - 增加移动速度
     */
    public static final ISkill SWIFT_MOVEMENT = register(new SimpleSkill(
            new BaseSkill.Builder(id("swift_movement"))
                    .name(Component.translatable("skill.corpseorigin.swift_movement"))
                    .description(Component.translatable("skill.corpseorigin.swift_movement.desc"))
                    .cost(2)
                    .skillType(ISkill.SkillType.AGILITY_MUTATION)
                    .requiredLevel(2)
                    .passive(true)
                    .attributeModifier(Attributes.MOVEMENT_SPEED, 
                            new AttributeModifier(id("swift_movement"), 0.2, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL))
    ) {
        @Override
        public void onActivate(Player player) {
            // 被动技能
        }
    });
    
    /**
     * 跳跃强化 - 增加跳跃高度
     */
    public static final ISkill LEAP = register(new SimpleSkill(
            new BaseSkill.Builder(id("leap"))
                    .name(Component.translatable("skill.corpseorigin.leap"))
                    .description(Component.translatable("skill.corpseorigin.leap.desc"))
                    .cost(2)
                    .skillType(ISkill.SkillType.AGILITY_MUTATION)
                    .requiredLevel(3)
                    .prerequisite(id("swift_movement"))
                    .passive(true)
    ) {
        @Override
        public void onActivate(Player player) {
            // 被动效果在跳跃事件中处理
        }
    });
    
    /**
     * 闪避 - 几率闪避攻击
     */
    public static final ISkill EVASION = register(new SimpleSkill(
            new BaseSkill.Builder(id("evasion"))
                    .name(Component.translatable("skill.corpseorigin.evasion"))
                    .description(Component.translatable("skill.corpseorigin.evasion.desc"))
                    .cost(3)
                    .skillType(ISkill.SkillType.AGILITY_MUTATION)
                    .requiredLevel(4)
                    .prerequisite(id("swift_movement"))
                    .passive(true)
    ) {
        @Override
        public void onActivate(Player player) {
            // 被动效果在受到伤害事件中处理
        }
    });
    
    // ==================== 特殊型变异技能 ====================
    
    /**
     * 毒液 - 攻击带毒
     */
    public static final ISkill VENOM = register(new SimpleSkill(
            new BaseSkill.Builder(id("venom"))
                    .name(Component.translatable("skill.corpseorigin.venom"))
                    .description(Component.translatable("skill.corpseorigin.venom.desc"))
                    .cost(3)
                    .skillType(ISkill.SkillType.SPECIAL_MUTATION)
                    .requiredLevel(3)
                    .passive(true)
    ) {
        @Override
        public void onActivate(Player player) {
            // 被动效果在攻击事件中处理
        }
    });
    
    /**
     * 快速再生 - 加快生命恢复
     */
    public static final ISkill REGENERATION = register(new SimpleSkill(
            new BaseSkill.Builder(id("regeneration"))
                    .name(Component.translatable("skill.corpseorigin.regeneration"))
                    .description(Component.translatable("skill.corpseorigin.regeneration.desc"))
                    .cost(2)
                    .skillType(ISkill.SkillType.SPECIAL_MUTATION)
                    .requiredLevel(2)
                    .passive(true)
    ) {
        @Override
        public void onActivate(Player player) {
            // 被动效果在tick事件中处理
        }
    });
    
    /**
     * 恐惧光环 - 使附近敌人恐惧
     */
    public static final ISkill FEAR_AURA = register(new SimpleSkill(
            new BaseSkill.Builder(id("fear_aura"))
                    .name(Component.translatable("skill.corpseorigin.fear_aura"))
                    .description(Component.translatable("skill.corpseorigin.fear_aura.desc"))
                    .cost(4)
                    .skillType(ISkill.SkillType.SPECIAL_MUTATION)
                    .requiredLevel(4)
                    .prerequisite(id("venom"))
                    .cooldown(400) // 20秒
                    .duration(100) // 5秒
    ) {
        @Override
        public void onActivate(Player player) {
            // 激活时给附近敌人添加虚弱和缓慢效果
            player.level().getEntities(player, player.getBoundingBox().inflate(5.0), 
                    entity -> entity instanceof net.minecraft.world.entity.LivingEntity && 
                             !(entity instanceof Player))
                    .forEach(entity -> {
                        if (entity instanceof net.minecraft.world.entity.LivingEntity living) {
                            living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0));
                            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 0));
                        }
                    });
        }
    });
    
    // ==================== 多眼形态专属技能 ====================

    /**
     * 多眼感知 - 多眼形态专属技能
     * 类型：主动技能
     * 效果：触发时能快速感知到周围的敌意生物，模拟多眼360度视角
     * 消耗：进化获得，不需要消耗进化点
     */
    public static final ISkill MULTI_EYE_PERCEPTION = register(new MultiEyePerceptionSkill());

    // ==================== 神级能力 ====================

    /**
     * 不死之身 - 死亡时复活一次
     */
    public static final ISkill IMMORTAL_BODY = register(new SimpleSkill(
            new BaseSkill.Builder(id("immortal_body"))
                    .name(Component.translatable("skill.corpseorigin.immortal_body"))
                    .description(Component.translatable("skill.corpseorigin.immortal_body.desc"))
                    .cost(5)
                    .skillType(ISkill.SkillType.DIVINE_ABILITY)
                    .requiredLevel(5)
                    .prerequisite(id("regeneration"))
                    .cooldown(12000) // 10分钟
                    .passive(true)
    ) {
        @Override
        public void onActivate(Player player) {
            // 被动效果在死亡事件中处理
        }
    });
    
    /**
     * 尸王之力 - 终极力量技能
     * 可以感染其他玩家并短暂控制被感染的玩家
     */
    public static final ISkill CORPSE_KING_POWER = register(new CorpseKingPowerSkill(
            new BaseSkill.Builder(id("corpse_king_power"))
                    .name(Component.translatable("skill.corpseorigin.corpse_king_power"))
                    .description(Component.translatable("skill.corpseorigin.corpse_king_power.desc"))
                    .cost(5)
                    .skillType(ISkill.SkillType.SUPREME_ABILITY)
                    .requiredLevel(5)
                    .prerequisites(id("berserk"), id("heavy_strike"))
                    .cooldown(600) // 30秒基础冷却
                    .duration(200) // 10秒控制持续时间
    ));

    /**
     * 影袭 - 终极敏捷技能
     */
    public static final ISkill SHADOW_STRIKE = register(new SimpleSkill(
            new BaseSkill.Builder(id("shadow_strike"))
                    .name(Component.translatable("skill.corpseorigin.shadow_strike"))
                    .description(Component.translatable("skill.corpseorigin.shadow_strike.desc"))
                    .cost(5)
                    .skillType(ISkill.SkillType.SUPREME_ABILITY)
                    .requiredLevel(5)
                    .prerequisites(id("evasion"), id("leap"))
                    .cooldown(600) // 30秒冷却
                    .duration(100) // 添加持续时间，5秒（100 tick）
            // 注意：不要加 .passive(true)！
    ) {
        @Override
        public void onActivate(Player player) {
            // 影袭效果
            player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 100, 0));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 100, 2));

            // 瞬移逻辑
            if (!player.level().isClientSide) {
                // 获取玩家看向的方向
                Vec3 lookVec = player.getLookAngle();
                // 向前传送5格
                double newX = player.getX() + lookVec.x * 5;
                double newY = player.getY();
                double newZ = player.getZ() + lookVec.z * 5;

                // 检查位置是否安全
                player.teleportTo(newX, newY, newZ);

                // 对传送路径上的敌人造成伤害
                AABB area = player.getBoundingBox().inflate(5);
                player.level().getEntities(player, area).forEach(entity -> {
                    if (entity instanceof LivingEntity target && !(target instanceof Player)) {
                        target.hurt(player.damageSources().playerAttack(player), 8.0f);
                    }
                });
            }
        }
    });
    
    /**
     * 伪装 - 隐藏尸兄骨骼
     * 类型：主动技能
     * 效果：隐藏外骨骼、羽翼和鱼尾
     * 消耗：2进化点
     * 冷却：30秒
     * 持续时间：60秒
     */
    public static final ISkill DISGUISE = register(new SimpleSkill(
            new BaseSkill.Builder(id("disguise"))
                    .name(Component.translatable("skill.corpseorigin.disguise"))
                    .description(Component.translatable("skill.corpseorigin.disguise.desc"))
                    .cost(2)
                    .skillType(ISkill.SkillType.SPECIAL_MUTATION)
                    .requiredLevel(3)
                    .cooldown(600) // 30秒冷却
                    .duration(1200) // 60秒持续时间
    ) {
        @Override
        public void onActivate(Player player) {
            if (!player.level().isClientSide) {
                // 切换伪装状态
                boolean isDisguised = !PlayerCorpseData.isDisguised(player);
                PlayerCorpseData.setDisguised(player, isDisguised);
                
                // 同步到客户端
                if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                    com.phagens.corpseorigin.network.PlayerCorpseSyncPacket packet = new com.phagens.corpseorigin.network.PlayerCorpseSyncPacket(
                            serverPlayer.getId(),
                            PlayerCorpseData.isCorpse(serverPlayer),
                            PlayerCorpseData.getCorpseType(serverPlayer),
                            PlayerCorpseData.getCorpseData(serverPlayer)
                    );
                    net.neoforged.neoforge.network.PacketDistributor.sendToAllPlayers(packet);
                }
                
                // 发送提示
                if (isDisguised) {
                    player.sendSystemMessage(Component.literal("§a你激活了伪装技能，骨骼已隐藏！"));
                } else {
                    player.sendSystemMessage(Component.literal("§a你解除了伪装，骨骼已显现！"));
                }
            }
        }
    });
    
    /**
     * 注册技能到技能管理器
     *
     * @param skill 要注册的技能
     * @return 注册后的技能
     */
    private static ISkill register(ISkill skill) {
        SKILLS.put(skill.getId(), skill);
        SkillManager.getInstance().registerSkill(skill);
        return skill;
    }

    /**
     * 创建资源位置
     *
     * @param path 路径
     * @return 资源位置
     */
    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(CorpseOrigin.MODID, path);
    }

    /**
     * 获取所有已注册的技能
     *
     * @return 技能映射表（副本）
     */
    public static Map<ResourceLocation, ISkill> getAllSkills() {
        return new HashMap<>(SKILLS);
    }

    /**
     * 简单技能实现类
     * 继承BaseSkill，用于定义简单的技能
     */
    public static abstract class SimpleSkill extends BaseSkill {
        public SimpleSkill(Builder builder) {
            super(builder);
        }
    }

    /**
     * 初始化所有技能
     * 在模组初始化时调用，记录注册的技能数量
     */
    public static void init() {
        CorpseOrigin.LOGGER.info("注册了 {} 个尸兄技能", SKILLS.size());
    }
}
