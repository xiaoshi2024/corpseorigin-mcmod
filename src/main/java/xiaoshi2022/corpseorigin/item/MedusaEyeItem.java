package xiaoshi2022.corpseorigin.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import xiaoshi2022.corpseorigin.character.CharacterManager;
import xiaoshi2022.corpseorigin.character.PlayerCharacterData;
import xiaoshi2022.corpseorigin.network.CorpseNetwork;

import java.util.function.Consumer;

public class MedusaEyeItem extends Item {

    private static final String[] SKILL_PATHS = {
            "osmium_gold",
            "osmium_ice_spike"
    };

    public MedusaEyeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide()
                && !"xiaolu".equals(CharacterManager.getInstance().getPlayerCharacterId(player))) {
            player.sendOverlayMessage(
                    Component.translatable("item.corpseorigin.medusa_eye.not_xiaolu"));
            return InteractionResult.FAIL;
        }
        return super.use(level, player, hand);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide() && entity instanceof ServerPlayer sp) {
            PlayerCharacterData data = PlayerCharacterData.get(sp);

            boolean learnedAny = false;
            for (String path : SKILL_PATHS) {
                if (!data.hasLearned(sp.getUUID(), path)) {
                    data.learnSkill(sp.getUUID(), path);
                    learnedAny = true;
                }
            }

            if (learnedAny) {
                CorpseNetwork.sendEvolutionSync(sp);
                sp.sendOverlayMessage(
                        Component.translatable("item.corpseorigin.medusa_eye.learned")
                                .withStyle(ChatFormatting.GREEN));
            } else {
                sp.sendOverlayMessage(
                        Component.translatable("item.corpseorigin.medusa_eye.already")
                                .withStyle(ChatFormatting.GRAY));
            }
        }
        return super.finishUsingItem(stack, level, entity);
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.EAT;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> builder, TooltipFlag flag) {
        builder.accept(Component.translatable("item.corpseorigin.medusa_eye.tooltip")
                .withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, context, display, builder, flag);
    }
}