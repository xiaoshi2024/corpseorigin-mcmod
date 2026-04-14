package com.phagens.corpseorigin.Item.Organic;

import com.phagens.corpseorigin.player.PlayerCorpseData;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;

/**
 * 尸兄器官基础类
 * 器官是尸兄玩家的食物，食用后可以获得进化能力
 */
public abstract class ZbOrganItem extends Item implements GeoItem {
    
    private static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("idle");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    
    // 器官类型
    protected final OrganType organType;
    // 进化概率 (0.0 - 1.0)
    protected final float evolutionChance;
    // 需要食用的数量才能进化
    protected final int requiredAmount;
    // 饥饿值恢复
    protected final int nutrition;
    // 饱和度恢复
    protected final float saturation;
    
    public enum OrganType {
        EYE("eye", "眼睛"),
        HEART("heart", "心脏"),
        LUNG("lung", "肺"),
        LIVER("liver", "肝脏"),
        KIDNEY("kidney", "肾脏"),
        BRAIN("brain", "大脑"),
        MUSCLE("muscle", "肌肉"),
        BONE("bone", "骨骼"),
        TENTACLE("tentacle", "触手"),
        WING("wing", "翅膀");
        
        private final String id;
        private final String displayName;
        
        OrganType(String id, String displayName) {
            this.id = id;
            this.displayName = displayName;
        }
        
        public String getId() { return id; }
        public String getDisplayName() { return displayName; }
    }
    
    public ZbOrganItem(OrganType organType, float evolutionChance, int requiredAmount, int nutrition, float saturation) {
        super(new Item.Properties()
                .food(new FoodProperties.Builder()
                        .nutrition(nutrition)
                        .saturationModifier(saturation)
                        .alwaysEdible()
                        .build()));
        
        this.organType = organType;
        this.evolutionChance = evolutionChance;
        this.requiredAmount = requiredAmount;
        this.nutrition = nutrition;
        this.saturation = saturation;
    }
    
    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide && entity instanceof Player player) {
            // 只有尸兄玩家才能消化器官
            if (PlayerCorpseData.isCorpse(player)) {
                onOrganConsumed(player, stack);
            } else {
                // 普通玩家食用会受到惩罚
                onNormalPlayerConsume(player, stack);
            }
        }
        return super.finishUsingItem(stack, level, entity);
    }
    
    /**
     * 尸兄玩家食用器官时的处理
     */
    protected abstract void onOrganConsumed(Player player, ItemStack stack);
    
    /**
     * 普通玩家食用器官时的惩罚
     */
    protected void onNormalPlayerConsume(Player player, ItemStack stack) {
        player.sendSystemMessage(Component.translatable("item.corpseorigin.organ.not_corpse_warning"));
        // 给予中毒效果
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.POISON, 200, 1));
    }
    
    /**
     * 尝试进化
     * @return 是否成功进化
     */
    protected boolean tryEvolve(Player player) {
        if (player.getRandom().nextFloat() < evolutionChance) {
            return applyEvolution(player);
        }
        return false;
    }
    
    /**
     * 应用进化效果，子类实现具体的进化逻辑
     * @return 是否成功应用进化
     */
    protected abstract boolean applyEvolution(Player player);
    
    /**
     * 获取玩家已食用此器官的数量
     */
    protected int getConsumedCount(Player player) {
        String key = "organ_consumed_" + organType.getId();
        return PlayerCorpseData.getCorpseData(player).getInt(key);
    }
    
    /**
     * 增加玩家食用此器官的计数
     */
    protected void incrementConsumedCount(Player player) {
        String key = "organ_consumed_" + organType.getId();
        net.minecraft.nbt.CompoundTag data = PlayerCorpseData.getCorpseData(player);
        int current = data.getInt(key);
        data.putInt(key, current + 1);
        player.setData(com.phagens.corpseorigin.player.CorpsePlayerAttachment.CORPSE_DATA, data);
    }
    
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        
        tooltipComponents.add(Component.translatable("item.corpseorigin.organ.type", organType.getDisplayName()));
        tooltipComponents.add(Component.translatable("item.corpseorigin.organ.evolution_chance", (int)(evolutionChance * 100)));
        tooltipComponents.add(Component.translatable("item.corpseorigin.organ.required_amount", requiredAmount));
        tooltipComponents.add(Component.empty());
        tooltipComponents.add(Component.translatable("item.corpseorigin.organ.corpse_only").withStyle(net.minecraft.ChatFormatting.RED));
        
        // 添加具体效果描述
        addEffectDescription(tooltipComponents);
    }
    
    /**
     * 添加具体效果描述，子类实现
     */
    protected abstract void addEffectDescription(List<Component> tooltip);
    
    // GeoItem 接口实现
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "idle_controller", 0, state -> {
            state.getController().setAnimation(IDLE_ANIM);
            return PlayState.CONTINUE;
        }));
    }
    
    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
    
    public OrganType getOrganType() {
        return organType;
    }
    
    public float getEvolutionChance() {
        return evolutionChance;
    }
    
    public int getRequiredAmount() {
        return requiredAmount;
    }
}
