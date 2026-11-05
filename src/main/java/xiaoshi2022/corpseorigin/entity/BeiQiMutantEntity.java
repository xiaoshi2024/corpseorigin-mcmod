package xiaoshi2022.corpseorigin.entity;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * 变异贝奇（{@code beiqi_mutant}）—— 被穆博士针筒注射后的狂躁形态。
 * <p>
 * 剧本桥段：穆博士把晓佳和小惠捆住后，贝奇冲上去救主，被穆博士一针管下去 ——
 * 贝奇当场变异：体型变大、眼睛变红、六亲不认地攻击一切活物。
 * <p>
 * 与普通 {@link BeiQiEntity} 的区别：
 * <ul>
 *   <li>体型更大（1.0 × 1.4）</li>
 *   <li>血量翻倍（40）、攻击力翻倍（7）</li>
 *   <li>移动速度更快（0.42），追着人咬</li>
 *   <li>六亲不认 —— 攻击一切活物</li>
 *   <li>变异过程由 {@link BeiQiEntity#mutate()} 触发</li>
 * </ul>
 */
public class BeiQiMutantEntity extends TamableAnimal implements GeoEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("mutant_idle");
    private static final RawAnimation RUN = RawAnimation.begin().thenLoop("mutant_run");
    private static final RawAnimation BITE = RawAnimation.begin().thenPlay("mutant_bite");
    private static final RawAnimation HOWL = RawAnimation.begin().thenPlay("mutant_howl");

    public BeiQiMutantEntity(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
        this.xpReward = 15;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40)
                .add(Attributes.MOVEMENT_SPEED, 0.42)
                .add(Attributes.ATTACK_DAMAGE, 7)
                .add(Attributes.FOLLOW_RANGE, 28)
                .add(Attributes.ARMOR, 4);
    }

    @Override
    protected void registerGoals() {
        // 变异贝奇：狂躁但仍认主人 —— 主人打谁它咬谁，主人被打会护主，
        // 主人让它坐下能冷静；但**绝不主动攻击一切活物**（只跟随主人行为）
        goalSelector.addGoal(0, new FloatGoal(this));
        // ★ 主人让它坐下（冷静）—— 保留 TamableAnimal 的驯服指令
        goalSelector.addGoal(1, new SitWhenOrderedToGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.4, true));
        goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.9));

        // 优先级 0：主人打谁，我咬谁！（原版 TamableAnimal 自带的目标跟随）
        targetSelector.addGoal(0, new OwnerHurtTargetGoal(this));
        // 优先级 1：主人被攻击时护主
        targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        // 优先级 2：自己被攻击也记仇反咬
        targetSelector.addGoal(2, new HurtByTargetGoal(this).setAlertOthers());
        // ★ 不主动加 NearestAttackableTargetGoal —— 狂躁但不主动攻击一切活物，只跟随主人
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<BeiQiMutantEntity>("movement", 4,
                test -> test.setAndContinue(test.isMoving() ? RUN : IDLE)));
        controllers.add(new AnimationController<BeiQiMutantEntity>("attack", 2,
                test -> this.swinging ? test.setAndContinue(BITE)
                        : com.geckolib.animation.object.PlayState.STOP));
        controllers.add(new AnimationController<BeiQiMutantEntity>("action", 2,
                test -> com.geckolib.animation.object.PlayState.STOP)
                .triggerableAnim("howl", HOWL));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    // ==================== 变异后的特殊行为 ====================

    /** 变异后不能再被驯养 —— 但主人绑定状态会被保留 */
    @Override
    public boolean isFood(net.minecraft.world.item.ItemStack stack) {
        return false;
    }

    @Override
    public net.minecraft.world.entity.AgeableMob getBreedOffspring(ServerLevel level, net.minecraft.world.entity.AgeableMob partner) {
        return null;
    }
}
