package xiaoshi2022.corpseorigin.skill.heixiaofei;
import net.minecraft.server.level.*;
import net.minecraft.world.effect.MobEffects;
import xiaoshi2022.corpseorigin.skill.*;
import xiaoshi2022.corpseorigin.skill.chapter.*;
/** Server-authoritative skill; stable ID retained for existing saves. */
public class HoundUnleashedSkill extends AbstractSkill {
 public static final String PATH="hound_unleashed";
 public HoundUnleashedSkill(){super(PATH,SkillType.COMBAT,300);}

 private static xiaoshi2022.corpseorigin.entity.HamEntity findNearbyHam(ServerPlayer p) {
     return p.level().getEntitiesOfClass(xiaoshi2022.corpseorigin.entity.HamEntity.class,
             p.getBoundingBox().inflate(6), h -> h.isAlive() && !h.isRemoved() && !h.isSpectator()
                     && h.isOwnedBy(p) && !h.isPassenger() && !h.isVehicle() && !h.isLeashed()
                     && p.distanceToSqr(h)<=36 && p.hasLineOfSight(h))
             .stream().min(java.util.Comparator.comparingDouble(h -> p.distanceToSqr(h))).orElse(null);
 }
 @Override public net.minecraft.network.chat.Component checkUsable(ServerPlayer p) {
     return findNearbyHam(p)==null
             ? net.minecraft.network.chat.Component.translatable("skill.corpseorigin.hound_unleashed.no_ham") : null;
 }
 @Override public void onActivate(ServerPlayer p) {
     // Revalidate immediately before launch; never spawn, duplicate, or tame a replacement.
     var dog=findNearbyHam(p);
     if(dog==null)return;
     dog.getNavigation().stop();
     dog.setOrderedToSit(false);
     dog.setInSittingPose(false);
     dog.setTarget(ChapterCombat.aim(p,24));
     dog.setDeltaMovement(p.getLookAngle().scale(1.5).add(0,.3,0));
     dog.hurtMarked=true;
     p.swing(net.minecraft.world.InteractionHand.MAIN_HAND,true);
 }
}
