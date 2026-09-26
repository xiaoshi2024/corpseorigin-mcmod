package xiaoshi2022.corpseorigin.skill.xiaolu;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.item.MedusaEyeItem;
import xiaoshi2022.corpseorigin.skill.ISkill;
import xiaoshi2022.corpseorigin.skill.SkillType;
import xiaoshi2022.corpseorigin.skill.unlock.SkillUnlockSource;

import java.util.List;

/**
 * 小鹿·锇金化 - 短暂获得抗性 II
 */
public class OsmiumGoldSkill implements ISkill {

    @Override
    public Identifier getId() {
        return Identifier.fromNamespaceAndPath(CorpseOrigin.MOD_ID, "osmium_gold");
    }

    @Override
    public Component getName() {
        return Component.translatable("skill.corpseorigin.osmium_gold");
    }

    @Override
    public Component getDescription() {
        return Component.translatable("skill.corpseorigin.osmium_gold.desc");
    }

    @Override
    public SkillType getSkillType() {
        return SkillType.DEFENSE;
    }

    /**
     * 只有吃下「美杜莎之眼」才能觉醒。
     * <p>
     * 声明获取式来源即关闭技能树的点数路径：{@code SkillManager.learn} 对有来源的技能
     * 一律转交 {@code SkillUnlockManager}，不扣点、不检查等级；技能界面那行也会显示
     * 「需要 美杜莎之眼」而不是「点击学习」。
     */
    @Override
    public List<SkillUnlockSource> getUnlockSources() {
        return List.of(SkillUnlockSource.relic(MedusaEyeItem.RELIC_ID));
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
        player.setAttached(xiaoshi2022.corpseorigin.skill.chapter.SkillRework.GOLD, player.level().getGameTime()+400);
        player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 400, 3, false, true, true));
    }
}
