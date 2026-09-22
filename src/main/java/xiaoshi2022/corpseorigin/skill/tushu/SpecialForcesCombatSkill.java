package xiaoshi2022.corpseorigin.skill.tushu;
import net.minecraft.server.level.*;
import net.minecraft.world.effect.MobEffects;
import xiaoshi2022.corpseorigin.skill.*;
import xiaoshi2022.corpseorigin.skill.chapter.*;
/** Server-authoritative skill; stable ID retained for existing saves. */
public class SpecialForcesCombatSkill extends AbstractSkill {
 public static final String PATH="special_forces_combat";
 public SpecialForcesCombatSkill(){super(PATH,SkillType.COMBAT,60);}

 @Override public void onActivate(ServerPlayer p){var t=ChapterCombat.aim(p,4);if(t==null)return;t.hurtServer((ServerLevel)p.level(),p.damageSources().playerAttack(p),36);ImpactTerrain.launch(p,t,2.5);p.swing(net.minecraft.world.InteractionHand.MAIN_HAND,true);}
}
