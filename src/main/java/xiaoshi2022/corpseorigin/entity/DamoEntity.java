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
import net.minecraft.world.level.Level;
import xiaoshi2022.corpseorigin.growth.FreeGrowth;

/** Small Frog Damo, a peaceful one-time qi teacher. */
public class DamoEntity extends PathfinderMob implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public DamoEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20)
                .add(Attributes.MOVEMENT_SPEED, .22)
                .add(Attributes.FOLLOW_RANGE, 16);
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
