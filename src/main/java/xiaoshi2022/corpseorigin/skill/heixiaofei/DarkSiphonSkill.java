package xiaoshi2022.corpseorigin.skill.heixiaofei;

import net.minecraft.server.level.ServerPlayer;
import xiaoshi2022.corpseorigin.skill.AbstractSkill;
import xiaoshi2022.corpseorigin.skill.SkillType;
import xiaoshi2022.corpseorigin.skill.chapter.SkillRework;

/** Server-authoritative skill; stable ID retained for existing saves. */
public class DarkSiphonSkill extends AbstractSkill {
 public static final String PATH="dark_siphon";
 public DarkSiphonSkill(){super(PATH,SkillType.COMBAT,20);}
 @Override public net.minecraft.network.chat.Component checkUsable(ServerPlayer p){if(SkillRework.siphoning(p))return null;if(!p.getMainHandItem().is(xiaoshi2022.corpseorigin.registry.ModItems.BLOOD_WING_BLADE))return net.minecraft.network.chat.Component.translatable("message.corpseorigin.dark_siphon_skill.text_01");return null;}
 @Override public void onActivate(ServerPlayer p){SkillRework.siphon(p);}
}
