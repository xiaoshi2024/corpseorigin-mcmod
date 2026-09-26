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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import xiaoshi2022.corpseorigin.client.renderer.item.GuigunWeapRenderer;
import xiaoshi2022.corpseorigin.skill.chapter.RoleChapterSkill;

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

    /**
     * 让手里这把三节棍放一遍甩棍动画（{@code one}）。
     * <p>
     * 给「棍术横扫」这类<b>范围判定</b>的招式用：它们不会触发近战命中，
     * 所以走不到上面那条 {@link #hurtEnemy}，必须由技能显式叫一次。
     * <p>
     * 控制器名与触发名都留在这个类里，调用方不用记字符串。
     */
    public static void swing(ServerPlayer player, ItemStack stack, ServerLevel level) {
        if (stack.getItem() instanceof GuigunWeapItem weapon) {
            weapon.triggerAnim(player, GeoItem.getOrAssignId(stack, level), CONTROLLER, "swing");
        }
    }

    /**
     * 手里握着三节棍右键 → 直接打出「棍术横扫」，不必先在技能轮盘里选中。
     * <p>
     * 「兵器 + 角色」的匹配与判定都在 {@link RoleChapterSkill#castWithWeapon} 里，
     * 所以角色不符（例如人类鬼棍握着尸棍）时这里不会出招，右键回到原本的行为。
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
