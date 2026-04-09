package com.phagens.corpseorigin.Item.YaoJi;

import com.phagens.corpseorigin.CorpseOrigin;
import com.phagens.corpseorigin.client.Renderer.item.SagentRenderer;
import com.phagens.corpseorigin.effect.SideEffect;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class Sagent extends Item implements GeoItem {
    private static final RawAnimation PRESS = RawAnimation.begin().thenPlay("press");
    private final List<AttributeData> attributeModifiers;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final String variant;

    // 存储正在等待动画完成的玩家和物品信息
    private static final ConcurrentHashMap<UUID, PendingUse> pendingUses = new ConcurrentHashMap<>();

    public Sagent(Properties properties) {
        this(properties, "null");
    }

    public Sagent(Properties properties, String variant) {
        super(properties);
        SingletonGeoAnimatable.registerSyncedAnimatable(this);
        this.attributeModifiers = new ArrayList<>();
        this.variant = variant;
    }

    public String getVariant() {
        return variant;
    }

    // 从物品栈中获取变种
    public static String getVariantFromItem(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag = customData != null ? customData.copyTag() : new CompoundTag();
        if (tag.contains("Variant")) {
            return tag.getString("Variant");
        }
        if (stack.getItem() instanceof Sagent sagent) {
            return sagent.getVariant();
        }
        return "null";
    }

    // 将变种设置到物品栈中
    public static void setVariantToItem(ItemStack stack, String variant) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag = customData != null ? customData.copyTag() : new CompoundTag();
        tag.putString("Variant", variant);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static class AttributeData {
        public final Holder<Attribute> attribute;
        public final AttributeModifier.Operation operation;
        public final double amount;
        public final String name;
        private final ResourceLocation modifierId;

        public AttributeData(Holder<Attribute> attribute, AttributeModifier.Operation operation, double amount, String name) {
            this.attribute = attribute;
            this.operation = operation;
            this.amount = amount;
            this.name = name;
            this.modifierId = ResourceLocation.fromNamespaceAndPath(CorpseOrigin.MODID, name);
        }
    }

    public Sagent addAttributeModifier(Holder<Attribute> attribute, AttributeModifier.Operation operation, double amount, String name) {
        this.attributeModifiers.add(new AttributeData(attribute, operation, amount, name));
        return this;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack itemStack = player.getItemInHand(usedHand);

        // 检查物品是否为 NULL_S_AGENT 实例
        if (itemStack.getItem() == com.phagens.corpseorigin.register.Moditems.NULL_S_AGENT.get()) {
            return InteractionResultHolder.fail(itemStack);
        }

        // 检查物品的变种，如果是 null 变种则不能使用
        String currentVariant = getVariantFromItem(itemStack);
        if (currentVariant.equals("null")) {
            return InteractionResultHolder.fail(itemStack);
        }

        // 客户端：触发动画
        if (level.isClientSide) {
            return InteractionResultHolder.sidedSuccess(itemStack, true);
        }

        // 服务器端
        if (level instanceof ServerLevel serverLevel) {
            // 检查是否已经在使用中
            if (pendingUses.containsKey(player.getUUID())) {
                return InteractionResultHolder.fail(itemStack);
            }

            try {
                int itemId = (int) GeoItem.getOrAssignId(itemStack, serverLevel);

                // 保存当前要使用的信息
                PendingUse pending = new PendingUse(player, usedHand, itemStack, currentVariant);
                pendingUses.put(player.getUUID(), pending);

                // 触发动画，并设置一个定时器在动画结束后执行替换
                triggerAnim(player, itemId, "press_controller", "press");

                // 应用效果（不延迟）
                appAttrid(player);
                saveModifiersToPlayerData(player);

                if (currentVariant.equals("yellow")) {
                    SideEffect.applySideEffect(player, 1);
                    CorpseOrigin.LOGGER.info("玩家 {} 使用了黄色强化剂，获得副作用", player.getName().getString());
                }

                if (currentVariant.equals("blue")) {
                    SideEffect.clearSideEffect(player);
                    CorpseOrigin.LOGGER.info("玩家 {} 使用了蓝色中和剂", player.getName().getString());
                }

                // 使用调度器延迟执行物品替换
                int animationTicks = getDefaultAnimationSpeed();
                serverLevel.getServer().execute(() -> {
                    // 等待动画时长后执行
                    new Thread(() -> {
                        try {
                            Thread.sleep(animationTicks * 50);
                            serverLevel.getServer().execute(() -> {
                                replaceWithNullAgent(pending);
                            });
                        } catch (InterruptedException e) {
                            CorpseOrigin.LOGGER.error("动画延迟被中断", e);
                            serverLevel.getServer().execute(() -> {
                                replaceWithNullAgent(pending);
                            });
                        }
                    }).start();
                });

            } catch (Exception e) {
                CorpseOrigin.LOGGER.error("触发动画失败: {}", e.getMessage());
                // 清除等待状态并立即替换
                pendingUses.remove(player.getUUID());
                if (!player.isCreative()) {
                    ItemStack nullAgentStack = new ItemStack(com.phagens.corpseorigin.register.Moditems.NULL_S_AGENT.get());
                    player.setItemInHand(usedHand, nullAgentStack);
                }
            }
        }

        return InteractionResultHolder.sidedSuccess(itemStack, false);
    }

    private void replaceWithNullAgent(PendingUse pending) {
        Player player = pending.player;
        InteractionHand usedHand = pending.hand;
        ItemStack originalStack = pending.stack;

        // 清除等待状态
        pendingUses.remove(player.getUUID());

        // 检查玩家是否还在线且物品未改变
        if (player.isAlive() && !player.isRemoved()) {
            ItemStack currentStack = player.getItemInHand(usedHand);
            // 检查手中是否还是原来的药剂（防止在动画期间切换了物品）
            if (currentStack == originalStack && currentStack.getItem() instanceof Sagent) {
                if (!player.isCreative()) {
                    ItemStack nullAgentStack = new ItemStack(com.phagens.corpseorigin.register.Moditems.NULL_S_AGENT.get());
                    player.setItemInHand(usedHand, nullAgentStack);
                    CorpseOrigin.LOGGER.info("动画播放完毕，为玩家 {} 替换物品为 NULL_S_AGENT", player.getName().getString());
                }
            }
        }
    }

    // 内部类，存储待处理的药剂使用信息
    private static class PendingUse {
        final Player player;
        final InteractionHand hand;
        final ItemStack stack;
        final String variant;

        PendingUse(Player player, InteractionHand hand, ItemStack stack, String variant) {
            this.player = player;
            this.hand = hand;
            this.stack = stack;
            this.variant = variant;
        }
    }

    private int getAnimationDuration() {
        return getDefaultAnimationSpeed() * 50;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        AnimationController<Sagent> controller = new AnimationController<Sagent>(this, "press_controller", 0, this::predicate);
        controller.triggerableAnim("press", PRESS);
        controllers.add(controller);
    }

    private PlayState predicate(AnimationState<Sagent> state) {
        return PlayState.CONTINUE;
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private SagentRenderer renderer;

            @Override
            public GeoItemRenderer<Sagent> getGeoItemRenderer() {
                if (this.renderer == null)
                    this.renderer = new SagentRenderer();
                return this.renderer;
            }
        });
    }

    public int getDefaultAnimationSpeed() {
        return 40; // 40 ticks = 2秒
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);

        String currentVariant = getVariantFromItem(stack);

        if (currentVariant.equals("yellow")) {
            tooltipComponents.add(Component.translatable("tooltip.corpseorigin.s_agent"));
            tooltipComponents.add(Component.translatable("tooltip.corpseorigin.s_agent.warning"));
        }

        if (currentVariant.equals("blue")) {
            tooltipComponents.add(Component.translatable("tooltip.corpseorigin.blue_s_agent"));
        }

        if (currentVariant.equals("null")) {
            tooltipComponents.add(Component.translatable("tooltip.corpseorigin.null_s_agent"));
        }
    }

    private void appAttrid(Player player){
        for (AttributeData attributeData : attributeModifiers){
            AttributeInstance attributeInstance = player.getAttribute(attributeData.attribute);
            if (attributeInstance != null){
                if (attributeInstance.getModifier(attributeData.modifierId) == null) {
                    AttributeModifier modifier = new AttributeModifier(attributeData.modifierId, attributeData.amount, attributeData.operation);
                    attributeInstance.addTransientModifier(modifier);
                }
            }
        }
    }

    private void saveModifiersToPlayerData(Player player) {
        CompoundTag playerData = player.getPersistentData();
        ListTag modifiersList = new ListTag();
        for (AttributeData data : attributeModifiers) {
            CompoundTag modifierTag = new CompoundTag();
            modifierTag.putString("AttributeName", data.attribute.unwrapKey().orElseThrow().location().toString());
            modifierTag.putString("ModifierId", data.modifierId.toString());
            modifierTag.putString("ModifierName", data.name);
            modifiersList.add(modifierTag);
        }
        playerData.put("CorpseOrigin_Attributes", modifiersList);
    }

    public static void removeAllPlayerAttributes(Player player) {
        CompoundTag playerData = player.getPersistentData();

        if (playerData.contains("CorpseOrigin_Attributes")) {
            ListTag modifiersList = playerData.getList("CorpseOrigin_Attributes", 10);

            for (int i = 0; i < modifiersList.size(); i++) {
                CompoundTag modifierTag = modifiersList.getCompound(i);
                String modifierIdStr = modifierTag.getString("ModifierId");
                ResourceLocation modifierId = ResourceLocation.tryParse(modifierIdStr);

                if (modifierId != null) {
                    removeModifierFromAllAttributes(player, modifierId);
                }
            }

            playerData.remove("CorpseOrigin_Attributes");
        }
    }

    private static void removeModifierFromAllAttributes(Player player, ResourceLocation modifierId) {
        tryRemoveModifier(player, net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH, modifierId);
        tryRemoveModifier(player, net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED, modifierId);
        tryRemoveModifier(player, net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE, modifierId);
        tryRemoveModifier(player, net.minecraft.world.entity.ai.attributes.Attributes.ARMOR, modifierId);
        tryRemoveModifier(player, net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE, modifierId);
    }

    private static void tryRemoveModifier(Player player, Holder<Attribute> attribute, ResourceLocation modifierId) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance != null) {
            AttributeModifier modifier = instance.getModifier(modifierId);
            if (modifier != null) {
                instance.removeModifier(modifier);
            }
        }
    }
}