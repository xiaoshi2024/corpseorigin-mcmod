package xiaoshi2022.corpseorigin.skill.longyou;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.entity.LowerLevelZbEntity;
import xiaoshi2022.corpseorigin.registry.ModEntities;
import xiaoshi2022.corpseorigin.skill.ISkill;
import xiaoshi2022.corpseorigin.skill.SkillType;

/**
 * 龙右·尸王次声波 - 在玩家附近召唤 2 个低级尸兄
 */
public class CorpseKingInfrasoundSkill implements ISkill {

    private static final int SUMMON_COUNT = 2;

    @Override
    public Identifier getId() {
        return Identifier.fromNamespaceAndPath(CorpseOrigin.MOD_ID, "corpse_king_infrasound");
    }

    @Override
    public Component getName() {
        return Component.translatable("skill.corpseorigin.corpse_king_infrasound");
    }

    @Override
    public Component getDescription() {
        return Component.translatable("skill.corpseorigin.corpse_king_infrasound.desc");
    }

    @Override
    public SkillType getSkillType() {
        return SkillType.ULTIMATE;
    }

    @Override
    public boolean isActivatable() {
        return true;
    }

    @Override
    public int getCooldownTicks() {
        return 2400;  // 120s
    }

    @Override
    public void onActivate(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        for (int i = 0; i < SUMMON_COUNT; i++) {
            LowerLevelZbEntity zb = new LowerLevelZbEntity(ModEntities.LOWER_LEVEL_ZB, serverLevel);
            double offsetX = (player.getRandom().nextDouble() - 0.5) * 4.0;
            double offsetZ = (player.getRandom().nextDouble() - 0.5) * 4.0;
            zb.setPos(player.getX() + offsetX, player.getY(), player.getZ() + offsetZ);
            serverLevel.addFreshEntity(zb);
        }
    }
}
