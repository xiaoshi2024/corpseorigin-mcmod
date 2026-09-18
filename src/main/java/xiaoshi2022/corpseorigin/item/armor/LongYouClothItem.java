package xiaoshi2022.corpseorigin.item.armor;

import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.constant.DataTickets;
import com.geckolib.renderer.GeoArmorRenderer;
import com.geckolib.util.GeckoLibUtil;
import com.google.common.base.Suppliers;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import org.jetbrains.annotations.Nullable;
import xiaoshi2022.corpseorigin.client.renderer.armor.LongYouClothArmorRenderer;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 尸王专属服装（龙右）—— 头盔 / 胸甲 / 护腿 / 靴子共用。
 * <p>
 * 动画只有两条：{@code idle} 常驻，{@code attack} 由穿戴者的挥击驱动
 * （GeckoLib 的 {@link DataTickets#SWINGING_ARM}，渲染器会从穿戴者身上填进来）。
 * 摆件/披风之类的形状全在 {@code longyoucloth.geo.json} + {@code longyoucloth.animation.json} 里，
 * 这里只负责"什么状态播哪条"。
 */
public class LongYouClothItem extends Item implements GeoItem {

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("attack");

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    public LongYouClothItem(ArmorMaterial material, ArmorType type, Properties properties) {
        super(properties.humanoidArmor(material, type));
        GeoItem.registerSyncedAnimatable(this);
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private final Supplier<LongYouClothArmorRenderer<?>> renderer =
                    Suppliers.memoize(LongYouClothArmorRenderer::new);

            @Nullable
            @Override
            public GeoArmorRenderer<?, ?> getGeoArmorRenderer(ItemStack itemStack, EquipmentSlot equipmentSlot) {
                return this.renderer.get();
            }
        });
    }

    @Override
    public void registerControllers(final AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<LongYouClothItem>("main", 2, new MainHandler()));
    }

    /**
     * idle / attack 二选一的控制器。
     * <p>
     * 和天线宝宝那套一样不用 {@code triggerableAnim}：触发通道和 handler 的返回值挤在同一条控制器上，
     * handler 返 STOP 时会挡掉后续触发，表现就是"只播得出来一次"。这里读挥击信号，
     * 并在<b>上升沿</b>把时间轴拉回 0 帧，保证每次挥击都从头播。
     * <p>
     * {@code attack} 有 3 秒，比一次挥击（零点几秒）长得多，所以进去之后按动画自己的时长播完
     * （{@link AnimationController#hasAnimationFinished()}），不会被打断在半路。
     */
    private static final class MainHandler implements AnimationController.AnimationStateHandler<LongYouClothItem> {

        private boolean wasAttacking;
        /** 一次 attack 是否还在播（挥击信号断了也要让动画走完） */
        private boolean attackPlaying;

        @Override
        public PlayState handle(AnimationTest<LongYouClothItem> test) {
            boolean attacking = test.getDataOrDefault(DataTickets.SWINGING_ARM, false);
            boolean newSwing = attacking && !wasAttacking;
            wasAttacking = attacking;

            if (newSwing) {
                attackPlaying = true;
                test.setAndContinue(ATTACK);
                test.controller().setAnimationTime(0.0D);
                return PlayState.CONTINUE;
            }
            if (attackPlaying) {
                if (!test.controller().hasAnimationFinished()) {
                    test.setAndContinue(ATTACK);
                    return PlayState.CONTINUE;
                }
                attackPlaying = false;
            }

            return test.setAndContinue(IDLE);
        }
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }
}
