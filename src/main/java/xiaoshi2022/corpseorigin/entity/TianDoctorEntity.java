package xiaoshi2022.corpseorigin.entity;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import xiaoshi2022.corpseorigin.registry.ModEntities;

/** 天·博士：可交易、可雇佣的黑色火线特派 NPC。
 * <p>
 * 设定为<b>天级</b>强者（境界表等级 9），身板对齐 {@link xiaoshi2022.corpseorigin.growth.RealmRules}：
 * 生命 {@code health(9)} = 4000；受击时按天级境界减伤 45%
 * （{@code protection(9,0)} 的折算）+ 护甲 10 / 韧性 10 / 击退免疫，
 * 低境界玩家无法用木棍/石剑之类把他"乱棍打死"。
 */
public class TianDoctorEntity extends PathfinderMob implements GeoEntity, RealmRated {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private java.util.UUID employer;
    private static final net.minecraft.network.syncher.EntityDataAccessor<Boolean> HIRED =
            net.minecraft.network.syncher.SynchedEntityData.defineId(TianDoctorEntity.class,
                    net.minecraft.network.syncher.EntityDataSerializers.BOOLEAN);
    private static final String EMPLOYER_TAG = "CorpseOriginEmployer";
    private int weaponCooldown;
    private int robotCooldown;
    /** 天级 = 境界表等级 9（人1-4=1-4、地1-4=5-8、天=9） */
    private static final int SKY_TIER_LEVEL = 9;
    /** 天级境界减伤：{@code protection(9,0)} = 45%（护体罡气） */
    private static final float SKY_TIER_DAMAGE_REDUCTION = 0.45F;
    public TianDoctorEntity(EntityType<? extends PathfinderMob> type, Level level) { super(type, level); setPersistenceRequired(); }
    /** Jade 准星联动：显示"境界：天" */
    @Override
    public int corpseRealmLevel() {
        return SKY_TIER_LEVEL;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, xiaoshi2022.corpseorigin.growth.RealmRules.health(SKY_TIER_LEVEL))
                .add(Attributes.MOVEMENT_SPEED, .28)
                .add(Attributes.ATTACK_DAMAGE, 8)
                .add(Attributes.FOLLOW_RANGE, 24)
                .add(Attributes.ARMOR, 10)
                .add(Attributes.ARMOR_TOUGHNESS, 10)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    /** 天级护体：所有伤害先打 45% 折扣再进原版结算（与玩家境界减伤同源数值） */
    @Override
    public boolean hurtServer(ServerLevel level, net.minecraft.world.damagesource.DamageSource source, float amount) {
        return super.hurtServer(level, source, amount * (1F - SKY_TIER_DAMAGE_REDUCTION));
    }
    @Override protected void registerGoals() { goalSelector.addGoal(3,new WaterAvoidingRandomStrollGoal(this,.7)); goalSelector.addGoal(4,new LookAtPlayerGoal(this,Player.class,10)); goalSelector.addGoal(5,new RandomLookAroundGoal(this)); }
    @Override protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(HIRED, false);
    }
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar c) { c.add(new AnimationController<TianDoctorEntity>("main",5,this::animation)); }
    private PlayState animation(AnimationTest<TianDoctorEntity> t) { return t.setAndContinue(t.isMoving()?RawAnimation.begin().thenLoop("walk"):RawAnimation.begin().thenLoop("idle")); }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
    @Override protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        if (employer != null) output.putString(EMPLOYER_TAG, employer.toString());
    }
    @Override protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        employer = input.getString(EMPLOYER_TAG).map(value -> {
            try { return java.util.UUID.fromString(value); }
            catch (IllegalArgumentException ignored) { return null; }
        }).orElse(null);
        entityData.set(HIRED, employer != null);
    }
    @Override public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack=player.getItemInHand(hand);
        if (!level().isClientSide() && stack.is(Items.EMERALD)) {
            if (!player.getAbilities().instabuild) stack.shrink(1);
            player.getInventory().placeItemBackInInventory(new ItemStack(xiaoshi2022.corpseorigin.registry.ModItems.BEE_WHEEL));
            player.sendSystemMessage(Component.translatable("message.corpseorigin.tian_doctor.trade"));
            return InteractionResult.SUCCESS;
        }
        if (!level().isClientSide() && stack.is(Items.COOKED_CHICKEN)) {
            employer=player.getUUID();
            entityData.set(HIRED, true);
            if (!player.getAbilities().instabuild) stack.shrink(1);
            player.sendSystemMessage(Component.translatable("message.corpseorigin.tian_doctor.hired"));
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
    @Override public void tick() {
        super.tick();
        if (level() instanceof ServerLevel deploymentLevel && --robotCooldown <= 0) {
            robotCooldown = 200;
            boolean chamberNearby = net.minecraft.core.BlockPos.betweenClosedStream(
                            blockPosition().offset(-6, -2, -6), blockPosition().offset(6, 2, 6))
                    .anyMatch(pos -> deploymentLevel.getBlockState(pos).getBlock()
                            instanceof xiaoshi2022.corpseorigin.block.CloneChamberBlock);
            boolean wormNearby = !deploymentLevel.getEntitiesOfClass(MultiHeadCorpseWormEntity.class,
                    getBoundingBox().inflate(32), LivingEntity::isAlive).isEmpty();
            if ((chamberNearby || wormNearby) && deploymentLevel.getEntitiesOfClass(
                    DoctorBeeRobotEntity.class, getBoundingBox().inflate(32), LivingEntity::isAlive).size() < 2) {
                DoctorBeeRobotEntity robot = ModEntities.DOCTOR_BEE_ROBOT.create(deploymentLevel,
                        net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
                if (robot != null) {
                    robot.setPos(getX(), getY() + 1.3, getZ());
                    deploymentLevel.addFreshEntity(robot);
                }
            }
        }
        if (!(level() instanceof ServerLevel serverLevel) || employer == null || --weaponCooldown > 0) return;
        Player player = serverLevel.getPlayerByUUID(employer);
        LivingEntity target = getTarget();
        if (player == null || !player.isAlive() || distanceToSqr(player) >= 24 * 24) { weaponCooldown = 20; return; }
        if (target == null || !target.isAlive() || !ZombieKin.isZombieKin(target)) {
            target = serverLevel.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(24),
                            monster -> monster.isAlive() && ZombieKin.isZombieKin(monster)
                                    && !ZombieKin.isZombieKing(monster))
                    .stream().min(java.util.Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
            setTarget(target);
        }
        if (target == null) { weaponCooldown = 20; return; }
        Vec3 start = getEyePosition();
        Vec3 direction = target.getBoundingBox().getCenter().subtract(start).normalize();
        BeeWheelEntity wheel = (BeeWheelEntity) ModEntities.BEE_WHEEL.create(serverLevel, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
        if (wheel == null) { weaponCooldown = 20; return; }
        wheel.initializeDoctor(this, 40);
        wheel.setPos(start);
        wheel.setDeltaMovement(direction.scale(1.2));
        if (serverLevel.addFreshEntity(wheel)) weaponCooldown = 100;
        else weaponCooldown = 20;
    }
}
