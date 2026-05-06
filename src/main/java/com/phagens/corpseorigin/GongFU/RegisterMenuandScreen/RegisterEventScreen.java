package com.phagens.corpseorigin.GongFU.RegisterMenuandScreen;

import com.phagens.corpseorigin.CorpseOrigin;
import com.phagens.corpseorigin.GongFU.MenuTypeRegister;
import com.phagens.corpseorigin.GongFU.Sceen.GongFuSceen;
import com.phagens.corpseorigin.client.gui.TechniqueSwapTable.TechniqueSwapTableScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
//容器屏幕注册
@EventBusSubscriber(modid = CorpseOrigin.MODID, value = Dist.CLIENT)
public class RegisterEventScreen {
    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        // 注册菜单对应的屏幕
        event.register(MenuTypeRegister.GONG_FU_MENU.get(),GongFuSceen::new);
        event.register(MenuTypeRegister.TECHNIQUE_SWAP_TABLE_MENU.get(), TechniqueSwapTableScreen::new);
    }
}
