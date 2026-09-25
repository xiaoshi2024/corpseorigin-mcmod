package xiaoshi2022.corpseorigin.item.weapon;

import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import org.jetbrains.annotations.Nullable;
import xiaoshi2022.corpseorigin.client.renderer.item.GuigunWeapRenderer;

import java.util.function.Consumer;

/**
 * 鬼棍·人类的三节棍 —— 五六米长的三段棍身以铁环相连。
 * <p>
 * 三维模型由 Blockbench 的 Geo 模型直接绘制（{@code geckolib/models/item/guigun_weap.geo.json}），
 * 动画取自同名的 {@code geckolib/animations/item/guigun_weap.animation.json}：
 * <ul>
 *   <li>{@code call} —— 三节棍身甩开并保持的展开态，作为默认表现只播一次；</li>
 *   <li>{@code one} —— 命中敌人时末端两节翻转一周的甩棍表现，由 {@link #hurtEnemy} 触发。</li>
 * </ul>
 */
public final class GuigunWeapItem extends Item implements GeoItem {

    private static final String CONTROLLER = "main";
    private static final RawAnimation UNFOLD = RawAnimation.begin().thenPlayAndHold("call");
    private static final RawAnimation SWING = RawAnimation.begin().thenPlay("one");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public GuigunWeapItem(Properties properties) {
        super(properties);
        GeoItem.registerSyncedAnimatable(this);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<GuigunWeapItem>(CONTROLLER, 0, test -> test.setAndContinue(UNFOLD))
                .triggerableAnim("swing", SWING));
    }

    /** 命中敌人时甩棍：播放一次 {@code one} 的翻转动作。 */
    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker.level() instanceof ServerLevel serverLevel) {
            triggerAnim(attacker, GeoItem.getOrAssignId(stack, serverLevel), CONTROLLER, "swing");
        }
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private GuigunWeapRenderer renderer;

            @Override
            public @Nullable GeoItemRenderer<GuigunWeapItem> getGeoItemRenderer() {
                if (renderer == null) renderer = new GuigunWeapRenderer();
                return renderer;
            }
        });
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
            Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.corpseorigin.guigun_weap.desc"));
    }
}
