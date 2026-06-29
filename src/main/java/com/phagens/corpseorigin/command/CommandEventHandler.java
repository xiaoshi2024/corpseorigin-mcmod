package com.phagens.corpseorigin.command;

import com.phagens.corpseorigin.CorpseOrigin;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = CorpseOrigin.MODID)
public class CommandEventHandler {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        // 注册召唤尸兄指令
        SummonZbCommand.register(event.getDispatcher());
        // 注册玩家尸体指令
        CorpsePlayerCommand.register(event.getDispatcher());
        // 注册尸兄手下命令系统
        MinionCommand.register(event.getDispatcher());
        // 注册尸王命令系统
        LongyouCommand.register(event.getDispatcher());
        // 注册角色指令
        CharacterCommand.register(event.getDispatcher());
    }
}
