package com.phagens.corpseorigin.event;

import com.mojang.blaze3d.platform.InputConstants;
import com.phagens.corpseorigin.CorpseOrigin;
import com.phagens.corpseorigin.network.JuQueTakeOutPacket;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * 巨阙剑技能轮盘按键事件
 * 功能：当巨阙装备在 Curios 背部槽位时，按下技能轮盘键自动将剑取出到主手
 */
@EventBusSubscriber(modid = CorpseOrigin.MODID, value = Dist.CLIENT)
public class JuQueSkillWheelEvent {

    // ==================== 按键绑定配置 ====================
    public static final String KEY_CATEGORY = "key.corpseorigin.category";
    public static final String KEY_TAKE_OUT_JUQUE = "key.corpseorigin.take_out_juque";

    public static KeyMapping takeOutJuQueKey = new KeyMapping(
            KEY_TAKE_OUT_JUQUE,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            KEY_CATEGORY
    );

    /**
     * 注册按键绑定
     */
    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(takeOutJuQueKey);
    }

    // ==================== 按键输入处理 ====================
    @EventBusSubscriber(modid = CorpseOrigin.MODID, value = Dist.CLIENT)
    public static class KeyInputHandler {

        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.screen != null) {
                return;
            }

            // 检测按键按下
            if (takeOutJuQueKey.consumeClick()) {
                // 发送网络包到服务端
                PacketDistributor.sendToServer(new JuQueTakeOutPacket());
                CorpseOrigin.LOGGER.debug("发送取出巨阙剑网络包到服务端");
            }
        }
    }
}