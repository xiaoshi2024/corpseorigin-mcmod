package xiaoshi2022.corpseorigin.growth;

import com.mojang.serialization.Codec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.*;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import java.util.*;

/** Per-player, per-dimension visited chunks. Does not infer exploration from world generation. */
public final class ThermalSurveyData extends SavedData {
    private final Map<String,Set<Long>> visited=new HashMap<>();
    public static final Codec<ThermalSurveyData> CODEC=Codec.unboundedMap(Codec.STRING,Codec.LONG.listOf())
            .xmap(ThermalSurveyData::new,ThermalSurveyData::encode);
    public static final SavedDataType<ThermalSurveyData> TYPE=new SavedDataType<>(CorpseOrigin.id("thermal_surveys"),ThermalSurveyData::new,CODEC,null);
    public ThermalSurveyData(){}
    private ThermalSurveyData(Map<String,List<Long>> saved){saved.forEach((k,v)->visited.put(k,new HashSet<>(v)));}
    private Map<String,List<Long>> encode(){Map<String,List<Long>> result=new HashMap<>();visited.forEach((k,v)->result.put(k,List.copyOf(v)));return result;}
    public static ThermalSurveyData get(ServerPlayer p){return p.level().getServer().overworld().getDataStorage().computeIfAbsent(TYPE);}
    private static String key(ServerPlayer p){return p.getUUID()+"@"+p.level().dimension().identifier();}
    public void visit(ServerPlayer p){if(visited.computeIfAbsent(key(p),k->new HashSet<>()).add(p.chunkPosition().pack()))setDirty();}
    public Set<Long> chunks(ServerPlayer p){return Collections.unmodifiableSet(visited.getOrDefault(key(p),Set.of()));}
}
