package xiaoshi2022.corpseorigin.entity;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.fish.AbstractFish;
import net.minecraft.world.entity.player.Player;
import xiaoshi2022.corpseorigin.component.PlayerCorpseComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** 1.21.1 corpse fish, adapted to 26.2 aquatic navigation and corpse faction rules. */
public class ZbrFishEntity extends AbstractFish implements GeoEntity, ZombieKin {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private static final RawAnimation SWIM = RawAnimation.begin().thenLoop("swim");
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private int hunger = 100, acidCooldown;

    public ZbrFishEntity(EntityType<? extends AbstractFish> type, Level level) {
        super(type, level);
        moveControl = new SmoothSwimmingMoveControl<>(this, 85, 10, .1f, .02f, false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 10)
                .add(Attributes.MOVEMENT_SPEED, .5).add(Attributes.ATTACK_DAMAGE, 2)
                .add(Attributes.FOLLOW_RANGE, 16);
    }

    @Override protected void registerGoals() {
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2, true));
        goalSelector.addGoal(3, new RandomSwimmingGoal(this, 1, 40));
        goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false,
                (target, level) -> validAquaticPrey(target)));
    }

    private boolean validAquaticPrey(LivingEntity target) {
        return target != this && target.isAlive() && target.isInWater() && !target.isSpectator()
                && !(target instanceof Player p && p.isCreative())
                && ZombieKin.canAttack(this, target) && !isAlliedTo(target);
    }

    @Override protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (acidCooldown > 0) acidCooldown--;
        if (tickCount % 200 == 0) { hunger = Math.max(0, hunger - 1); if (hunger > 30) heal(.5f); }
        LivingEntity target = getTarget();
        if (target != null && !validAquaticPrey(target)) { setTarget(null); return; }
        if (target != null && isInWater() && hunger >= 10 && acidCooldown == 0
                && distanceToSqr(target) > 4 && distanceToSqr(target) <= 64 && hasLineOfSight(target)) {
            target.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0));
            var from = position().add(0, getBbHeight() * .5, 0);
            var delta = target.getBoundingBox().getCenter().subtract(from);
            for (int i = 0; i < 10; i++) {
                var at = from.add(delta.scale(i / 10.0));
                level.sendParticles(ParticleTypes.DRIPPING_HONEY, at.x, at.y, at.z, 2, .05, .05, .05, .01);
            }
            playSound(SoundEvents.SLIME_SQUISH, .5f, 1.4f);
            hunger -= 10; acidCooldown = 60;
        }
    }

    @Override public boolean doHurtTarget(ServerLevel level, Entity target) {
        if (!(target instanceof LivingEntity living) || !validAquaticPrey(living)) return false;
        boolean hit = super.doHurtTarget(level, target);
        if (hit) { hunger = Math.min(100, hunger + 5); heal(1); playSound(SoundEvents.GENERIC_EAT.value(), .6f, 1.3f); }
        return hit;
    }

    @Override public int getHunger() { return hunger; }
    @Override protected SoundEvent getFlopSound() { return SoundEvents.COD_FLOP; }
    @Override public ItemStack getBucketItemStack() { return ItemStack.EMPTY; }
    // A hostile fish cannot be deleted by an empty vanilla bucket result.
    // 尸兄玩家右键吸食：生吃河里的尸兄鱼回饥饿回血（龙右原著桥段）
    @Override protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (PlayerCorpseComponent.isCorpse(player) && hand == InteractionHand.MAIN_HAND) {
            if (!player.level().isClientSide()) {
                player.getFoodData().eat(6, 0.6F);
                player.heal(4.0F);
                playSound(SoundEvents.GENERIC_EAT.value(), 0.8F, 0.9F);
                if (player.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.HEART,
                            getX(), getY() + 0.5, getZ(), 5, 0.5, 0.5, 0.5, 0.01);
                }
                discard();
            }
            return player.level().isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }
    @Override public boolean canBeAffected(MobEffectInstance effect) {
        return !effect.is(MobEffects.POISON) && !effect.is(xiaoshi2022.corpseorigin.registry.ModEffects.QIANS)
                && super.canBeAffected(effect);
    }
    @Override protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out); out.putInt("CorpseHunger", hunger); out.putInt("AcidCooldown", acidCooldown);
    }
    @Override protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in); hunger = Math.clamp(in.getIntOr("CorpseHunger", 100), 0, 100);
        acidCooldown = Math.clamp(in.getIntOr("AcidCooldown", 0), 0, 60);
    }
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<ZbrFishEntity>("swim", 5, t -> t.setAndContinue(t.isMoving() ? SWIM : IDLE)));
    }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
