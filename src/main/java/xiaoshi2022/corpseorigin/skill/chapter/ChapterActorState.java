package xiaoshi2022.corpseorigin.skill.chapter;

import net.fabricmc.fabric.api.attachment.v1.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.codec.ByteBufCodecs;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.character.CharacterManager;

/** Public appearance only; authoritative role selection remains in CharacterManager. */
public final class ChapterActorState {
    public static final AttachmentType<String> ROLE = AttachmentRegistry.create(CorpseOrigin.id("chapter_role"),
            builder -> builder.initializer(() -> "").syncWith(ByteBufCodecs.STRING_UTF8, AttachmentSyncPredicate.all()));
    public static final AttachmentType<Long> BITE_UNTIL = AttachmentRegistry.create(CorpseOrigin.id("fish_bite_until"),
            builder -> builder.initializer(() -> 0L).syncWith(ByteBufCodecs.VAR_LONG, AttachmentSyncPredicate.all()));
    private ChapterActorState() {}
    public static final String XIAOHUI="xiaohui";
    public static final AttachmentType<com.mojang.authlib.GameProfile> DISGUISE_PROFILE=AttachmentRegistry.create(CorpseOrigin.id("chameleon_profile"),
            builder->builder.syncWith(ByteBufCodecs.GAME_PROFILE,AttachmentSyncPredicate.all()));
    public static final AttachmentType<String> DISGUISE = AttachmentRegistry.create(CorpseOrigin.id("chameleon_skin"),
            builder -> builder.initializer(() -> "").syncWith(ByteBufCodecs.STRING_UTF8, AttachmentSyncPredicate.all()));
    public static final AttachmentType<Long> DISGUISE_UNTIL = AttachmentRegistry.create(CorpseOrigin.id("chameleon_until"),
            builder -> builder.initializer(() -> 0L));
    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (var player : server.getPlayerList().getPlayers()) {
                String role = CharacterManager.getInstance().getPlayerCharacterId(player);
                if (!role.equals(player.getAttachedOrCreate(ROLE))) player.setAttached(ROLE,role);
                if (!player.getAttachedOrCreate(DISGUISE).isEmpty() && (!"bianselong_zb".equals(role)
                        || !player.isAlive() || player.level().getGameTime()>=player.getAttachedOrCreate(DISGUISE_UNTIL)))
                    player.setAttached(DISGUISE,"");
            }
        });
    }
}
