//package xiaoshi2022.corpseorigin.infection;
//
//import com.mojang.serialization.Codec;
//import com.mojang.serialization.codecs.RecordCodecBuilder;
//import net.minecraft.resources.Identifier;
//import net.minecraft.server.MinecraftServer;
//import net.minecraft.server.level.ServerLevel;
//import net.minecraft.world.entity.player.Player;
//import net.minecraft.world.level.saveddata.SavedData;
//import net.minecraft.world.level.saveddata.SavedDataType;
//import xiaoshi2022.corpseorigin.CorpseOrigin;
//
//import java.util.HashMap;
//import java.util.Map;
//import java.util.UUID;
//
//public class PlayerInfectionData extends SavedData {
//
//    public static final int MAX_INFECTION = 100;
//
//    private static final Codec<PlayerInfectionData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
//            Codec.unboundedMap(Codec.STRING, Codec.INT)
//                    .xmap(PlayerInfectionData::uuidMapFrom, PlayerInfectionData::uuidMapTo)
//                    .optionalFieldOf("players", Map.of())
//                    .forGetter(d -> Map.copyOf(d.infectionLevels))
//    ).apply(inst, PlayerInfectionData::new));
//
//    // ✅ 修复：使用 null 作为第四个参数
//    public static final SavedDataType<PlayerInfectionData> TYPE = new SavedDataType<>(
//            Identifier.fromNamespaceAndPath(CorpseOrigin.MOD_ID, "player_infection"),
//            PlayerInfectionData::new,
//            CODEC,
//            null
//    );
//
//    private final Map<UUID, Integer> infectionLevels = new HashMap<>();
//
//    public PlayerInfectionData() {
//    }
//
//    private PlayerInfectionData(Map<UUID, Integer> levels) {
//        this.infectionLevels.putAll(levels);
//    }
//
//    private static Map<UUID, Integer> uuidMapFrom(Map<String, Integer> raw) {
//        Map<UUID, Integer> result = new HashMap<>();
//        raw.forEach((key, value) -> {
//            try {
//                result.put(UUID.fromString(key), value);
//            } catch (IllegalArgumentException ignored) {
//            }
//        });
//        return result;
//    }
//
//    private static Map<String, Integer> uuidMapTo(Map<UUID, Integer> map) {
//        Map<String, Integer> result = new HashMap<>();
//        map.forEach((uuid, value) -> result.put(uuid.toString(), value));
//        return result;
//    }
//
//    public static PlayerInfectionData get(MinecraftServer server) {
//        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
//    }
//
//    public static PlayerInfectionData get(Player player) {
//        if (player.level() instanceof ServerLevel serverLevel) {
//            return get(serverLevel.getServer());
//        }
//        return new PlayerInfectionData();
//    }
//
//    public int get(UUID uuid) {
//        return infectionLevels.getOrDefault(uuid, 0);
//    }
//
//    public void set(UUID uuid, int value) {
//        if (value <= 0) {
//            infectionLevels.remove(uuid);
//        } else {
//            infectionLevels.put(uuid, Math.min(value, MAX_INFECTION));
//        }
//        setDirty();
//    }
//}