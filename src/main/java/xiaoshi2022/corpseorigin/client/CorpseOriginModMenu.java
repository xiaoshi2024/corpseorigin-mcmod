package xiaoshi2022.corpseorigin.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/**
 * ModMenu 集成入口 —— 提供「模组设置」按钮打开 HUD 设置界面。
 * <p>
 * 这是一个可选功能：玩家没装 ModMenu 也能通过 F9 键打开 HUD 设置界面。
 */
public class CorpseOriginModMenu implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> new HudSettingsScreen();
    }
}
