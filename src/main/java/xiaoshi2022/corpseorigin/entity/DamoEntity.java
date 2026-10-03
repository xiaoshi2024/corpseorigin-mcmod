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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import xiaoshi2022.corpseorigin.growth.FreeGrowth;
import xiaoshi2022.corpseorigin.growth.RealmRules;

/**
 * Small Frog Damo, a peaceful one-time qi teacher.
 * <p>
 * 虽是和平 NPC（不主动攻击、喂食即传授气感后消失），但设定上他是<b>地级4 强者</b>（低级巅峰），
 * 不该被玩家乱棍打死。身板对齐境界表 {@link RealmRules}：地级4 = 境界 8 ——
 * 生命 {@code health(8)} = 800；受击按地级4 境界减伤 20%（{@code protection(8,0)}）；
 * 护甲 12 / 韧性 8 / 击退抗性 0.8，低境界玩家的普通近战几乎无法撼动他，
 * 只有境界压制（地级以上攻击）或火攻/药水等绕过护甲的手段才能有效伤害。
 */
public class DamoEntity extends PathfinderMob implements GeoEntity, RealmRated {

    /** 地级4 在境界表中的等级（人1-4=1-4、地1-4=5-8） */
    private static final int EARTH_TIER_4_LEVEL = 8;
    /** 地级4 境界减伤：{@code protection(8,0)} = 20%（护体罡气） */
    private static final float EARTH_TIER_4_DAMAGE_REDUCTION = 0.20F;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public DamoEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    /** Jade 准星联动：显示"境界：地4" */
    @Override
    public int corpseRealmLevel() {
        return EARTH_TIER_4_LEVEL;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, RealmRules.health(EARTH_TIER_4_LEVEL))
                .add(Attributes.MOVEMENT_SPEED, .22)
                .add(Attributes.FOLLOW_RANGE, 16)
                .add(Attributes.ARMOR, 12)
                .add(Attributes.ARMOR_TOUGHNESS, 8)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8D);
    }

    /** 地级4 护体：所有伤害先打 80% 折扣再进原版结算（与玩家境界减伤同源数值） */
    @Override
    public boolean hurtServer(ServerLevel level, net.minecraft.world.damagesource.DamageSource source, float amount) {
        return super.hurtServer(level, source, amount * (1F - EARTH_TIER_4_DAMAGE_REDUCTION));
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, .65));
        goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8));
        goalSelector.addGoal(4, new RandomLookAroundGoal(this));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<DamoEntity>("main", 5, this::animation));
    }

    private PlayState animation(AnimationTest<DamoEntity> test) {
        return test.setAndContinue(test.isMoving()
                ? RawAnimation.begin().thenLoop("walk")
                : RawAnimation.begin().thenLoop("idle"));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack food = player.getItemInHand(hand);
        if (!food.has(DataComponents.FOOD)) return InteractionResult.PASS;
        if (!level().isClientSide() && player instanceof ServerPlayer serverPlayer) {
            if (FreeGrowth.hasQiSense(serverPlayer) || FreeGrowth.innerPower(serverPlayer) > 0) {
                serverPlayer.sendSystemMessage(Component.translatable("message.corpseorigin.damo.already_awakened"));
                return InteractionResult.SUCCESS;
            }
            FreeGrowth.awaken(serverPlayer);
            if (!FreeGrowth.hasQiSense(serverPlayer)) return InteractionResult.SUCCESS;
            serverPlayer.sendSystemMessage(Component.translatable("message.corpseorigin.damo.qi"));
            if (!player.getAbilities().instabuild) food.shrink(1);
            discard();
        }
        return InteractionResult.SUCCESS;
    }
}
