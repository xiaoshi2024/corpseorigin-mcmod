package xiaoshi2022.corpseorigin.client;

import java.util.List;
import java.util.Arrays;
import net.fabricmc.fabric.api.client.networking.v1.*;
import xiaoshi2022.corpseorigin.growth.*;
import xiaoshi2022.corpseorigin.network.OrganEditorPayload;

public final class OrganClient {
    public static List<OrganDefinition> catalog = List.of();
    public static String status = "";
    public static boolean pending;
    private static boolean refreshing;
    public static void refreshPacks(){
        if(refreshing)return;
        var mc=net.minecraft.client.Minecraft.getInstance();
        refreshing=true;status="正在扫描并加载器官包…";
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
                if(error!=null){status="资源包加载失败：检查pack.mcmeta、模型和动画格式";return;}
                xiaoshi2022.corpseorigin.client.render.layer.CustomOrganLayer.clearCatalog();
                var local=new java.util.LinkedHashMap<String,OrganDefinition>();
                String problem=OrganPackCatalog.load(root,local);
                if(!problem.isEmpty()&&mc.player!=null)mc.player.sendSystemMessage(net.minecraft.network.chat.Component.literal(problem));
                if(mc.player!=null)ClientPlayNetworking.send(new OrganEvolutionPayload("","refresh"));
                status="资源已加载，等待服务器更新器官列表…";
            }));
        }catch(Exception e){refreshing=false;status="刷新失败，请检查资源包目录与游戏日志";
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
        ClientPlayConnectionEvents.DISCONNECT.register((h,c)-> {catalog=List.of();pending=false;status="";
            xiaoshi2022.corpseorigin.client.render.layer.CustomOrganLayer.clearCatalog();});
    }
}
