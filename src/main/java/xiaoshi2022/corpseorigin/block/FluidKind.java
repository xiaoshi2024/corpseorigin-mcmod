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
import org.jetbrains.annotations.Nullable;
import xiaoshi2022.corpseorigin.registry.ModFluidTags;

import java.util.ArrayList;
import java.util.List;

/**
 * 克隆仓里装的液体（方块状态取值）。
 * <p>
 * 不直接拿 {@link Fluid} 当状态值：方块状态的属性要求取值 Comparable，而 Fluid 不是。
 * 合法取值 = 空仓 + 注册表快照 + {@link #OTHER}，所以任意模组的液体桶都能直接用。
 * <p>
 * ⚠️ 取值表是在本模组注册方块那一刻从注册表收的，**注册顺序排在本模组之后的模组**，它的流体
 * 不在表里。这类液体统一记成 {@link #OTHER}，具体是哪种由方块实体存起来——桶、加速、是否尸水
 * 都从那里取。
 */
public final class FluidKind implements Comparable<FluidKind> {

    /** 空仓 */
    public static final FluidKind NONE = new FluidKind("none", Fluids.EMPTY);

    /** 快照之后才注册进来的其它模组液体（具体是哪种看方块实体） */
    public static final FluidKind OTHER = new FluidKind("other", Fluids.EMPTY);

    private static List<FluidKind> all;

    private final Fluid fluid;
    private final String name;

    private FluidKind(String name, Fluid fluid) {
        this.name = name;
        this.fluid = fluid;
    }

    private FluidKind(Fluid fluid) {
        this.fluid = fluid;
        // 方块状态的值名只允许 [a-z0-9_]：命名空间 + 路径，其它字符一律换成下划线
        Identifier id = fluid.builtInRegistryHolder().key().identifier();
        this.name = (id.getNamespace() + "_" + id.getPath()).replaceAll("[^a-z0-9_]", "_");
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
            kinds.add(OTHER);
            for (Fluid fluid : BuiltInRegistries.FLUID) {
                if (fluid != Fluids.EMPTY) {
                    kinds.add(new FluidKind(fluid));
                }
            }
            all = List.copyOf(kinds);
        }
        return all;
    }

    /**
     * 该流体对应的取值。
     * <p>
     * 注册表里没有（快照之后才注册的模组液体）→ {@link #OTHER}，空流体 → {@link #NONE}。
     */
    public static FluidKind of(Fluid fluid) {
        if (fluid == Fluids.EMPTY) {
            return NONE;
        }
        for (FluidKind kind : all()) {
            if (kind.fluid == fluid) {
                return kind;
            }
        }
        return OTHER;
    }

    public Fluid fluid() {
        return this.fluid;
    }

    public String name() {
        return this.name;
    }

    public boolean isEmpty() {
        return this == NONE;
    }

    /** 是不是"具体是哪种液体"没存在方块状态里（存在方块实体的那种） */
    public boolean isUnknown() {
        return this == OTHER;
    }

    public FluidState fluidState() {
        // 可装桶的流体都是"静止形态"，其默认流体状态就是源方块状态；OTHER 取不到具体流体，按空算
        return this.fluid.defaultFluidState();
    }

    // ==================== 分类（实例版走方块状态，静态版走方块实体存的流体） ====================

    /**
     * 这种液体对培育速度的加成（1.0 = 正常速度）。
     * <p>
     * 尸水 / 其它模组的血水会加速培育。
     */
    public float growthMultiplier() {
        return this == NONE ? 1.0F : growthMultiplier(this.fluid);
    }

    public static float growthMultiplier(Fluid fluid) {
        return isBloodLike(fluid) ? 2.0F : 1.0F;
    }

    /**
     * 是不是本模组的尸水。
     * <p>
     * ★ 只有它会把克隆体养成尸兄（变异）；其他模组的血水只加速培育，不改性质。
     */
    public boolean isCorpseWater() {
        return this != NONE && isCorpseWater(this.fluid);
    }

    public static boolean isCorpseWater(Fluid fluid) {
        return hasId(fluid, "infected_water") || hasId(sourceOf(fluid), "infected_water");
    }

    /**
     * 是不是血水类培养液（培育提速）。
     * <p>
     * 判定顺序：本模组尸水 → {@link ModFluidTags#BLOOD_CULTURE_FLUID} 标签（其它模组/数据包显式声明，
     * 源与流动形态都算）→ 按流体 id 里的 blood / ichor 兜底（没打标签的模组也能吃到加速）。
     */
    public boolean isBloodLike() {
        return this != NONE && isBloodLike(this.fluid);
    }

    public static boolean isBloodLike(Fluid fluid) {
        if (isCorpseWater(fluid)) {
            return true;
        }
        if (fluid == Fluids.EMPTY) {
            return false;
        }
        if (isIn(fluid, ModFluidTags.BLOOD_CULTURE_FLUID)) {
            return true;
        }
        return idOf(fluid).contains("blood") || idOf(fluid).contains("ichor");
    }

    /** 该流体是否在给定标签里（源与流动形态都查一遍，标签里可能只写了其中一种） */
    private static boolean isIn(Fluid fluid, TagKey<Fluid> tag) {
        return fluid.is(tag) || sourceOf(fluid).is(tag);
    }

    /** 舀走时返还的桶（没桶就是空手） */
    public ItemStack bucketStack() {
        return this == NONE ? ItemStack.EMPTY : bucketStack(this.fluid);
    }

    public static ItemStack bucketStack(Fluid fluid) {
        Item bucket = fluid.getBucket();
        if ((bucket == null || bucket == Items.AIR) && fluid instanceof FlowingFluid flowing) {
            bucket = flowing.getSource().getBucket();
        }
        return bucket == null || bucket == Items.AIR ? ItemStack.EMPTY : new ItemStack(bucket);
    }

    // ==================== 小工具 ====================

    /** 流动形态取源形态，其它原样返回 */
    private static Fluid sourceOf(Fluid fluid) {
        return fluid instanceof FlowingFluid flowing ? flowing.getSource() : fluid;
    }

    private static String idOf(Fluid fluid) {
        return fluid.builtInRegistryHolder().key().identifier().getPath();
    }

    private static boolean hasId(@Nullable Fluid fluid, String path) {
        return fluid != null && fluid != Fluids.EMPTY && idOf(fluid).equals(path);
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
