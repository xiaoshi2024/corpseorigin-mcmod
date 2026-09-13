package xiaoshi2022.corpseorigin.item.sword;

import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.util.GeckoLibUtil;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import xiaoshi2022.corpseorigin.client.renderer.item.JuQueRenderer;
import xiaoshi2022.corpseorigin.entity.FlyingGreatSwordEntity;
import xiaoshi2022.corpseorigin.entity.JuQueBeamEntity;
import xiaoshi2022.corpseorigin.network.JuQueBeamPacket;
import xiaoshi2022.corpseorigin.registry.ModDataAttachments;
import xiaoshi2022.corpseorigin.registry.ModEntities;
import xiaoshi2022.corpseorigin.skill.baixiaofei.AncientPoetrySwordSkill;
import xiaoshi2022.corpseorigin.skill.baixiaofei.aps.APSTerrainManager;

import java.util.Objects;
import java.util.function.Consumer;

public class JuQue extends Item implements GeoItem {

    private static final RawAnimation IDLE = RawAnimation.begin().thenPlay("idle");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    // 2阶巨阙参数
    private static final float THRESHOLD = 0.4F;
    private static final float SLOW_CHANCE = 0.3F;
    private static final float DAMAGE_CHANCE = 0.3F;
    private static final int SLOW_DURATION = 150;
    private static final int SLOW_AMPLIFIER = 3;
    private static final float MULTIPLIER = 3.0F;
    public static final float BEAM_DAMAGE_MULT = 0.8F;
    public static final int BEAM_LEVEL = 2;
    public static final int COOLDOWN = 60;

    public JuQue(Properties properties) {
        super(properties);
    }

    public static JuQue create(ResourceKey<Item> id) {
        return new JuQue(new Item.Properties()
                .sword(ToolMaterial.DIAMOND, 3.0F, -2.4F)
                .setId(id));
    }

    // ==================== 公共发射逻辑（静态，use() 和网络包共用） ====================

    /** 普通剑气 */
    public static void releaseBeamStatic(Player player, ItemStack stack) {
        if (player.getCooldowns().isOnCooldown(stack)) return;

        stack.hurtAndBreak(1, player,
                player.getUsedItemHand() == InteractionHand.MAIN_HAND
                        ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_SWEEP,
                net.minecraft.sounds.SoundSource.PLAYERS, 0.4F, 0.5F);

        float baseDamage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        JuQueBeamEntity beam = new JuQueBeamEntity(player.level(), player);
        beam.setDamage(baseDamage * BEAM_DAMAGE_MULT);
        beam.setLevel(BEAM_LEVEL);
        beam.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F,
                beam.getVelocity(), 1.0F);
        player.level().addFreshEntity(beam);
        player.getCooldowns().addCooldown(stack, COOLDOWN);
    }

    /** 剑意核心（大剑实体） */
    /** 剑意核心（大剑实体） */
    public static void releaseGreatSwordStatic(Player player, ItemStack stack, InteractionHand hand) {
        if (player.getCooldowns().isOnCooldown(stack)) return;

        stack.hurtAndBreak(1, player,
                hand == InteractionHand.MAIN_HAND
                        ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                net.minecraft.sounds.SoundEvents.TRIDENT_RIPTIDE_1,
                net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.4F);

        int stage = player.getAttachedOrCreate(ModDataAttachments.APS_STATE)
                .getInt(AncientPoetrySwordSkill.STAGE_KEY).orElse(0);

        // ✅ 统一工厂
        if (player.level() instanceof ServerLevel sl) {
            FlyingGreatSwordEntity.spawnDirected(
                    sl, player, stack,
                    2.5F + stage * 0.5F,
                    0f
            );
        }

        player.getCooldowns().addCooldown(stack, COOLDOWN);
    }

    // ==================== 物品行为 ====================

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        // ① 残血斩杀
        if (target.isAlive() && target.getHealth() / target.getMaxHealth() <= THRESHOLD) {
            if (attacker instanceof Player player) {
                target.hurt(attacker.damageSources().playerAttack(player), Float.MAX_VALUE);
            } else {
                target.hurt(attacker.damageSources().genericKill(), Float.MAX_VALUE);
            }
            return;
        }

        // ② 减速
        if (Math.random() <= SLOW_CHANCE) {
            target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS,
                    SLOW_DURATION, SLOW_AMPLIFIER));
        }

        // ③ 额外伤害
        if (Math.random() <= DAMAGE_CHANCE) {
            float currentDamage = (float) Objects.requireNonNull(
                    attacker.getAttribute(Attributes.ATTACK_DAMAGE)).getValue();
            if (attacker instanceof Player player) {
                target.hurt(attacker.damageSources().playerAttack(player), currentDamage * MULTIPLIER);
            } else {
                target.hurt(attacker.damageSources().generic(), currentDamage * MULTIPLIER);
            }
        }
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide()) {
                // 服务端：直接执行（静态方法）
                if (APSTerrainManager.hasActiveAPS(player)) {
                    releaseGreatSwordStatic(player, stack, hand);
                } else {
                    releaseBeamStatic(player, stack);
                }
            } else {
                // 客户端：发包，由服务端统一处理
                ClientPlayNetworking.send(new JuQueBeamPacket());
            }
            player.swing(hand);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    public static ItemAttributeModifiers createAttributes(ToolMaterial material, int attackDamage, float attackSpeed) {
        return ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(BASE_ATTACK_DAMAGE_ID,
                                (double) (attackDamage + material.attackDamageBonus()),
                                AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.HAND)
                .add(Attributes.ATTACK_SPEED,
                        new AttributeModifier(BASE_ATTACK_SPEED_ID,
                                (double) attackSpeed,
                                AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.HAND)
                .build();
    }

    // ==================== GeoItem 接口 ====================
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<JuQue>("idle_controller", 0, this::predicate)
                .triggerableAnim("idle", IDLE));
    }

    private PlayState predicate(AnimationTest<JuQue> animationTest) {
        return animationTest.setAndContinue(IDLE);
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private JuQueRenderer renderer;

            @Override
            public @Nullable GeoItemRenderer<JuQue> getGeoItemRenderer() {
                if (this.renderer == null)
                    this.renderer = new JuQueRenderer();
                return this.renderer;
            }
        });
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}