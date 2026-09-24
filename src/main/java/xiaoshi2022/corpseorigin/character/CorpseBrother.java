package xiaoshi2022.corpseorigin.character;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import xiaoshi2022.corpseorigin.component.PlayerCorpseComponent;
import xiaoshi2022.corpseorigin.network.CorpseNetwork;
import xiaoshi2022.corpseorigin.skill.ISkill;

import java.util.List;

/**
 * 尸兄（通用）。
 * <p>
 * 凡人被尸水感染成尸兄之后会自动转入这个角色（见 {@code BYeffect#convertPlayerToCorpse}），
 * 也可以直接用角色选择书切过来。
 * <p>
 * 之前凡人变尸兄只是"阵营变了、角色还是凡人"，导致尸兄那一套进化效果没有角色可以挂；
 * 现在进化效果（技能树、进化等级带来的强化）统一挂在这个角色上 ——
 * 需要新增时往 {@link #SKILLS} 里加，或覆写 {@link #onAcquire} 写与进化等级联动的逻辑。
 */
public class CorpseBrother implements ICharacter {

    public static final String ID = "corpse_brother";

    /** 尸兄的进化效果 / 专属技能都挂这里 */
    private static final List<ISkill> SKILLS = List.of();

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public Component getName() {
        return Component.translatable("character.corpseorigin." + ID);
    }

    @Override
    public Component getDescription() {
        return Component.translatable("character.corpseorigin." + ID + ".desc");
    }

    @Override
    public Identifier getIcon() {
        return ICharacter.iconId(ID);
    }

    @Override
    public boolean isPassive() {
        return false;
    }

    @Override
    public List<Component> getTraits() {
        return List.of(
                Component.translatable("character.corpseorigin." + ID + ".trait1"),
                Component.translatable("character.corpseorigin." + ID + ".trait2"),
                Component.translatable("character.corpseorigin." + ID + ".trait3")
        );
    }

    @Override
    public List<ISkill> getSkills() {
        return xiaoshi2022.corpseorigin.growth.FreeGrowth.skills();
    }

    /** 已经是尸兄了，再被尸水"感染"没有意义 */
    @Override
    public float getInfectionMultiplier() {
        return 0.0f;
    }

    @Override
    public void onAcquire(Player player) {
        // 直接从角色书切过来也要是尸兄；但走感染路径进来时本来就已经是了 ——
        // 那种情况不能重写状态，否则感染给的尸兄类型 / 变种会被抹掉。
        if (PlayerCorpseComponent.isCorpse(player)) {
            return;
        }
        PlayerCorpseComponent.setPlayerAsCorpse(player, PlayerCorpseComponent.TYPE_NORMAL);
        if (player instanceof ServerPlayer serverPlayer) {
            CorpseNetwork.broadcastPlayerCorpseSync(serverPlayer);
        }
    }

    @Override
    public void onLose(Player player) {
        // 失去尸兄身份 → 连带清掉尸兄状态（和天线宝宝尸兄角色一致）
        PlayerCorpseComponent.removeCorpseState(player);
    }
}
