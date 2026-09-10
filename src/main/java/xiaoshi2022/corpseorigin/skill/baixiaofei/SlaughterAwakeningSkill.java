package xiaoshi2022.corpseorigin.skill.baixiaofei;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.network.CorpseNetwork;
import xiaoshi2022.corpseorigin.skill.ISkill;
import xiaoshi2022.corpseorigin.skill.SkillType;

/**
 * 白小飞·杀戮觉醒 - 短暂获得力量 II
 */
public class SlaughterAwakeningSkill implements ISkill {

    private static final int DURATION = 200;   // 10 秒
    private static final int AMPLIFIER_II = 1;
    private static final int AMPLIFIER_I  = 0;

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
        // ==================== 正面效果 ====================
        player.addEffect(new MobEffectInstance(MobEffects.STRENGTH,    DURATION, AMPLIFIER_II, false, true, true));
        player.addEffect(new MobEffectInstance(MobEffects.SPEED,       DURATION, AMPLIFIER_II, false, true, true));
        player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE,  DURATION, AMPLIFIER_I,  false, true, true));
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION,DURATION, AMPLIFIER_I,  false, true, true));
        player.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST,  DURATION, AMPLIFIER_I,  false, true, true));

        // ==================== 代价：饥饿 II ====================
        player.addEffect(new MobEffectInstance(MobEffects.HUNGER,      DURATION, AMPLIFIER_II, false, true, true));

        // ==================== 红眼特效 ====================
        CorpseNetwork.broadcastTempRedEye(player, DURATION);
    }
}
