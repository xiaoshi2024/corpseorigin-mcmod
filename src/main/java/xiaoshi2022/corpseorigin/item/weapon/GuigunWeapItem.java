package xiaoshi2022.corpseorigin.item.weapon;

import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * 鬼棍·人类的三节棍 —— 五六米长的三段棍身以铁环相连。
 * <p>
 * 三维模型由 Blockbench 的 Geo 模型直接绘制（{@code geckolib/models/item/guigun_weap.geo.json}）。
 * 模型默认姿态是<b>三节棍折叠态</b>（{@code group2}/{@code group3} 在 geo.json 里各自偏转 22.5°/-42.5°，
 * 呈现三节棍错开折叠的样子）。
 * <p>
 * 动画取自同名的 {@code geckolib/animations/item/guigun_weap.animation.json}：
 * <ul>
 *   <li>{@code call} —— 折叠态展开为直棍并保持的长棍态动画，由右键「切为长棍」触发；</li>
 *   <li>{@code fold} —— 长棍态收回折叠为三节棍并保持的三节棍态动画，由右键「切回三节棍」触发；</li>
 *   <li>{@code one} —— 命中敌人时末端两节翻转一周的甩棍表现，由 {@link #hurtEnemy} 触发。</li>
 * </ul>
 * <p>
 * 右键行为是<b>双向切换形态</b>（无消耗、无冷却）：三节棍态 ↔ 长棍态。
 * 原「棍术横扫」（{@code guigun_sweep}）技能改由技能轮盘释放，不再占用右键。
 */
public final class GuigunWeapItem extends Item implements GeoItem {

    private static final String CONTROLLER = "main";
    /** 折叠态 → 直棍并保持（长棍态） */
    private static final RawAnimation TO_LONG = RawAnimation.begin().thenPlayAndHold("call");
    /** 直棍 → 折叠态并保持（三节棍态） */
    private static final RawAnimation TO_SHORT = RawAnimation.begin().thenPlayAndHold("fold");
    /** 命中甩棍（一次性播完） */
    private static final RawAnimation SWING = RawAnimation.begin().thenPlay("one");

    /**
     * 服务端记录每个玩家手里这把三节棍的当前形态：{@code true}=长棍态，缺省/「false」=三节棍态。
     * <p>
     * 客户端不需要这份状态——{@link #triggerAnim} 已是同步触发，客户端跟着播对应动画并保持即可。
     * 玩家死亡/换角时不清除：下次右键的 toggle 与客户端显示可能短暂错位，
     * 但同步触发会立即把客户端矫正到正确形态。
     */
    private static final Map<UUID, Boolean> LONG_FORM = new HashMap<>();

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public GuigunWeapItem(Properties properties) {
        super(properties);
        GeoItem.registerSyncedAnimatable(this);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // 默认 PlayState.CONTINUE：不主动覆盖动画，让 triggerAnim 的 thenPlayAndHold 自然保持最终帧。
        // 模型默认姿态是三节棍折叠态（geo.json 里 group2/group3 各自偏转 22.5°/-42.5°）。
        controllers.add(new AnimationController<GuigunWeapItem>(CONTROLLER, 0, test -> PlayState.CONTINUE)
                .triggerableAnim("to_long", TO_LONG)
                .triggerableAnim("to_short", TO_SHORT)
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
     * 给「棍术横扫」这类<b>范围判定</b>的招式用：它们不会触发近战命中（{@link #hurtEnemy}），
     * 所以必须由技能显式叫一次。
     * <p>
     * 控制器名与触发名都留在这个类里，调用方不用记字符串。
     */
    public static void swing(ServerPlayer player, ItemStack stack, ServerLevel level) {
        if (stack.getItem() instanceof GuigunWeapItem weapon) {
            weapon.triggerAnim(player, GeoItem.getOrAssignId(stack, level), CONTROLLER, "swing");
        }
    }

    /**
     * 右键 = 双向切换形态（无消耗、无冷却）：
     * <ul>
     *   <li>三节棍态 → 播 {@code call} 展开为长棍并保持；</li>
     *   <li>长棍态 → 播 {@code fold} 收回为三节棍并保持。</li>
     * </ul>
     * {@code triggerAnim} 是同步动画（构造器里 {@link GeoItem#registerSyncedAnimatable}），
     * 客户端会自动跟着播。原「棍术横扫」改由技能轮盘释放，不再占用右键。
     */
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer server && level instanceof ServerLevel serverLevel) {
            boolean longForm = LONG_FORM.getOrDefault(player.getUUID(), false);
            longForm = !longForm;
            LONG_FORM.put(player.getUUID(), longForm);
            triggerAnim(server, GeoItem.getOrAssignId(player.getItemInHand(hand), serverLevel),
                    CONTROLLER, longForm ? "to_long" : "to_short");
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
        tooltip.accept(Component.translatable("item.corpseorigin.guigun_weap.toggle"));
    }
}
