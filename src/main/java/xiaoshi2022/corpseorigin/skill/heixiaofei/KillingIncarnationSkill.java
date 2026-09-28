package xiaoshi2022.corpseorigin.skill.heixiaofei;

import net.minecraft.server.level.ServerPlayer;
import xiaoshi2022.corpseorigin.entity.SkillConstructEntity;
import xiaoshi2022.corpseorigin.registry.ModEntities;
import xiaoshi2022.corpseorigin.skill.AbstractSkill;
import xiaoshi2022.corpseorigin.skill.SkillType;

public class KillingIncarnationSkill extends AbstractSkill {
 public static final String PATH="killing_incarnation";
 public KillingIncarnationSkill(){super(PATH,SkillType.ULTIMATE,2400);}
 @Override public void onActivate(ServerPlayer p){SkillConstructEntity.spawn(p,ModEntities.SLAUGHTER_INCARNATION,p,400);}
}
