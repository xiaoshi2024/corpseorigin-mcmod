package xiaoshi2022.corpseorigin.block;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.registry.ModFluidTags;

import java.util.ArrayList;
import java.util.List;

/**
 * 克隆仓里装的液体（方块状态取值）。
 * <p>
 * 不直接拿 {@link Fluid} 当状态值：方块状态的属性要求取值 Comparable，而 Fluid 不是。
 * 合法取值 = 空仓 + 注册表里所有"有对应桶物品"的流体（流动形态没有桶，会被排除），
 * 所以任意模组的液体桶都能直接用。
 */
public final class FluidKind implements Comparable<FluidKind> {

    /** 空仓 */
    public static final FluidKind NONE = new FluidKind(Fluids.EMPTY);

    private static List<FluidKind> all;

    private final Fluid fluid;
    private final String name;

    private FluidKind(Fluid fluid) {
        this.fluid = fluid;
        if (fluid == Fluids.EMPTY) {
            this.name = "none";
        } else {
            // 方块状态的值名只允许 [a-z0-9_]：命名空间 + 路径，其它字符一律换成下划线
            Identifier id = fluid.builtInRegistryHolder().key().identifier();
            this.name = (id.getNamespace() + "_" + id.getPath()).replaceAll("[^a-z0-9_]", "_");
        }
    }

    /**
     * 全部合法取值，首次调用时从流体注册表收集。
     * <p>
     * 只按注册表收，不碰桶物品：本方法在方块类初始化期执行，而物品注册在方块之后，
     * 这时去问 {@code Fluid#getBucket()} 会触发 ModItems 的初始化并撞上 ModBlocks 的初始化循环。
     * 因此流动形态也会进来（桶里装的永远是静止形态，它们只是取不到，不影响使用）。
     */
    public static List<FluidKind> all() {
        if (all == null) {
            List<FluidKind> kinds = new ArrayList<>();
            kinds.add(NONE);
            for (Fluid fluid : BuiltInRegistries.FLUID) {
                if (fluid != Fluids.EMPTY) {
                    kinds.add(new FluidKind(fluid));
                }
            }
            all = List.copyOf(kinds);
        }
        return all;
    }

    /** 该流体对应的取值（不是可装桶的液体就返回 {@link #NONE}） */
    public static FluidKind of(Fluid fluid) {
        for (FluidKind kind : all()) {
            if (kind.fluid == fluid) {
                return kind;
            }
        }
        return NONE;
    }

    public Fluid fluid() {
        return this.fluid;
    }

    public String name() {
        return this.name;
    }

    public boolean isEmpty() {
        return this.fluid == Fluids.EMPTY;
    }

    public FluidState fluidState() {
        // 可装桶的流体都是"静止形态"，其默认流体状态就是源方块状态
        return this.fluid.defaultFluidState();
    }

    /**
     * 这种液体对培育速度的加成（1.0 = 正常速度）。
     * <p>
     * 尸水 / 其他模组的血水会加速培育。
     */
    public float growthMultiplier() {
        return this.isBloodLike() ? 2.0F : 1.0F;
    }

    /**
     * 是不是本模组的尸水。
     * <p>
     * ★ 只有它会把克隆体养成尸兄（变异）；其他模组的血水只加速培育，不改性质。
     */
    public boolean isCorpseWater() {
        return !this.isEmpty()
                && this.fluid.builtInRegistryHolder().key().identifier().equals(CorpseOrigin.id("infected_water"));
    }

    /**
     * 是不是血水类培养液（培育提速）。
     * <p>
     * 判定顺序：本模组尸水 → {@link ModFluidTags#BLOOD_CULTURE_FLUID} 标签（其它模组/数据包显式声明，
     * 源与流动形态都算）→ 按流体 id 里的 blood / ichor 兜底（没打标签的模组也能吃到加速）。
     */
    public boolean isBloodLike() {
        if (this.isCorpseWater()) {
            return true;
        }
        if (this.isEmpty()) {
            return false;
        }
        if (this.isIn(ModFluidTags.BLOOD_CULTURE_FLUID)) {
            return true;
        }
        String path = this.fluid.builtInRegistryHolder().key().identifier().getPath();
        return path.contains("blood") || path.contains("ichor");
    }

    /** 该流体是否在给定标签里（源与流动形态都查一遍，标签里可能只写了其中一种） */
    private boolean isIn(TagKey<Fluid> tag) {
        if (this.isEmpty()) {
            return false;
        }
        if (this.fluid.is(tag)) {
            return true;
        }
        return this.fluid instanceof FlowingFluid flowing && flowing.getFlowing().is(tag);
    }

    /** 舀走时返还的桶（没桶就是空手） */
    public ItemStack bucketStack() {
        if (this.isEmpty()) {
            return ItemStack.EMPTY;
        }
        Item bucket = this.fluid.getBucket();
        return bucket == null || bucket == Items.AIR ? ItemStack.EMPTY : new ItemStack(bucket);
    }

    @Override
    public int compareTo(FluidKind other) {
        return this.name.compareTo(other.name);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof FluidKind kind && this.name.equals(kind.name);
    }

    @Override
    public int hashCode() {
        return this.name.hashCode();
    }

    @Override
    public String toString() {
        return this.name;
    }
}
