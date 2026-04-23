package com.phagens.corpseorigin.Item;

import com.phagens.corpseorigin.client.Renderer.item.JuQueRenderer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Objects;
import java.util.function.Consumer;

public class JuQue extends SwordItem implements GeoItem {
    private static final RawAnimation IDLE = RawAnimation.begin().thenPlay("idle");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final String variant;

    public JuQue(Tier tier, int attackDamage, float attackSpeed, Properties properties) {
        this(tier, attackDamage, attackSpeed, properties, "base");
    }

    public JuQue(Tier tier, int attackDamage, float attackSpeed, Properties properties, String variant) {
        super(tier, properties.component(DataComponents.ATTRIBUTE_MODIFIERS, createAttributes(tier, attackDamage, attackSpeed)));
        this.variant = variant;
    }

    public String getVariant() {
        return variant;
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        // 根据变种调整效果
        float threshold = "tw".equals(variant) ? 0.4F : 0.3F; // 2阶变种阈值更高
        float slowChance = (float) ("tw".equals(variant) ? 0.3 : 0.2); // 2阶变种减速几率更高
        float damageChance = (float) ("tw".equals(variant) ? 0.3 : 0.2); // 2阶变种双倍伤害几率更高
        int slowDuration = "tw".equals(variant) ? 150 : 100; // 2阶变种减速持续时间更长
        int slowAmplifier = "tw".equals(variant) ? 3 : 2; // 2阶变种减速效果更强
        
        // 存活                  //最高生命              //最低生命
        if (target.isAlive() && target.getHealth() / target.getMaxHealth() <= threshold) {
            target.setHealth(0.0F);
        }

        if (Math.random() <= slowChance) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, slowDuration, slowAmplifier));
        }

        if (Math.random() <= damageChance) {
            float currentDamage = (float) Objects.requireNonNull(attacker.getAttribute(Attributes.ATTACK_DAMAGE)).getValue();
            // 2阶变种造成3倍伤害
            float multiplier = "tw".equals(variant) ? 3.0F : 2.0F;
            target.hurt(attacker.damageSources().generic(), currentDamage * multiplier);
        }

        return super.hurtEnemy(stack, target, attacker);
    }

    public static ItemAttributeModifiers createAttributes(Tier tier, int attackDamage, float attackSpeed) {
        return ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(
                                BASE_ATTACK_DAMAGE_ID,
                                (double) (attackDamage + tier.getAttackDamageBonus()),
                                AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.HAND)
                .add(Attributes.ATTACK_SPEED, new AttributeModifier(     //属性构造器
                                BASE_ATTACK_SPEED_ID,                   //属性标识符
                                (double) attackSpeed,                    //数值
                                AttributeModifier.Operation.ADD_VALUE),  //计算方式
                        EquipmentSlotGroup.HAND)                        //应用地
                .build();
    }

    // GeoItem 接口实现
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<JuQue>(this, "idle_controller", 0, this::predicate)
                .triggerableAnim("idle", IDLE));
    }

    private PlayState predicate(AnimationState<JuQue> state) {
        state.getController().setAnimation(IDLE);
        return PlayState.CONTINUE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private JuQueRenderer renderer;

            @Override
            public GeoItemRenderer<JuQue> getGeoItemRenderer() {
                if (this.renderer == null)
                    this.renderer = new JuQueRenderer();
                return this.renderer;
            }
        });
    }
    
    // ================= Curios 饰品支持 =================
    
    /**
     * 处理 Curios 相关的方法调用
     * 使用反射机制实现软依赖
     */
    public Object invokeCuriosMethod(String methodName, Object... args) {
        if (!com.phagens.corpseorigin.compat.curios.CuriosIntegration.isCuriosAvailable()) {
            return null;
        }
        
        try {
            com.phagens.corpseorigin.compat.curios.ReflectiveCurioItem curioItem = new com.phagens.corpseorigin.compat.curios.ReflectiveCurioItem(this);
            
            switch (methodName) {
                case "onEquip":
                    curioItem.onEquip(args[0], (net.minecraft.world.item.ItemStack) args[1], (net.minecraft.world.item.ItemStack) args[2]);
                    return null;
                case "onUnequip":
                    curioItem.onUnequip(args[0], (net.minecraft.world.item.ItemStack) args[1], (net.minecraft.world.item.ItemStack) args[2]);
                    return null;
                case "canEquip":
                    return curioItem.canEquip(args[0], (net.minecraft.world.item.ItemStack) args[1]);
                case "canUnequip":
                    return curioItem.canUnequip(args[0], (net.minecraft.world.item.ItemStack) args[1]);
                default:
                    return null;
            }
        } catch (Exception e) {
            return null;
        }
    }
}
