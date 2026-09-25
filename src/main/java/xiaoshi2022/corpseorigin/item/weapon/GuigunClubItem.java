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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import org.jetbrains.annotations.Nullable;
import xiaoshi2022.corpseorigin.client.renderer.item.GuigunClubRenderer;

import java.util.function.Consumer;

/**
 * 鬼棍·尸兄的棍棒 —— 布满骷髅头的次声波尸棍。
 * <p>
 * 三维模型由 Blockbench 的 Geo 模型直接绘制（{@code geckolib/models/item/guigun_club.geo.json}），
 * 附带的 {@code idle} 动画是骷髅头轻微起伏的待机表现。
 */
public final class GuigunClubItem extends Item implements GeoItem {

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public GuigunClubItem(Properties properties) {
        super(properties);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<GuigunClubItem>("main", 0, test -> test.setAndContinue(IDLE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private GuigunClubRenderer renderer;

            @Override
            public @Nullable GeoItemRenderer<GuigunClubItem> getGeoItemRenderer() {
                if (renderer == null) renderer = new GuigunClubRenderer();
                return renderer;
            }
        });
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
            Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.corpseorigin.guigun_club.desc"));
    }
}
