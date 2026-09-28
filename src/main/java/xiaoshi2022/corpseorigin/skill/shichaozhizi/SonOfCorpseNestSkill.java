package xiaoshi2022.corpseorigin.skill.shichaozhizi;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import xiaoshi2022.corpseorigin.character.ShiChaoZhiZi;
import xiaoshi2022.corpseorigin.component.PlayerCorpseComponent;
import xiaoshi2022.corpseorigin.skill.AbstractSkill;
import xiaoshi2022.corpseorigin.skill.SkillType;

/** 尸巢之子的第二形态开关。 */
public final class SonOfCorpseNestSkill extends AbstractSkill {
    public static final String PATH = "son_of_corpse_nest";
    public static final int REQUIRED_CORPSE_KILLS = 1000;

    public SonOfCorpseNestSkill() {
        super(PATH, SkillType.ULTIMATE, 0);
    }

    @Override
    public Component checkUsable(ServerPlayer player) {
        PlayerCorpseComponent corpse = PlayerCorpseComponent.get(player);
        if (corpse.getVariant() != PlayerCorpseComponent.VARIANT_SHICHAOZHIZI
                && !player.isCreative() && corpse.getKills() < REQUIRED_CORPSE_KILLS) {
            return Component.translatable("skill.corpseorigin." + PATH + ".need_kills",
                    corpse.getKills(), REQUIRED_CORPSE_KILLS);
        }
        return null;
    }

    @Override
    public void onActivate(ServerPlayer player) {
        PlayerCorpseComponent corpse = PlayerCorpseComponent.get(player);
        boolean enabling = corpse.getVariant() != PlayerCorpseComponent.VARIANT_SHICHAOZHIZI;
        float oldMaxHealth = player.getMaxHealth();
        corpse.setVariant(enabling
                ? PlayerCorpseComponent.VARIANT_SHICHAOZHIZI
                : PlayerCorpseComponent.VARIANT_NO_EXOSKELETON);
        if (enabling) {
            ShiChaoZhiZi.applySecondFormAttributes(player);
            player.setHealth(Math.min(player.getMaxHealth(),
                    player.getHealth() + player.getMaxHealth() - oldMaxHealth));
        } else {
            ShiChaoZhiZi.removeSecondFormAttributes(player);
        }
        player.sendOverlayMessage(Component.translatable(
                "skill.corpseorigin." + PATH + (enabling ? ".on" : ".off")));
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                enabling ? SoundEvents.WARDEN_ROAR : SoundEvents.WARDEN_HEARTBEAT,
                SoundSource.PLAYERS, 1.0F, enabling ? 0.7F : 1.2F);
    }
}
