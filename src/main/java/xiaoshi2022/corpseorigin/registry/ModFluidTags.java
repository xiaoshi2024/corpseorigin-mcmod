package xiaoshi2022.corpseorigin.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;
import xiaoshi2022.corpseorigin.CorpseOrigin;

/**
 * 克隆仓培养液相关的流体标签。
 * <p>
 * 参考其它模组（如 Biome O' Plenty 的 blood 标签）的做法：用标签声明"哪些液体算培养液"，
 * 这样其它模组 / 数据包只要把自己的流体加进来，就能直接当克隆仓的培养液用，不需要改代码。
 * <p>
 * 对应数据文件：{@code data/corpseorigin/tags/fluid/blood_culture_fluid.json}
 */
public final class ModFluidTags {

    /**
     * 血水类培养液：在克隆仓里培育更快，且会提示"血水类培养液正在加速培育"。
     * <p>
     * 默认已列入本模组的尸水与 Biome O' Plenty 的血；其它模组的血水只要加进这个标签即可。
     * 没打标签的也不是完全没救——{@link xiaoshi2022.corpseorigin.block.FluidKind#isBloodLike()}
     * 还会按流体 id 里的 blood / ichor 兜底识别。
     */
    public static final TagKey<Fluid> BLOOD_CULTURE_FLUID =
            TagKey.create(Registries.FLUID, CorpseOrigin.id("blood_culture_fluid"));

    private ModFluidTags() {
    }
}
