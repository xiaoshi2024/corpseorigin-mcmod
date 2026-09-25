package xiaoshi2022.corpseorigin.entity;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import xiaoshi2022.corpseorigin.skill.chapter.QiEffects;

/** 小言子的狗变种伙伴。主人与坐下状态由原版驯养系统同步和保存。 */
public class HamEntity extends TamableAnimal implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int summonLifetime;
    public void markTemporarySummon() { summonLifetime = 600; }
    public boolean isTemporarySummon() { return summonLifetime > 0; }
    @Override public void tick() {
        super.tick();
        if (!level().isClientSide() && summonLifetime > 0 && --summonLifetime == 0) discard();
    }
    @Override protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("HamSummonLifetime", summonLifetime);
    }
    @Override protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        summonLifetime = Math.max(0, input.getIntOr("HamSummonLifetime", 0));
    }
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");

    public HamEntity(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 30)
                .add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.ATTACK_DAMAGE, 4)
                .add(Attributes.FOLLOW_RANGE, 24);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new SitWhenOrderedToGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, true));
        goalSelector.addGoal(3, new HamBiteToyGoal(this));
        goalSelector.addGoal(3, new FollowOwnerGoal(this, 1.1, 5, 2));
        goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 6));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.getItem() instanceof xiaoshi2022.corpseorigin.item.MagicianRabbitItem)
            return xiaoshi2022.corpseorigin.item.MagicianRabbitItem.offer(stack, player, this);
        if (stack.getItem() instanceof xiaoshi2022.corpseorigin.item.DogCageItem)
            return xiaoshi2022.corpseorigin.item.DogCageItem.capture(stack, player, this);
        if (!isTame() && stack.is(Items.BONE)) {
            if (level() instanceof ServerLevel server) {
                tame(player);
                stack.consume(1, player);
                QiEffects.burst(server, getX(), getY() + 1, getZ(), 0xff5c8a, 7, .3);
            }
            return InteractionResult.SUCCESS;
        }
        if (isOwnedBy(player)) {
            if (!level().isClientSide()) {
                setOrderedToSit(!isOrderedToSit());
                getNavigation().stop();
                setTarget(null);
            }
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    @Override public boolean isFood(ItemStack stack) { return false; }
    @Override public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) { return null; }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<HamEntity>("movement", 3,
                test -> test.setAndContinue(test.isMoving() && !isOrderedToSit() ? WALK : IDLE)));
        controllers.add(new AnimationController<HamEntity>("action", 2, test -> com.geckolib.animation.object.PlayState.STOP)
                .triggerableAnim("summon", RawAnimation.begin().thenPlay("summon"))
                .triggerableAnim("bite", RawAnimation.begin().thenPlay("fire"))
                .triggerableAnim("fire", RawAnimation.begin().thenPlay("fire")));
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
