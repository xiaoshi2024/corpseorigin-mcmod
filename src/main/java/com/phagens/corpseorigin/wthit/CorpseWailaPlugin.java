package com.phagens.corpseorigin.wthit;

import com.phagens.corpseorigin.CorpseOrigin;
import mcp.mobius.waila.api.ICommonRegistrar;
import mcp.mobius.waila.api.IWailaCommonPlugin;
import net.minecraft.world.entity.player.Player;

/**
 * CorpseOrigin的WTHIT通用端插件注册类 - 用于服务器端数据同步
 */
@SuppressWarnings("unused")
public class CorpseWailaPlugin implements IWailaCommonPlugin {

    @Override
    public void register(ICommonRegistrar registrar) {
        CorpseOrigin.LOGGER.info("注册尸兄WTHIT插件 - 通用端（数据同步）");

        // 注册实体数据提供器 - 用于同步尸兄数据从服务器到客户端
        registrar.entityData(CorpsePlayerDataProvider.INSTANCE, Player.class);
    }
}