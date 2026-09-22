package xiaoshi2022.corpseorigin.event;
import net.fabricmc.fabric.api.event.player.*;
import net.fabricmc.fabric.api.attachment.v1.*;
import net.minecraft.world.InteractionResult;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.component.PlayerCorpseComponent;
import xiaoshi2022.corpseorigin.registry.ModItems;
import xiaoshi2022.corpseorigin.network.CorpseNetwork;
public final class ConsciousnessInteractions {
    /** Only ordinary corpse roles use the random consciousness/recovery progression. */
    public static boolean requiresRecovery(String role) {
        return "corpse_brother".equals(role);
    }
    private static boolean restricted(net.minecraft.world.entity.player.Player player) {
        return player instanceof ServerPlayer sp
                && requiresRecovery(xiaoshi2022.corpseorigin.character.CharacterManager.getInstance().getPlayerCharacterId(sp))
                && PlayerCorpseComponent.get(player).isMindless();
    }
    public static final AttachmentType<Integer> FLESH = AttachmentRegistry.create(CorpseOrigin.id("consciousness_flesh"),
            b -> b.initializer(() -> 0).persistent(com.mojang.serialization.Codec.INT));
    public static void register() {
        UseBlockCallback.EVENT.register((p, level, hand, hit) -> {
            if (level.isClientSide() || !restricted(p)) return InteractionResult.PASS;
            p.sendOverlayMessage(Component.literal("尚未恢复意识，无法操作方块。吞食血肉或穆博士的眼睛恢复意识。"));
            return InteractionResult.FAIL;
        });
        UseItemCallback.EVENT.register((p, level, hand) -> {
            var stack = p.getItemInHand(hand);
            boolean eye = stack.is(ModItems.DR_MU_EYE);
            if (!eye && !stack.is(ModItems.ZBR_FLESH)) return InteractionResult.PASS;
            if (level.isClientSide()) return InteractionResult.PASS;
            var comp = PlayerCorpseComponent.get(p);
            if (!restricted(p)) return InteractionResult.PASS;
            stack.shrink(1);
            int eaten = p.getAttachedOrCreate(FLESH) + 1;
            p.setAttached(FLESH, eaten);
            if (eye || eaten >= 20) {
                comp.restoreConsciousness();
                p.sendOverlayMessage(Component.literal("你恢复了自我意识，可以操作门和工作台了。"));
                if (p instanceof ServerPlayer sp) CorpseNetwork.broadcastPlayerCorpseSync(sp);
            } else p.sendOverlayMessage(Component.literal("吞食血肉：意识恢复进度 " + eaten + " / 20"));
            return InteractionResult.SUCCESS;
        });
    }
}
