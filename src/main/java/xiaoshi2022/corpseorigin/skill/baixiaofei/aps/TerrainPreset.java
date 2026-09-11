package xiaoshi2022.corpseorigin.skill.baixiaofei.aps;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public class TerrainPreset {
    private final String name;
    private final ResourceKey<Biome> biome;
    private final Block surfaceBlock;
    private final Block subsurfaceBlock;
    private final int dayTime;
    private final int radius;
    private final int depth;
    private final int fogColor;
    private final float fogStart;
    private final float fogEnd;

    private TerrainPreset(Builder b) {
        this.name = b.name;
        this.biome = b.biome;
        this.surfaceBlock = b.surfaceBlock;
        this.subsurfaceBlock = b.subsurfaceBlock;
        this.dayTime = b.dayTime;
        this.radius = b.radius;
        this.depth = b.depth;
        this.fogColor = b.fogColor;
        this.fogStart = b.fogStart;
        this.fogEnd = b.fogEnd;
    }

    public String getName() { return name; }
    public ResourceKey<Biome> getBiome() { return biome; }
    public Block getSurfaceBlock() { return surfaceBlock; }
    public Block getSubsurfaceBlock() { return subsurfaceBlock; }
    public int getDayTime() { return dayTime; }
    public int getRadius() { return radius; }
    public int getDepth() { return depth; }
    public int getFogColor() { return fogColor; }
    public float getFogStart() { return fogStart; }
    public float getFogEnd() { return fogEnd; }

    public static Builder builder(String name) { return new Builder(name); }

    public static class Builder {
        private final String name;
        private ResourceKey<Biome> biome =
                ResourceKey.create(Registries.BIOME, Identifier.withDefaultNamespace("plains"));
        private Block surfaceBlock = Blocks.GRASS_BLOCK;
        private Block subsurfaceBlock = Blocks.DIRT;
        private int dayTime = 6000;
        private int radius = 48;
        private int depth = 5;
        private int fogColor = 12632256;
        private float fogStart = 20.0F;
        private float fogEnd = 80.0F;

        public Builder(String name) { this.name = name; }

        public Builder biome(ResourceKey<Biome> b) { this.biome = b; return this; }
        public Builder biome(String id) {
            this.biome = ResourceKey.create(Registries.BIOME, Identifier.parse(id));
            return this;
        }
        public Builder surfaceBlock(Block b) { this.surfaceBlock = b; return this; }
        public Builder subsurfaceBlock(Block b) { this.subsurfaceBlock = b; return this; }
        public Builder dayTime(int t) { this.dayTime = t; return this; }
        public Builder radius(int r) { this.radius = r; return this; }
        public Builder depth(int d) { this.depth = d; return this; }
        public Builder fogColor(int c) { this.fogColor = c; return this; }
        public Builder fogStart(float s) { this.fogStart = s; return this; }
        public Builder fogEnd(float e) { this.fogEnd = e; return this; }

        public TerrainPreset build() { return new TerrainPreset(this); }
    }
}