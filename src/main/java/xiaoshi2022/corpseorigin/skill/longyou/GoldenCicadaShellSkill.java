package xiaoshi2022.corpseorigin.skill.longyou;

import net.minecraft.server.level.ServerPlayer;
import xiaoshi2022.corpseorigin.skill.AbstractSkill;
import xiaoshi2022.corpseorigin.skill.SkillType;

/**
 * 龙右·金蝉脱壳。
 * <p>
 * 原著依据：尸王的真身是藏在主体内、<b>拇指大小</b>的「原体」，这也是他掉了脑袋也不死的秘密。
 * 这一招就是把外壳（当前身体）蜕下来 —— 蜕下来的壳留在原地化成分身（背包、装备都在壳上），
 * 意识则缩进那颗小小的原体里。
 * <p>
 * 冷却 10 秒：换回来靠身体列表（壳就是分身，选它就能回去），不需要再给一招。
 */
public class GoldenCicadaShellSkill extends AbstractSkill {

    public static final String PATH = "golden_cicada_shell";

    /** 冷却：10 秒（别拿来刷分身） */
    private static final int COOLDOWN = 200;

    public GoldenCicadaShellSkill() {
        super(PATH, SkillType.UTILITY, COOLDOWN);
    }

    @Override
    public void onActivate(ServerPlayer player) {
        // 换身由 BodyTransplantHandler 排队执行：镜头过场播完才真正脱壳，
        // 完成提示也在那时候发 —— 现在发会被过场的黑场盖掉。
        BodyTransplantHandler.shedIntoOriginalBody(player);
    }
}
