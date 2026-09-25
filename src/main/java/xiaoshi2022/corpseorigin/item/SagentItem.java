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
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.util.GeckoLibUtil;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.character.PlayerCharacterData;
import xiaoshi2022.corpseorigin.effect.SideEffect;
import xiaoshi2022.corpseorigin.event.EvolutionEventHandler;
import xiaoshi2022.corpseorigin.registry.ModItems;
import xiaoshi2022.corpseorigin.skill.EvolutionManager;
import xiaoshi2022.corpseorigin.skill.chapter.ChapterScenes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * 黑色火线的强化药剂 —— 插入心脏注射，2 秒「press」动作期间不可再次使用。
 * <p>
 * 三种形态共用一个 Geo 模型，只换贴图（{@code variant} 决定用哪张）：
 * <ul>
 *   <li>{@link #YELLOW} 黄色强化剂：注射后永久提高最大生命，但会叠加 {@link SideEffect} 副作用；</li>
 *   <li>{@link #BLUE} 蓝色中和剂：只中和副作用，不加属性；</li>
 *   <li>{@link #EMPTY} 空药剂：注射完留下的空瓶，不能再注射。</li>
 * </ul>
 * 注射动作与旧版一致：服务端触发几何动画 → 记下待替换的药剂 → 动画播完后换成空瓶。
 * （旧版用裸线程 sleep 等待动画，这里改为随服务器 tick 结算，避免线程安全问题。）
 */
public final class SagentItem extends Item implements GeoItem {

    public static final String YELLOW = "yellow";
    public static final String BLUE = "blue";
    public static final String EMPTY = "null";

    /** 「press」（插入心脏）动作时长：40 tick = 2 秒，与动画文件长度一致 */
    private static final int PRESS_TICKS = 40;
    /** 注射后施加的属性修饰符 id */
    public static final Identifier MODIFIER_YELLOW = CorpseOrigin.id("s_agent_yellow");
    public static final Identifier MODIFIER_EMPTY = CorpseOrigin.id("s_agent_empty");

    private static final RawAnimation PRESS = RawAnimation.begin().thenPlay("press");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final String variant;
    private final List<AttributeData> attributeModifiers = new ArrayList<>();
    /** 注射后按权重随机顶到的目标等级；黄色强化剂是「人3 或 人4」的赌注 */
    private final List<Boost> boosts = new ArrayList<>();

    /** 正在注射的玩家：动画播完后把药剂换成空瓶 */
    private record Pending(ServerPlayer player, InteractionHand hand, ItemStack stack, int replaceAtTick) {}
    private static final Map<UUID, Pending> PENDING = new HashMap<>();

    public SagentItem(Properties properties, String variant) {
        super(properties);
        GeoItem.registerSyncedAnimatable(this);
        this.variant = variant;
    }

    public String variant() {
        return variant;
    }

    /** 注射后要永久附加的属性。 */
    public SagentItem addAttributeModifier(net.minecraft.core.Holder<Attribute> attribute,
            AttributeModifier.Operation operation, double amount, Identifier modifierId) {
        attributeModifiers.add(new AttributeData(attribute, operation, amount, modifierId));
        return this;
    }

    /**
     * 登记一个注射结果：{@code weight} 份概率把进化等级顶到 {@code level}。
     * <p>
     * 多次调用即为一组赌注，例如 {@code promoteChance(3, 50).promoteChance(4, 50)} 就是各半地
     * 冲上人3 / 人4。补点数的方式走正规升级流程，玩家能直接看到自己抽到了哪一级。
     */
    public SagentItem promoteChance(int level, int weight) {
        if (level > 1 && weight > 0) boosts.add(new Boost(level, weight));
        return this;
    }

    private record Boost(int level, int weight) {}

    private record AttributeData(net.minecraft.core.Holder<Attribute> attribute,
            AttributeModifier.Operation operation, double amount, Identifier modifierId) {}

    /** 在服务器初始化器里挂上动画收尾与死亡清理。 */
    public static void register() {
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> PENDING.clear());
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (PENDING.isEmpty()) return;
            var iterator = PENDING.entrySet().iterator();
            while (iterator.hasNext()) {
                Pending pending = iterator.next().getValue();
                if (pending.player().tickCount < pending.replaceAtTick()) continue;
                iterator.remove();
                replaceWithEmptyVial(pending);
            }
        });
        // 强化属性会在死亡重生后清掉（对应旧版 playerDie 里的 removeAllPlayerAttributes）
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> clearEnhancement(newPlayer));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        // 空药剂不能再注射
        if (EMPTY.equals(variant)) return InteractionResult.FAIL;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer server) || !(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.FAIL;
        }
        // 上一针还没打完，不能连打
        if (PENDING.containsKey(server.getUUID())) return InteractionResult.FAIL;

        PENDING.put(server.getUUID(), new Pending(server, hand, stack, server.tickCount + PRESS_TICKS));
        triggerAnim(server, GeoItem.getOrAssignId(stack, serverLevel), "press_controller", "press");
        // 玩家本体也摆出「扎心」姿态：双手内扣按住胸口把针压进心脏，与物品动画同一段时长
        ChapterScenes.action(server, "s_agent_press", PRESS_TICKS);

        // 药效立刻结算，只有物品替换等动画播完
        applyEnhancement(server);
        applyLevelBoost(server);
        if (YELLOW.equals(variant)) SideEffect.applySideEffect(server, 1);
        if (BLUE.equals(variant)) SideEffect.clearSideEffect(server);

        CorpseOrigin.LOGGER.info("玩家 {} 注射了 {} 强化剂", server.getName().getString(), variant);
        return InteractionResult.SUCCESS;
    }

    /** 把药剂记下的属性永久加到玩家身上（已有同 id 修饰符则不重复叠加）。 */
    private void applyEnhancement(ServerPlayer player) {
        for (AttributeData data : attributeModifiers) {
            AttributeInstance instance = player.getAttribute(data.attribute());
            if (instance == null || instance.getModifier(data.modifierId()) != null) continue;
            instance.addPermanentModifier(new AttributeModifier(data.modifierId(), data.amount(), data.operation()));
        }
    }

    /**
     * 按权重抽一个目标等级，把进化点数补到该级门槛后走正规升级反馈。
     * <p>
     * 药效本身是不稳定的：同一个目标等级已经在身后（比如已经是人4）时不再补点，
     * 这一针就只剩副作用了。
     */
    private void applyLevelBoost(ServerPlayer player) {
        int total = 0;
        for (Boost boost : boosts) total += boost.weight();
        if (total <= 0) return;
        int roll = player.getRandom().nextInt(total);
        int target = boosts.get(boosts.size() - 1).level();
        for (Boost boost : boosts) {
            roll -= boost.weight();
            if (roll < 0) {
                target = boost.level();
                break;
            }
        }
        PlayerCharacterData data = PlayerCharacterData.get(player);
        int delta = EvolutionManager.getThreshold(target) - data.getEarnedPoints(player.getUUID());
        if (delta <= 0) return;
        EvolutionEventHandler.awardPoints(player, delta);
    }

    /** 死亡重生后清掉强化剂给的属性（进化等级属于永久成长，不在这里回收）。 */
    public static void clearEnhancement(ServerPlayer player) {
        for (Identifier id : List.of(MODIFIER_YELLOW, MODIFIER_EMPTY)) {
            AttributeInstance instance = player.getAttribute(Attributes.MAX_HEALTH);
            if (instance == null) continue;
            AttributeModifier modifier = instance.getModifier(id);
            if (modifier != null) instance.removeModifier(modifier);
        }
    }

    /** 动画播完：手里的药剂变成空瓶（创造模式不消耗）。 */
    private static void replaceWithEmptyVial(Pending pending) {
        ServerPlayer player = pending.player();
        if (!player.isAlive() || player.isRemoved() || player.isCreative()) return;
        // 动画期间换了手持物品就不替换，免得把别的东西吃掉
        if (player.getItemInHand(pending.hand()) != pending.stack()) return;
        if (!(pending.stack().getItem() instanceof SagentItem)) return;
        player.setItemInHand(pending.hand(), new ItemStack(ModItems.NULL_S_AGENT));
    }

    /** 注射动作完全交给几何动画（press），不走原版的饮用/举臂动作。 */
    @Override
    public net.minecraft.world.item.ItemUseAnimation getUseAnimation(ItemStack stack) {
        return net.minecraft.world.item.ItemUseAnimation.NONE;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
            Consumer<Component> tooltip, TooltipFlag flag) {
        switch (variant) {
            case YELLOW -> {
                tooltip.accept(Component.translatable("tooltip.corpseorigin.s_agent"));
                tooltip.accept(Component.translatable("tooltip.corpseorigin.s_agent.level"));
                tooltip.accept(Component.translatable("tooltip.corpseorigin.s_agent.warning"));
            }
            case BLUE -> tooltip.accept(Component.translatable("tooltip.corpseorigin.blue_s_agent"));
            default -> tooltip.accept(Component.translatable("tooltip.corpseorigin.null_s_agent"));
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<SagentItem>("press_controller", 0, test -> PlayState.CONTINUE)
                .triggerableAnim("press", PRESS));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private GeoItemRenderer<SagentItem> renderer;

            @Override
            public GeoItemRenderer<SagentItem> getGeoItemRenderer() {
                if (renderer == null) renderer = new GeoItemRenderer<>(new DefaultedItemGeoModel<SagentItem>(CorpseOrigin.id("s_agent")) {
                    @Override
                    public Identifier getTextureResource(GeoRenderState state) {
                        // 三个变种共用同一套模型，贴图跟着物品走
                        Item current = state.hasGeckolibData(GeoItemRenderer.CURRENT_ITEM)
                                ? state.getGeckolibData(GeoItemRenderer.CURRENT_ITEM) : null;
                        String which = current instanceof SagentItem item ? item.variant() : variant;
                        return CorpseOrigin.id("textures/item/" + which + "_s_agent.png");
                    }
                });
                return renderer;
            }
        });
    }
}
