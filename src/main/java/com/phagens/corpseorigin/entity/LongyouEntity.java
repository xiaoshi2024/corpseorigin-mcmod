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
import net.minecraft.world.InteractionHand;
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
    protected static final RawAnimation DODGE_ANIM = RawAnimation.begin().thenPlay("dodge");
    protected static final RawAnimation SKILL_1_ANIM = RawAnimation.begin().thenPlay("skill_1");
    protected static final RawAnimation SKILL_2_ANIM = RawAnimation.begin().thenPlay("skill_2");
    protected static final RawAnimation SKILL_3_ANIM = RawAnimation.begin().thenPlay("skill_3");

    public static final EntityDataAccessor<Boolean> DATA_PLAYING_SHIEYE = 
            SynchedEntityData.defineId(LongyouEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> DATA_PLAYING_AURA_SKILL = 
            SynchedEntityData.defineId(LongyouEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> DATA_PLAYING_DODGE = 
            SynchedEntityData.defineId(LongyouEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> DATA_PLAYING_SKILL_1 = 
            SynchedEntityData.defineId(LongyouEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> DATA_PLAYING_SKILL_2 = 
            SynchedEntityData.defineId(LongyouEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> DATA_PLAYING_SKILL_3 = 
            SynchedEntityData.defineId(LongyouEntity.class, EntityDataSerializers.BOOLEAN);

    private int shieyeCooldown = 0;
    private int shieyeAnimationTicks = 0;
    private int auraSkillTicks = 0;
    public int dodgeAnimationTicks = 0;
    public int skill1AnimationTicks = 0;
    public int skill2AnimationTicks = 0;
    public int skill3AnimationTicks = 0;
    
    // 技能冷却
    private int xuanwuBodyCooldown = 0;
    private int geckoTechniqueCooldown = 0;
    private int tianGangQiCooldown = 0;
    private int earthquakeCooldown = 0;
    private int summonMinionsCooldown = 0;
    private int nestSummonCooldown = 0; // 尸巢召唤冷却
    
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
    
    // 逆破拳状态（修复Thread.sleep问题）
    private boolean niPoQuanActive = false;
    private int niPoQuanTick = 0;
    private int niPoQuanHits = 0;
    private static final int NI_PO_QUAN_HIT_INTERVAL = 5; // 每5tick攻击一次
    private static final int NI_PO_QUAN_MAX_HITS = 3; // 最多攻击3次
    
    // 属性重置系统（修复属性不恢复问题）
    private int attributeResetTick = 0;
    private double originalScale = 1.0D;
    private double originalSpeed = 0.4D;
    private double originalDamage = 15.0D;
    private double originalArmor = 10.0D;
    private boolean attributesModified = false;
    private static final int ATTRIBUTE_RESET_DELAY = 600; // 30秒后重置属性
    
    // 连招系统
    private enum SkillType {
        MELEE,    // 近战技能
        RANGED,   // 远程技能
        AOE,      // 范围技能
        BUFF      // 增益技能
    }
    
    private static class Skill {
        final String name;
        final SkillType type;
        final int cooldown;
        final Runnable execute;
        
        Skill(String name, SkillType type, int cooldown, Runnable execute) {
            this.name = name;
            this.type = type;
            this.cooldown = cooldown;
            this.execute = execute;
        }
    }
    
    private final List<Skill> availableSkills = new ArrayList<>();
    private int comboTick = 0;
    private int comboChain = 0;
    private static final int COMBO_RESET_DELAY = 20; // 1秒内没有技能释放则重置连招
    private static final int MAX_COMBO_CHAIN = 5; // 最大连招链长度

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
    private static final int HUNGER_THRESHOLD = 20; // 饥饿度低于此值才会攻击（龙右作为尸兄始祖，饥饿阈值更低）
    private static final int MOOD_THRESHOLD = 70; // 心情值高于此值不会攻击
    private static final int INTEREST_THRESHOLD = 60; // 兴趣值高于此值不会攻击
    
    // 饥饿度减少速度（龙右作为尸兄始祖，饿得更慢）
    private static final int HUNGER_DECREASE_INTERVAL = 600; // 每30秒减少1点饥饿度
    
    // 被攻击状态
    private int lastHurtTick = -1000; // 上次被攻击的游戏刻
    private static final int HURT_MEMORY_DURATION = 200; // 被攻击记忆持续时间（10秒）
    
    // 尸兄玩家攻击计数（用于判断是否造反）
    private final java.util.Map<java.util.UUID, Integer> corpsePlayerAttacks = new java.util.HashMap<>();
    private final java.util.Map<java.util.UUID, Integer> corpsePlayerLastAttackTick = new java.util.HashMap<>(); // 记录每个玩家最后攻击的tick
    private static final int CORPSE_PLAYER_ATTACK_THRESHOLD = 3; // 尸兄玩家攻击阈值（超过此值视为造反）
    private static final int ATTACK_RESET_DURATION = 600; // 攻击计数重置时间（30秒）
    
    // 战斗阶段系统
    private enum Phase {
        PHASE_1, // 第一阶段：普通形态
        PHASE_2, // 第二阶段：愤怒形态
        PHASE_3  // 第三阶段：终极形态
    }
    
    private Phase currentPhase = Phase.PHASE_1;
    private boolean phaseTransitioning = false;
    private int phaseTransitionTicks = 0;
    private static final int PHASE_TRANSITION_DURATION = 40; // 阶段转换动画持续时间（2秒）
    
    // 阶段转换阈值
    private static final double PHASE_1_TO_2_THRESHOLD = 0.7; // 70%生命值
    private static final double PHASE_2_TO_3_THRESHOLD = 0.3; // 30%生命值
    
    // 战术性闪避系统
    private boolean dodging = false;
    private int dodgeTicks = 0;
    private static final int DODGE_DURATION = 10; // 闪避持续时间（0.5秒）
    private static final int DODGE_COOLDOWN = 40; // 闪避冷却时间（2秒）
    private int dodgeCooldown = 0;
    private static final double DODGE_CHANCE = 0.3; // 闪避概率
    private static final double DODGE_SPEED = 1.5; // 闪避速度

    public LongyouEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        initializeSkills();
    }
    
    /**
     * 初始化可用技能列表
     */
    private void initializeSkills() {
        availableSkills.add(new Skill("逆破拳", SkillType.MELEE, 200, () -> {
            if (niPoQuanCooldown <= 0) {
                com.phagens.corpseorigin.entity.skills.LongyouTianGangQi.useNiPoQuan(this);
                niPoQuanCooldown = 200;
            }
        }));
        
        availableSkills.add(new Skill("天罡气", SkillType.RANGED, 200, () -> {
            if (tianGangQiCooldown <= 0) {
                LongyouSkills.useTianGangQi(this);
                tianGangQiCooldown = 200;
            }
        }));
        
        availableSkills.add(new Skill("地震", SkillType.AOE, 400, () -> {
            if (earthquakeCooldown <= 0) {
                LongyouSkills.useEarthquake(this);
                earthquakeCooldown = 400;
            }
        }));
        
        availableSkills.add(new Skill("玄武体", SkillType.BUFF, 600, () -> {
            if (xuanwuBodyCooldown <= 0) {
                LongyouSkills.useXuanwuBody(this);
                xuanwuBodyCooldown = 600;
            }
        }));
        
        availableSkills.add(new Skill("天罡气·灭", SkillType.RANGED, 300, () -> {
            if (tianGangQiMieCooldown <= 0) {
                com.phagens.corpseorigin.entity.skills.LongyouTianGangQi.useTianGangQiMie(this);
                tianGangQiMieCooldown = 300;
            }
        }));
        
        availableSkills.add(new Skill("天罡破", SkillType.AOE, 1000, () -> {
            if (tianGangPoCooldown <= 0) {
                com.phagens.corpseorigin.entity.skills.LongyouTianGangQi.useTianGangPo(this);
                tianGangPoCooldown = 1000;
            }
        }));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_PLAYING_SHIEYE, false);
        builder.define(DATA_PLAYING_AURA_SKILL, false);
        builder.define(DATA_PLAYING_DODGE, false);
        builder.define(DATA_PLAYING_SKILL_1, false);
        builder.define(DATA_PLAYING_SKILL_2, false);
        builder.define(DATA_PLAYING_SKILL_3, false);
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

        // 龙右作为尸王，攻击优先级：玩家 → 怪物 → 动物 → 尸兄（造反的）
        // 使用自定义条件判断是否攻击：只有饥饿或被攻击时才会主动出击
        
        // 第一优先级：攻击非尸兄玩家（正常活人）
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, 0, true, false, this::shouldAttackNormalPlayer));
        
        // 第二优先级：攻击非尸兄的其他怪物
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, net.minecraft.world.entity.Mob.class, 0, true, false, this::shouldAttackNonCorpseMob));
        
        // 第三优先级：攻击动物
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Animal.class, 0, true, false, this::shouldAttackAnimal));
        
        // 第四优先级：攻击村民
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Villager.class, 0, true, false, this::shouldAttackVillager));
        
        // 第五优先级：攻击造反的尸兄玩家
        this.targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(this, Player.class, 0, true, false, this::shouldAttackRebelCorpsePlayer));
    }

    /**
     * 判断是否应该攻击非尸兄玩家（正常活人）
     * 这是第一优先级目标
     */
    private boolean shouldAttackNormalPlayer(net.minecraft.world.entity.LivingEntity entity) {
        // 只攻击玩家
        if (!(entity instanceof Player player)) {
            return false;
        }
        
        // 不攻击尸兄玩家（同类）
        if (com.phagens.corpseorigin.player.PlayerCorpseData.isCorpse(player)) {
            return false;
        }
        
        // 只有龙右应该主动出击时才会选择目标
        return shouldInitiateAttack();
    }
    
    /**
     * 判断是否应该攻击非尸兄的其他怪物
     * 这是第二优先级目标
     */
    private boolean shouldAttackNonCorpseMob(net.minecraft.world.entity.LivingEntity entity) {
        // 不攻击尸兄实体（同类）
        if (entity instanceof LowerLevelZbEntity) {
            return false;
        }
        
        // 不攻击尸兄鱼（同类）
        if (entity instanceof ZbrFishEntity) {
            return false;
        }
        
        // 不攻击玩家（玩家由单独的目标处理）
        if (entity instanceof Player) {
            return false;
        }
        
        // 不攻击村民（村民由单独的目标处理）
        if (entity instanceof Villager) {
            return false;
        }
        
        // 不攻击动物（动物由单独的目标处理）
        if (entity instanceof Animal) {
            return false;
        }
        
        // 只有龙右应该主动出击时才会选择目标
        return shouldInitiateAttack();
    }
    
    /**
     * 判断是否应该攻击动物
     * 这是第三优先级目标
     */
    private boolean shouldAttackAnimal(net.minecraft.world.entity.LivingEntity entity) {
        // 只攻击动物
        if (!(entity instanceof Animal)) {
            return false;
        }
        
        // 只有龙右应该主动出击时才会选择目标
        return shouldInitiateAttack();
    }
    
    /**
     * 判断是否应该攻击村民
     * 这是第四优先级目标
     */
    private boolean shouldAttackVillager(net.minecraft.world.entity.LivingEntity entity) {
        // 只攻击村民
        if (!(entity instanceof Villager)) {
            return false;
        }
        
        // 只有龙右应该主动出击时才会选择目标
        return shouldInitiateAttack();
    }
    
    /**
     * 判断是否应该攻击造反的尸兄玩家
     * 这是第五优先级目标
     */
    private boolean shouldAttackRebelCorpsePlayer(net.minecraft.world.entity.LivingEntity entity) {
        // 只攻击玩家
        if (!(entity instanceof Player player)) {
            return false;
        }
        
        // 只攻击已成为尸兄的玩家
        if (!com.phagens.corpseorigin.player.PlayerCorpseData.isCorpse(player)) {
            return false;
        }
        
        // 检查该尸兄玩家是否造反（攻击次数超过阈值）
        java.util.UUID playerId = player.getUUID();
        int attackCount = corpsePlayerAttacks.getOrDefault(playerId, 0);
        return attackCount >= CORPSE_PLAYER_ATTACK_THRESHOLD;
    }

    @Override
    protected net.minecraft.world.entity.ai.navigation.PathNavigation createNavigation(Level level) {
        net.minecraft.world.entity.ai.navigation.GroundPathNavigation navigation = new net.minecraft.world.entity.ai.navigation.GroundPathNavigation(this, level);
        navigation.setCanOpenDoors(true);
        return navigation;
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
        
        if (this.entityData.get(DATA_PLAYING_DODGE)) {
            return event.setAndContinue(DODGE_ANIM);
        }
        
        if (this.entityData.get(DATA_PLAYING_SKILL_1)) {
            return event.setAndContinue(SKILL_1_ANIM);
        }
        
        if (this.entityData.get(DATA_PLAYING_SKILL_2)) {
            return event.setAndContinue(SKILL_2_ANIM);
        }
        
        if (this.entityData.get(DATA_PLAYING_SKILL_3)) {
            return event.setAndContinue(SKILL_3_ANIM);
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

        // 尝试闪避
        if (tryDodge(source)) {
            return false; // 闪避成功，没有受到伤害
        }

        // 仅服务端处理 - 任何攻击都播放动画
        if (this.level() != null && !this.level().isClientSide()) {
            // 播放气场技能动画（任何攻击都要播放）
            this.triggerAuraSkill();

            // 检测尸兄玩家攻击
            Entity attackerEntity = source.getEntity();
            if (attackerEntity instanceof Player player && com.phagens.corpseorigin.player.PlayerCorpseData.isCorpse(player)) {
                // 增加尸兄玩家攻击计数
                java.util.UUID playerId = player.getUUID();
                int attackCount = corpsePlayerAttacks.getOrDefault(playerId, 0) + 1;
                corpsePlayerAttacks.put(playerId, attackCount);
                corpsePlayerLastAttackTick.put(playerId, this.tickCount); // 记录最后攻击时间
                
                // 只在刚好达到攻击阈值时发送一次警告消息（避免重复通知）
                if (attackCount == CORPSE_PLAYER_ATTACK_THRESHOLD) {
                    // 视为造反，发送警告消息
                    player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("dialogue.longyou.rebellion"));
                    // 广播消息给附近玩家
                    if (this.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                        for (Player nearbyPlayer : serverLevel.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(64))) {
                            if (!nearbyPlayer.getUUID().equals(playerId)) {
                                nearbyPlayer.sendSystemMessage(net.minecraft.network.chat.Component.translatable("dialogue.longyou.rebellion.broadcast", player.getName().getString()));
                            }
                        }
                    }
                }
            }

            // 远程攻击特殊处理
            if (isRangedAttack) {
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
    
    /**
     * 尝试闪避攻击
     */
    private boolean tryDodge(DamageSource source) {
        if (dodging || dodgeCooldown > 0 || !this.isAlive()) {
            return false;
        }
        
        // 只有来自生物的攻击才闪避
        if (!(source.getEntity() instanceof LivingEntity)) {
            return false;
        }
        
        // 闪避概率
        if (this.random.nextFloat() < DODGE_CHANCE) {
            startDodge((LivingEntity) source.getEntity());
            return true;
        }
        
        return false;
    }
    
    /**
     * 开始闪避
     */
    private void startDodge(LivingEntity attacker) {
        dodging = true;
        dodgeTicks = DODGE_DURATION;
        dodgeCooldown = DODGE_COOLDOWN;
        
        // 计算闪避方向（远离攻击者）
        Vec3 direction = this.position().subtract(attacker.position()).normalize();
        direction = direction.add(0, 0.2, 0); // 稍微向上
        
        // 应用闪避速度
        this.setDeltaMovement(direction.scale(DODGE_SPEED));
        this.hasImpulse = true;
        
        // 播放闪避动画和音效
        if (!this.level().isClientSide) {
            // 触发闪避动画
            this.entityData.set(DATA_PLAYING_DODGE, true);
            this.dodgeAnimationTicks = 20; // 1秒动画
            
            ServerLevel serverLevel = (ServerLevel) this.level();
            serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(), 
                    net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_SWEEP, 
                    net.minecraft.sounds.SoundSource.HOSTILE, 
                    1.0F, 1.2F);
            
            // 生成闪避粒子
            for (int i = 0; i < 10; i++) {
                double x = this.getX() + (this.random.nextDouble() - 0.5) * 1.0;
                double y = this.getY() + this.getBbHeight() * 0.5 + (this.random.nextDouble() - 0.5) * 1.0;
                double z = this.getZ() + (this.random.nextDouble() - 0.5) * 1.0;
                serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.POOF, x, y, z, 1, 0.1, 0.1, 0.1, 0.1);
            }
        }
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
        
        // 闪避动画计时
        if (!this.level().isClientSide && this.entityData.get(DATA_PLAYING_DODGE)) {
            dodgeAnimationTicks--;
            if (dodgeAnimationTicks <= 0) {
                this.entityData.set(DATA_PLAYING_DODGE, false);
            }
        }
        
        // 技能1动画计时
        if (!this.level().isClientSide && this.entityData.get(DATA_PLAYING_SKILL_1)) {
            skill1AnimationTicks--;
            if (skill1AnimationTicks <= 0) {
                this.entityData.set(DATA_PLAYING_SKILL_1, false);
            }
        }
        
        // 技能2动画计时
        if (!this.level().isClientSide && this.entityData.get(DATA_PLAYING_SKILL_2)) {
            skill2AnimationTicks--;
            if (skill2AnimationTicks <= 0) {
                this.entityData.set(DATA_PLAYING_SKILL_2, false);
            }
        }
        
        // 技能3动画计时
        if (!this.level().isClientSide && this.entityData.get(DATA_PLAYING_SKILL_3)) {
            skill3AnimationTicks--;
            if (skill3AnimationTicks <= 0) {
                this.entityData.set(DATA_PLAYING_SKILL_3, false);
            }
        }
        
        // 服务端：阶段系统处理
        if (!this.level().isClientSide && this.isAlive()) {
            handlePhaseTransition();
        }
        
        // 服务端：连招系统处理
        if (!this.level().isClientSide && this.isAlive()) {
            handleComboSystem();
        }
        
        // 处理闪避状态
        if (dodging) {
            dodgeTicks--;
            if (dodgeTicks <= 0) {
                dodging = false;
            }
        }
        
        // 减少闪避冷却
        if (dodgeCooldown > 0) {
            dodgeCooldown--;
        }
        
        // 服务端：技能触发逻辑
        if (!this.level().isClientSide && this.isAlive() && !phaseTransitioning) {
            useSkills();
        }
        
        // 服务端：逆破拳连续攻击处理
        if (!this.level().isClientSide && niPoQuanActive) {
            tickNiPoQuan();
        }
        
        // 服务端：属性重置处理
        if (!this.level().isClientSide && attributesModified) {
            tickAttributeReset();
        }

        // 服务端：状态系统更新
        if (!this.level().isClientSide) {
            // 每600 tick（30秒）减少1点饥饿度（龙右作为尸兄始祖，饿得更慢）
            if (this.tickCount % HUNGER_DECREASE_INTERVAL == 0 && hunger > 0) {
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
            
            // 检查每个尸兄玩家的攻击计数是否超时重置
            // 只有当玩家攻击后超过30秒没有继续攻击，才重置该玩家的计数
            java.util.Iterator<java.util.Map.Entry<java.util.UUID, Integer>> iterator = corpsePlayerLastAttackTick.entrySet().iterator();
            while (iterator.hasNext()) {
                java.util.Map.Entry<java.util.UUID, Integer> entry = iterator.next();
                java.util.UUID playerId = entry.getKey();
                int lastAttackTick = entry.getValue();
                
                // 如果超过30秒没有继续攻击，重置该玩家的攻击计数
                if (this.tickCount - lastAttackTick > ATTACK_RESET_DURATION) {
                    corpsePlayerAttacks.remove(playerId);
                    iterator.remove();
                    CorpseOrigin.LOGGER.debug("龙右重置玩家 {} 的攻击计数（超时未继续攻击）", playerId);
                }
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
     * 处理阶段转换
     */
    private void handlePhaseTransition() {
        if (phaseTransitioning) {
            phaseTransitionTicks++;
            if (phaseTransitionTicks >= PHASE_TRANSITION_DURATION) {
                phaseTransitioning = false;
                phaseTransitionTicks = 0;
                applyPhaseEffects(currentPhase);
            }
            return;
        }
        
        double healthPercent = this.getHealth() / this.getMaxHealth();
        Phase newPhase = currentPhase;
        
        if (healthPercent <= PHASE_2_TO_3_THRESHOLD && currentPhase != Phase.PHASE_3) {
            newPhase = Phase.PHASE_3;
        } else if (healthPercent <= PHASE_1_TO_2_THRESHOLD && currentPhase != Phase.PHASE_2) {
            newPhase = Phase.PHASE_2;
        }
        
        if (newPhase != currentPhase) {
            startPhaseTransition(newPhase);
        }
    }
    
    /**
     * 开始阶段转换
     */
    private void startPhaseTransition(Phase newPhase) {
        currentPhase = newPhase;
        phaseTransitioning = true;
        phaseTransitionTicks = 0;
        
        // 播放阶段转换动画
        triggerAuraSkill();
        
        // 发送消息给附近的玩家
        if (this.level() instanceof ServerLevel serverLevel) {
            String phaseMessage = switch (newPhase) {
                case PHASE_2 -> "§c§l龙右进入愤怒形态！";
                case PHASE_3 -> "§4§l龙右进入终极形态！";
                default -> "§e§l龙右进入战斗形态！";
            };
            
            for (Player player : serverLevel.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(64))) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(phaseMessage));
            }
        }
    }
    
    /**
     * 应用阶段效果
     */
    private void applyPhaseEffects(Phase phase) {
        switch (phase) {
            case PHASE_2 -> {
                // 愤怒形态：增加攻击速度和伤害
                this.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED).setBaseValue(0.5D);
                this.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).setBaseValue(20.0D);
                this.markAttributesModified();
            }
            case PHASE_3 -> {
                // 终极形态：大幅增加所有属性
                this.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED).setBaseValue(0.6D);
                this.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).setBaseValue(25.0D);
                this.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR).setBaseValue(15.0D);
                this.markAttributesModified();
                
                // 添加强度效果
                this.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.DAMAGE_BOOST,
                        1200, // 60秒
                        2    // 等级3
                ));
            }
            default -> {
                // 普通形态：恢复默认属性
                this.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED).setBaseValue(0.4D);
                this.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).setBaseValue(15.0D);
                this.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR).setBaseValue(10.0D);
                this.markAttributesModified();
            }
        }
    }
    
    /**
     * 处理连招系统
     */
    private void handleComboSystem() {
        comboTick++;
        
        // 如果超过重置延迟，重置连招
        if (comboTick > COMBO_RESET_DELAY) {
            comboChain = 0;
            comboTick = 0;
        }
        
        // 当有目标且不在阶段转换时，尝试执行连招
        if (this.getTarget() != null && !phaseTransitioning && this.random.nextFloat() < 0.1F) {
            executeCombo();
        }
    }
    
    /**
     * 执行连招
     */
    private void executeCombo() {
        // 根据当前阶段和连招链长度选择技能
        List<Skill> eligibleSkills = getEligibleSkills();
        if (eligibleSkills.isEmpty()) return;
        
        // 选择技能
        Skill selectedSkill = selectSkill(eligibleSkills);
        if (selectedSkill != null) {
            // 执行技能
            selectedSkill.execute.run();
            
            // 增加连招链长度
            comboChain = Math.min(comboChain + 1, MAX_COMBO_CHAIN);
            comboTick = 0;
            
            // 连招链长度超过3时，增加伤害加成
            if (comboChain >= 3) {
                applyComboBonus();
            }
        }
    }
    
    /**
     * 获取当前可使用的技能
     */
    private List<Skill> getEligibleSkills() {
        List<Skill> eligible = new ArrayList<>();
        for (Skill skill : availableSkills) {
            // 检查技能冷却
            boolean isReady = switch (skill.name) {
                case "逆破拳" -> niPoQuanCooldown <= 0;
                case "天罡气" -> tianGangQiCooldown <= 0;
                case "地震" -> earthquakeCooldown <= 0;
                case "玄武体" -> xuanwuBodyCooldown <= 0;
                case "天罡气·灭" -> tianGangQiMieCooldown <= 0;
                case "天罡破" -> tianGangPoCooldown <= 0;
                default -> false;
            };
            
            if (isReady) {
                eligible.add(skill);
            }
        }
        return eligible;
    }
    
    /**
     * 选择技能
     */
    private Skill selectSkill(List<Skill> eligibleSkills) {
        if (eligibleSkills.isEmpty()) return null;
        
        // 根据当前情况选择合适的技能
        LivingEntity target = this.getTarget();
        double distance = this.distanceToSqr(target);
        
        // 近战范围内优先选择近战技能
        if (distance < 4.0D) {
            List<Skill> meleeSkills = eligibleSkills.stream()
                    .filter(skill -> skill.type == SkillType.MELEE)
                    .toList();
            if (!meleeSkills.isEmpty()) {
                return meleeSkills.get(this.random.nextInt(meleeSkills.size()));
            }
        }
        
        // 生命值低时优先选择增益技能
        if (this.getHealth() < this.getMaxHealth() * 0.5) {
            List<Skill> buffSkills = eligibleSkills.stream()
                    .filter(skill -> skill.type == SkillType.BUFF)
                    .toList();
            if (!buffSkills.isEmpty()) {
                return buffSkills.get(0);
            }
        }
        
        // 随机选择
        return eligibleSkills.get(this.random.nextInt(eligibleSkills.size()));
    }
    
    /**
     * 应用连招加成
     */
    private void applyComboBonus() {
        // 增加伤害加成
        this.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.DAMAGE_BOOST,
                60, // 3秒
                comboChain - 3 // 加成等级
        ));
    }

    /**
     * 龙右的智能判断系统
     * 评估周围的尸兄，决定哪些是手下，哪些是粮仓
     * 评估周围的村民，决定哪些作为食物，哪些有利用价值感染
     * 体现龙右的高傲性格，对弱者不屑动手
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
                
                // 龙右对弱者的态度：只有在饥饿时才会对村民下手
                if (shouldAttackVillager()) {
                    if (value == VillagerValue.INFECT) {
                        // 感染村民
                        infectVillager(targetVillager, serverLevel);
                    } else {
                        // 作为食物
                        consumeVillager(targetVillager, serverLevel);
                    }
                    // 设置冷却时间
                    villagerConsumeCooldown = VILLAGER_CONSUME_INTERVAL;
                } else {
                    CorpseOrigin.LOGGER.info("龙右对村民不屑动手，认为他们太弱了");
                }
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
                // 评估玩家的实力
                float playerThreat = evaluatePlayerThreat(targetPlayer);
                
                // 龙右对玩家的态度：根据玩家实力和自身状态决定
                if (shouldAttackPlayer(playerThreat)) {
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
                } else if (playerThreat < 0.3) {
                    CorpseOrigin.LOGGER.info("龙右对玩家不屑动手，认为他们太弱了");
                } else if (mood > MOOD_THRESHOLD) {
                    CorpseOrigin.LOGGER.info("龙右心情不错，暂时不想动手");
                }
            }
        }

        // 清理已死亡的尸兄
        minions.removeIf(id -> serverLevel.getEntity(id) == null || !(serverLevel.getEntity(id) instanceof LowerLevelZbEntity));
        foodReserves.removeIf(id -> serverLevel.getEntity(id) == null || !(serverLevel.getEntity(id) instanceof LowerLevelZbEntity));
    }
    
    /**
     * 评估玩家的威胁程度
     * 0.0-1.0，越高表示威胁越大
     */
    private float evaluatePlayerThreat(Player player) {
        float threat = 0.0F;
        
        // 根据玩家生命值
        float healthPercent = player.getHealth() / player.getMaxHealth();
        threat += healthPercent * 0.3;
        
        // 根据玩家装备
        for (ItemStack item : player.getArmorSlots()) {
            if (!item.isEmpty()) {
                // 检查装备的品质
                if (item.isEnchanted()) {
                    threat += 0.15;
                } else {
                    threat += 0.05;
                }
            }
        }
        
        // 根据玩家手持武器
        ItemStack mainHand = player.getMainHandItem();
        if (!mainHand.isEmpty()) {
            if (mainHand.isEnchanted()) {
                threat += 0.2;
            } else {
                threat += 0.1;
            }
        }
        
        // 根据玩家经验等级
        threat += Math.min(player.experienceLevel / 50.0F, 0.2);
        
        // 限制威胁等级在0-1之间
        return Math.min(threat, 1.0F);
    }
    
    /**
     * 判断龙右是否应该攻击村民
     * 龙右作为尸兄始祖，非常有人格，不会随便食用村民
     * 只有在特殊时刻才会：1. 被攻击时反击 2. 极度饥饿时 3. 心情极差时
     */
    private boolean shouldAttackVillager() {
        boolean isHungry = hunger < HUNGER_THRESHOLD;
        boolean wasRecentlyHurt = (this.tickCount - lastHurtTick) < HURT_MEMORY_DURATION;
        boolean isExtremelyHungry = hunger < HUNGER_THRESHOLD / 2; // 极度饥饿
        boolean isBadMood = mood < 20; // 心情极差
        
        // 被攻击时可以反击
        if (wasRecentlyHurt) {
            return true;
        }
        
        // 只有在极度饥饿或心情极差时才会主动攻击村民
        // 体现龙右的人格和高傲，不会随便对弱者下手
        return isExtremelyHungry || isBadMood;
    }
    
    /**
     * 判断龙右是否应该攻击玩家
     * 考虑玩家威胁程度和自身状态
     */
    private boolean shouldAttackPlayer(float playerThreat) {
        boolean isHungry = hunger < HUNGER_THRESHOLD;
        boolean isHappy = mood > MOOD_THRESHOLD;
        boolean wasRecentlyHurt = (this.tickCount - lastHurtTick) < HURT_MEMORY_DURATION;
        
        // 被攻击时可以反击
        if (wasRecentlyHurt) {
            return true;
        }
        
        // 心情好时不会主动攻击
        if (isHappy) {
            return false;
        }
        
        // 对实力强的玩家，即使不饿也会感兴趣
        if (playerThreat > 0.7) {
            return true;
        }
        
        // 对实力中等的玩家，只有在饥饿时才会攻击
        if (playerThreat > 0.3) {
            return isHungry;
        }
        
        // 对实力弱的玩家，不屑动手
        return false;
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
     * 龙右作为尸王，不会食用同类（尸兄），但会攻击造反的尸兄玩家
     * 在不饿或高兴时不会主动攻击其他生物
     */
    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity entity) {
        // 龙右不会攻击尸兄（同类），但会攻击造反的尸兄玩家
        if (entity instanceof LowerLevelZbEntity) {
            CorpseOrigin.LOGGER.debug("龙右拒绝攻击尸兄（同类）");
            return false;
        }
        
        // 检查是否为造反的尸兄玩家
        if (entity instanceof Player player && com.phagens.corpseorigin.player.PlayerCorpseData.isCorpse(player)) {
            java.util.UUID playerId = player.getUUID();
            int attackCount = corpsePlayerAttacks.getOrDefault(playerId, 0);
            if (attackCount < CORPSE_PLAYER_ATTACK_THRESHOLD) {
                CorpseOrigin.LOGGER.debug("龙右拒绝攻击普通尸兄玩家（同类）");
                return false;
            }
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
        compound.putInt("NestSummonCooldown", this.nestSummonCooldown);
        
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
        if (compound.contains("NestSummonCooldown")) {
            this.nestSummonCooldown = compound.getInt("NestSummonCooldown");
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
        if (this.getTarget() == null) return;
        
        // 评估目标实力
        float targetThreat = evaluateTargetThreat(this.getTarget());
        
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
        
        // 天罡气 - 根据目标威胁等级使用
        if (tianGangQiCooldown <= 0 && targetThreat > 0.3 && this.random.nextFloat() < 0.05F) {
            LongyouSkills.useTianGangQi(this);
            tianGangQiCooldown = 200; // 10秒冷却
        }
        
        // 地震 - 根据目标威胁等级使用
        if (earthquakeCooldown <= 0 && targetThreat > 0.5 && this.random.nextFloat() < 0.03F) {
            LongyouSkills.useEarthquake(this);
            earthquakeCooldown = 400; // 20秒冷却
        }
        
        // 召唤手下 - 当手下数量较少且目标威胁较高时使用
        if (summonMinionsCooldown <= 0 && this.getMinionCount() < 5 && targetThreat > 0.4 && this.random.nextFloat() < 0.02F) {
            LongyouSkills.useSummonMinions(this);
            summonMinionsCooldown = 600; // 30秒冷却
        }
        
        // 尸巢召唤 - 当生命值较低且手下数量较多时使用
        if (nestSummonCooldown <= 0 && this.getHealth() < this.getMaxHealth() * 0.3 && this.getMinionCount() >= 3 && this.random.nextFloat() < 0.01F) {
            // 执行尸巢召唤技能
            com.phagens.corpseorigin.GongFU.JSskill.JSSkillEngine.getInstance().executeSkillForEntity("尸巢召唤", this, null);
            nestSummonCooldown = 1200; // 60秒冷却
        }
        
        // 天罡气技能
        useTianGangQiSkills(targetThreat);
    }
    
    /**
     * 评估目标威胁等级
     */
    private float evaluateTargetThreat(LivingEntity target) {
        float threat = 0.0F;
        
        // 根据目标生命值
        float healthPercent = target.getHealth() / target.getMaxHealth();
        threat += healthPercent * 0.3;
        
        // 根据目标装备
        for (ItemStack item : target.getArmorSlots()) {
            if (!item.isEmpty()) {
                threat += 0.1;
            }
        }
        
        // 根据目标手持武器
        ItemStack mainHand = target.getMainHandItem();
        if (!mainHand.isEmpty()) {
            threat += 0.2;
        }
        
        // 根据目标是否为玩家
        if (target instanceof Player) {
            threat += 0.2;
        }
        
        // 限制威胁等级在0-1之间
        return Math.min(threat, 1.0F);
    }
    
    /**
     * 天罡气技能触发逻辑
     */
    private void useTianGangQiSkills(float targetThreat) {
        if (this.getTarget() == null) return;
        
        // 天罡气二重·疾 - 追击目标时使用
        if (!this.isWithinMeleeAttackRange(this.getTarget()) && tianGangQiJiCooldown <= 0 && this.random.nextFloat() < 0.1F) {
            com.phagens.corpseorigin.entity.skills.LongyouTianGangQi.useTianGangQiJi(this);
            tianGangQiJiCooldown = 600; // 30秒冷却
        }
        
        // 天罡气三重·力 - 近距离战斗时使用
        if (this.isWithinMeleeAttackRange(this.getTarget()) && tianGangQiLiCooldown <= 0 && this.random.nextFloat() < 0.08F) {
            com.phagens.corpseorigin.entity.skills.LongyouTianGangQi.useTianGangQiLi(this);
            tianGangQiLiCooldown = 600; // 30秒冷却
        }
        
        // 天罡气六重·毁 - 根据目标威胁等级使用，造成大范围破坏
        if (tianGangQiHuiCooldown <= 0 && targetThreat > 0.6 && this.random.nextFloat() < 0.03F) {
            com.phagens.corpseorigin.entity.skills.LongyouTianGangQi.useTianGangQiHui(this);
            tianGangQiHuiCooldown = 800; // 40秒冷却
        }
        
        // 天罡气七重·灭 - 根据目标威胁等级使用，发出红色冲击波
        if (tianGangQiMieCooldown <= 0 && targetThreat > 0.4 && this.random.nextFloat() < 0.05F) {
            com.phagens.corpseorigin.entity.skills.LongyouTianGangQi.useTianGangQiMie(this);
            tianGangQiMieCooldown = 300; // 15秒冷却
        }
        
        // 天罡气八重·无 - 根据目标威胁等级使用，可破除防御
        if (tianGangQiWuCooldown <= 0 && targetThreat > 0.5 && this.random.nextFloat() < 0.05F) {
            com.phagens.corpseorigin.entity.skills.LongyouTianGangQi.useTianGangQiWu(this);
            tianGangQiWuCooldown = 400; // 20秒冷却
        }
        
        // 天罡气九重·神 - 当生命值低于20%时使用，增强所有属性
        if (this.getHealth() < this.getMaxHealth() * 0.2 && tianGangQiShenCooldown <= 0) {
            com.phagens.corpseorigin.entity.skills.LongyouTianGangQi.useTianGangQiShen(this);
            tianGangQiShenCooldown = 1200; // 60秒冷却
        }
        
        // 逆破拳 - 近距离战斗时使用，连续拳击
        if (this.isWithinMeleeAttackRange(this.getTarget()) && niPoQuanCooldown <= 0 && this.random.nextFloat() < 0.08F) {
            com.phagens.corpseorigin.entity.skills.LongyouTianGangQi.useNiPoQuan(this);
            niPoQuanCooldown = 200; // 10秒冷却
        }
        
        // 破罡 - 根据目标威胁等级使用，可击破防御
        if (poGangCooldown <= 0 && targetThreat > 0.3 && this.random.nextFloat() < 0.05F) {
            com.phagens.corpseorigin.entity.skills.LongyouTianGangQi.usePoGang(this);
            poGangCooldown = 300; // 15秒冷却
        }
        
        // 天罡破 - 根据目标威胁等级使用，从空中向下进攻
        if (tianGangPoCooldown <= 0 && targetThreat > 0.7 && this.random.nextFloat() < 0.02F) {
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
    
    /**
     * 启动逆破拳技能
     * 由 LongyouTianGangQi.useNiPoQuan 调用
     */
    public void startNiPoQuan() {
        this.niPoQuanActive = true;
        this.niPoQuanTick = 0;
        this.niPoQuanHits = 0;
    }
    
    /**
     * 逆破拳连续攻击处理（每tick调用）
     * 替代原来的Thread.sleep阻塞方式
     */
    private void tickNiPoQuan() {
        niPoQuanTick++;
        
        // 每隔NI_PO_QUAN_HIT_INTERVAL tick执行一次攻击
        if (niPoQuanTick % NI_PO_QUAN_HIT_INTERVAL == 0 && niPoQuanHits < NI_PO_QUAN_MAX_HITS) {
            LivingEntity target = this.getTarget();
            if (target != null && target.isAlive() && this.isWithinMeleeAttackRange(target)) {
                ServerLevel serverLevel = (ServerLevel) this.level();
                
                // 生成拳击粒子
                for (int j = 0; j < 10; j++) {
                    double x = target.getX() + (this.random.nextDouble() - 0.5) * 1.0;
                    double y = target.getY() + target.getBbHeight() * 0.5 + (this.random.nextDouble() - 0.5) * 1.0;
                    double z = target.getZ() + (this.random.nextDouble() - 0.5) * 1.0;
                    serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.POOF, x, y, z, 1, 0.1, 0.1, 0.1, 0.1);
                }
                
                // 造成伤害
                target.hurt(serverLevel.damageSources().mobAttack(this), 10.0F);
                
                // 播放攻击音效
                serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(), 
                        net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_STRONG, 
                        net.minecraft.sounds.SoundSource.HOSTILE, 
                        1.0F, 0.8F + this.random.nextFloat() * 0.2F);
                
                niPoQuanHits++;
            }
        }
        
        // 攻击完成或目标丢失，结束技能
        if (niPoQuanHits >= NI_PO_QUAN_MAX_HITS || this.getTarget() == null || !this.getTarget().isAlive()) {
            niPoQuanActive = false;
            niPoQuanTick = 0;
            niPoQuanHits = 0;
        }
    }
    
    /**
     * 标记属性已被修改，启动重置计时器
     */
    public void markAttributesModified() {
        if (!attributesModified) {
            // 保存原始属性值
            originalScale = this.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.SCALE).getBaseValue();
            originalSpeed = this.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED).getBaseValue();
            originalDamage = this.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).getBaseValue();
            originalArmor = this.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR).getBaseValue();
        }
        attributesModified = true;
        attributeResetTick = ATTRIBUTE_RESET_DELAY;
    }
    
    /**
     * 属性重置处理（每tick调用）
     */
    private void tickAttributeReset() {
        attributeResetTick--;
        
        if (attributeResetTick <= 0) {
            resetAttributes();
        }
    }
    
    /**
     * 重置所有属性到原始值
     */
    public void resetAttributes() {
        this.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.SCALE).setBaseValue(originalScale);
        this.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED).setBaseValue(originalSpeed);
        this.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).setBaseValue(originalDamage);
        this.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR).setBaseValue(originalArmor);
        attributesModified = false;
        attributeResetTick = 0;
        
        CorpseOrigin.LOGGER.debug("龙右属性已重置: scale={}, speed={}, damage={}, armor={}", 
                originalScale, originalSpeed, originalDamage, originalArmor);
    }
    
    // ==================== 右键交互系统 ====================

    /**
     * 处理实体交互
     */
    public net.minecraft.world.InteractionResult mobInteract(Player player, InteractionHand hand) {
        // 只处理主手右键
        if (hand != InteractionHand.MAIN_HAND) {
            return net.minecraft.world.InteractionResult.PASS;
        }

        // 只在服务端处理逻辑
        if (player.level().isClientSide) {
            return net.minecraft.world.InteractionResult.PASS;
        }

        // 检查玩家是否为尸兄
        if (com.phagens.corpseorigin.player.PlayerCorpseData.isCorpse(player)) {
            // 发送打开对话框的数据包
            openDialogueGui(player);
            return net.minecraft.world.InteractionResult.SUCCESS;
        } else {
            // 非尸兄玩家，显示警告信息
            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("dialogue.longyou.only_corpse"));
            return net.minecraft.world.InteractionResult.PASS;
        }
    }
    
    /**
     * 打开对话框GUI
     */
    private void openDialogueGui(Player player) {
        // 发送对话选项数据包到客户端
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            String question = net.minecraft.network.chat.Component.translatable("dialogue.longyou.greeting").getString();
            java.util.List<String> options = java.util.Arrays.asList(
                net.minecraft.network.chat.Component.translatable("dialogue.longyou.option.command").getString(),
                net.minecraft.network.chat.Component.translatable("dialogue.longyou.option.task").getString(),
                net.minecraft.network.chat.Component.translatable("dialogue.longyou.option.receive_mission").getString(),
                net.minecraft.network.chat.Component.translatable("dialogue.longyou.option.submit_mission").getString(),
                net.minecraft.network.chat.Component.translatable("dialogue.longyou.option.appointment").getString(),
                net.minecraft.network.chat.Component.translatable("dialogue.longyou.option.about").getString()
            );
            
            com.phagens.corpseorigin.network.LongyouDialogueOptionsPacket packet = 
                new com.phagens.corpseorigin.network.LongyouDialogueOptionsPacket(question, options);
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(serverPlayer, packet);
        }
    }
    
    /**
     * 处理对话选项选择
     */
    public void handleDialogueOption(Player player, String option) {
        String commandKey = net.minecraft.network.chat.Component.translatable("dialogue.longyou.option.command").getString();
        String taskKey = net.minecraft.network.chat.Component.translatable("dialogue.longyou.option.task").getString();
        String receiveMissionKey = net.minecraft.network.chat.Component.translatable("dialogue.longyou.option.receive_mission").getString();
        String submitMissionKey = net.minecraft.network.chat.Component.translatable("dialogue.longyou.option.submit_mission").getString();
        String appointmentKey = net.minecraft.network.chat.Component.translatable("dialogue.longyou.option.appointment").getString();
        String aboutKey = net.minecraft.network.chat.Component.translatable("dialogue.longyou.option.about").getString();
        
        if (option.equals(commandKey)) {
            handleWhatDoYouCommand(player);
        } else if (option.equals(taskKey)) {
            handleTaskUpgradeSystem(player);
        } else if (option.equals(receiveMissionKey)) {
            handleReceiveMission(player);
        } else if (option.equals(submitMissionKey)) {
            handleSubmitMission(player);
        } else if (option.equals(appointmentKey)) {
            handleCorpseKingAppointment(player);
        } else if (option.equals(aboutKey)) {
            handleAboutCorpseClan(player);
        } else {
            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("dialogue.longyou.invalid_option"));
        }
    }
    
    /**
     * 处理"大人您有何吩咐？"选项
     */
    private void handleWhatDoYouCommand(Player player) {
        player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("dialogue.longyou.command.line1"));
        player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("dialogue.longyou.command.line2"));
        player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("dialogue.longyou.command.line3"));
    }
    
    /**
     * 处理"任务升级系统"选项
     */
    private void handleTaskUpgradeSystem(Player player) {
        player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("dialogue.longyou.task.line1"));
        player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("dialogue.longyou.task.line2"));
        player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("dialogue.longyou.task.line3"));
    }
    
    /**
     * 处理"领取任务"选项
     */
    private void handleReceiveMission(Player player) {
        if (!com.phagens.corpseorigin.player.PlayerCorpseData.isCorpse(player)) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("dialogue.longyou.only_corpse"));
            return;
        }
        
        if (hasMissionScroll(player)) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("dialogue.longyou.mission.already_has"));
            return;
        }
        
        java.util.Random random = new java.util.Random();
        int missionType = random.nextInt(5);
        
        net.minecraft.world.item.ItemStack missionScroll;
        String missionName;
        
        switch (missionType) {
            case 0 -> {
                int targetCount = 3 + random.nextInt(5);
                missionScroll = com.phagens.corpseorigin.Item.MissionScrollItem.createMissionScroll(
                    "kill_villager", targetCount, targetCount * 2, 1
                );
                missionName = net.minecraft.network.chat.Component.translatable("mission.corpseorigin.kill_villager").getString();
                player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("dialogue.longyou.mission.kill_villager", targetCount));
            }
            case 1 -> {
                int targetCount = 5 + random.nextInt(10);
                missionScroll = com.phagens.corpseorigin.Item.MissionScrollItem.createMissionScroll(
                    "kill_zombie", targetCount, targetCount, 1
                );
                missionName = net.minecraft.network.chat.Component.translatable("mission.corpseorigin.kill_zombie").getString();
                player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("dialogue.longyou.mission.kill_zombie", targetCount));
            }
            case 2 -> {
                int targetCount = 5 + random.nextInt(8);
                missionScroll = com.phagens.corpseorigin.Item.MissionScrollItem.createMissionScroll(
                    "kill_any", targetCount, targetCount, 1
                );
                missionName = net.minecraft.network.chat.Component.translatable("mission.corpseorigin.kill_any").getString();
                player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("dialogue.longyou.mission.kill_any", targetCount));
            }
            case 3 -> {
                int targetCount = 1 + random.nextInt(2);
                missionScroll = com.phagens.corpseorigin.Item.MissionScrollItem.createMissionScroll(
                    "infect_player", targetCount, targetCount * 5, 2
                );
                missionName = net.minecraft.network.chat.Component.translatable("mission.corpseorigin.infect_player").getString();
                player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("dialogue.longyou.mission.infect_player", targetCount));
            }
            default -> {
                int targetCount = 3 + random.nextInt(5);
                missionScroll = com.phagens.corpseorigin.Item.MissionScrollItem.createMissionScroll(
                    "collect_item", targetCount, targetCount * 3, 1
                );
                missionName = net.minecraft.network.chat.Component.translatable("mission.corpseorigin.collect_item").getString();
                player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("dialogue.longyou.mission.collect_item", targetCount));
            }
        }
        
        if (!player.getInventory().add(missionScroll)) {
            player.drop(missionScroll, false);
        }
        
        player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("dialogue.longyou.mission.received", missionName));
    }
    
    /**
     * 处理"提交任务"选项
     */
    private void handleSubmitMission(Player player) {
        if (!com.phagens.corpseorigin.player.PlayerCorpseData.isCorpse(player)) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("dialogue.longyou.only_corpse"));
            return;
        }
        
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            net.minecraft.world.item.ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() instanceof com.phagens.corpseorigin.Item.MissionScrollItem) {
                if (com.phagens.corpseorigin.Item.MissionScrollItem.isCompleted(stack)) {
                    int rewardPoints = com.phagens.corpseorigin.Item.MissionScrollItem.getRewardEvolutionPoints(stack);
                    int rewardLevels = com.phagens.corpseorigin.Item.MissionScrollItem.getRewardLevels(stack);
                    
                    stack.shrink(1);
                    
                    com.phagens.corpseorigin.skill.ISkillHandler handler = com.phagens.corpseorigin.skill.SkillAttachment.getSkillHandler(player);
                    if (handler != null) {
                        handler.addEvolutionPoints(rewardPoints);
                    }
                    
                    int currentLevel = com.phagens.corpseorigin.player.PlayerCorpseData.getEvolutionLevel(player);
                    int newLevel = Math.min(5, currentLevel + rewardLevels);
                    com.phagens.corpseorigin.player.PlayerCorpseData.setEvolutionLevel(player, newLevel);
                    
                    player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("dialogue.longyou.mission.completed"));
                    player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("dialogue.longyou.mission.reward_points", rewardPoints));
                    player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("dialogue.longyou.mission.reward_level", newLevel));
                    
                    return;
                }
            }
        }
        
        player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("dialogue.longyou.mission.no_completed"));
    }
    
    /**
     * 检查玩家是否已有任务纸条
     */
    private boolean hasMissionScroll(Player player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            net.minecraft.world.item.ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() instanceof com.phagens.corpseorigin.Item.MissionScrollItem) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 处理"尸王钦点"选项
     */
    private void handleCorpseKingAppointment(Player player) {
        player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("dialogue.longyou.appointment.line1"));
        player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("dialogue.longyou.appointment.line2"));
        player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("dialogue.longyou.appointment.line3"));
        
        // 可以在这里添加实际的能力赐予逻辑
    }
    
    /**
     * 处理"关于尸族"选项
     */
    private void handleAboutCorpseClan(Player player) {
        player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("dialogue.longyou.about.line1"));
        player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("dialogue.longyou.about.line2"));
        player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("dialogue.longyou.about.line3"));
    }
} 
