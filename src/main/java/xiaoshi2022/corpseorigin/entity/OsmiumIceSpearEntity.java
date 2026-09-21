package xiaoshi2022.corpseorigin.entity;

import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import xiaoshi2022.corpseorigin.registry.ModEntities;
import java.util.UUID;

public class OsmiumIceSpearEntity extends Entity implements GeoEntity {
    private UUID owner;
    private Vec3 velocity = Vec3.ZERO;
    private int age;

    public OsmiumIceSpearEntity(EntityType<? extends OsmiumIceSpearEntity> type, Level level) { super(type, level); setNoGravity(true); }

    // ==================== GeckoLib ====================
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    protected static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("idle");


    public static OsmiumIceSpearEntity create(ServerLevel level, LivingEntity owner, Vec3 position, Vec3 velocity) {
        OsmiumIceSpearEntity e = new OsmiumIceSpearEntity(ModEntities.OSMIUM_ICE_SPEAR, level);
        e.owner = owner.getUUID(); e.velocity = velocity; e.setPos(position); return e;
    }
    @Override public void tick() {
        super.tick(); if (level().isClientSide()) return;
        Vec3 next = position().add(velocity); AABB sweep = getBoundingBox().expandTowards(velocity).inflate(.35);
        for (LivingEntity target : level().getEntitiesOfClass(LivingEntity.class, sweep, e -> e.isAlive() && !e.getUUID().equals(owner))) {
            target.hurtServer((ServerLevel) level(), ownerSource(target), 12f); discard(); return;
        }
        setPos(next); if (++age > 50 || !level().noCollision(getBoundingBox())) discard();
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        return false;
    }

    private DamageSource ownerSource(LivingEntity target) { return level().damageSources().generic(); }
    @Override protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder b) {}
    @Override protected void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput i) {}
    @Override protected void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput o) {}

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("idle", 5, this::IdleController));

    }

    private PlayState IdleController(AnimationTest<GeoAnimatable> geoAnimatableAnimationTest) {
        return geoAnimatableAnimationTest.setAndContinue(IDLE_ANIM);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
