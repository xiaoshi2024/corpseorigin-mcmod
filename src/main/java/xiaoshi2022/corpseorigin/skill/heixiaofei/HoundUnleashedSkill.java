package xiaoshi2022.corpseorigin.skill.heixiaofei;
import net.minecraft.server.level.*;
import net.minecraft.world.effect.MobEffects;
import xiaoshi2022.corpseorigin.skill.*;
import xiaoshi2022.corpseorigin.skill.chapter.*;
/** Server-authoritative skill; stable ID retained for existing saves. */
public class HoundUnleashedSkill extends AbstractSkill {
 public static final String PATH="hound_unleashed";
 public HoundUnleashedSkill(){super(PATH,SkillType.COMBAT,300);}

 @Override public void onActivate(ServerPlayer p){var level=(ServerLevel)p.level();var dog=level.getEntitiesOfClass(xiaoshi2022.corpseorigin.entity.HamEntity.class,p.getBoundingBox().inflate(48),h->h.isOwnedBy(p)).stream().findFirst().orElse(null);if(dog==null){dog=new xiaoshi2022.corpseorigin.entity.HamEntity(xiaoshi2022.corpseorigin.registry.ModEntities.HAM,level);dog.tame(p);dog.setPos(p.position());level.addFreshEntity(dog);}dog.setOrderedToSit(false);dog.setTarget(ChapterCombat.aim(p,24));dog.setDeltaMovement(p.getLookAngle().scale(1.5).add(0,.3,0));dog.hurtMarked=true;}
}
