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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import xiaoshi2022.corpseorigin.client.renderer.item.GuigunClubRenderer;
import xiaoshi2022.corpseorigin.skill.chapter.RoleChapterSkill;

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

    /**
     * 手里握着尸棍右键 → 直接打出尸兄鬼棍的棍招（尸棍共振 / 尸棍重击），不必先在技能轮盘里选中。
     * <p>
     * 这两招绑同一把兵器，{@link RoleChapterSkill#castWithWeapon} 会放当前能放的那一招；
     * 「兵器 + 角色」都要匹配，所以人类鬼棍握着尸棍不会出招。
     */
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer serverPlayer
                && RoleChapterSkill.castWithWeapon(serverPlayer, serverPlayer.getItemInHand(hand))) {
            return InteractionResult.SUCCESS_SERVER;
        }
        return super.use(level, player, hand);
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
