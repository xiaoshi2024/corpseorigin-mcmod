package xiaoshi2022.corpseorigin.entity;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.level.Level;

/** Large corpse centipede that grows a face for every consumed living target. */
public class MultiHeadCorpseWormEntity extends PathfinderMob implements GeoEntity, ZombieKin {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private static final EntityDataAccessor<String> FACE_SKINS = SynchedEntityData.defineId(MultiHeadCorpseWormEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> BURIED = SynchedEntityData.defineId(MultiHeadCorpseWormEntity.class, EntityDataSerializers.BOOLEAN);
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.multi_head_corpse_worm.idle");
    private static final RawAnimation CRAWL = RawAnimation.begin().thenLoop("animation.multi_head_corpse_worm.crawl");
    private static final RawAnimation DIG = RawAnimation.begin().thenLoop("animation.multi_head_corpse_worm.dig");
    private static final RawAnimation EAT = RawAnimation.begin().thenPlay("animation.multi_head_corpse_worm.eat");
    private static final RawAnimation GRAB = RawAnimation.begin().thenPlay("animation.multi_head_corpse_worm.grab");
    private int eatTicks;
    private int buriedTicks;
    private int surfaceCooldown;
    private double surfaceY;

    public MultiHeadCorpseWormEntity(EntityType<? extends PathfinderMob> type, Level level) { super(type, level); }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FACE_SKINS, "");
        builder.define(BURIED, false);
    }
    public static AttributeSupplier.Builder createAttributes() { return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 160).add(Attributes.MOVEMENT_SPEED, .22).add(Attributes.ATTACK_DAMAGE, 12)
            .add(Attributes.KNOCKBACK_RESISTANCE, .9); }
    @Override public boolean isHungry() { return true; }
    private boolean isPrey(LivingEntity entity) {
        return entity != this && entity.isAlive() && !ZombieKin.isZombieKing(entity)
                && (entity instanceof Player || entity instanceof AbstractVillager || ZombieKin.isZombieKin(entity));
    }
    @Override protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.15, true));
        goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, .8));
        goalSelector.addGoal(3, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false,
                (target, level) -> isPrey(target)
                        && (target.hurtTime > 0 || target.getHealth() < target.getMaxHealth())));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false,
                (target, level) -> isPrey(target)));
    }
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar c) {
        c.add(new AnimationController<>("movement", 4, this::movement));
        c.add(new AnimationController<>("attack", 0, this::attack));
    }
    private PlayState movement(AnimationTest<MultiHeadCorpseWormEntity> t) {
        if (isBurrowing()) return t.setAndContinue(DIG);
        return getDeltaMovement().horizontalDistanceSqr() > .002 ? t.setAndContinue(CRAWL) : t.setAndContinue(IDLE);
    }
    private PlayState attack(AnimationTest<MultiHeadCorpseWormEntity> t) {
        if (eatTicks > 0) return t.setAndContinue(eatTicks > 8 ? EAT : GRAB);
        return PlayState.STOP;
    }
    @Override public void tick() {
        super.tick();
        if (eatTicks > 0) eatTicks--;
        if (!level().isClientSide()) {
            if (surfaceCooldown > 0) surfaceCooldown--;
            LivingEntity target = getTarget();
            if (target != null && !isPrey(target)) {
                setTarget(null);
                target = null;
            }
            if (tickCount % 10 == 0 && (target == null || !target.isAlive())) {
                for (LivingEntity nearby : level().getEntitiesOfClass(LivingEntity.class,
                        getBoundingBox().inflate(16), entity -> isPrey(entity)
                                && (entity.hurtTime > 0 || entity.getHealth() < entity.getMaxHealth()))) {
                    setTarget(nearby);
                    break;
                }
            }
            target = getTarget();
            if (isBurrowing()) {
                buriedTicks++;
                getNavigation().stop();
                noPhysics = true;
                setNoGravity(true);
                if (target != null && target.isAlive()) {
                    Vec3 delta = target.position().subtract(position());
                    Vec3 horizontal = new Vec3(delta.x, 0, delta.z);
                    if (horizontal.lengthSqr() > 1) {
                        Vec3 step = horizontal.normalize().scale(.36);
                        move(MoverType.SELF, step);
                    }
                    if (horizontal.lengthSqr() < 9 && buriedTicks > 12) emergeNear(target);
                } else if (buriedTicks > 120) emergeNear(null);
            } else if (target == null && surfaceCooldown == 0 && onGround() && tickCount % 200 == 0) {
                surfaceY = getY();
                entityData.set(BURIED, true);
                buriedTicks = 0;
                getNavigation().stop();
                setPos(getX(), surfaceY - 2, getZ());
                noPhysics = true;
                setNoGravity(true);
            }
        }
    }
    private void emergeNear(LivingEntity target) {
        double x = target == null ? getX() : target.getX();
        double z = target == null ? getZ() : target.getZ();
        BlockPos pos = BlockPos.containing(x, target == null ? surfaceY : target.getY(), z);
        for (int dy = 0; dy <= 3; dy++) {
            double y = pos.getY() + dy;
            if (level().noCollision(this, getBoundingBox().move(x - getX(), y - getY(), z - getZ()))) {
                setPos(x, y, z);
                noPhysics = false;
                setNoGravity(false);
                entityData.set(BURIED, false);
                surfaceCooldown = 100;
                return;
            }
        }
        if (target == null) buriedTicks = 100;
    }
    @Override public boolean doHurtTarget(ServerLevel level, Entity target) {
        if (ZombieKin.isZombieKing(target)) return false;
        boolean hit = super.doHurtTarget(level, target);
        if (hit) { eatTicks = 16; if (target instanceof LivingEntity living && !living.isAlive()) addFace(living); }
        return hit;
    }
    private void addFace(LivingEntity victim) {
        String source = "c";
        if (victim instanceof Player player) {
            source = "p:" + player.getUUID();
        } else if (victim instanceof LowerLevelZbEntity zombie && !zombie.getPlayerSkinName().isEmpty()) {
            String skinName = zombie.getPlayerSkinName();
            Player player = level().getServer().getPlayerList().getPlayerByName(skinName);
            source = player != null ? "p:" + player.getUUID() : "n:" + skinName;
        }
        String existing = entityData.get(FACE_SKINS);
        String[] entries = existing.isEmpty() ? new String[0] : existing.split(",");
        if (entries.length >= 6) return;
        entityData.set(FACE_SKINS, existing.isEmpty() ? source : existing + "," + source);
    }
    public String[] getFaceSkins() {
        String value = entityData.get(FACE_SKINS);
        return value.isEmpty() ? new String[0] : value.split(",");
    }
    public int getFaces() { return getFaceSkins().length; }
    public boolean isBurrowing() { return entityData.get(BURIED); }
    @Override protected void addAdditionalSaveData(ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("FaceSkins", entityData.get(FACE_SKINS));
        tag.putBoolean("Buried", isBurrowing());
        tag.putDouble("SurfaceY", surfaceY);
    }
    @Override protected void readAdditionalSaveData(ValueInput tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(FACE_SKINS, tag.getStringOr("FaceSkins", ""));
        if (tag.getBooleanOr("Buried", false)) setPos(getX(), tag.getDoubleOr("SurfaceY", getY() + 2), getZ());
        entityData.set(BURIED, false);
        noPhysics = false;
        setNoGravity(false);
    }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
