package xiaoshi2022.corpseorigin.growth;
import net.minecraft.network.chat.Component;
import xiaoshi2022.corpseorigin.util.LocalizedException;

import java.nio.file.*;
import java.util.*;
import java.util.zip.ZipFile;
import java.nio.charset.StandardCharsets;

/** Read manifests in place; never extract user archives or guess model bindings. */
public final class OrganPackCatalog {
    private OrganPackCatalog() {}
    public static Component load(Path directory, Map<String,OrganDefinition> output,Map<String,OrganLibrary.Origin> origins) {
        var errors=new ArrayList<Component>();
        try {
            Files.createDirectories(directory);
            try(var paths=Files.list(directory)) {
                for(var path:paths.sorted().toList()) {
                    if(path.getFileName().toString().equals("examples"))continue;
                    // 随模组分发的示例包只给玩家预览，不算玩家自定义，不参与尸兄突变
                    var origin=path.getFileName().toString().equals(OrganLibrary.EXAMPLE_PACK_ZIP)
                            ?OrganLibrary.Origin.EXAMPLE:OrganLibrary.Origin.PACK;
                    try {
                        if(Files.isDirectory(path) && Files.exists(path.resolve("pack.mcmeta"))) {
                            if(!Files.exists(path.resolve("organ.json")))throw new LocalizedException("message.corpseorigin.organ.validation.6");
                            try(var in=Files.newInputStream(path.resolve("organ.json"))){read(in,output,origins,origin);}
                        } else if(path.toString().toLowerCase(Locale.ROOT).endsWith(".zip")) {
                            try(var zip=new ZipFile(path.toFile())) {
                                if(zip.getEntry("pack.mcmeta")==null)throw new LocalizedException("message.corpseorigin.organ.validation.7");
                                var entry=zip.getEntry("organ.json");
                                if(entry==null)throw new LocalizedException("message.corpseorigin.organ.validation.6");
                                try(var in=zip.getInputStream(entry)){read(in,output,origins,origin);}
                            }
                        }
                    }catch(Exception e){errors.add(Component.translatable("message.corpseorigin.organ.pack_error", path.getFileName().toString(), LocalizedException.describe(e)));}
                }
            }
        }catch(Exception e){errors.add(Component.translatable("message.corpseorigin.organ.directory_error"));}
        return errors.isEmpty()?Component.empty():errors.getFirst();
    }
    private static void read(java.io.InputStream in,Map<String,OrganDefinition> output,Map<String,OrganLibrary.Origin> origins,OrganLibrary.Origin origin)throws Exception {
        byte[] bytes=in.readNBytes(262145);
        if(bytes.length>262144)throw new LocalizedException("message.corpseorigin.organ.validation.8");
        var entries=OrganLibrary.JSON.fromJson(new String(bytes,StandardCharsets.UTF_8),OrganDefinition[].class);
        if(entries==null)throw new LocalizedException("message.corpseorigin.organ.validation.9");
        var candidate=new LinkedHashMap<>(output);
        var candidateOrigins=new LinkedHashMap<>(origins);
        for(var def:entries){
            if(def==null || !def.valid())throw new LocalizedException("message.corpseorigin.organ.validation.10");
            candidate.put(def.id(),def);
            candidateOrigins.put(def.id(),origin);
        }
        if(candidate.size()>128 || OrganLibrary.JSON.toJson(candidate.values()).length()>262144)
            throw new LocalizedException("message.corpseorigin.organ.validation.11");
        output.clear();output.putAll(candidate);
        origins.clear();origins.putAll(candidateOrigins);
    }
}
