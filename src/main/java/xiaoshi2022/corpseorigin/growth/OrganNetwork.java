package xiaoshi2022.corpseorigin.growth;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntitySpawnReason;
import xiaoshi2022.corpseorigin.entity.LowerLevelZbEntity;
import xiaoshi2022.corpseorigin.registry.ModEntities;
import xiaoshi2022.corpseorigin.component.PlayerCorpseComponent;
import xiaoshi2022.corpseorigin.network.OrganEditorPayload;
import xiaoshi2022.corpseorigin.util.LocalizedException;

public final class OrganNetwork {
    private static long lastReload=Long.MIN_VALUE;
    private OrganNetwork() {}
    public static void register() {
        PayloadTypeRegistry.serverboundPlay().register(OrganEditorPayload.TYPE, OrganEditorPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(OrganEditorPayload.Summon.TYPE, OrganEditorPayload.Summon.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(OrganEditorPayload.Catalog.TYPE, OrganEditorPayload.Catalog.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(OrganEditorPayload.Result.TYPE, OrganEditorPayload.Result.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(OrganEvolutionPayload.TYPE, OrganEvolutionPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(OrganEvolutionPayload.TYPE,(packet,context)->context.server().execute(()->{
            if(packet.action().equals("water_fire")||packet.action().equals("water_drink")){
                context.player().sendOverlayMessage(OrganAbilities.use(context.player(),packet.action().equals("water_drink")));
                return;
            }
            if(packet.action().equals("refresh")){
                long now=context.player().level().getGameTime();
                if(lastReload==Long.MIN_VALUE || now<lastReload || now-lastReload>=100){OrganLibrary.load();lastReload=now;}
                for(var recipient:context.server().getPlayerList().getPlayers())
                    ServerPlayNetworking.send(recipient,new OrganEditorPayload.Catalog(OrganLibrary.JSON.toJson(OrganLibrary.definitions())));
                Component result=OrganLibrary.packStatus.equals(Component.empty())?Component.translatable("message.corpseorigin.organ.refreshed", OrganLibrary.definitions().size()):OrganLibrary.packStatus;
                ServerPlayNetworking.send(context.player(),new OrganEditorPayload.Result(true,result));
                return;
            }
            Component message=OrganEvolution.act(context.player(),packet.organ(),packet.action());
            ServerPlayNetworking.send(context.player(),new OrganEditorPayload.Result(message.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t && t.getKey().equals("message.corpseorigin.organ.evolved"),message));
        }));
        ServerLifecycleEvents.SERVER_STARTED.register(s -> OrganLibrary.load());
        ServerPlayConnectionEvents.JOIN.register((handler,sender,server) ->
                ServerPlayNetworking.send(handler.player, new OrganEditorPayload.Catalog(OrganLibrary.JSON.toJson(OrganLibrary.definitions()))));
        ServerPlayNetworking.registerGlobalReceiver(OrganEditorPayload.TYPE, (packet,context) -> context.server().execute(() -> {
            var player = context.player();
            try {
                if (!player.isAlive() || player.isSpectator())
                    throw new LocalizedException("message.corpseorigin.organ.validation.12");
                var slots = OrganLibrary.parseSlots(packet.json());
                OrganEvolution.tick(player);
                boolean corpse = PlayerCorpseComponent.isCorpse(player);
                int level = xiaoshi2022.corpseorigin.skill.EvolutionManager.getLevel(
                        xiaoshi2022.corpseorigin.character.PlayerCharacterData.get(player).getEarnedPoints(player.getUUID()));
                if (!slots.isEmpty() && !player.isCreative() && !corpse)
                    throw new LocalizedException("message.corpseorigin.organ.validation.13");
                boolean appearanceOnly = false;
                for (var slot : slots) {
                    var def = OrganLibrary.definitions().stream().filter(d -> d.id().equals(slot.organ())).findFirst()
                            .orElseThrow(() -> new LocalizedException("message.corpseorigin.organ.missing",slot.organ()));
                    boolean unlocked = OrganEvolution.stage(player, def.id()) > 0;
                    if (!OrganAccessRules.mayMount(player.isCreative(), corpse, level, def.trait(), unlocked)) {
throw new LocalizedException("message.corpseorigin.organ.locked",def.displayName());
                    }
                    appearanceOnly |= !def.trait().equals("cosmetic") && !unlocked;
                }
                var body = player.getAttachedOrCreate(SurvivalGrowth.BODY).copy();
                body.putString(OrganLibrary.BODY_KEY, OrganLibrary.JSON.toJson(slots));
                player.setAttached(SurvivalGrowth.BODY, body);
                ServerPlayNetworking.send(player, new OrganEditorPayload.Result(true, appearanceOnly
                        ? Component.translatable("message.corpseorigin.organ.appearance_saved") : Component.translatable("message.corpseorigin.organ.saved")));
            } catch (IllegalArgumentException e) {
                Component reason=LocalizedException.describe(e);
                ServerPlayNetworking.send(player, new OrganEditorPayload.Result(false,
                        Component.translatable("message.corpseorigin.organ.save_failed", reason)));
            } catch (Exception e) {
                xiaoshi2022.corpseorigin.CorpseOrigin.LOGGER.warn("Cannot save organ loadout for {}", player.getUUID(), e);
                ServerPlayNetworking.send(player, new OrganEditorPayload.Result(false, Component.translatable("message.corpseorigin.organ.server_error")));
            }
        }));
        ServerPlayNetworking.registerGlobalReceiver(OrganEditorPayload.Summon.TYPE, (packet, context) ->
                context.server().execute(() -> {
                    var player = context.player();
                    if (!player.isAlive() || player.isSpectator()) return;
                    if (!player.isCreative() && countFlesh(player) < 20) {
                        player.sendSystemMessage(Component.translatable("message.corpseorigin.organ.summon_need_flesh", 20));
                        return;
                    }
                    var level = (net.minecraft.server.level.ServerLevel) player.level();
                    LowerLevelZbEntity zb = ModEntities.LOWER_LEVEL_ZB.create(level, EntitySpawnReason.TRIGGERED);
                    if (zb == null) return;
                    zb.setPlayerSkinName(player.getGameProfile().name());
                    zb.setCustomId(player.getStringUUID());
                    zb.setOrganLoadout(player.getAttachedOrCreate(SurvivalGrowth.BODY)
                            .getStringOr(OrganLibrary.BODY_KEY, "[]"));
                    zb.setPos(player.getX() + 2.0, player.getY(), player.getZ() + 2.0);
                    zb.setYRot(player.getYRot());
                    if (level.noCollision(zb) && level.addFreshEntity(zb)) {
                        if (!player.isCreative()) consumeFlesh(player, 20);
                        player.sendSystemMessage(Component.translatable("message.corpseorigin.organ.summoned"));
                    }
                }));
    }

    private static int countFlesh(net.minecraft.server.level.ServerPlayer player) {
        int total = 0;
        var inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            var stack = inventory.getItem(slot);
            if (stack.is(xiaoshi2022.corpseorigin.registry.ModItems.ZBR_FLESH)) total += stack.getCount();
        }
        return total;
    }

    private static void consumeFlesh(net.minecraft.server.level.ServerPlayer player, int amount) {
        var inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize() && amount > 0; slot++) {
            var stack = inventory.getItem(slot);
            if (!stack.is(xiaoshi2022.corpseorigin.registry.ModItems.ZBR_FLESH)) continue;
            int consumed = Math.min(amount, stack.getCount());
            stack.shrink(consumed);
            amount -= consumed;
        }
        inventory.setChanged();
    }
}
