package com.phagens.corpseorigin.wthit;

import com.phagens.corpseorigin.CorpseOrigin;
import com.phagens.corpseorigin.entity.Animals.CocoPenguinEntity;
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

        // 注册尸兄玩家的实体数据提供器
        registrar.entityData(CorpsePlayerDataProvider.INSTANCE, Player.class);

        // ========== 注册企鹅数据同步 ==========
        registrar.entityData(CocoPenguinDataProvider.INSTANCE, CocoPenguinEntity.class);
    }
}