package xiaoshi2022.corpseorigin.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.chat.Component;
import xiaoshi2022.corpseorigin.growth.OrganDefinition;
import xiaoshi2022.corpseorigin.growth.OrganEvolutionPayload;
import xiaoshi2022.corpseorigin.growth.OrganLibrary;
import xiaoshi2022.corpseorigin.growth.OrganPackCatalog;
import xiaoshi2022.corpseorigin.network.OrganEditorPayload;

import java.util.Arrays;
import java.util.List;

public final class OrganClient {
    public static List<OrganDefinition> catalog = List.of();
    public static Component status = Component.empty();
    public static boolean pending;
    private static boolean refreshing;
    public static void refreshPacks(){
        if(refreshing)return;
        var mc=net.minecraft.client.Minecraft.getInstance();
        refreshing=true;status=Component.translatable("gui.corpseorigin.label.088");
        try{
            var root=net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve("corpseorigin/organ");
            java.nio.file.Files.createDirectories(root);
            var repository=mc.getResourcePackRepository();
            repository.reload();
            var selected=new java.util.LinkedHashSet<>(repository.getSelectedIds());
            try(var paths=java.nio.file.Files.list(root)){
                for(var path:paths.toList()){
                    String id="file/"+path.getFileName();
                    if(repository.isAvailable(id))selected.add(id);
                }
            }
            repository.setSelected(selected);
            mc.reloadResourcePacks().whenComplete((ignored,error)->mc.execute(()->{
                refreshing=false;
                if(error!=null){status=Component.translatable("gui.corpseorigin.label.089");return;}
                xiaoshi2022.corpseorigin.client.render.layer.CustomOrganLayer.clearCatalog();
                var local=new java.util.LinkedHashMap<String,OrganDefinition>();
                Component problem=OrganPackCatalog.load(root,local,new java.util.LinkedHashMap<>());
                if(!problem.equals(Component.empty())&&mc.player!=null)mc.player.sendSystemMessage(problem);
                if(mc.player!=null)ClientPlayNetworking.send(new OrganEvolutionPayload("","refresh"));
                status=Component.translatable("gui.corpseorigin.label.090");
            }));
        }catch(Exception e){refreshing=false;status=Component.translatable("gui.corpseorigin.label.091");
            xiaoshi2022.corpseorigin.CorpseOrigin.LOGGER.warn("Cannot refresh organ packs",e);}
    }
    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(OrganEditorPayload.Catalog.TYPE, (p,c)->c.client().execute(()-> {
            try { catalog = Arrays.stream(OrganLibrary.JSON.fromJson(p.json(),OrganDefinition[].class)).filter(OrganDefinition::valid).limit(128).toList(); }
            catch (Exception e) { catalog = List.of(); }
            xiaoshi2022.corpseorigin.client.render.layer.CustomOrganLayer.clearCatalog();
            if(c.client().gui.screen() instanceof OrganEditorScreen screen)screen.refreshCatalog();
        }));
        ClientPlayNetworking.registerGlobalReceiver(OrganEditorPayload.Result.TYPE,(p,c)->c.client().execute(()-> {
            pending=false; status=p.message();
        }));
        ClientPlayConnectionEvents.DISCONNECT.register((h,c)-> {catalog=List.of();pending=false;status=Component.empty();
            xiaoshi2022.corpseorigin.client.render.layer.CustomOrganLayer.clearCatalog();});
    }
}
