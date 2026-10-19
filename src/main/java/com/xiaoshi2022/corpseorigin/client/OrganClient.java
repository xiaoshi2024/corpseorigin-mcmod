package com.xiaoshi2022.corpseorigin.client;

import com.xiaoshi2022.corpseorigin.CorpseOrigin;
import com.xiaoshi2022.corpseorigin.growth.*;
import com.xiaoshi2022.corpseorigin.network.OrganNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.*;
import net.minecraft.server.packs.repository.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.nio.file.*;
import java.util.*;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = CorpseOrigin.MODID, value = Dist.CLIENT)
public final class OrganClient {
    public static List<OrganDefinition> catalog = List.of();
    public static Component status = OrganText.text("hint");
    public static boolean pending;
    public static boolean reloading;
    public static int generation;
    public static final KeyMapping EDITOR = new KeyMapping("key.corpseorigin.organ_editor", GLFW.GLFW_KEY_KP_9, "key.categories.corpseorigin");
    private OrganClient() {}
    public static void state(UUID id) {
        if (Minecraft.getInstance().screen instanceof OrganEditorScreen screen) screen.receiveState(id);
    }
    public static void accept(OrganNetwork.Reply reply) {
        if (!reply.catalog().isEmpty()) {
            var defs = OrganLibrary.JSON.fromJson(reply.catalog(), OrganDefinition[].class);
            if (defs == null || defs.length > 128 || Arrays.stream(defs).anyMatch(d -> d == null || !d.valid())) {
                status = OrganText.text("bad_catalog"); pending = false; return;
            }
            catalog = Arrays.stream(defs).map(OrganDefinition::normalized).toList(); generation++;
            com.xiaoshi2022.corpseorigin.client.render.layer.CustomOrganLayer.clear();
        }
        pending = false; status = reply.status();
        if (Minecraft.getInstance().screen instanceof OrganEditorScreen screen) screen.refreshCatalog();
    }
    public static void refreshPacks(int target) {
        if (reloading) return;
        var mc = Minecraft.getInstance(); reloading = true; status = OrganText.text("loading");
        // Required packs discovered by our source are selected automatically by PackRepository.
        mc.getResourcePackRepository().reload();
        mc.reloadResourcePacks().whenComplete((v, error) -> mc.execute(() -> {
            reloading = false;
            com.xiaoshi2022.corpseorigin.client.render.layer.CustomOrganLayer.clear(); generation++;
            if (error != null) { status = OrganText.text("reload_failed"); return; }
            if (mc.getConnection() != null) OrganNetwork.request("refresh", target, "[]");
        }));
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        var mc = Minecraft.getInstance();
        while (EDITOR.consumeClick()) if (mc.player != null && mc.screen == null) {
            mc.setScreen(new OrganEditorScreen()); OrganNetwork.request("catalog", -1, "[]");
        }
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut e) {
        catalog = List.of(); pending = false; status = Component.empty(); generation++;
        NativeCorpseState.clearClientState(); com.xiaoshi2022.corpseorigin.client.render.layer.CustomOrganLayer.clear();
    }
    @Mod.EventBusSubscriber(modid = CorpseOrigin.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        @SubscribeEvent public static void keys(RegisterKeyMappingsEvent e) { e.register(EDITOR); }
        @SubscribeEvent public static void reload(RegisterClientReloadListenersEvent e) {
            e.registerReloadListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener) manager -> {
                com.xiaoshi2022.corpseorigin.client.render.layer.CustomOrganLayer.clear(); generation++;
            });
        }
        @SubscribeEvent public static void packs(net.minecraftforge.event.AddPackFindersEvent e) {
            if (e.getPackType() != PackType.CLIENT_RESOURCES) return;
            e.addRepositorySource(output -> {
                try {
                    int index = 0;
                    for (Path source : OrganPacks.sources(OrganLibrary.directory())) {
                        try {
                            OrganPacks.readManifest(source, new LinkedHashMap<>(), new HashSet<>());
                            Path cache = OrganLibrary.directory().resolveSibling("organ-cache").resolve(UUID.randomUUID() + ".zip");
                            OrganPacks.convert(source, cache);
                            String id = "corpseorigin-organ/" + source.getFileName();
                            Pack pack = Pack.readMetaAndCreate(id, Component.literal(source.getFileName().toString()), true,
                                    name -> new FilePackResources(name, cache.toFile(), false), PackType.CLIENT_RESOURCES, Pack.Position.TOP, PackSource.DEFAULT);
                            if (pack != null) output.accept(pack);
                            if (++index >= 128) break;
                        } catch (Exception ex) {
                            status = OrganText.text("pack_error", source.getFileName().toString(), OrganText.failure(ex));
                            CorpseOrigin.LOGGER.warn("Cannot load organ pack {}", source, ex);
                        }
                    }
                } catch (Exception ex) { CorpseOrigin.LOGGER.warn("Cannot discover organ packs", ex); }
            });
        }
    }
}
