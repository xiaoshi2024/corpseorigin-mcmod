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
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * 贝奇（{@code beiqi}）—— 晓佳的宠物小狗。
 * <p>
 * 剧本桥段：晓佳和小惠被穆博士捆绑，贝奇试图救主，结果被穆博士的针筒注射变异。
 * <p>
 * 与 {@link HamEntity}（小言子的狗变种）不同，贝奇是一只普通小狗：
 * <ul>
 *   <li>体型更小（0.5 × 0.8）</li>
 *   <li>可以被驯养（给骨头）</li>
 *   <li>默认跟随主人，主人被攻击时会反击</li>
 *   <li>可以被穆博士注射变异 —— 变异后会转换为 {@link BeiQiMutantEntity}</li>
 * </ul>
 * 模型资源待建模。
 */
public class BeiQiEntity extends TamableAnimal implements GeoEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation BARK = RawAnimation.begin().thenPlay("bark");

    public BeiQiEntity(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20)
                .add(Attributes.MOVEMENT_SPEED, 0.35)
                .add(Attributes.ATTACK_DAMAGE, 3)
                .add(Attributes.FOLLOW_RANGE, 20);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new SitWhenOrderedToGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, true));
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
        if (!isTame() && stack.is(Items.BONE)) {
            if (level() instanceof ServerLevel server) {
                tame(player);
                stack.consume(1, player);
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

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(Items.BEEF) || stack.is(Items.COOKED_BEEF)
                || stack.is(Items.PORKCHOP) || stack.is(Items.COOKED_PORKCHOP);
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return null; // 贝奇不繁殖
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<BeiQiEntity>("movement", 3,
                test -> test.setAndContinue(test.isMoving() && !isOrderedToSit() ? WALK : IDLE)));
        controllers.add(new AnimationController<BeiQiEntity>("action", 2,
                test -> com.geckolib.animation.object.PlayState.STOP)
                .triggerableAnim("bark", BARK));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
    }

    /**
     * 被针筒注射变异：转换为变异形态。
     */
    public BeiQiMutantEntity mutate() {
        if (level().isClientSide()) return null;
        BeiQiMutantEntity mutant = xiaoshi2022.corpseorigin.registry.ModEntities.BEIQI_MUTANT.create(
                level(), EntitySpawnReason.TRIGGERED);
        if (mutant == null) return null;

        mutant.setPos(getX(), getY(), getZ());
        mutant.setDeltaMovement(getDeltaMovement());
        mutant.setHealth(getHealth());

        level().addFreshEntity(mutant);
        discard();
        return mutant;
    }
}
