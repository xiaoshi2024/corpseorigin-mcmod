package xiaoshi2022.corpseorigin.character;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import xiaoshi2022.corpseorigin.component.PlayerCorpseComponent;
import xiaoshi2022.corpseorigin.network.CorpseNetwork;
import xiaoshi2022.corpseorigin.skill.ISkill;
import xiaoshi2022.corpseorigin.skill.longyou.CorpseKingInfrasoundSkill;
import xiaoshi2022.corpseorigin.skill.longyou.CorpseKingThunderSkill;
import xiaoshi2022.corpseorigin.skill.longyou.UndyingChestSkill;
import xiaoshi2022.corpseorigin.skill.longyou.WaterPollutionSkill;

import java.util.List;

/**
 * 龙右 - 尸王
 * <p>
 * 成为龙右即直接变为满级尸兄（尸王类型），并保留意识。
 * <p>
 * 含《尸巢之战篇》设定的不死髅体、雷电骨架技能，以及"尸水之源"——
 * 走过的水源会被污染成尸水（被动，实装在 {@code LongYouEventHandler}）。
 */
public class LongYou implements ICharacter {

    public static final String ID = "longyou";

    private static final List<ISkill> SKILLS = List.of(
            new UndyingChestSkill(),
            new CorpseKingThunderSkill(),
            new CorpseKingInfrasoundSkill(),
            new WaterPollutionSkill()
    );

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public Component getName() {
        return Component.translatable("character.corpseorigin.longyou");
    }

    @Override
    public Component getDescription() {
        return Component.translatable("character.corpseorigin.longyou.desc");
    }

    @Override
    public Identifier getIcon() {
        return ICharacter.iconId("longyou");
    }

    @Override
    public boolean isPassive() {
        return false;
    }

    @Override
    public List<Component> getTraits() {
        return List.of(
                Component.translatable("character.corpseorigin.longyou.trait1"),
                Component.translatable("character.corpseorigin.longyou.trait2"),
                Component.translatable("character.corpseorigin.longyou.trait3")
        );
    }

    @Override
    public List<ISkill> getSkills() {
        return SKILLS;
    }

    @Override
    public float getInfectionMultiplier() {
        return 0.0f;
    }

    /** 龙右（尸王）：千年修为，内力深厚，上限 200 */
    @Override
    public int getMaxInnerPower() {
        return 200;
    }

    /**
     * 这位玩家是不是「尸水之源」本人 —— 完全免疫尸水（角色特质：{@code trait3}）。
     * <p>
     * 尸水的中毒/感染判定散在三处（{@code InfectedWaterFluid}、{@code InfectedWaterBlock}、
     * {@code ByWaterBottleItem}），统一走这里，免得以后漏改一处。
     * <p>
     * 注意这是<b>角色特质</b>，不需要学会技能：是龙右就生效。
     */
    public static boolean isImmuneToInfectedWater(Player player) {
        return player != null
                && ID.equals(CharacterManager.getInstance().getPlayerCharacterId(player));
    }

    @Override
    public void onAcquire(Player player) {
        // ✅ 成为龙右 → 直接变为尸王（满级 + 有意识）
        PlayerCorpseComponent.setPlayerAsCorpse(player, PlayerCorpseComponent.TYPE_KING);
        PlayerCorpseComponent comp = PlayerCorpseComponent.get(player);
        comp.setEvolutionLevel(PlayerCorpseComponent.MAX_EVOLUTION_LEVEL);
        comp.restoreConsciousness();

        // ⚠️ setEvolutionLevel/restoreConsciousness 不会触发同步，需显式补一次。
        //    必须用广播：尸兄外观是别的玩家看你时才渲染的，只发给自己别人看不到。
        if (player instanceof ServerPlayer serverPlayer) {
            CorpseNetwork.broadcastPlayerCorpseSync(serverPlayer);
        }
    }

    @Override
    public void onLose(Player player) {
        // 失去龙右身份 → 清除尸兄状态（removeCorpseState 内部已同步）
        PlayerCorpseComponent.removeCorpseState(player);
    }
}