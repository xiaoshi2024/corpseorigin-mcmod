package xiaoshi2022.corpseorigin.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/**
 * ModMenu 集成入口 —— 提供「模组设置」按钮打开分页配置总界面（{@link CorpseConfigScreen}）。
 * <p>
 * 这是一个可选功能：玩家没装 ModMenu 也能通过游戏内按键 / 指令进入相应界面。
 */
public class CorpseOriginModMenu implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> new CorpseConfigScreen();
    }
}
