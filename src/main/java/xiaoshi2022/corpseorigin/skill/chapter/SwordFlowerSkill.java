package xiaoshi2022.corpseorigin.skill.chapter;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import xiaoshi2022.corpseorigin.skill.*;
public class SwordFlowerSkill extends AbstractSkill {
    public SwordFlowerSkill(){ super("sword_flower",SkillType.COMBAT,240); }
    @Override public void onActivate(ServerPlayer player){
        var level=(ServerLevel)player.level();
        for(int r=1;r<=6;r++) ChapterCombat.ring(level,player.position().add(0,.9,0),r,0xff76b3,32);
        for(LivingEntity target:level.getEntitiesOfClass(LivingEntity.class,player.getBoundingBox().inflate(6)))
            if(ChapterCombat.canHit(player,target) && player.distanceToSqr(target)<=36 && player.hasLineOfSight(target))
                target.hurtServer(level,player.damageSources().playerAttack(player),9);
        player.swing(net.minecraft.world.InteractionHand.MAIN_HAND,true);
    }
}
