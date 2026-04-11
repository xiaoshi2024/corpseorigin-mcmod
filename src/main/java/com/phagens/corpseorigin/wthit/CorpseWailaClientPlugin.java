package com.phagens.corpseorigin.wthit;

import com.phagens.corpseorigin.CorpseOrigin;
import mcp.mobius.waila.api.IClientRegistrar;
import mcp.mobius.waila.api.IWailaClientPlugin;
import net.minecraft.world.entity.player.Player;

/**
 * CorpseOrigin的WTHIT客户端插件注册类 - 用于UI显示
 */
@SuppressWarnings("unused")
public class CorpseWailaClientPlugin implements IWailaClientPlugin {

    @Override
    public void register(IClientRegistrar registrar) {
        CorpseOrigin.LOGGER.info("注册尸兄WTHIT插件 - 客户端（UI显示）");

        // 注册尸兄玩家的头部组件
        registrar.head(CorpsePlayerProvider.INSTANCE, Player.class);

        // 注册尸兄玩家的身体组件（主要信息）
        registrar.body(CorpsePlayerProvider.INSTANCE, Player.class);

        // 注册尸兄玩家的图标提供者
        registrar.icon(CorpsePlayerProvider.INSTANCE, Player.class);
    }
}