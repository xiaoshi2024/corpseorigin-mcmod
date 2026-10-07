package xiaoshi2022.corpseorigin.entity;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class SeveredZbHeadEntity extends PathfinderMob implements GeoEntity {
    private static final EntityDataAccessor<String> SKIN = SynchedEntityData.defineId(SeveredZbHeadEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> EYE = SynchedEntityData.defineId(SeveredZbHeadEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> CRACKED = SynchedEntityData.defineId(SeveredZbHeadEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> SOURCE = SynchedEntityData.defineId(SeveredZbHeadEntity.class, EntityDataSerializers.INT);
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int lifetime;

    public SeveredZbHeadEntity(EntityType<? extends PathfinderMob> type, Level level) { super(type, level); }
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 1).add(Attributes.MOVEMENT_SPEED, 0);
    }
    public void setAppearance(String skin, boolean eye, boolean cracked) {
        entityData.set(SKIN, skin);
        entityData.set(EYE, eye);
        entityData.set(CRACKED, cracked);
    }
    public String skinName() { return entityData.get(SKIN); }
    public boolean hasCorpseEye() { return entityData.get(EYE); }
    public boolean isCracked() { return entityData.get(CRACKED); }
    public void setSource(int id) { entityData.set(SOURCE, id); }
    public int sourceId() { return entityData.get(SOURCE); }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SKIN, ""); builder.define(EYE, false); builder.define(CRACKED, false);
        builder.define(SOURCE, -1);
    }
    @Override protected void registerGoals() {}
    @Override public void tick() {
        super.tick();
        if (!level().isClientSide() && ++lifetime >= 1200) discard();
    }
    @Override protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putString("Skin", skinName()); out.putBoolean("Eye", hasCorpseEye());
        out.putBoolean("Cracked", isCracked()); out.putInt("Lifetime", lifetime);
    }
    @Override protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        setAppearance(in.getStringOr("Skin", ""), in.getBooleanOr("Eye", false), in.getBooleanOr("Cracked", false));
        lifetime = in.getIntOr("Lifetime", 0);
    }
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {}
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
