//package xiaoshi2022.corpseorigin.infection;
//
//import com.mojang.serialization.Codec;
//import com.mojang.serialization.codecs.RecordCodecBuilder;
//import net.minecraft.core.BlockPos;
//import net.minecraft.core.Direction;
//import net.minecraft.resources.Identifier;
//import net.minecraft.server.level.ServerLevel;
//import net.minecraft.tags.FluidTags;
//import net.minecraft.world.level.saveddata.SavedData;
//import net.minecraft.world.level.saveddata.SavedDataType;
//import xiaoshi2022.corpseorigin.CorpseOrigin;
//
//import java.util.HashMap;
//import java.util.Map;
//
//public class InfectionData extends SavedData {
//
//    public static final int MAX_ENERGY = 15;
//
//    private static final Codec<InfectionData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
//            Codec.unboundedMap(Codec.LONG, Codec.INT)
//                    .optionalFieldOf("water_energy", Map.of())
//                    .forGetter(d -> Map.copyOf(d.waterEnergyMap))
//    ).apply(inst, InfectionData::new));
//
//    // ✅ 修复：使用 null 作为第四个参数
//    public static final SavedDataType<InfectionData> TYPE = new SavedDataType<>(
//            Identifier.fromNamespaceAndPath(CorpseOrigin.MOD_ID, "infection_data"),
//            InfectionData::new,
//            CODEC,
//            null
//    );
//
//    private final Map<Long, Integer> waterEnergyMap = new HashMap<>();
//
//    public InfectionData() {
//    }
//
//    private InfectionData(Map<Long, Integer> waterEnergyMap) {
//        this.waterEnergyMap.putAll(waterEnergyMap);
//    }
//
//    public static InfectionData get(ServerLevel level) {
//        return level.getDataStorage().computeIfAbsent(TYPE);
//    }
//
//    private static Long posToKey(BlockPos pos) {
//        return pos.asLong();
//    }
//
//    // ==================== 查询与标记 ====================
//
//    public boolean isWaterInfected(BlockPos pos) {
//        return waterEnergyMap.getOrDefault(posToKey(pos), 0) > 0;
//    }
//
//    public int getWaterEnergy(BlockPos pos) {
//        return waterEnergyMap.getOrDefault(posToKey(pos), 0);
//    }
//
//    public void setWaterEnergy(BlockPos pos, int energy) {
//        if (energy > 0) {
//            waterEnergyMap.put(posToKey(pos), Math.min(energy, MAX_ENERGY));
//        } else {
//            waterEnergyMap.remove(posToKey(pos));
//        }
//        setDirty();
//    }
//
//    public int getMaxNeighborEnergy(BlockPos pos) {
//        int max = 0;
//        for (Direction direction : Direction.values()) {
//            int energy = getWaterEnergy(pos.relative(direction));
//            if (energy > max) {
//                max = energy;
//            }
//        }
//        return max;
//    }
//
//    public void markWaterInfected(BlockPos pos) {
//        int maxNeighbor = getMaxNeighborEnergy(pos);
//        if (maxNeighbor > 0) {
//            int newEnergy = maxNeighbor - 1;
//            if (newEnergy > 0) {
//                setWaterEnergy(pos, newEnergy);
//            }
//        } else {
//            setWaterEnergy(pos, MAX_ENERGY);
//        }
//    }
//
//    public void clearWater(BlockPos pos) {
//        if (waterEnergyMap.remove(posToKey(pos)) != null) {
//            setDirty();
//        }
//    }
//
//    public int clearArea(BlockPos center, int radius) {
//        int cleared = 0;
//        for (Map.Entry<Long, Integer> entry : new HashMap<>(waterEnergyMap).entrySet()) {
//            BlockPos pos = BlockPos.of(entry.getKey());
//            if (pos.distSqr(center) <= (double) radius * radius) {
//                waterEnergyMap.remove(entry.getKey());
//                cleared++;
//            }
//        }
//        if (cleared > 0) {
//            setDirty();
//        }
//        return cleared;
//    }
//
//    public int markArea(ServerLevel level, BlockPos center, int radius) {
//        int marked = 0;
//        BlockPos min = center.offset(-radius, -radius, -radius);
//        BlockPos max = center.offset(radius, radius, radius);
//        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
//            if (level.getFluidState(pos).is(FluidTags.WATER)
//                    && level.getFluidState(pos).isSource()) {
//                if (!isWaterInfected(pos)) {
//                    setWaterEnergy(pos.immutable(), MAX_ENERGY);
//                    marked++;
//                }
//            }
//        }
//        return marked;
//    }
//
//    // ==================== 静态便捷方法 ====================
//
//    public static boolean isWaterInfectedStatic(ServerLevel level, BlockPos pos) {
//        return get(level).isWaterInfected(pos);
//    }
//
//    public static int getEnergyStatic(ServerLevel level, BlockPos pos) {
//        return get(level).getWaterEnergy(pos);
//    }
//}