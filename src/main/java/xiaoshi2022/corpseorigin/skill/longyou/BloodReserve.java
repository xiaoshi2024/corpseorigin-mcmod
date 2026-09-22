package xiaoshi2022.corpseorigin.skill.longyou;

import net.fabricmc.fabric.api.attachment.v1.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.codec.ByteBufCodecs;
import xiaoshi2022.corpseorigin.CorpseOrigin;

public final class BloodReserve {
    public static final int MAX = 600, RESTORE_COST = 200;
    public static final AttachmentType<Integer> VALUE = AttachmentRegistry.create(CorpseOrigin.id("blood_reserve"),
            b -> b.initializer(() -> 0).persistent(com.mojang.serialization.Codec.INT)
                    .syncWith(ByteBufCodecs.VAR_INT, AttachmentSyncPredicate.all()));
    public static void init() {
        net.fabricmc.fabric.api.event.player.UseItemCallback.EVENT.register((player, level, hand) -> {
            var stack = player.getItemInHand(hand);
            if (!player.isShiftKeyDown() || !stack.is(xiaoshi2022.corpseorigin.registry.ModItems.ZBR_FLESH)
                    || !"longyou".equals(player.getAttachedOrCreate(
                        xiaoshi2022.corpseorigin.skill.chapter.ChapterActorState.ROLE)))
                return net.minecraft.world.InteractionResult.PASS;
            if (player instanceof ServerPlayer serverPlayer) {
                if (serverPlayer.getAttachedOrCreate(VALUE) >= MAX)
                    return net.minecraft.world.InteractionResult.FAIL;
                stack.shrink(1);
                add(serverPlayer, 20);
                serverPlayer.sendOverlayMessage(net.minecraft.network.chat.Component.literal("吸收血肉：气血 +20"));
            }
            return net.minecraft.world.InteractionResult.SUCCESS;
        });
    }
    public static void add(ServerPlayer p, int amount) {
        p.setAttached(VALUE, Math.clamp(p.getAttachedOrCreate(VALUE) + amount, 0, MAX));
    }
    public static boolean spend(ServerPlayer p) {
        if (p.getAttachedOrCreate(VALUE) < RESTORE_COST) {
            p.sendOverlayMessage(net.minecraft.network.chat.Component.literal("气血不足：恢复不死髅体需要 200 点气血"));
            return false;
        }
        add(p, -RESTORE_COST);
        return true;
    }
}
