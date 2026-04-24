package com.phagens.corpseorigin.Item;

import com.phagens.corpseorigin.client.Renderer.item.JuQueRenderer;
import com.phagens.corpseorigin.network.JuQueBeamPacket;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
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

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        
        if (player.isShiftKeyDown()) {
            // 潜行右键释放剑气
            if (!level.isClientSide()) {
                // 服务端直接处理
                releaseBeam(player, stack);
            } else {
                // 客户端发送网络包
                PacketDistributor.sendToServer(new JuQueBeamPacket());
            }
            
            player.swing(hand);
            return InteractionResultHolder.success(stack);
        }
        
        return super.use(level, player, hand);
    }

    private void releaseBeam(Player player, ItemStack stack) {
        // 检查冷却时间
        if (player.getCooldowns().isOnCooldown(this)) {
            return;
        }
        
        // 消耗耐久度
        // 改为：
        stack.hurtAndBreak(1, player,
                player.getUsedItemHand() == InteractionHand.MAIN_HAND ?
                        EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        
        // 播放音效
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_SWEEP, 
                net.minecraft.sounds.SoundSource.PLAYERS, 0.4F, 0.5F);
        
        // 计算伤害
        float baseDamage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        float damage = baseDamage * getDamageMultiplier(this.variant);
        
        // 创建剑气实体
        com.phagens.corpseorigin.entity.JuQueBeamEntity beam = new com.phagens.corpseorigin.entity.JuQueBeamEntity(player.level(), player);
        beam.setDamage(damage);
        beam.setLevel(getBeamLevel(this.variant));
        
        // 设置发射方向和速度
        beam.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, beam.getVelocity(), 1.0F);
        player.level().addFreshEntity(beam);
        
        // 设置冷却时间
        int cooldown = getCooldown(this.variant);
        player.getCooldowns().addCooldown(this, cooldown);
    }

    private float getDamageMultiplier(String variant) {
        return "tw".equals(variant) ? 0.8F : 0.6F; // 2阶变种伤害更高
    }
    
    private int getBeamLevel(String variant) {
        return "tw".equals(variant) ? 2 : 1; // 2阶变种剑气等级更高
    }
    
    private int getCooldown(String variant) {
        return "tw".equals(variant) ? 60 : 80; // 2阶变种冷却时间更短
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
