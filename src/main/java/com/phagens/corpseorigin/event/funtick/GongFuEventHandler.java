package com.phagens.corpseorigin.event.funtick;

import com.phagens.corpseorigin.CorpseOrigin;
import com.phagens.corpseorigin.GongFU.JSskill.Factory.ProjectileManager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * GongFu 系统事件处理器
 * 
 * @author Phagens
 */
@EventBusSubscriber(modid = CorpseOrigin.MODID)
public class GongFuEventHandler {
    
    /**
     * 服务端 Tick 事件
     * 用于更新 ProjectileManager 中的粒子投射物
     */
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        ProjectileManager.getInstance().tick();
    }
}
