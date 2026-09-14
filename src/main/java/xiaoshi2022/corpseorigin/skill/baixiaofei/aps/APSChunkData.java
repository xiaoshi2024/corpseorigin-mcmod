package xiaoshi2022.corpseorigin.skill.baixiaofei.aps;

import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class APSChunkData {
    private static final byte[] MAGIC = {'A', 'P', 'S', '1'};
    private static final int VERSION = 2;

    public final int chunkX, chunkZ, baseY, heightRange;
    private final int[] stateIds;
    private final List<BiomeEntry> biomeEntries;
    private final Map<Integer, CompoundTag> blockEntityNbtMap;

    public APSChunkData(int chunkX, int chunkZ, int baseY, int heightRange) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.baseY = baseY;
        this.heightRange = heightRange;
        this.stateIds = new int[256 * heightRange];
        this.biomeEntries = new ArrayList<>();
        this.blockEntityNbtMap = new HashMap<>();
    }

    private int index(int lx, int lz, int ly) { return ly * 256 + lx * 16 + lz; }

    public void saveBlock(int lx, int lz, int worldY, BlockState state) {
        int ly = worldY - baseY;
        if (ly < 0 || ly >= heightRange) return;
        int idx = index(lx, lz, ly);
        if (stateIds[idx] == 0) {
            stateIds[idx] = Block.BLOCK_STATE_REGISTRY.getId(state) + 1;
        }
    }

    public BlockState getBlock(int lx, int lz, int worldY) {
        int ly = worldY - baseY;
        if (ly < 0 || ly >= heightRange) return null;
        int s = stateIds[index(lx, lz, ly)];
        return s == 0 ? null : Block.BLOCK_STATE_REGISTRY.byId(s - 1);
    }

    public void saveBlockEntity(int lx, int lz, int worldY, CompoundTag nbt) {
        int ly = worldY - baseY;
        if (ly < 0 || ly >= heightRange) return;
        blockEntityNbtMap.putIfAbsent(index(lx, lz, ly), nbt.copy());
    }

    public CompoundTag getBlockEntity(int lx, int lz, int worldY) {
        int ly = worldY - baseY;
        if (ly < 0 || ly >= heightRange) return null;
        return blockEntityNbtMap.get(index(lx, lz, ly));
    }

    public Map<Integer, CompoundTag> copyBlockEntityNbt() {
        Map<Integer, CompoundTag> c = new HashMap<>();
        blockEntityNbtMap.forEach((k, v) -> c.put(k, v.copy()));
        return c;
    }

    public void saveBiome(int lx, int lz, int ly, ResourceKey<Biome> key) {
        for (BiomeEntry e : biomeEntries)
            if (e.relX == lx && e.relZ == lz && e.relY == ly) return;
        biomeEntries.add(new BiomeEntry((byte) lx, (byte) lz, (short) ly, key));
    }

    public ResourceKey<Biome> getBiome(int lx, int lz, int ly) {
        for (BiomeEntry e : biomeEntries)
            if (e.relX == lx && e.relZ == lz && e.relY == ly) return e.biomeKey;
        return null;
    }

    public List<BiomeEntry> getBiomeEntries() { return biomeEntries; }

    public int getBlockCount() {
        int c = 0;
        for (int id : stateIds) if (id != 0) c++;
        return c;
    }

    public int[] copyStateIds() { return stateIds.clone(); }
    public List<BiomeEntry> copyBiomeEntries() { return new ArrayList<>(biomeEntries); }

    public void forEachBlock(BlockConsumer c) {
        for (int ly = 0; ly < heightRange; ly++)
            for (int lx = 0; lx < 16; lx++)
                for (int lz = 0; lz < 16; lz++) {
                    int s = stateIds[index(lx, lz, ly)];
                    if (s != 0) {
                        BlockState st = Block.BLOCK_STATE_REGISTRY.byId(s - 1);
                        if (st != null) c.accept(lx, lz, baseY + ly, st);
                    }
                }
    }

    public void forEachBlockXZ(XZConsumer c) {
        boolean[] seen = new boolean[256];
        for (int ly = 0; ly < heightRange; ly++)
            for (int lx = 0; lx < 16; lx++)
                for (int lz = 0; lz < 16; lz++) {
                    int xz = lx * 16 + lz;
                    if (!seen[xz] && stateIds[index(lx, lz, ly)] != 0) {
                        seen[xz] = true;
                        c.accept(lx, lz);
                    }
                }
    }

    public static byte[] serialize(int chunkX, int chunkZ, int baseY, int heightRange,
                                   int[] stateIds, List<BiomeEntry> biomes,
                                   Map<Integer, CompoundTag> nbt) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(4096);
            try (GZIPOutputStream gz = new GZIPOutputStream(baos);
                 DataOutputStream dos = new DataOutputStream(gz)) {
                dos.write(MAGIC);
                dos.writeInt(VERSION);
                dos.writeInt(chunkX);
                dos.writeInt(chunkZ);
                dos.writeInt(baseY);
                dos.writeInt(heightRange);
                for (int id : stateIds) dos.writeInt(id);
                dos.writeInt(biomes.size());
                for (BiomeEntry e : biomes) {
                    dos.writeByte(e.relX);
                    dos.writeByte(e.relZ);
                    dos.writeShort(e.relY);
                    dos.writeUTF(e.biomeKey.identifier().toString());
                }
                dos.writeInt(nbt.size());
                for (Map.Entry<Integer, CompoundTag> en : nbt.entrySet()) {
                    dos.writeInt(en.getKey());
                    NbtIo.write(en.getValue(), dos);
                }
            }
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("APS serialize failed", e);
        }
    }

    public static APSChunkData deserialize(byte[] data) {
        try (GZIPInputStream gz = new GZIPInputStream(new ByteArrayInputStream(data));
             DataInputStream dis = new DataInputStream(gz)) {
            byte[] magic = new byte[4];
            dis.readFully(magic);
            if (magic[0] != MAGIC[0] || magic[1] != MAGIC[1]
                    || magic[2] != MAGIC[2] || magic[3] != MAGIC[3])
                throw new IOException("bad APS magic");
            int version = dis.readInt();
            if (version != 1 && version != 2) throw new IOException("bad APS version");
            int cx = dis.readInt(), cz = dis.readInt();
            int baseY = dis.readInt(), hr = dis.readInt();
            APSChunkData chunk = new APSChunkData(cx, cz, baseY, hr);
            for (int i = 0; i < chunk.stateIds.length; i++) chunk.stateIds[i] = dis.readInt();
            int bc = dis.readInt();
            for (int i = 0; i < bc; i++) {
                byte rx = dis.readByte();
                byte rz = dis.readByte();
                short ry = version >= 2 ? dis.readShort() : dis.readByte();
                String s = dis.readUTF();
                chunk.biomeEntries.add(new BiomeEntry(rx, rz, ry,
                        ResourceKey.create(Registries.BIOME, Identifier.parse(s))));
            }
            if (version >= 2 && dis.available() > 0) {
                int nc = dis.readInt();
                for (int i = 0; i < nc; i++) {
                    int idx = dis.readInt();
                    chunk.blockEntityNbtMap.put(idx,
                            NbtIo.read(dis, NbtAccounter.unlimitedHeap()));
                }
            }
            return chunk;
        } catch (IOException e) {
            throw new RuntimeException("APS deserialize failed", e);
        }
    }

    public record BiomeEntry(byte relX, byte relZ, short relY, ResourceKey<Biome> biomeKey) {}

    @FunctionalInterface public interface BlockConsumer { void accept(int lx, int lz, int y, BlockState s); }
    @FunctionalInterface public interface XZConsumer { void accept(int lx, int lz); }
}