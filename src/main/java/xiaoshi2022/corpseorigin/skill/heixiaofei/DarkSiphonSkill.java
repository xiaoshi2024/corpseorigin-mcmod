package xiaoshi2022.corpseorigin.skill.heixiaofei;
import net.minecraft.server.level.*;
import net.minecraft.world.effect.MobEffects;
import xiaoshi2022.corpseorigin.skill.*;
import xiaoshi2022.corpseorigin.skill.chapter.*;
/** Server-authoritative skill; stable ID retained for existing saves. */
public class DarkSiphonSkill extends AbstractSkill {
 public static final String PATH="dark_siphon";
 public DarkSiphonSkill(){super(PATH,SkillType.COMBAT,20);}
 @Override public net.minecraft.network.chat.Component checkUsable(ServerPlayer p){if(SkillRework.siphoning(p))return null;if(!p.getMainHandItem().is(xiaoshi2022.corpseorigin.registry.ModItems.BLOOD_WING_BLADE))return net.minecraft.network.chat.Component.literal("需要主手持血翼黑刃；贴近目标后发动，潜行或换手拔刀。");return null;}
 @Override public void onActivate(ServerPlayer p){SkillRework.siphon(p);}
}
