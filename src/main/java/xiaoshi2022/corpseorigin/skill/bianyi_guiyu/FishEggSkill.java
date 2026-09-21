package xiaoshi2022.corpseorigin.skill.bianyi_guiyu;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import xiaoshi2022.corpseorigin.entity.CorpseFishEggEntity;
import xiaoshi2022.corpseorigin.skill.*;
public class FishEggSkill extends AbstractSkill {
    public FishEggSkill() { super("corpse_fish_eggs",SkillType.COMBAT,200); }
    @Override
    public Component checkUsable(ServerPlayer p) {
        return p.level().getEntitiesOfClass(CorpseFishEggEntity.class, p.getBoundingBox().inflate(32),
                egg -> egg.ownedBy(p)).size() > 9
                ? Component.translatable("skill.corpseorigin.corpse_fish_eggs.limit") : null;
    }
    @Override public void onActivate(ServerPlayer p) {
        for(int i=0;i<3;i++) p.level().addFreshEntity(CorpseFishEggEntity.create(p,i));
    }
}
