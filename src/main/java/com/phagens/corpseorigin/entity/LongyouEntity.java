package com.phagens.corpseorigin.entity;

import com.phagens.corpseorigin.CorpseOrigin;
import com.phagens.corpseorigin.effect.BYeffect;
import com.phagens.corpseorigin.entity.skills.LongyouSkills;
import com.phagens.corpseorigin.event.custom.WeaponBreakEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.*;

public class LongyouEntity extends PathfinderMob implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    protected static final RawAnimation WALK_ANIM = RawAnimation.begin().thenLoop("walk");
    protected static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("idle");
    protected static final RawAnimation ATTACK_ANIM = RawAnimation.begin().thenPlay("attack");
    protected static final RawAnimation SHIEYE_ANIM = RawAnimation.begin().thenPlay("shieye");

    private static final EntityDataAccessor<Boolean> DATA_PLAYING_SHIEYE =
            SynchedEntityData.defineId(LongyouEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_PLAYING_AURA_SKILL =
            SynchedEntityData.defineId(LongyouEntity.class, EntityDataSerializers.BOOLEAN);

    private int shieyeCooldown = 0;
    private int shieyeAnimationTicks = 0;
    private int auraSkillTicks = 0;
    
    // 技能冷却
    private int xuanwuBodyCooldown = 0;
    private int geckoTechniqueCooldown = 0;
    private int tianGangQiCooldown = 0;
    private int earthquakeCooldown = 0;
    private int summonMinionsCooldown = 0;
    
    // 天罡气技能冷却
    private int tianGangQiJiCooldown = 0;
    private int tianGangQiLiCooldown = 0;
    private int tianGangQiHuiCooldown = 0;
    private int tianGangQiMieCooldown = 0;
    private int tianGangQiWuCooldown = 0;
    private int tianGangQiShenCooldown = 0;
    private int niPoQuanCooldown = 0;
    private int poGangCooldown = 0;
    private int tianGangPoCooldown = 0;

    // 龙右的智能系统
    private int intelligenceCheckCooldown = 0;
    private static final int INTELLIGENCE_CHECK_INTERVAL = 100; // 每5秒检查一次
    
    // 村民处理冷却（避免龙右吃村民过快）
    private int villagerConsumeCooldown = 0;
    private static final int VILLAGER_CONSUME_INTERVAL = 300; // 每15秒才能吃/感染一个村民

    // 手下列表（被龙右认可的尸兄）
    private final Set<UUID> minions = new HashSet<>();
    // 粮仓列表（被标记为食物的尸兄）
    private final Set<UUID> foodReserves = new HashSet<>();
    // 手下数量上限
    private static final int MAX_MINIONS = 10;
    // 识别范围（用于评估尸兄和村民）
    private static final double RECOGNITION_RANGE = 32.0D;
    // 吞噬范围（只有在这个范围内的粮仓/村民才能被吞噬）
    private static final double CONSUME_RANGE = 4.0D;
    // 村民处理范围（只有在这个范围内的村民才能被感染或吃掉）
    private static final double VILLAGER_INTERACTION_RANGE = 4.0D;
    
    // 龙右的状态系统
    private int hunger = 100; // 饥饿度 (0-100, 100为饱腹)
    private int mood = 50; // 心情值 (0-100, 50为中性)
    private int interest = 0; // 兴趣值 (0-100, 0为无兴趣)
    
    // 状态阈值
    private static final int HUNGER_THRESHOLD = 30; // 饥饿度低于此值才会攻击
    private static final int MOOD_THRESHOLD = 70; // 心情值高于此值不会攻击
    private static final int INTEREST_THRESHOLD = 60; // 兴趣值高于此值不会攻击
    
    // 被攻击状态
    private int lastHurtTick = -1000; // 上次被攻击的游戏刻
    private static final int HURT_MEMORY_DURATION = 200; // 被攻击记忆持续时间（10秒）

    public LongyouEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_PLAYING_SHIEYE, false);
        builder.define(DATA_PLAYING_AURA_SKILL, false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2D, true));
        // 龙右作为尸王可以开门
        this.goalSelector.addGoal(3, new OpenDoorGoal(this, true));
        this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));

        // 龙右作为尸王，不会攻击尸兄（同类），但会攻击其他生物
        // 使用自定义条件判断是否攻击：只有饥饿或被攻击时才会主动出击
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, 0, true, false, this::shouldAttackTarget));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Animal.class, 0, true, false, this::shouldAttackTarget));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Villager.class, 0, true, false, this::shouldAttackTarget));
        // 不会主动攻击 LowerLevelZbEntity（尸兄同类）
    }

    @Override
    protected net.minecraft.world.entity.ai.navigation.PathNavigation createNavigation(Level level) {
        net.minecraft.world.entity.ai.navigation.GroundPathNavigation navigation = new net.minecraft.world.entity.ai.navigation.GroundPathNavigation(this, level);
        navigation.setCanOpenDoors(true);
        return navigation;
    }
    
    /**
     * 判断是否应该攻击某个目标
     * 用于AI目标选择
     * 龙右不会主动攻击已成为尸兄的玩家（同类）
     */
    private boolean shouldAttackTarget(net.minecraft.world.entity.LivingEntity entity) {
        // 不攻击已成为尸兄的玩家（同类）
        if (entity instanceof Player player) {
            if (com.phagens.corpseorigin.player.PlayerCorpseData.isCorpse(player)) {
                return false; // 龙右作为尸王，不会攻击同类尸兄玩家
            }
        }
        
        // 只有龙右应该主动出击时才会选择目标
        return shouldInitiateAttack();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 500.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.4D)
                .add(Attributes.ATTACK_DAMAGE, 15.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.ARMOR, 10.0D);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 5, this::controlAnimation));
        // 注册气场技能控制器
        controllers.add(new AnimationController<>(this, "controller.animation.longyou.aura_skill", 0, this::auraSkillController));

    }

    // 动画控制器状态处理
    private PlayState auraSkillController(AnimationState<LongyouEntity> state) {
        LongyouEntity entity = state.getAnimatable();

        // 触发气场技能
        if (entity.entityData.get(DATA_PLAYING_AURA_SKILL)) {
            // 播放串联动画：idle_anger → aura_blast → contempt_end
            state.getController().setAnimation(RawAnimation.begin()
                    .thenPlay("idle_anger")
                    .thenPlay("aura_blast")
                    .thenPlay("contempt_end"));
            return PlayState.CONTINUE;
        }

        // 未触发时，停止此控制器，让主控制器处理
        return PlayState.STOP;
    }

    // 外部调用：触发气场技能（比如被远程攻击时调用）
    public void triggerAuraSkill() {
        if (!this.level().isClientSide() && !this.entityData.get(DATA_PLAYING_AURA_SKILL)) {
            this.entityData.set(DATA_PLAYING_AURA_SKILL, true);
            this.auraSkillTicks = 80; // 约4秒动画时间
        }
    }


    private <E extends LongyouEntity> software.bernie.geckolib.animation.PlayState controlAnimation(AnimationState<E> event) {
        if (this.entityData.get(DATA_PLAYING_SHIEYE)) {
            return event.setAndContinue(SHIEYE_ANIM);
        }

        if (this.getAttackAnim(event.getPartialTick()) > 0) {
            if (!this.level().isClientSide && shieyeCooldown <= 0 && this.random.nextFloat() < 0.4F) {
                triggerShieyeAnimation();
                return event.setAndContinue(SHIEYE_ANIM);
            }
            return event.setAndContinue(ATTACK_ANIM);
        }

        if (event.isMoving()) {
            return event.setAndContinue(WALK_ANIM);
        }

        return event.setAndContinue(IDLE_ANIM);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // 1.21.1 正确判断远程投射物攻击（NeoForge 兼容）
        boolean isRangedAttack = source != null && source.is(DamageTypeTags.IS_PROJECTILE);

        // 仅服务端处理 - 任何攻击都播放动画
        if (this.level() != null && !this.level().isClientSide()) {
            // 播放气场技能动画（任何攻击都要播放）
            this.triggerAuraSkill();

            // 远程攻击特殊处理
            if (isRangedAttack) {
                Entity attackerEntity = source.getEntity();
                if (attackerEntity instanceof LivingEntity attacker && attacker.isAlive()) {
                    // 获取攻击者手持物品
                    ItemStack mainHandItem = attacker.getItemBySlot(EquipmentSlot.MAINHAND);
                    ItemStack offHandItem = attacker.getItemBySlot(EquipmentSlot.OFFHAND);

                    // 检查是否可以防御（普通枪械/弓弩，且弓弩力量附魔不超过2级）
                    boolean canDefendMainHand = canDefendAgainstRanged(mainHandItem);
                    boolean canDefendOffHand = canDefendAgainstRanged(offHandItem);

                    // 如果无法防御（力量3级以上），只播放动画，不执行防御逻辑
                    if (!canDefendMainHand && !canDefendOffHand) {
                        CorpseOrigin.LOGGER.info("[龙右] 攻击者武器太强（力量3+），无法防御，但播放气场动画！");
                        // 继续执行 super.hurt 让伤害通过
                    } else {
                        // 可以防御，执行震飞和武器破坏逻辑
                        // 震飞攻击者逻辑（保留原有代码，NeoForge 无变更）
                        if (this.position() != null && attacker.position() != null) {
                            Vec3 pushDir = this.position().subtract(attacker.position()).normalize().scale(1.5);
                            attacker.setDeltaMovement(pushDir.x, 0.5, pushDir.z);
                            attacker.hurtMarked = true;
                            attacker.hasImpulse = true;
                        }

                        // ========== 触发 NeoForge 自定义事件（核心修改） ==========
                        // 主手远程武器（原版弓箭，且力量附魔<=2级）
                        if (!mainHandItem.isEmpty() && mainHandItem.getItem() instanceof ProjectileWeaponItem && canDefendMainHand) {
                            WeaponBreakEvent breakEvent = new WeaponBreakEvent(attacker, EquipmentSlot.MAINHAND);
                            NeoForge.EVENT_BUS.post(breakEvent); // NeoForge 事件总线
                        }
                        // 副手远程武器（原版弓箭，且力量附魔<=2级）
                        else if (!offHandItem.isEmpty() && offHandItem.getItem() instanceof ProjectileWeaponItem && canDefendOffHand) {
                            WeaponBreakEvent breakEvent = new WeaponBreakEvent(attacker, EquipmentSlot.OFFHAND);
                            NeoForge.EVENT_BUS.post(breakEvent); // NeoForge 事件总线
                        }

                        // ========== Point Blank 枪械处理 ==========
                        // 检测主手是否为 Point Blank 枪械
                        if (!mainHandItem.isEmpty() && isPointBlankGun(mainHandItem)) {
                            CorpseOrigin.LOGGER.info("[龙右] 检测到玩家使用 Point Blank 枪械攻击，震碎枪械！");
                            WeaponBreakEvent breakEvent = new WeaponBreakEvent(attacker, EquipmentSlot.MAINHAND);
                            breakEvent.setDurabilityZero(true); // 直接移除
                            NeoForge.EVENT_BUS.post(breakEvent);
                        }
                        // 检测副手是否为 Point Blank 枪械
                        else if (!offHandItem.isEmpty() && isPointBlankGun(offHandItem)) {
                            CorpseOrigin.LOGGER.info("[龙右] 检测到玩家使用 Point Blank 枪械攻击，震碎枪械！");
                            WeaponBreakEvent breakEvent = new WeaponBreakEvent(attacker, EquipmentSlot.OFFHAND);
                            breakEvent.setDurabilityZero(true); // 直接移除
                            NeoForge.EVENT_BUS.post(breakEvent);
                        }
                    }
                }
            }
        }

        // 原有逻辑（保留）
        if (this.tickCount >= 0) {
            lastHurtTick = this.tickCount;
        }
        return super.hurt(source, amount);
    }

    @Override
    public void tick() {
        super.tick();

        if (shieyeCooldown > 0) {
            shieyeCooldown--;
        }
        
        // 减少村民处理冷却
        if (villagerConsumeCooldown > 0) {
            villagerConsumeCooldown--;
        }
        
        // 技能冷却更新
        if (xuanwuBodyCooldown > 0) xuanwuBodyCooldown--;
        if (geckoTechniqueCooldown > 0) geckoTechniqueCooldown--;
        if (tianGangQiCooldown > 0) tianGangQiCooldown--;
        if (earthquakeCooldown > 0) earthquakeCooldown--;
        if (summonMinionsCooldown > 0) summonMinionsCooldown--;
        
        // 天罡气技能冷却更新
        if (tianGangQiJiCooldown > 0) tianGangQiJiCooldown--;
        if (tianGangQiLiCooldown > 0) tianGangQiLiCooldown--;
        if (tianGangQiHuiCooldown > 0) tianGangQiHuiCooldown--;
        if (tianGangQiMieCooldown > 0) tianGangQiMieCooldown--;
        if (tianGangQiWuCooldown > 0) tianGangQiWuCooldown--;
        if (tianGangQiShenCooldown > 0) tianGangQiShenCooldown--;
        if (niPoQuanCooldown > 0) niPoQuanCooldown--;
        if (poGangCooldown > 0) poGangCooldown--;
        if (tianGangPoCooldown > 0) tianGangPoCooldown--;

        if (!this.level().isClientSide && this.entityData.get(DATA_PLAYING_SHIEYE)) {
            shieyeAnimationTicks--;
            if (shieyeAnimationTicks <= 0) {
                this.entityData.set(DATA_PLAYING_SHIEYE, false);
            }
        }

        // 光环技能动画计时
        if (!this.level().isClientSide && this.entityData.get(DATA_PLAYING_AURA_SKILL)) {
            auraSkillTicks--;
            if (auraSkillTicks <= 0) {
                this.entityData.set(DATA_PLAYING_AURA_SKILL, false);
            }
        }
        
        // 服务端：技能触发逻辑
        if (!this.level().isClientSide && this.isAlive()) {
            useSkills();
        }

        // 服务端：状态系统更新
        if (!this.level().isClientSide) {
            // 每100 tick（5秒）减少1点饥饿度
            if (this.tickCount % 100 == 0 && hunger > 0) {
                hunger--;
            }
            
            // 心情值自然恢复（每200 tick恢复1点）
            if (this.tickCount % 200 == 0 && mood < 100) {
                mood++;
            }
            
            // 兴趣值自然衰减（每150 tick减少1点）
            if (this.tickCount % 150 == 0 && interest > 0) {
                interest--;
            }
        }

        // 服务端：执行智能判断
        if (!this.level().isClientSide && this.level() instanceof ServerLevel) {
            if (intelligenceCheckCooldown-- <= 0) {
                intelligenceCheckCooldown = INTELLIGENCE_CHECK_INTERVAL;
                performIntelligenceCheck();
            }
        }

        // 服务端：龙右自动开门
        if (!this.level().isClientSide) {
            tickDoorInteraction();
        }
    }

    /**
     * 龙右的智能判断系统
     * 评估周围的尸兄，决定哪些是手下，哪些是粮仓
     * 评估周围的村民，决定哪些作为食物，哪些有利用价值感染
     */
    private void performIntelligenceCheck() {
        if (!(this.level() instanceof ServerLevel serverLevel)) return;

        // 获取范围内的所有尸兄
        List<LowerLevelZbEntity> nearbyZombies = serverLevel.getEntitiesOfClass(
                LowerLevelZbEntity.class,
                this.getBoundingBox().inflate(RECOGNITION_RANGE)
        );

        for (LowerLevelZbEntity zb : nearbyZombies) {
            UUID zbId = zb.getUUID();

            // 如果已经分类过，跳过
            if (minions.contains(zbId) || foodReserves.contains(zbId)) {
                continue;
            }

            // 评估这个尸兄的价值
            ZombieValue value = evaluateZombie(zb);

            if (value == ZombieValue.MINION && minions.size() < MAX_MINIONS) {
                // 收为手下
                minions.add(zbId);
                CorpseOrigin.LOGGER.info("龙右将尸兄 {} 收为手下", zbId);
                // 可以在这里添加视觉效果或状态效果
                serverLevel.broadcastEntityEvent(zb, (byte) 7); // 爱心粒子效果
            } else {
                // 标记为粮仓
                foodReserves.add(zbId);
                CorpseOrigin.LOGGER.info("龙右将尸兄 {} 标记为粮仓", zbId);
            }
        }

        // 处理村民（有冷却时间限制）
        if (villagerConsumeCooldown <= 0) {
            // 获取范围内的所有村民（但只在很近的距离内才能处理）
            List<Villager> nearbyVillagers = serverLevel.getEntitiesOfClass(
                    Villager.class,
                    this.getBoundingBox().inflate(VILLAGER_INTERACTION_RANGE)
            );

            // 每次只处理一个最近的村民，避免一次性消灭所有村民
            Villager targetVillager = null;
            double minVillagerDistance = Double.MAX_VALUE;
            
            for (Villager villager : nearbyVillagers) {
                double distance = this.distanceToSqr(villager);
                if (distance < minVillagerDistance) {
                    minVillagerDistance = distance;
                    targetVillager = villager;
                }
            }
            
            // 只处理最近的一个村民
            if (targetVillager != null) {
                // 评估村民的价值
                VillagerValue value = evaluateVillager(targetVillager);

                if (value == VillagerValue.INFECT) {
                    // 感染村民
                    infectVillager(targetVillager, serverLevel);
                } else {
                    // 作为食物
                    consumeVillager(targetVillager, serverLevel);
                }
                // 设置冷却时间
                villagerConsumeCooldown = VILLAGER_CONSUME_INTERVAL;
            }
        }

        // 处理玩家（有冷却时间限制，与村民共享冷却）
        if (villagerConsumeCooldown <= 0) {
            // 获取范围内的所有玩家（但只在很近的距离内才能处理）
            List<Player> nearbyPlayers = serverLevel.getEntitiesOfClass(
                    Player.class,
                    this.getBoundingBox().inflate(VILLAGER_INTERACTION_RANGE)
            );

            // 每次只处理一个最近的玩家
            Player targetPlayer = null;
            double minPlayerDistance = Double.MAX_VALUE;
            
            for (Player player : nearbyPlayers) {
                // 跳过创造模式和旁观模式的玩家
                if (player.isCreative() || player.isSpectator()) {
                    continue;
                }
                double distance = this.distanceToSqr(player);
                if (distance < minPlayerDistance) {
                    minPlayerDistance = distance;
                    targetPlayer = player;
                }
            }
            
            // 只处理最近的一个玩家
            if (targetPlayer != null) {
                // 评估玩家的价值
                PlayerValue value = evaluatePlayer(targetPlayer);

                if (value == PlayerValue.INFECT) {
                    // 感染玩家
                    infectPlayer(targetPlayer, serverLevel);
                } else {
                    // 作为食物
                    consumePlayer(targetPlayer, serverLevel);
                }
                // 设置冷却时间
                villagerConsumeCooldown = VILLAGER_CONSUME_INTERVAL;
            }
        }

        // 清理已死亡的尸兄
        minions.removeIf(id -> serverLevel.getEntity(id) == null || !(serverLevel.getEntity(id) instanceof LowerLevelZbEntity));
        foodReserves.removeIf(id -> serverLevel.getEntity(id) == null || !(serverLevel.getEntity(id) instanceof LowerLevelZbEntity));
    }

    /**
     * 评估村民的价值
     * 大多数村民作为食物（85%），极少数感染成同伴（15%）
     */
    private VillagerValue evaluateVillager(Villager villager) {
        // 随机决定村民的价值，15%概率感染，85%概率作为食物
        // 龙右作为尸王，更倾向于把村民当作食物来恢复自身
        return this.random.nextFloat() < 0.15 ? VillagerValue.INFECT : VillagerValue.FOOD;
    }

    /**
     * 评估玩家的价值
     * 根据玩家的装备、生命值等因素决定是感染还是吃掉
     * 20%概率感染（有潜力的玩家），80%概率作为食物
     */
    private PlayerValue evaluatePlayer(Player player) {
        // 计算玩家的"潜力值"
        int potential = 0;
        
        // 根据装备计算潜力
        for (net.minecraft.world.item.ItemStack item : player.getInventory().armor) {
            if (!item.isEmpty()) {
                potential += 5;
            }
        }
        
        // 根据生命值计算潜力
        float healthPercent = player.getHealth() / player.getMaxHealth();
        potential += (int)(healthPercent * 10);
        
        // 根据经验等级计算潜力
        potential += player.experienceLevel;
        
        // 潜力高的玩家有更高概率被感染（最高40%概率）
        float infectChance = 0.2f + Math.min(0.2f, potential / 100f);
        
        return this.random.nextFloat() < infectChance ? PlayerValue.INFECT : PlayerValue.FOOD;
    }

    /**
     * 感染玩家
     */
    private void infectPlayer(Player player, ServerLevel serverLevel) {
        CorpseOrigin.LOGGER.info("龙右感染玩家 {}", player.getName().getString());
        
        // 对玩家添加感染效果（更长的延迟，给玩家反应时间）
        int duration = 200 + serverLevel.getRandom().nextInt(400); // 10-30秒
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                com.phagens.corpseorigin.register.EffectRegister.QIANS,
                duration,
                0,
                false,
                true,
                true
        ));
        
        // 发送消息给玩家
        player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§c§l你感受到了尸王的感染！快寻找解药！"));
        
        // 播放效果
        serverLevel.broadcastEntityEvent(this, (byte) 7);
    }

    /**
     * 消耗玩家作为食物
     */
    private void consumePlayer(Player player, ServerLevel serverLevel) {
        CorpseOrigin.LOGGER.info("龙右消耗玩家 {} 作为食物", player.getName().getString());
        
        // 对玩家造成大量伤害（但不一定立即死亡，给逃跑机会）
        float damage = player.getMaxHealth() * 0.5f; // 50%生命值伤害
        player.hurt(serverLevel.damageSources().mobAttack(this), damage);
        
        // 恢复龙右生命值
        this.heal(this.getMaxHealth() * 0.3F);
        
        // 播放效果
        serverLevel.broadcastEntityEvent(this, (byte) 35);
        this.playSound(net.minecraft.sounds.SoundEvents.GENERIC_EAT, 2.0F, 0.8F);
        
        // 发送消息给玩家
        player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§4§l尸王吞噬了你的生命力！"));
    }

    /**
     * 感染村民
     */
    private void infectVillager(Villager villager, ServerLevel serverLevel) {
        CorpseOrigin.LOGGER.info("龙右感染村民 {}", villager.getUUID());
        
        // 使用 BYeffect 来感染村民（3-15秒随机延迟后变异）
        BYeffect.applyInfection(villager, serverLevel);
    }

    /**
     * 消耗村民作为食物
     */
    private void consumeVillager(Villager villager, ServerLevel serverLevel) {
        CorpseOrigin.LOGGER.info("龙右消耗村民 {} 作为食物", villager.getUUID());
        
        // 恢复生命值
        this.heal(this.getMaxHealth() * 0.2F);
        
        // 播放效果
        serverLevel.broadcastEntityEvent(this, (byte) 35);
        this.playSound(net.minecraft.sounds.SoundEvents.GENERIC_EAT, 2.0F, 0.8F);
        
        // 移除村民
        villager.remove(net.minecraft.world.entity.Entity.RemovalReason.KILLED);
    }

    /**
     * 评估尸兄的价值
     */
    private ZombieValue evaluateZombie(LowerLevelZbEntity zb) {
        int level = zb.getEvolutionLevel();
        int kills = zb.getKills();
        float healthPercent = zb.getHealth() / zb.getMaxHealth();

        // 计算潜力值
        int potential = level * 10 + kills + (int)(healthPercent * 10);

        // 潜力值高的适合做手下
        if (potential >= 25) {
            return ZombieValue.MINION;
        }

        // 潜力值低的作为粮仓
        return ZombieValue.FOOD_RESERVE;
    }

    /**
     * 判断是否应该攻击某个目标
     * 龙右作为尸王，不会食用同类（尸兄）
     * 在不饿或高兴时不会主动攻击其他生物
     */
    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity entity) {
        // 龙右不会攻击尸兄（同类）
        if (entity instanceof LowerLevelZbEntity) {
            CorpseOrigin.LOGGER.debug("龙右拒绝攻击尸兄（同类）");
            return false;
        }
        
        // 检查状态是否允许攻击
        if (!shouldAttack()) {
            CorpseOrigin.LOGGER.debug("龙右当前状态不允许攻击");
            return false;
        }

        return super.doHurtTarget(entity);
    }
    
    /**
     * 判断龙右是否应该攻击
     * 只有在饥饿度低于阈值，且心情和兴趣值不高于阈值时才会攻击
     * 如果龙右吃饱了且没有被攻击，不会主动出击
     */
    private boolean shouldAttack() {
        boolean isHungry = hunger < HUNGER_THRESHOLD;
        boolean isHappy = mood > MOOD_THRESHOLD;
        boolean isInterested = interest > INTEREST_THRESHOLD;
        boolean wasRecentlyHurt = (this.tickCount - lastHurtTick) < HURT_MEMORY_DURATION;
        
        // 如果被攻击了，可以反击
        if (wasRecentlyHurt) {
            return true;
        }
        
        // 如果没被攻击且吃饱了（饥饿度>=阈值），不会主动出击
        if (!isHungry) {
            return false;
        }
        
        // 饥饿且心情和兴趣不高时才会主动攻击
        return !isHappy && !isInterested;
    }
    
    /**
     * 判断龙右是否应该主动出击（用于AI目标选择）
     */
    public boolean shouldInitiateAttack() {
        return shouldAttack();
    }

    /**
     * 检测物品是否为 Point Blank 枪械
     */
    private boolean isPointBlankGun(ItemStack stack) {
        if (stack.isEmpty()) return false;

        ResourceLocation registryName = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (registryName != null) {
            return "pointblank".equals(registryName.getNamespace());
        }
        return false;
    }

    /**
     * 检查是否可以防御该远程武器
     * - 普通枪械（Point Blank）：可以防御
     * - 原版弓弩：力量附魔<=2级可以防御，>=3级无法防御
     * - 其他远程武器：无法防御
     */
    private boolean canDefendAgainstRanged(ItemStack stack) {
        if (stack.isEmpty()) return false;

        // Point Blank 枪械可以防御
        if (isPointBlankGun(stack)) {
            return true;
        }

        // 原版弓弩检查附魔等级
        if (stack.getItem() instanceof ProjectileWeaponItem) {
            // 获取力量附魔等级 (1.21 API)
            int powerLevel = 0;
            var enchantments = stack.getEnchantments();
            if (enchantments != null) {
                for (var entry : enchantments.entrySet()) {
                    if (entry.getKey().is(net.minecraft.world.item.enchantment.Enchantments.POWER)) {
                        powerLevel = entry.getIntValue();
                        break;
                    }
                }
            }
            // 力量3级以上无法防御
            if (powerLevel >= 3) {
                CorpseOrigin.LOGGER.info("[龙右] 检测到力量{}级弓弩，无法防御！", powerLevel);
                return false;
            }
            // 力量0-2级可以防御
            return true;
        }

        // 其他远程武器无法防御
        return false;
    }

    /**
     * 龙右是否需要进食
     * 作为尸王，他不需要像普通尸兄那样吞噬同类
     */
    public boolean needsToEat() {
        // 龙右作为尸王，饥饿度低于阈值时才会考虑进食
        return hunger < HUNGER_THRESHOLD;
    }

    /**
     * 消耗粮仓（当需要恢复时）
     */
    public void consumeFoodReserve() {
        if (!(this.level() instanceof ServerLevel serverLevel)) return;
        if (foodReserves.isEmpty()) return;

        // 寻找距离最近的粮仓
        LowerLevelZbEntity nearestFood = null;
        double minDistance = Double.MAX_VALUE;
        UUID nearestFoodId = null;
        
        for (UUID foodId : foodReserves) {
            if (serverLevel.getEntity(foodId) instanceof LowerLevelZbEntity food) {
                double distance = this.distanceToSqr(food);
                if (distance < minDistance) {
                    minDistance = distance;
                    nearestFood = food;
                    nearestFoodId = foodId;
                }
            }
        }
        
        // 检查距离是否在吞噬范围内
        if (nearestFood != null && minDistance < CONSUME_RANGE) {
            CorpseOrigin.LOGGER.info("龙右消耗粮仓 {} 恢复生命值", nearestFoodId);

            // 恢复生命值
            this.heal(this.getMaxHealth() * 0.3F);
            
            // 恢复饥饿度
            hunger = 100;

            // 播放效果
            serverLevel.broadcastEntityEvent(this, (byte) 35);
            this.playSound(net.minecraft.sounds.SoundEvents.GENERIC_EAT, 2.0F, 0.8F);

            // 移除粮仓
            nearestFood.discard();
            foodReserves.remove(nearestFoodId);
        } else if (nearestFood != null) {
            CorpseOrigin.LOGGER.info("龙右距离粮仓太远，无法消耗");
        }
    }

    /**
     * 获取手下数量
     */
    public int getMinionCount() {
        return minions.size();
    }

    /**
     * 获取粮仓数量
     */
    public int getFoodReserveCount() {
        return foodReserves.size();
    }

    private void triggerShieyeAnimation() {
        this.entityData.set(DATA_PLAYING_SHIEYE, true);
        this.shieyeCooldown = 150;
        this.shieyeAnimationTicks = 60;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putBoolean("PlayingShieye", this.entityData.get(DATA_PLAYING_SHIEYE));
        compound.putInt("ShieyeCooldown", this.shieyeCooldown);
        
        // 保存技能冷却
        compound.putInt("XuanwuBodyCooldown", this.xuanwuBodyCooldown);
        compound.putInt("GeckoTechniqueCooldown", this.geckoTechniqueCooldown);
        compound.putInt("TianGangQiCooldown", this.tianGangQiCooldown);
        compound.putInt("EarthquakeCooldown", this.earthquakeCooldown);
        compound.putInt("SummonMinionsCooldown", this.summonMinionsCooldown);
        
        // 保存天罡气技能冷却
        compound.putInt("TianGangQiJiCooldown", this.tianGangQiJiCooldown);
        compound.putInt("TianGangQiLiCooldown", this.tianGangQiLiCooldown);
        compound.putInt("TianGangQiHuiCooldown", this.tianGangQiHuiCooldown);
        compound.putInt("TianGangQiMieCooldown", this.tianGangQiMieCooldown);
        compound.putInt("TianGangQiWuCooldown", this.tianGangQiWuCooldown);
        compound.putInt("TianGangQiShenCooldown", this.tianGangQiShenCooldown);
        compound.putInt("NiPoQuanCooldown", this.niPoQuanCooldown);
        compound.putInt("PoGangCooldown", this.poGangCooldown);
        compound.putInt("TianGangPoCooldown", this.tianGangPoCooldown);
        
        // 保存状态系统数据
        compound.putInt("Hunger", this.hunger);
        compound.putInt("Mood", this.mood);
        compound.putInt("Interest", this.interest);
        compound.putInt("LastHurtTick", this.lastHurtTick);
        compound.putInt("VillagerConsumeCooldown", this.villagerConsumeCooldown);

        // 保存手下列表
        CompoundTag minionsTag = new CompoundTag();
        int i = 0;
        for (UUID id : minions) {
            minionsTag.putUUID("Minion" + i++, id);
        }
        compound.put("Minions", minionsTag);
        compound.putInt("MinionCount", minions.size());

        // 保存粮仓列表
        CompoundTag foodTag = new CompoundTag();
        i = 0;
        for (UUID id : foodReserves) {
            foodTag.putUUID("Food" + i++, id);
        }
        compound.put("FoodReserves", foodTag);
        compound.putInt("FoodReserveCount", foodReserves.size());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains("PlayingShieye")) {
            this.entityData.set(DATA_PLAYING_SHIEYE, compound.getBoolean("PlayingShieye"));
        }
        if (compound.contains("ShieyeCooldown")) {
            this.shieyeCooldown = compound.getInt("ShieyeCooldown");
        }
        
        // 读取技能冷却
        if (compound.contains("XuanwuBodyCooldown")) {
            this.xuanwuBodyCooldown = compound.getInt("XuanwuBodyCooldown");
        }
        if (compound.contains("GeckoTechniqueCooldown")) {
            this.geckoTechniqueCooldown = compound.getInt("GeckoTechniqueCooldown");
        }
        if (compound.contains("TianGangQiCooldown")) {
            this.tianGangQiCooldown = compound.getInt("TianGangQiCooldown");
        }
        if (compound.contains("EarthquakeCooldown")) {
            this.earthquakeCooldown = compound.getInt("EarthquakeCooldown");
        }
        if (compound.contains("SummonMinionsCooldown")) {
            this.summonMinionsCooldown = compound.getInt("SummonMinionsCooldown");
        }
        
        // 读取天罡气技能冷却
        if (compound.contains("TianGangQiJiCooldown")) {
            this.tianGangQiJiCooldown = compound.getInt("TianGangQiJiCooldown");
        }
        if (compound.contains("TianGangQiLiCooldown")) {
            this.tianGangQiLiCooldown = compound.getInt("TianGangQiLiCooldown");
        }
        if (compound.contains("TianGangQiHuiCooldown")) {
            this.tianGangQiHuiCooldown = compound.getInt("TianGangQiHuiCooldown");
        }
        if (compound.contains("TianGangQiMieCooldown")) {
            this.tianGangQiMieCooldown = compound.getInt("TianGangQiMieCooldown");
        }
        if (compound.contains("TianGangQiWuCooldown")) {
            this.tianGangQiWuCooldown = compound.getInt("TianGangQiWuCooldown");
        }
        if (compound.contains("TianGangQiShenCooldown")) {
            this.tianGangQiShenCooldown = compound.getInt("TianGangQiShenCooldown");
        }
        if (compound.contains("NiPoQuanCooldown")) {
            this.niPoQuanCooldown = compound.getInt("NiPoQuanCooldown");
        }
        if (compound.contains("PoGangCooldown")) {
            this.poGangCooldown = compound.getInt("PoGangCooldown");
        }
        if (compound.contains("TianGangPoCooldown")) {
            this.tianGangPoCooldown = compound.getInt("TianGangPoCooldown");
        }
        
        // 读取状态系统数据
        if (compound.contains("Hunger")) {
            this.hunger = compound.getInt("Hunger");
        }
        if (compound.contains("Mood")) {
            this.mood = compound.getInt("Mood");
        }
        if (compound.contains("Interest")) {
            this.interest = compound.getInt("Interest");
        }
        if (compound.contains("LastHurtTick")) {
            this.lastHurtTick = compound.getInt("LastHurtTick");
        }
        if (compound.contains("VillagerConsumeCooldown")) {
            this.villagerConsumeCooldown = compound.getInt("VillagerConsumeCooldown");
        }

        // 读取手下列表
        if (compound.contains("Minions")) {
            CompoundTag minionsTag = compound.getCompound("Minions");
            int count = compound.getInt("MinionCount");
            for (int i = 0; i < count; i++) {
                if (minionsTag.hasUUID("Minion" + i)) {
                    minions.add(minionsTag.getUUID("Minion" + i));
                }
            }
        }

        // 读取粮仓列表
        if (compound.contains("FoodReserves")) {
            CompoundTag foodTag = compound.getCompound("FoodReserves");
            int count = compound.getInt("FoodReserveCount");
            for (int i = 0; i < count; i++) {
                if (foodTag.hasUUID("Food" + i)) {
                    foodReserves.add(foodTag.getUUID("Food" + i));
                }
            }
        }
    }

    @Override
    public boolean canBeAffected(net.minecraft.world.effect.MobEffectInstance effect) {
        // 尸王免疫所有毒素和有害效果
        if (effect.getEffect().value() == net.minecraft.world.effect.MobEffects.POISON.value() ||
            effect.getEffect().value() == net.minecraft.world.effect.MobEffects.HUNGER.value() ||
            effect.getEffect().value() == net.minecraft.world.effect.MobEffects.WITHER.value() ||
            effect.getEffect().value().getCategory() == net.minecraft.world.effect.MobEffectCategory.HARMFUL) {
            return false;
        }
        return super.canBeAffected(effect);
    }

    /**
     * 尸兄价值枚举
     */
    private enum ZombieValue {
        MINION,      // 手下 - 有潜力的尸兄
        FOOD_RESERVE // 粮仓 - 作为储备食物的尸兄
    }

    /**
     * 村民价值枚举
     */
    private enum VillagerValue {
        INFECT, // 有利用价值，感染为尸兄
        FOOD    // 作为食物消耗
    }

    /**
     * 玩家价值枚举
     */
    private enum PlayerValue {
        INFECT, // 有潜力，感染为尸兄
        FOOD    // 作为食物消耗
    }

    // ==================== 开门系统（龙右作为尸王更聪明） ====================

    /**
     * 龙右作为尸王，可以打开所有门
     */
    public boolean canOpenDoors() {
        return true;
    }

    /**
     * 龙右作为尸王，可以破坏门
     */
    public boolean canBreakDoors() {
        return true;
    }

    /**
     * 技能触发逻辑
     */
    private void useSkills() {
        // 玄武体 - 当生命值低于50%时使用
        if (this.getHealth() < this.getMaxHealth() * 0.5 && xuanwuBodyCooldown <= 0) {
            LongyouSkills.useXuanwuBody(this);
            xuanwuBodyCooldown = 600; // 30秒冷却
        }
        
        // 壁虎功 - 当生命值低于30%时使用
        if (this.getHealth() < this.getMaxHealth() * 0.3 && geckoTechniqueCooldown <= 0) {
            LongyouSkills.useGeckoTechnique(this);
            geckoTechniqueCooldown = 300; // 15秒冷却
        }
        
        // 天罡气 - 有目标时使用
        if (this.getTarget() != null && tianGangQiCooldown <= 0 && this.random.nextFloat() < 0.1F) {
            LongyouSkills.useTianGangQi(this);
            tianGangQiCooldown = 200; // 10秒冷却
        }
        
        // 地震 - 随机使用
        if (earthquakeCooldown <= 0 && this.random.nextFloat() < 0.05F) {
            LongyouSkills.useEarthquake(this);
            earthquakeCooldown = 400; // 20秒冷却
        }
        
        // 召唤手下 - 当手下数量较少时使用
        if (summonMinionsCooldown <= 0 && this.getMinionCount() < 5 && this.random.nextFloat() < 0.03F) {
            LongyouSkills.useSummonMinions(this);
            summonMinionsCooldown = 600; // 30秒冷却
        }
        
        // 天罡气技能
        useTianGangQiSkills();
    }
    
    /**
     * 天罡气技能触发逻辑
     */
    private void useTianGangQiSkills() {
        // 天罡气二重·疾 - 追击目标时使用
        if (this.getTarget() != null && !this.isWithinMeleeAttackRange(this.getTarget()) && tianGangQiJiCooldown <= 0 && this.random.nextFloat() < 0.15F) {
            com.phagens.corpseorigin.entity.skills.LongyouTianGangQi.useTianGangQiJi(this);
            tianGangQiJiCooldown = 600; // 30秒冷却
        }
        
        // 天罡气三重·力 - 近距离战斗时使用
        if (this.getTarget() != null && this.isWithinMeleeAttackRange(this.getTarget()) && tianGangQiLiCooldown <= 0 && this.random.nextFloat() < 0.1F) {
            com.phagens.corpseorigin.entity.skills.LongyouTianGangQi.useTianGangQiLi(this);
            tianGangQiLiCooldown = 600; // 30秒冷却
        }
        
        // 天罡气六重·毁 - 随机使用，造成大范围破坏
        if (tianGangQiHuiCooldown <= 0 && this.random.nextFloat() < 0.05F) {
            com.phagens.corpseorigin.entity.skills.LongyouTianGangQi.useTianGangQiHui(this);
            tianGangQiHuiCooldown = 800; // 40秒冷却
        }
        
        // 天罡气七重·灭 - 有目标时使用，发出红色冲击波
        if (this.getTarget() != null && tianGangQiMieCooldown <= 0 && this.random.nextFloat() < 0.1F) {
            com.phagens.corpseorigin.entity.skills.LongyouTianGangQi.useTianGangQiMie(this);
            tianGangQiMieCooldown = 300; // 15秒冷却
        }
        
        // 天罡气八重·无 - 随机使用，可破除防御
        if (tianGangQiWuCooldown <= 0 && this.random.nextFloat() < 0.08F) {
            com.phagens.corpseorigin.entity.skills.LongyouTianGangQi.useTianGangQiWu(this);
            tianGangQiWuCooldown = 400; // 20秒冷却
        }
        
        // 天罡气九重·神 - 当生命值低于20%时使用，增强所有属性
        if (this.getHealth() < this.getMaxHealth() * 0.2 && tianGangQiShenCooldown <= 0) {
            com.phagens.corpseorigin.entity.skills.LongyouTianGangQi.useTianGangQiShen(this);
            tianGangQiShenCooldown = 1200; // 60秒冷却
        }
        
        // 逆破拳 - 近距离战斗时使用，连续拳击
        if (this.getTarget() != null && this.isWithinMeleeAttackRange(this.getTarget()) && niPoQuanCooldown <= 0 && this.random.nextFloat() < 0.12F) {
            com.phagens.corpseorigin.entity.skills.LongyouTianGangQi.useNiPoQuan(this);
            niPoQuanCooldown = 200; // 10秒冷却
        }
        
        // 破罡 - 有目标时使用，可击破防御
        if (this.getTarget() != null && poGangCooldown <= 0 && this.random.nextFloat() < 0.08F) {
            com.phagens.corpseorigin.entity.skills.LongyouTianGangQi.usePoGang(this);
            poGangCooldown = 300; // 15秒冷却
        }
        
        // 天罡破 - 随机使用，从空中向下进攻
        if (tianGangPoCooldown <= 0 && this.random.nextFloat() < 0.05F) {
            com.phagens.corpseorigin.entity.skills.LongyouTianGangQi.useTianGangPo(this);
            tianGangPoCooldown = 1000; // 50秒冷却
        }
    }
    
    /**
     * 处理开门逻辑
     * 当龙右路径中包含门时调用
     */
    public void setDoorToOpen(BlockState state, BlockPos pos, boolean open) {
        if (state.getBlock() instanceof DoorBlock doorBlock) {
            // 检查门是否已经是目标状态
            boolean isCurrentlyOpen = state.getValue(DoorBlock.OPEN);
            if (isCurrentlyOpen != open) {
                // 切换门的状态
                this.level().setBlock(pos, state.cycle(DoorBlock.OPEN), 10);
                
                // 播放开门/关门音效
                this.level().levelEvent(null, open ? 1005 : 1011, pos, 0);
                
                CorpseOrigin.LOGGER.debug("龙右{}了门 at {}", open ? "打开" : "关闭", pos);
            }
        }
    }

    /**
     * 在tick中检查并处理路径上的门
     * 龙右会自动打开路径上的门
     */
    private void tickDoorInteraction() {
        // 获取当前路径
        var path = this.getNavigation().getPath();
        if (path == null || path.isDone()) {
            return;
        }

        // 获取下一个路径节点
        var nextNode = path.getNextNode();
        if (nextNode == null) {
            return;
        }

        BlockPos pos = nextNode.asBlockPos();
        BlockState state = this.level().getBlockState(pos);

        // 检查是否是门
        if (state.getBlock() instanceof DoorBlock) {
            // 检查门是否关闭
            if (!state.getValue(DoorBlock.OPEN)) {
                // 计算距离
                double distance = this.distanceToSqr(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
                
                // 距离门2格以内时开门
                if (distance < 4.0D) {
                    setDoorToOpen(state, pos, true);
                }
            }
        }
    }
}
