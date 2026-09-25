package xiaoshi2022.corpseorigin.item;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import xiaoshi2022.corpseorigin.entity.ChapterBombEntity;
import xiaoshi2022.corpseorigin.registry.ModItems;

import java.util.function.Consumer;

/**
 * 八号台球 —— 也能当炸弹丢出去。
 * <p>
 * 右键投掷走 {@link ChapterBombEntity#launch}：出手带随机偏散、落地后滚动几 tick 再引爆，
 * 爆开的是 24 点范围伤害（比爆炸包裹的 16 点高）。每投出一颗消耗一个。
 */
public final class BilliardEightItem extends Item {

    public BilliardEightItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer server)) return InteractionResult.PASS;
        ChapterBombEntity.launch(server, ModItems.BILLIARD_EIGHT);
        if (!server.isCreative()) server.getItemInHand(hand).consume(1, server);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
            Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.corpseorigin.billiard_eight.desc"));
    }
}
