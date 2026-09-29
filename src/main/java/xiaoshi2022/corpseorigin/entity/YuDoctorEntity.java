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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import xiaoshi2022.corpseorigin.registry.ModItems;

public class YuDoctorEntity extends PathfinderMob implements GeoEntity {
    private static final Item[] MATERIALS = {Items.NETHER_STAR, Items.BLAZE_POWDER, Items.GOLDEN_APPLE, Items.GLASS_BOTTLE};
    private static final int[] COSTS = {1, 4, 1, 1};
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public YuDoctorEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 24)
                .add(Attributes.MOVEMENT_SPEED, .30).add(Attributes.FOLLOW_RANGE, 16);
    }
    @Override protected void registerGoals() {
        goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 1.0));
        goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8));
        goalSelector.addGoal(4, new RandomLookAroundGoal(this));
    }
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("main", 5, this::animation));
    }
    private PlayState animation(AnimationTest<YuDoctorEntity> test) {
        return test.setAndContinue(test.isMoving() ? RawAnimation.begin().thenLoop("run") : RawAnimation.begin().thenLoop("idle"));
    }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }

    @Override public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (level().isClientSide()) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer server)) return InteractionResult.PASS;
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        for (int i = 0; i < MATERIALS.length; i++) {
            int missing = COSTS[i] - count(server, MATERIALS[i]);
            if (!server.getAbilities().instabuild && missing > 0) {
                server.sendSystemMessage(Component.translatable("message.corpseorigin.yu_doctor.missing",
                        missing, MATERIALS[i].getName(new ItemStack(MATERIALS[i]))));
                return InteractionResult.SUCCESS;
            }
        }
        if (!server.getAbilities().instabuild) for (int i = 0; i < MATERIALS.length; i++) take(server, MATERIALS[i], COSTS[i]);
        ItemStack antidote = new ItemStack(ModItems.CORPSE_ANTIDOTE);
        if (!server.addItem(antidote)) server.drop(antidote, false);
        server.sendSystemMessage(Component.translatable("message.corpseorigin.yu_doctor.ready"));
        return InteractionResult.SUCCESS;
    }
    private static int count(ServerPlayer player, Item item) {
        int count = 0;
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) if (stack.is(item)) count += stack.getCount();
        return count;
    }
    private static void take(ServerPlayer player, Item item, int amount) {
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (stack.is(item)) {
                int n = Math.min(amount, stack.getCount());
                stack.shrink(n);
                amount -= n;
                if (amount == 0) return;
            }
        }
    }
}
