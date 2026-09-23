package xiaoshi2022.corpseorigin.skill.heixiaofei;

import net.fabricmc.fabric.api.attachment.v1.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.*;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.commands.Commands;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.item.ItemStack;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.character.*;
import xiaoshi2022.corpseorigin.network.*;

public final class HeartImplant {
    public static final AttachmentType<Boolean> FEIGNING = AttachmentRegistry.create(CorpseOrigin.id("heart_feigning"),
            b -> b.initializer(() -> false).syncWith(ByteBufCodecs.BOOL, AttachmentSyncPredicate.all()));
    private static final net.minecraft.resources.Identifier IMMOBILE = CorpseOrigin.id("heart_immobile");
    private HeartImplant() {}
    public static boolean active(Player p) { return p.getAttachedOrCreate(FEIGNING); }

    public static InteractionResult implant(ServerPlayer actor, ServerPlayer target, ItemStack stack) {
        if (!target.isAlive() || target.isSpectator() || active(actor) || active(target)
                || !"heixiaofei".equals(CharacterManager.getInstance().getPlayerCharacterId(target))) {
            actor.sendOverlayMessage(Component.translatable("item.corpseorigin.black_gold_heart.invalid"));
            return InteractionResult.FAIL;
        }
        var data = PlayerCharacterData.get(target);
        if (xiaoshi2022.corpseorigin.component.PlayerRelicComponent.has(target, "black_gold_heart")) {
            actor.sendOverlayMessage(Component.translatable("item.corpseorigin.black_gold_heart.already"));
            return InteractionResult.FAIL;
        }
        data.learnSkill(target.getUUID(), "black_gold_heart");
        // Actual implantation also satisfies the existing organ/infection progression.
        // Learning the skill through commands or the tree alone is not an implanted organ.
        xiaoshi2022.corpseorigin.component.PlayerRelicComponent.grant(target, "black_gold_heart");
        if (!actor.getAbilities().instabuild) stack.shrink(1);
        target.stopRiding();
        target.stopUsingItem();
        target.setSprinting(false);
        target.setAttached(FEIGNING, true);
        target.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        CorpseNetwork.sendEvolutionSync(target);
        return InteractionResult.SUCCESS;
    }

    public static void wake(ServerPlayer p) {
        if (!active(p)) return;
        clear(p);
        if (p.isAlive() && "heixiaofei".equals(CharacterManager.getInstance().getPlayerCharacterId(p))) {
            p.setHealth(p.getMaxHealth());
            p.sendOverlayMessage(Component.translatable("gui.corpseorigin.heart.awake"));
        }
    }
    private static void clear(ServerPlayer p) {
        p.setAttached(FEIGNING, false);
        var speed = p.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) speed.removeModifier(IMMOBILE);
        p.setPose(net.minecraft.world.entity.Pose.STANDING);
    }

    public static void register() {
        PayloadTypeRegistry.serverboundPlay().register(HeartWakePayload.TYPE, HeartWakePayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(HeartWakePayload.TYPE,
                (payload, context) -> context.server().execute(() -> wake(context.player())));
        CommandRegistrationCallback.EVENT.register((dispatcher, access, environment) ->
                dispatcher.register(Commands.literal("heartwake").executes(c -> {
                    wake(c.getSource().getPlayerOrException()); return 1;
                })));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> clear(handler.player));
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (var p : server.getPlayerList().getPlayers()) {
                var speed = p.getAttribute(Attributes.MOVEMENT_SPEED);
                if (!active(p)) {
                    if (speed != null) speed.removeModifier(IMMOBILE);
                    continue;
                }
                if (!p.isAlive() || p.isSpectator() || !"heixiaofei".equals(CharacterManager.getInstance().getPlayerCharacterId(p))) {
                    clear(p); continue;
                }
                if (speed != null && !speed.hasModifier(IMMOBILE)) speed.addTransientModifier(
                        new AttributeModifier(IMMOBILE, -1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
                p.setSprinting(false);
                p.stopUsingItem();
                if (p.tickCount % 60 == 0) p.sendOverlayMessage(Component.translatable("gui.corpseorigin.heart.hint"));
            }
        });
        AttackEntityCallback.EVENT.register((p,l,h,e,hit) -> active(p) ? InteractionResult.FAIL : InteractionResult.PASS);
        AttackBlockCallback.EVENT.register((p,l,h,pos,dir) -> active(p) ? InteractionResult.FAIL : InteractionResult.PASS);
        UseItemCallback.EVENT.register((p,l,h) -> active(p) ? InteractionResult.FAIL : InteractionResult.PASS);
        UseBlockCallback.EVENT.register((p,l,h,hit) -> active(p) ? InteractionResult.FAIL : InteractionResult.PASS);
        UseEntityCallback.EVENT.register((p,l,h,e,hit) -> active(p) ? InteractionResult.FAIL : InteractionResult.PASS);
    }
}
