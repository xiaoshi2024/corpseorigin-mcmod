package com.phagens.corpseorigin.wthit;

import com.phagens.corpseorigin.entity.Animals.CocoPenguinEntity;
import mcp.mobius.waila.api.IDataProvider;
import mcp.mobius.waila.api.IDataWriter;
import mcp.mobius.waila.api.IPluginConfig;
import mcp.mobius.waila.api.IServerAccessor;
import net.minecraft.nbt.CompoundTag;

public enum CocoPenguinDataProvider implements IDataProvider<CocoPenguinEntity> {

    INSTANCE;

    @Override
    public void appendData(IDataWriter data, IServerAccessor<CocoPenguinEntity> accessor, IPluginConfig config) {
        CocoPenguinEntity penguin = accessor.getTarget();

        CompoundTag tag = data.raw();

        // 同步企鹅数据到客户端
        tag.putInt("hunger", penguin.getHunger());
        tag.putInt("maxHunger", penguin.getMaxHunger());
        tag.putBoolean("isHungry", penguin.isHungry());
        tag.putBoolean("canEat", penguin.canEat());

        // 添加氧气数据同步
        tag.putInt("airSupply", penguin.getAirSupply());
        tag.putInt("maxAir", penguin.getMaxAir());
    }
}