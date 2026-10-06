package xiaoshi2022.corpseorigin.item;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import xiaoshi2022.corpseorigin.entity.ChapterBombEntity;
import xiaoshi2022.corpseorigin.registry.ModItems;

/**
 * 爆炸包裹（玩家手投版）：右键直接消耗一个包裹丢出去，落地爆炸。
 * 与快递员尸兄的包裹炸弹技能共用 {@link ChapterBombEntity}。
 * 冷却 1 秒，防止手速连丢变成榴弹炮。
 */
public class ParcelBombItem extends Item {

    public ParcelBombItem(Properties properties) { super(properties); }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide()) {
            ChapterBombEntity.launch(player, ModItems.PARCEL_BOMB);
            stack.shrink(1);
            // 26.2 冷却按 ItemStack 记
            player.getCooldowns().addCooldown(stack, 20);
            if (player instanceof ServerPlayer sp)
                sp.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                        "message.corpseorigin.parcel_bomb.thrown"));
        }
        return InteractionResult.SUCCESS;
    }
}
