package com.phagens.corpseorigin.wthit;

import com.phagens.corpseorigin.player.PlayerCorpseData;
import mcp.mobius.waila.api.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

/**
 * 尸兄玩家数据提供者 - 用于从服务器同步数据到客户端
 * 
 * 由于WTHIT在服务器环境中需要从服务器获取数据，
 * 这个类负责将尸兄玩家的数据同步到客户端显示
 */
public enum CorpsePlayerDataProvider implements IDataProvider<Player> {

    INSTANCE;

    @Override
    public void appendData(IDataWriter data, IServerAccessor<Player> accessor, IPluginConfig config) {
        Player player = accessor.getTarget();
        
        // 只同步尸兄玩家的数据
        if (!PlayerCorpseData.isCorpse(player)) {
            return;
        }

        CompoundTag tag = data.raw();
        
        // 同步尸兄数据到客户端
        tag.putBoolean("isCorpse", true);
        tag.putInt("evolutionLevel", PlayerCorpseData.getEvolutionLevel(player));
        tag.putInt("kills", PlayerCorpseData.getKills(player));
        tag.putInt("hunger", PlayerCorpseData.getHunger(player));
        tag.putBoolean("hasConsciousness", PlayerCorpseData.hasConsciousness(player));
        tag.putBoolean("consciousnessRestored", PlayerCorpseData.isConsciousnessRestored(player));
        tag.putBoolean("hasWing", PlayerCorpseData.hasWing(player));
        tag.putBoolean("hasTail", PlayerCorpseData.hasTail(player));
        tag.putInt("extraEyeCount", PlayerCorpseData.getExtraEyeCount(player));
        
        // 同步技能数据
        var skillHandler = com.phagens.corpseorigin.skill.SkillAttachment.getSkillHandler(player);
        if (skillHandler != null) {
            tag.putInt("learnedSkills", skillHandler.getLearnedSkills().size());
            tag.putInt("evolutionPoints", skillHandler.getEvolutionPoints());
        }
    }
}
