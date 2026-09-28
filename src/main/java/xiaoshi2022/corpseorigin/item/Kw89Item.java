package xiaoshi2022.corpseorigin.item;

import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.model.DefaultedItemGeoModel;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.character.PlayerCharacterData;
import xiaoshi2022.corpseorigin.effect.SideEffect;
import xiaoshi2022.corpseorigin.event.EvolutionEventHandler;
import xiaoshi2022.corpseorigin.registry.ModDataAttachments;
import xiaoshi2022.corpseorigin.skill.EvolutionManager;

import java.util.function.Consumer;

/**
 * KW89 口服强化药剂 —— 直接饮用，不走注射那套「扎心」动作。
 * <p>
 * 效果取决于玩家<b>过去是否注射过黄色强化剂</b>（{@link ModDataAttachments#S_AGENT_INJECTED}，持久保存）：
 * <ul>
 *   <li>没注射过：常规强化 —— 永久最大生命 +30%，境界顶到人3；</li>
 *   <li>注射过：效果更强 —— 永久最大生命 +60%，境界顶到人4。</li>
 * </ul>
 * 无论哪种都会叠加 1 级 {@link SideEffect} 副作用（药剂本身依旧不稳定）。
 * <p>
 * 外观按 GeoItem 走 3D 模型（模型名 {@code kw89}）：右键开始喝时触发几何动画 {@code use}，
 * 喝的动作完全由该动画表现（不走原版饮酒动作）；{@link #USE_TICKS} 到点后才结算药效并消耗物品。
 * 模型 / 动画 / 贴图后续自行添加，动画时长请与 {@link #USE_TICKS} 对齐。
 */
public final class Kw89Item extends Item implements GeoItem {

    /** 「use」（喝下）动作时长：动画播完后才结算药效并消耗，需与动画文件长度一致 */
    private static final int USE_TICKS = 32;
    private static final RawAnimation USE = RawAnimation.begin().thenPlay("use");

    /** 常规强化（未曾注射过黄色强化剂）：最大生命 +30%，境界顶到人3 */
    private static final double NORMAL_HEALTH_BONUS = 0.30;
    private static final int NORMAL_TARGET_LEVEL = 3;
    /** 已注射过黄色强化剂：效果更强，最大生命 +60%，境界顶到人4 */
    private static final double STRONG_HEALTH_BONUS = 0.60;
    private static final int STRONG_TARGET_LEVEL = 4;

    /** 口服后永久附加的最大生命修饰符 id（死亡重生后随强化一起清掉） */
    public static final Identifier MODIFIER_KW89 = CorpseOrigin.id("kw89");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public Kw89Item(Properties properties) {
        super(properties);
        GeoItem.registerSyncedAnimatable(this);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        // 口服：不管饱不饱都能喝（这不是食物，不做 canEat 检查）
        player.startUsingItem(hand);
        // 喝的动作交给几何动画「use」；动画播完（原版 use 计时到点）才走 finishUsingItem 结算
        if (player instanceof ServerPlayer server && level instanceof ServerLevel serverLevel) {
            triggerAnim(server, GeoItem.getOrAssignId(player.getItemInHand(hand), serverLevel),
                    "use_controller", "use");
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide() && entity instanceof ServerPlayer server) {
            boolean injected = server.getAttachedOrCreate(ModDataAttachments.S_AGENT_INJECTED);
            applyEnhancement(server, injected);
            applyLevelBoost(server, injected);
            SideEffect.applySideEffect(server, 1);
            server.sendSystemMessage(Component.translatable(injected
                    ? "message.corpseorigin.kw89.text_02" : "message.corpseorigin.kw89.text_01"));
            level.playSound(null, server.getX(), server.getY(), server.getZ(),
                    SoundEvents.GENERIC_DRINK, SoundSource.NEUTRAL, 0.5F,
                    level.getRandom().nextFloat() * 0.1F + 0.9F);
        }
        // 创造模式不消耗；否则喝完即消失
        if (entity instanceof Player player && player.getAbilities().instabuild) return stack;
        return ItemStack.EMPTY;
    }

    /** 永久提高最大生命（同一修饰符只生效一次，避免反复口服无限叠血）。 */
    private void applyEnhancement(ServerPlayer player, boolean injected) {
        AttributeInstance instance = player.getAttribute(Attributes.MAX_HEALTH);
        if (instance == null || instance.getModifier(MODIFIER_KW89) != null) return;
        instance.addPermanentModifier(new AttributeModifier(MODIFIER_KW89,
                injected ? STRONG_HEALTH_BONUS : NORMAL_HEALTH_BONUS,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
    }

    /** 把进化点数补到目标境界门槛后走正规升级反馈（已在该境界之上就不再补点）。 */
    private void applyLevelBoost(ServerPlayer player, boolean injected) {
        int target = injected ? STRONG_TARGET_LEVEL : NORMAL_TARGET_LEVEL;
        PlayerCharacterData data = PlayerCharacterData.get(player);
        int delta = EvolutionManager.getThreshold(target) - data.getEarnedPoints(player.getUUID());
        if (delta <= 0) return;
        EvolutionEventHandler.awardPoints(player, delta);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return USE_TICKS;
    }

    /** 喝的动作完全交给几何动画 use，不用原版饮酒动作 */
    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.NONE;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
            Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("tooltip.corpseorigin.kw89"));
        tooltip.accept(Component.translatable("tooltip.corpseorigin.kw89.normal"));
        tooltip.accept(Component.translatable("tooltip.corpseorigin.kw89.injected"));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<Kw89Item>("use_controller", 0, test -> PlayState.CONTINUE)
                .triggerableAnim("use", USE));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private GeoItemRenderer<Kw89Item> renderer;

            @Override
            public GeoItemRenderer<Kw89Item> getGeoItemRenderer() {
                if (renderer == null)
                    renderer = new GeoItemRenderer<>(new DefaultedItemGeoModel<Kw89Item>(CorpseOrigin.id("kw89")));
                return renderer;
            }
        });
    }
}
