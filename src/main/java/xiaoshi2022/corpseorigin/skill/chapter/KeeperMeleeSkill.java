package xiaoshi2022.corpseorigin.skill.chapter;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import xiaoshi2022.corpseorigin.skill.*;
public class KeeperMeleeSkill extends AbstractSkill {
    public KeeperMeleeSkill(){super("keeper_melee",SkillType.COMBAT,60);}
    @Override public Component checkUsable(ServerPlayer p){return null;}
    @Override public void onActivate(ServerPlayer p){
        var target=ChapterCombat.aim(p,3);if(target==null){xiaoshi2022.corpseorigin.skill.chapter.ChapterCombat.emptyCast(p);return;}
        target.hurtServer((net.minecraft.server.level.ServerLevel)p.level(),p.damageSources().playerAttack(p),6);
        p.swing(net.minecraft.world.InteractionHand.MAIN_HAND,true);
    }
}
