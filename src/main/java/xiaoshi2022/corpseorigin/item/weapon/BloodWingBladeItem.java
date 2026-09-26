package xiaoshi2022.corpseorigin.item.weapon;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import xiaoshi2022.corpseorigin.skill.SkillManager;
import xiaoshi2022.corpseorigin.skill.k.BloodWingBladeSkill;

public final class BloodWingBladeItem extends Item {
    public BloodWingBladeItem(Properties properties) { super(properties); }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (player instanceof ServerPlayer serverPlayer) {
            return SkillManager.activate(serverPlayer, BloodWingBladeSkill.PATH, false)
                    ? InteractionResult.SUCCESS_SERVER : InteractionResult.FAIL;
        }
        return InteractionResult.SUCCESS;
    }
}
