package xiaoshi2022.corpseorigin.skill.xiaoyanzi;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import xiaoshi2022.corpseorigin.item.DogCageItem;
import xiaoshi2022.corpseorigin.skill.AbstractSkill;
import xiaoshi2022.corpseorigin.skill.SkillType;
import xiaoshi2022.corpseorigin.skill.unlock.SkillUnlockSource;
import java.util.List;

/** Existing skill ID retained for saved unlocks; now fires the captured Ham from its cage. */
public class HamSummonSkill extends AbstractSkill {
    public static final String PATH = "ham_summon";
    public static final String PET_ID = "ham";
    public HamSummonSkill() { super(PATH, SkillType.COMBAT, DogCageItem.FIRE_COOLDOWN); }

    @Override public List<SkillUnlockSource> getUnlockSources() {
        return List.of(SkillUnlockSource.custom("pet:" + PET_ID,
                Component.translatable("item.corpseorigin.dog_cage.loaded"), DogCageItem::hasCapturedHam));
    }
    @Override public Component checkUsable(ServerPlayer player) {
        return DogCageItem.fireError(player, DogCageItem.heldCage(player));
    }
    @Override public void onActivate(ServerPlayer player) {
        DogCageItem.fire(player, DogCageItem.heldCage(player));
    }
}
