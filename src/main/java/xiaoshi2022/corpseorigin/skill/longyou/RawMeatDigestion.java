package xiaoshi2022.corpseorigin.skill.longyou;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.character.CharacterManager;
import xiaoshi2022.corpseorigin.character.KaiWeiNai;

/** One digestion queue per player; extra meals never accelerate the timer. */
public final class RawMeatDigestion {
    public static final int NONE = 0, SLOW = 1, INSTANT = 2;
    // Persist across logout, but deliberately do not copy the queue on death.
    public static final AttachmentType<Integer> PENDING = AttachmentRegistry.create(
            CorpseOrigin.id("raw_meat_pending"), b -> b.initializer(() -> 0)
                    .persistent(Codec.intRange(0, RawMeatDigestionRules.MAX_PENDING))
                    .syncWith(ByteBufCodecs.VAR_INT, AttachmentSyncPredicate.all()));
    private static final AttachmentType<Integer> TICKS = AttachmentRegistry.create(
            CorpseOrigin.id("raw_meat_ticks"), b -> b.initializer(() -> 0)
                    .persistent(Codec.intRange(0, RawMeatDigestionRules.TICKS_PER_POINT - 1)));
    private static final AttachmentType<Integer> MODE = AttachmentRegistry.create(
            CorpseOrigin.id("raw_meat_mode"), b -> b.initializer(() -> NONE)
                    .syncWith(ByteBufCodecs.VAR_INT, AttachmentSyncPredicate.all()));

    private RawMeatDigestion() {}

    public static int mode(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) return player.getAttachedOrCreate(MODE);
        if (!player.isAlive() || player.isSpectator() || !BloodReserve.isEligible(serverPlayer)) return NONE;
        return KaiWeiNai.ID.equals(CharacterManager.getInstance().getPlayerCharacterId(player)) ? INSTANT : SLOW;
    }

    public static boolean supports(Player player, ItemStack stack) {
        int mode = mode(player);
        return mode != NONE && (stack.is(ConventionalItemTags.RAW_MEAT_FOODS)
                || mode == INSTANT && stack.is(ConventionalItemTags.RAW_FISH_FOODS));
    }

    public static int accepted(Player player) {
        return RawMeatDigestionRules.accepted(player.getAttachedOrCreate(BloodReserve.VALUE),
                BloodReserve.MAX, player.getAttachedOrCreate(PENDING));
    }

    public static boolean canSupplement(Player player, ItemStack stack) {
        return player.isAlive() && !player.isSpectator() && supports(player, stack)
                && player.getAttachedOrCreate(BloodReserve.VALUE) < BloodReserve.MAX
                && (mode(player) == INSTANT || accepted(player) > 0);
    }

    public static void enqueue(ServerPlayer player) {
        int amount = accepted(player);
        if (amount <= 0) return;
        int pending = player.getAttachedOrCreate(PENDING);
        if (pending == 0) player.setAttached(TICKS, 0);
        player.setAttached(PENDING, pending + amount);
        player.sendOverlayMessage(Component.translatable("message.corpseorigin.raw_meat.queued",
                amount, pending + amount, RawMeatDigestionRules.MAX_PENDING));
    }

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                int mode = mode(player);
                if (player.getAttachedOrCreate(MODE) != mode) player.setAttached(MODE, mode);
                int pending = player.getAttachedOrCreate(PENDING);
                int ticks = player.getAttachedOrCreate(TICKS);
                var step = RawMeatDigestionRules.tick(pending, ticks, mode == SLOW,
                        BloodReserve.get(player) >= BloodReserve.MAX);
                if (step.pending() != pending) player.setAttached(PENDING, step.pending());
                if (step.ticks() != ticks) player.setAttached(TICKS, step.ticks());
                if (step.gained() > 0) BloodReserve.add(player, step.gained());
            }
        });
    }
}
