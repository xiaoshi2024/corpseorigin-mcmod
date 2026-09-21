package xiaoshi2022.corpseorigin.item;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import xiaoshi2022.corpseorigin.entity.HamEntity;
import xiaoshi2022.corpseorigin.registry.ModItems;
import java.util.function.Consumer;

/** A stuffed toy with an inset eye, not a living rabbit. */
public class MagicianRabbitItem extends Item {
    public MagicianRabbitItem(Properties properties) { super(properties); }

    /** Commits the reward before consuming one toy, including in creative mode. */
    public static boolean bite(HamEntity ham, ItemStack toy) {
        if (!(ham.level() instanceof ServerLevel level) || !ham.isAlive()
                || !toy.is(ModItems.MAGICIAN_RABBIT) || toy.isEmpty()) return false;
        ItemEntity eye = new ItemEntity(level, ham.getX(), ham.getY() + .3, ham.getZ(),
                new ItemStack(ModItems.MEDUSA_EYE));
        eye.setDefaultPickUpDelay();
        if (!level.addFreshEntity(eye)) return false;
        toy.shrink(1);
        ham.triggerAnim("action", "bite");
        level.playSound(null, ham.blockPosition(), SoundEvents.GENERIC_EAT.value(), SoundSource.NEUTRAL, .8f, .8f);
        return true;
    }

    public static InteractionResult offer(ItemStack stack, Player player, HamEntity ham) {
        if (ham.isTame() && !ham.isOwnedBy(player)) return InteractionResult.FAIL;
        if (player.level().isClientSide()) return InteractionResult.SUCCESS;
        return bite(ham, stack) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
    }

    @Override public InteractionResult interactLivingEntity(ItemStack stack, Player player,
                                                            LivingEntity target, InteractionHand hand) {
        return target instanceof HamEntity ham ? offer(stack, player, ham) : InteractionResult.PASS;
    }

    @Override public InteractionResult useOn(UseOnContext context) {
        var player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        if (!(context.getLevel() instanceof ServerLevel level)) return InteractionResult.SUCCESS;
        if (!level.mayInteract(player, context.getClickedPos())) return InteractionResult.FAIL;
        var pos = context.getClickedPos().relative(context.getClickedFace());
        ItemEntity toy = new ItemEntity(level, pos.getX() + .5, pos.getY() + .15, pos.getZ() + .5,
                context.getItemInHand().copyWithCount(1), 0, 0, 0);
        if (!level.getWorldBorder().isWithinBounds(toy.getBoundingBox())
                || !level.noCollision(toy, toy.getBoundingBox()) || level.containsAnyLiquid(toy.getBoundingBox())) {
            player.sendOverlayMessage(Component.translatable("item.corpseorigin.magician_rabbit.no_space"));
            return InteractionResult.FAIL;
        }
        toy.setPickUpDelay(40);
        if (!level.addFreshEntity(toy)) return InteractionResult.FAIL;
        context.getItemInHand().consume(1, player);
        return InteractionResult.SUCCESS;
    }

    @Override public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                          Consumer<Component> lines, TooltipFlag flag) {
        lines.accept(Component.translatable("item.corpseorigin.magician_rabbit.tooltip"));
    }
}
