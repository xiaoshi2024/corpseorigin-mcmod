package xiaoshi2022.corpseorigin.skill.baixiaofei;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.skill.ISkill;
import xiaoshi2022.corpseorigin.skill.SkillType;

/**
 * 白小飞·杀戮觉醒 - 短暂获得力量 II
 */
public class SlaughterAwakeningSkill implements ISkill {

    @Override
    public Identifier getId() {
        return Identifier.fromNamespaceAndPath(CorpseOrigin.MOD_ID, "slaughter_awakening");
    }

    @Override
    public Component getName() {
        return Component.translatable("skill.corpseorigin.slaughter_awakening");
    }

    @Override
    public Component getDescription() {
        return Component.translatable("skill.corpseorigin.slaughter_awakening.desc");
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
        return 1200;  // 60s
    }

    @Override
    public void onActivate(ServerPlayer player) {
        // 力量 II，10 秒（200 ticks）
        player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 200, 1, false, true, true));
    }
}
