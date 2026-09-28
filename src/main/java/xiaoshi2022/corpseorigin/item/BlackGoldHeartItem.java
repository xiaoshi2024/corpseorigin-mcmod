package xiaoshi2022.corpseorigin.item;

import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import xiaoshi2022.corpseorigin.skill.heixiaofei.HeartImplant;

public final class BlackGoldHeartItem extends Item implements GeoItem {
    private static final RawAnimation HEARTBEAT = RawAnimation.begin().thenLoop("heartbeat");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    public BlackGoldHeartItem(Properties properties) { super(properties); GeoItem.registerSyncedAnimatable(this); }
    @Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
        return player instanceof ServerPlayer p ? HeartImplant.implant(p,p,p.getItemInHand(hand)) : InteractionResult.SUCCESS;
    }
    @Override public InteractionResult interactLivingEntity(ItemStack stack, Player actor, LivingEntity target, InteractionHand hand) {
        if (!(target instanceof Player)) return InteractionResult.PASS;
        return actor instanceof ServerPlayer p && target instanceof ServerPlayer t
                ? HeartImplant.implant(p,t,stack) : InteractionResult.SUCCESS;
    }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context,
            net.minecraft.world.item.component.TooltipDisplay display,
            java.util.function.Consumer<net.minecraft.network.chat.Component> tooltip, TooltipFlag flag) {
        tooltip.accept(net.minecraft.network.chat.Component.translatable("item.corpseorigin.black_gold_heart.tooltip"));
    }
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<BlackGoldHeartItem>("heartbeat",0,t -> t.setAndContinue(HEARTBEAT)));
    }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
    @Override public void createGeoRenderer(java.util.function.Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private com.geckolib.renderer.GeoItemRenderer<BlackGoldHeartItem> renderer;
            @Override public com.geckolib.renderer.GeoItemRenderer<?> getGeoItemRenderer() {
                if (renderer == null) renderer = new com.geckolib.renderer.GeoItemRenderer<>(
                        new com.geckolib.model.DefaultedItemGeoModel<>(xiaoshi2022.corpseorigin.CorpseOrigin.id("black_gold_heart")) {
                            @Override public net.minecraft.resources.Identifier getModelResource(com.geckolib.renderer.base.GeoRenderState state) {
                                return xiaoshi2022.corpseorigin.CorpseOrigin.id("entity/black_gold_heart");
                            }
                            @Override public net.minecraft.resources.Identifier getAnimationResource(BlackGoldHeartItem item) {
                                return xiaoshi2022.corpseorigin.CorpseOrigin.id("entity/black_gold_heart");
                            }
                            @Override public net.minecraft.resources.Identifier getTextureResource(com.geckolib.renderer.base.GeoRenderState state) {
                                return xiaoshi2022.corpseorigin.CorpseOrigin.id("textures/entity/black_gold_heart.png");
                            }
                        });
                return renderer;
            }
        });
    }
}
