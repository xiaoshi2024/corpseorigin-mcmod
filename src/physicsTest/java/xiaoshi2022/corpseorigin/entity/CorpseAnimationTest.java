package xiaoshi2022.corpseorigin.entity;

import com.google.gson.*;
import java.nio.file.*;
import java.util.*;

/** Guards against a looping idle with mismatched endpoints or an early held last pose. */
public final class CorpseAnimationTest {
    private static final Path ROOT=Path.of("src/main/resources/assets/corpseorigin/geckolib");
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private static JsonArray vector(JsonElement value){
        if(value.isJsonArray())return value.getAsJsonArray();
        var o=value.getAsJsonObject();return vector(o.has("vector")?o.get("vector"):o.get("post"));
    }
    public static void main(String[] args)throws Exception{
        verify("lower_level_zb",Set.of("idle","gnaw"));
        verify("corpse_maggot",Set.of("idle","crawl","bite"));
        verify("corpse_ribs",Set.of("idle"));
        System.out.println("CorpseAnimationTest passed: exact loop endpoints, continuous keyed motion, valid bones and finite poses.");
    }
    private static void verify(String model,Set<String> clips)throws Exception{
        var geo=JsonParser.parseString(Files.readString(ROOT.resolve("models/entity/"+model+".geo.json"))).getAsJsonObject();
        Set<String> boneNames=new HashSet<>();
        for(var b:geo.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject().getAsJsonArray("bones"))boneNames.add(b.getAsJsonObject().get("name").getAsString());
        var animations=JsonParser.parseString(Files.readString(ROOT.resolve("animations/entity/"+model+".animation.json"))).getAsJsonObject().getAsJsonObject("animations");
        for(String clip:clips){
            var a=animations.getAsJsonObject(clip);check(a.get("loop").getAsBoolean(),clip+" must loop");double length=a.get("animation_length").getAsDouble();
            for(var bone:a.getAsJsonObject("bones").entrySet()){
                check(boneNames.contains(bone.getKey()),"missing bone "+bone.getKey());
                for(var channel:bone.getValue().getAsJsonObject().entrySet()){
                    TreeMap<Double,JsonArray> frames=new TreeMap<>();
                    for(var key:channel.getValue().getAsJsonObject().entrySet())frames.put(Double.valueOf(key.getKey()),vector(key.getValue()));
                    check(frames.firstKey()==0 && Math.abs(frames.lastKey()-length)<.0001,model+":"+clip+" must cover whole loop");
                    check(frames.firstEntry().getValue().equals(frames.lastEntry().getValue()),model+":"+clip+" must close "+bone.getKey());
                    double prev=0;boolean changes=false;
                    for(var key:frames.entrySet()){
                        check(key.getKey()-prev<=length/3,clip+" has an excessive unkeyed interval");prev=key.getKey();
                        for(var n:key.getValue())check(Double.isFinite(n.getAsDouble()),"non-finite pose");
                        changes|=!key.getValue().equals(frames.firstEntry().getValue());
                    }
                    check(changes,model+":"+clip+" channel stuck in same pose");
                }
            }
        }
    }
}
