package xiaoshi2022.corpseorigin.skill.longyou;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import xiaoshi2022.corpseorigin.skill.AbstractSkill;
import xiaoshi2022.corpseorigin.skill.SkillType;

/**
 * 龙右·血肉重塑。
 * <p>
 * 血肉重构成一具崭新的正常身体（用来从"拇指大小的原体"变回来，或者单纯换一具新的），
 * 代价是<b>一半饱食度</b>；旧身体同样蜕成分身留在原地，行囊也留在那具分身身上。
 * <p>
 * 没有冷却：代价就是饱食度，饿了就重塑不出来（见 {@code BodyTransplantHandler}）。
 */
public class FleshReshapeSkill extends AbstractSkill {

    public static final String PATH = "flesh_reshape";

    public FleshReshapeSkill() {
        super(PATH, SkillType.UTILITY, 0);
    }

    @Override
    public void onActivate(ServerPlayer player) {
        if (BodyTransplantHandler.reshapeBody(player)) {
            player.sendOverlayMessage(Component.translatable("skill.corpseorigin." + PATH + ".done"));
        }
    }
}
