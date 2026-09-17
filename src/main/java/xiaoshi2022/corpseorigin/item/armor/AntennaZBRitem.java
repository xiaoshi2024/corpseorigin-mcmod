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
import xiaoshi2022.corpseorigin.client.renderer.armor.AntennaArmorRenderData;
import xiaoshi2022.corpseorigin.client.renderer.armor.AntennaZBRitemRenderer;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 天线宝宝尸兄盔甲（头盔 / 胸甲 / 护腿共用）。
 * <p>
 * 动画三条：{@code idle} 常驻，{@code special_attack} 由穿戴者挥击驱动
 * （GeckoLib 的 {@link DataTickets#SWINGING_ARM}），{@code absorb} 由吸食状态驱动
 * （{@link AntennaArmorRenderData#ABSORBING}）。两个信号都由
 * {@code AntennaZBRitemRenderer} 在填充盔甲 render state 时写进去。
 */
public class AntennaZBRitem extends Item implements GeoItem {

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation SPECIAL_ATTACK = RawAnimation.begin().thenPlay("special_attack");
    private static final RawAnimation ABSORB = RawAnimation.begin().thenPlay("absorb");

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    public AntennaZBRitem(ArmorMaterial material, ArmorType type, Properties properties) {
        super(properties.humanoidArmor(material, type));
        GeoItem.registerSyncedAnimatable(this);
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private final Supplier<AntennaZBRitemRenderer<?>> renderer = Suppliers.memoize(AntennaZBRitemRenderer::new);

            @Nullable
            @Override
            public GeoArmorRenderer<?, ?> getGeoArmorRenderer(ItemStack itemStack, EquipmentSlot equipmentSlot) {
                return this.renderer.get();
            }
        });
    }

    @Override
    public void registerControllers(final AnimatableManager.ControllerRegistrar controllers) {
        // ⚠️ 三条动画必须挤在<b>同一条</b>控制器里。
        // GeckoLib 是按骨骼通道逐条动画往上盖的：分成多条控制器的话，某条 clip 里没写到的骨骼
        // 会保留另一条 clip 的姿态 —— absorb 只写了 bone2/3/5/6，而 idle 把 bone2~bone11 全摆过，
        // 于是游戏里播 absorb 时上半截天线还是 idle 那套折叠姿势，跟 Blockbench 里单播这条 clip
        // 完全不是一个形状（"插脑门"的直刺就出不来）。
        controllers.add(new AnimationController<AntennaZBRitem>("main", 2, new MainHandler()));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }

    /**
     * idle / special_attack / absorb 三选一的控制器。
     * <p>
     * 刻意不用 {@code triggerableAnim}：触发通道和 handler 的返回值挤在同一条控制器上，
     * handler 返 STOP 时会挡掉后续触发，表现就是"只播得出来一次"。这里改成读 render state
     * 上的信号，并在信号的<b>上升沿</b>把时间轴拉回 0 帧，保证每次挥击 / 每次吸食都从头播。
     * <p>
     * 挥击信号本身只有零点几秒，而 special_attack 有 1.25 秒，所以进入攻击后按动画自己的
     * 时长播完（{@link AnimationController#hasAnimationFinished()}），不会被打断在半路。
     * <p>
     * 几个"上一次在不在播"的字段放在这里（而不是物品类上）是有意的：
     * {@code registerControllers} 每个动画实例调一次，每份盔甲 ItemStack 各有一套控制器，
     * 所以不会把别人的状态串到自己身上。
     */
    private static final class MainHandler implements AnimationController.AnimationStateHandler<AntennaZBRitem> {

        private boolean wasAttacking;
        private boolean wasAbsorbing;
        /** 一次 special_attack 是否还在播（挥击信号断了也要让动画走完） */
        private boolean attackPlaying;

        @Override
        public PlayState handle(AnimationTest<AntennaZBRitem> test) {
            // 吸食优先：吸食期间不管手上在不在挥，只播 absorb（动画本身是"伸长直插脑门"那条）
            if (test.getDataOrDefault(AntennaArmorRenderData.ABSORBING, false)) {
                wasAttacking = false;
                attackPlaying = false;
                playFromStart(test, ABSORB, wasAbsorbing);
                wasAbsorbing = true;
                return PlayState.CONTINUE;
            }
            wasAbsorbing = false;

            // 挥击沿用 GeckoLib 的 SWINGING_ARM（渲染器里从穿戴者的 swinging 填进去）
            boolean attacking = test.getDataOrDefault(DataTickets.SWINGING_ARM, false);
            boolean newSwing = attacking && !wasAttacking;
            wasAttacking = attacking;

            if (newSwing) {
                attackPlaying = true;
                playFromStart(test, SPECIAL_ATTACK, false);
                return PlayState.CONTINUE;
            }
            if (attackPlaying) {
                if (!test.controller().hasAnimationFinished()) {
                    test.setAndContinue(SPECIAL_ATTACK);
                    return PlayState.CONTINUE;
                }
                attackPlaying = false;
            }

            // 其余时间常驻 idle
            return test.setAndContinue(IDLE);
        }

        /** 只在重新开始的瞬间把时间轴拉回 0 帧，否则同一次挥击会被每帧重播 */
        private static void playFromStart(AnimationTest<AntennaZBRitem> test,
                                          RawAnimation animation, boolean alreadyPlaying) {
            test.setAndContinue(animation);
            if (!alreadyPlaying) {
                test.controller().setAnimationTime(0.0D);
            }
        }
    }
}
