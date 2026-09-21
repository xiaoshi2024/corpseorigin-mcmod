package xiaoshi2022.corpseorigin.skill.k;

import xiaoshi2022.corpseorigin.skill.AbstractSkill;
import xiaoshi2022.corpseorigin.skill.SkillType;

/**
 * K·蝙蝠披风 —— 化蝙蝠披风束缚敌方。
 * <p>
 * 设定效果：化身蝙蝠披风笼罩周边敌人，将其束缚在原地。
 * 冷却：15 秒（300 ticks）。
 * 特效：蝙蝠群粒子 + 披风模型。
 * <p>
 * TODO 实装：范围束缚敌人（定身/减速）+ 蝙蝠粒子。
 */
public class BatCloakSkill extends AbstractSkill {

    public static final String PATH = "bat_cloak";

    public BatCloakSkill() {
        super(PATH, SkillType.UTILITY, 300, 15);
    }
    @Override public void onActivate(net.minecraft.server.level.ServerPlayer player) {
        for (int i=0;i<5;i++) {
            var bat = new xiaoshi2022.corpseorigin.entity.VampireBatEntity(
                    xiaoshi2022.corpseorigin.registry.ModEntities.VAMPIRE_BAT,player.level());
            bat.setOwner(player);
            bat.setPos(player.getEyePosition().add(Math.cos(i*Math.PI*2/5),.3,Math.sin(i*Math.PI*2/5)));
            player.level().addFreshEntity(bat);
        }
    }
}
