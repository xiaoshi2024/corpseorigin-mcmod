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
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import xiaoshi2022.corpseorigin.registry.ModEntities;

/** 天·博士：可交易、可雇佣的黑色火线特派 NPC。 */
public class TianDoctorEntity extends PathfinderMob implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private java.util.UUID employer;
    private int weaponCooldown;
    public TianDoctorEntity(EntityType<? extends PathfinderMob> type, Level level) { super(type, level); setPersistenceRequired(); }
    public static AttributeSupplier.Builder createAttributes() { return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 80).add(Attributes.MOVEMENT_SPEED, .28).add(Attributes.ATTACK_DAMAGE, 8).add(Attributes.FOLLOW_RANGE, 24); }
    @Override protected void registerGoals() { goalSelector.addGoal(1,new MeleeAttackGoal(this,1,true)); goalSelector.addGoal(3,new WaterAvoidingRandomStrollGoal(this,.7)); goalSelector.addGoal(4,new LookAtPlayerGoal(this,Player.class,10)); goalSelector.addGoal(5,new RandomLookAroundGoal(this)); targetSelector.addGoal(1,new NearestAttackableTargetGoal<>(this,Monster.class,true)); }
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar c) { c.add(new AnimationController<TianDoctorEntity>("main",5,this::animation)); }
    private PlayState animation(AnimationTest<TianDoctorEntity> t) { return t.setAndContinue(t.isMoving()?RawAnimation.begin().thenLoop("walk"):RawAnimation.begin().thenLoop("idle")); }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
    @Override public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack=player.getItemInHand(hand);
        if (!level().isClientSide() && stack.is(Items.EMERALD)) { if (!player.getAbilities().instabuild) stack.shrink(1); player.sendSystemMessage(Component.translatable("message.corpseorigin.tian_doctor.trade")); return InteractionResult.SUCCESS; }
        if (!level().isClientSide() && stack.is(Items.COOKED_CHICKEN)) { employer=player.getUUID(); if (!player.getAbilities().instabuild) stack.shrink(1); player.sendSystemMessage(Component.translatable("message.corpseorigin.tian_doctor.hired")); return InteractionResult.SUCCESS; }
        return InteractionResult.SUCCESS;
    }
    @Override public void tick() { super.tick(); if (!(level() instanceof ServerLevel sl) || employer==null || --weaponCooldown>0) return; Player p=sl.getPlayerByUUID(employer); if (p!=null && distanceToSqr(p)<24*24 && getTarget()!=null) { var wheel=new BeeWheelEntity(ModEntities.BEE_WHEEL,sl); wheel.setPos(getX(),getEyeY(),getZ()); wheel.setDeltaMovement(getTarget().position().subtract(position()).normalize().scale(1.2)); sl.addFreshEntity(wheel); weaponCooldown=100; } }
}
