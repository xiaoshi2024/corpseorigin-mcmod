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
                reconcileCorpseState(player);
                String role = CharacterManager.getInstance().getPlayerCharacterId(player);
                if (!role.equals(player.getAttachedOrCreate(ROLE))) player.setAttached(ROLE,role);
                if (!player.getAttachedOrCreate(DISGUISE).isEmpty() && (!"bianselong_zb".equals(role)
                        || !player.isAlive() || player.level().getGameTime()>=player.getAttachedOrCreate(DISGUISE_UNTIL)))
                    player.setAttached(DISGUISE,"");
            }
        });
    }

    /** 登录、重生发包前也执行，避免只同步角色名却发出尚未校正的尸兄状态。 */
    public static void reconcileCorpseState(net.minecraft.server.level.ServerPlayer player) {
        String role = CharacterManager.getInstance().getPlayerCharacterId(player);
        if (java.util.Set.of("longyou", "zuohufa", "kaiweinai", "tianxianbaobao_zb",
                "jingang_zb", "shichaozhizi", "bianselong_zb", "qingwa_zb", "hujie", "chongmu",
                "chongqun", "xiongxing_zb", "siyangyuan_zb", "kuaidiyuan_zb", "bianyi_guiyu",
                  "corpse_brother", "xiaojingang", "guigun_corpse", "hei_wuchou", "bai_wusheng").contains(role)) {
            var comp = xiaoshi2022.corpseorigin.component.PlayerCorpseComponent.get(player);
            if (!comp.isCorpse()) {
                xiaoshi2022.corpseorigin.component.PlayerCorpseComponent.setPlayerAsCorpse(player,
                        "longyou".equals(role)
                                ? xiaoshi2022.corpseorigin.component.PlayerCorpseComponent.TYPE_KING
                                : xiaoshi2022.corpseorigin.component.PlayerCorpseComponent.TYPE_NORMAL,
                        xiaoshi2022.corpseorigin.component.PlayerCorpseComponent.VARIANT_NO_EXOSKELETON);
            }
            // Repair existing saves as well as freshly selected advanced/animal roles.
            boolean restored = !xiaoshi2022.corpseorigin.event.ConsciousnessInteractions.requiresRecovery(role)
                    && !comp.hasConsciousness();
            if (restored) comp.restoreConsciousness();
            if (comp.getDataPublic().getInt("infection").orElse(0) != 100 || restored) {
                comp.setInfection(100);
                xiaoshi2022.corpseorigin.network.CorpseNetwork.sendInfectionSync(player);
                xiaoshi2022.corpseorigin.network.CorpseNetwork.broadcastPlayerCorpseSync(player);
            }
        }
    }
}
