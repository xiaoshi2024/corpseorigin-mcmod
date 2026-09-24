package xiaoshi2022.corpseorigin.growth;

import net.fabricmc.fabric.api.networking.v1.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import xiaoshi2022.corpseorigin.network.OrganEditorPayload;
import xiaoshi2022.corpseorigin.component.PlayerCorpseComponent;

public final class OrganNetwork {
    private static long lastReload=Long.MIN_VALUE;
    private OrganNetwork() {}
    public static void register() {
        PayloadTypeRegistry.serverboundPlay().register(OrganEditorPayload.TYPE, OrganEditorPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(OrganEditorPayload.Catalog.TYPE, OrganEditorPayload.Catalog.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(OrganEditorPayload.Result.TYPE, OrganEditorPayload.Result.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(OrganEvolutionPayload.TYPE, OrganEvolutionPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(OrganEvolutionPayload.TYPE,(packet,context)->context.server().execute(()->{
            if(packet.action().equals("water_fire")||packet.action().equals("water_drink")){
                context.player().sendOverlayMessage(net.minecraft.network.chat.Component.literal(OrganAbilities.use(context.player(),packet.action().equals("water_drink"))));
                return;
            }
            if(packet.action().equals("refresh")){
                long now=context.player().level().getGameTime();
                if(lastReload==Long.MIN_VALUE || now<lastReload || now-lastReload>=100){OrganLibrary.load();lastReload=now;}
                for(var recipient:context.server().getPlayerList().getPlayers())
                    ServerPlayNetworking.send(recipient,new OrganEditorPayload.Catalog(OrganLibrary.JSON.toJson(OrganLibrary.definitions())));
                String result=OrganLibrary.packStatus.isEmpty()?"目录已刷新，共"+OrganLibrary.definitions().size()+"个器官；联机新器官需服务器安装同一ZIP":OrganLibrary.packStatus;
                ServerPlayNetworking.send(context.player(),new OrganEditorPayload.Result(true,result.substring(0,Math.min(240,result.length()))));
                return;
            }
            String message=OrganEvolution.act(context.player(),packet.organ(),packet.action());
            ServerPlayNetworking.send(context.player(),new OrganEditorPayload.Result(message.startsWith("已进化"),message));
        }));
        ServerLifecycleEvents.SERVER_STARTED.register(s -> OrganLibrary.load());
        ServerPlayConnectionEvents.JOIN.register((handler,sender,server) ->
                ServerPlayNetworking.send(handler.player, new OrganEditorPayload.Catalog(OrganLibrary.JSON.toJson(OrganLibrary.definitions()))));
        ServerPlayNetworking.registerGlobalReceiver(OrganEditorPayload.TYPE, (packet,context) -> context.server().execute(() -> {
            var player = context.player();
            try {
                if (!player.isAlive() || player.isSpectator())
                    throw new IllegalArgumentException("死亡或旁观状态无法保存");
                var slots = OrganLibrary.parseSlots(packet.json());
                OrganEvolution.tick(player);
                boolean corpse = PlayerCorpseComponent.isCorpse(player);
                int level = xiaoshi2022.corpseorigin.skill.EvolutionManager.getLevel(
                        xiaoshi2022.corpseorigin.character.PlayerCharacterData.get(player).getEarnedPoints(player.getUUID()));
                if (!slots.isEmpty() && !player.isCreative() && !corpse)
                    throw new IllegalArgumentException("生存模式需要尸兄身体；创造模式可直接装配");
                boolean appearanceOnly = false;
                for (var slot : slots) {
                    var def = OrganLibrary.definitions().stream().filter(d -> d.id().equals(slot.organ())).findFirst()
                            .orElseThrow(() -> new IllegalArgumentException("服务器目录缺少器官：" + slot.organ()));
                    boolean unlocked = OrganEvolution.stage(player, def.id()) > 0;
                    if (!OrganAccessRules.mayMount(player.isCreative(), corpse, level, def.trait(), unlocked)) {
                        String part = switch (def.trait()) {
                            case "wings" -> "翅膀";
                            case "gills" -> "水肺";
                            case "vampire" -> "吸血鬼体质";
                            default -> def.trait();
                        };
                        throw new IllegalArgumentException("尚未解锁器官：" + def.name() + "；在器官进化天梯花5点解锁I阶");
                    }
                    appearanceOnly |= !def.trait().equals("cosmetic") && !unlocked;
                }
                var body = player.getAttachedOrCreate(SurvivalGrowth.BODY).copy();
                body.putString(OrganLibrary.BODY_KEY, OrganLibrary.JSON.toJson(slots));
                player.setAttached(SurvivalGrowth.BODY, body);
                ServerPlayNetworking.send(player, new OrganEditorPayload.Result(true, appearanceOnly
                        ? "已保存外观；飞行、呼吸、吸血能力需对应进化" : "器官装配已保存并同步"));
            } catch (IllegalArgumentException e) {
                String reason = e.getMessage() == null ? "预设格式无效" : e.getMessage();
                ServerPlayNetworking.send(player, new OrganEditorPayload.Result(false,
                        "保存失败：" + reason.substring(0, Math.min(180, reason.length()))));
            } catch (Exception e) {
                xiaoshi2022.corpseorigin.CorpseOrigin.LOGGER.warn("Cannot save organ loadout for {}", player.getUUID(), e);
                ServerPlayNetworking.send(player, new OrganEditorPayload.Result(false, "保存失败：服务器处理异常，请查看日志"));
            }
        }));
    }
}
