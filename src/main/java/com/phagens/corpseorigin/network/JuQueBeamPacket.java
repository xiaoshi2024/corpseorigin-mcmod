package com.phagens.corpseorigin.network;

import com.phagens.corpseorigin.CorpseOrigin;
import com.phagens.corpseorigin.entity.JuQueBeamEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Random;

public record JuQueBeamPacket() implements CustomPacketPayload {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(
            CorpseOrigin.MODID, "juque_beam");

    public static final Type<JuQueBeamPacket> TYPE = new Type<>(ID);

    public static final StreamCodec<ByteBuf, JuQueBeamPacket> STREAM_CODEC = StreamCodec.unit(new JuQueBeamPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(JuQueBeamPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                ItemStack stack = player.getMainHandItem();
                
                // 检查是否持有巨阙武器
                if (stack.getItem() instanceof com.phagens.corpseorigin.Item.JuQue juQue) {
                    // 检查冷却时间
                    if (player.getCooldowns().isOnCooldown(juQue)) {
                        return;
                    }
                    
                    // 消耗耐久度
                    if (!player.isCreative()) {
                        stack.hurtAndBreak(new Random().nextBoolean() ? 1 : 0, player,
                                player.getUsedItemHand() == InteractionHand.MAIN_HAND ?
                                        EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);

                    }
                    
                    // 播放音效
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                            net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_SWEEP, 
                            net.minecraft.sounds.SoundSource.PLAYERS, 0.4F, 0.5F);
                    
                    // 计算伤害
                    float baseDamage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
                    float damage = baseDamage * getDamageMultiplier(juQue.getVariant());
                    
                    // 创建剑气实体
                    JuQueBeamEntity beam = new JuQueBeamEntity(player.level(), player);
                    beam.setDamage(damage);
                    beam.setLevel(getBeamLevel(juQue.getVariant()));
                    
                    // 设置发射方向和速度
                    beam.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, beam.getVelocity(), 1.0F);
                    player.level().addFreshEntity(beam);
                    
                    // 设置冷却时间
                    int cooldown = getCooldown(juQue.getVariant());
                    player.getCooldowns().addCooldown(juQue, cooldown);
                }
            }
        });
    }
    
    private static float getDamageMultiplier(String variant) {
        return "tw".equals(variant) ? 0.8F : 0.6F; // 2阶变种伤害更高
    }
    
    private static int getBeamLevel(String variant) {
        return "tw".equals(variant) ? 2 : 1; // 2阶变种剑气等级更高
    }
    
    private static int getCooldown(String variant) {
        return "tw".equals(variant) ? 60 : 80; // 2阶变种冷却时间更短
    }
}