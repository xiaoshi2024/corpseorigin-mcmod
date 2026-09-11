package xiaoshi2022.corpseorigin.skill.baixiaofei;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.registry.ModDataAttachments;
import xiaoshi2022.corpseorigin.skill.ISkill;
import xiaoshi2022.corpseorigin.skill.SkillType;
import xiaoshi2022.corpseorigin.skill.baixiaofei.aps.APSTerrainManager;

public class AncientPoetrySwordSkill implements ISkill {

    public static final String STAGE_KEY = "aps_stage";
    public static final String CD_KEY = "aps_cd";
    public static final String ACTIVE_KEY = "aps_active";
    public static final int STAGE_RESET_TICKS = 200;  // 10 秒

    @Override
    public Identifier getId() {
        return Identifier.fromNamespaceAndPath(CorpseOrigin.MOD_ID, "ancient_poetry_sword");
    }

    @Override
    public Component getName() {
        return Component.translatable("skill.corpseorigin.ancient_poetry_sword");
    }

    @Override
    public Component getDescription() {
        return Component.translatable("skill.corpseorigin.ancient_poetry_sword.desc");
    }

    @Override
    public SkillType getSkillType() {
        return SkillType.COMBAT;
    }

    @Override
    public boolean isActivatable() {
        return true;
    }

    @Override
    public int getCooldownTicks() {
        return 0;
    }

    @Override
    public void onActivate(ServerPlayer player) {
        ServerLevel level = player.level();

        CompoundTag state = player.getAttachedOrCreate(ModDataAttachments.APS_STATE).copy();
        boolean active = state.getBoolean(ACTIVE_KEY).orElse(false);

        if (active) {
            state.putBoolean(ACTIVE_KEY, false);
            state.putInt(STAGE_KEY, 0);
            player.setAttached(ModDataAttachments.APS_STATE, state);
            player.sendSystemMessage(Component.translatable(
                    "skill.corpseorigin.ancient_poetry_sword.off"));
        } else {
            state.putBoolean(ACTIVE_KEY, true);
            state.putInt(STAGE_KEY, 0);
            state.putLong(CD_KEY, level.getGameTime());
            player.setAttached(ModDataAttachments.APS_STATE, state);
            player.sendSystemMessage(Component.translatable(
                    "skill.corpseorigin.ancient_poetry_sword.on"));
        }
    }
}