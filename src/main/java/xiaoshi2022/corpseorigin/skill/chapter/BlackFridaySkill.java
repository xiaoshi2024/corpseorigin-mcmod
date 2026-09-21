package xiaoshi2022.corpseorigin.skill.chapter;
import net.minecraft.server.level.ServerPlayer;
import xiaoshi2022.corpseorigin.skill.*;
import xiaoshi2022.corpseorigin.entity.ChapterBombEntity;
import xiaoshi2022.corpseorigin.registry.ModItems;
public class BlackFridaySkill extends AbstractSkill {
    public BlackFridaySkill(){ super("black_friday_eight",SkillType.COMBAT,160); }
    @Override public void onActivate(ServerPlayer player){ ChapterBombEntity.launch(player,ModItems.BILLIARD_EIGHT); }
}
