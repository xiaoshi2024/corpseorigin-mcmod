package xiaoshi2022.corpseorigin.growth;

import java.nio.file.*;
import java.util.*;
import java.util.zip.ZipFile;
import java.nio.charset.StandardCharsets;

/** Read manifests in place; never extract user archives or guess model bindings. */
public final class OrganPackCatalog {
    private OrganPackCatalog() {}
    public static String load(Path directory, Map<String,OrganDefinition> output) {
        var errors=new ArrayList<String>();
        try {
            Files.createDirectories(directory);
            try(var paths=Files.list(directory)) {
                for(var path:paths.sorted().toList()) {
                    if(path.getFileName().toString().equals("examples"))continue;
                    try {
                        if(Files.isDirectory(path) && Files.exists(path.resolve("pack.mcmeta"))) {
                            if(!Files.exists(path.resolve("organ.json")))throw new IllegalArgumentException("缺少根目录organ.json器官定义");
                            try(var in=Files.newInputStream(path.resolve("organ.json"))){read(in,output);}
                        } else if(path.toString().toLowerCase(Locale.ROOT).endsWith(".zip")) {
                            try(var zip=new ZipFile(path.toFile())) {
                                if(zip.getEntry("pack.mcmeta")==null)throw new IllegalArgumentException("pack.mcmeta不在ZIP根目录");
                                var entry=zip.getEntry("organ.json");
                                if(entry==null)throw new IllegalArgumentException("缺少根目录organ.json器官定义");
                                try(var in=zip.getInputStream(entry)){read(in,output);}
                            }
                        }
                    }catch(Exception e){errors.add(path.getFileName()+"："+(e.getMessage()==null?"无法读取":e.getMessage()));}
                }
            }
        }catch(Exception e){errors.add("器官资源包目录读取失败");}
        return errors.isEmpty()?"":errors.getFirst();
    }
    private static void read(java.io.InputStream in,Map<String,OrganDefinition> output)throws Exception {
        byte[] bytes=in.readNBytes(262145);
        if(bytes.length>262144)throw new IllegalArgumentException("organ.json超过256KB");
        var entries=OrganLibrary.JSON.fromJson(new String(bytes,StandardCharsets.UTF_8),OrganDefinition[].class);
        if(entries==null)throw new IllegalArgumentException("organ.json必须为器官定义数组");
        var candidate=new LinkedHashMap<>(output);
        for(var def:entries){
            if(def==null || !def.valid())throw new IllegalArgumentException("器官定义字段无效");
            candidate.put(def.id(),def);
        }
        if(candidate.size()>128 || OrganLibrary.JSON.toJson(candidate.values()).length()>262144)
            throw new IllegalArgumentException("器官目录超出同步限制");
        output.clear();output.putAll(candidate);
    }
}
