package xiaoshi2022.corpseorigin.shell;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import xiaoshi2022.corpseorigin.component.PlayerCorpseComponent;

public class CorpseShellStateComponent extends ShellStateComponent {

    private CompoundTag data = new CompoundTag();

    public CorpseShellStateComponent() {}

    public CorpseShellStateComponent(ServerPlayer player) {
        this.data = PlayerCorpseComponent.get(player).getDataPublic();
    }

    @Override public String getId() { return "corpseorigin:corpse"; }

    @Override
    public void clone(ShellStateComponent component) {
        CorpseShellStateComponent other = component.as(CorpseShellStateComponent.class);
        if (other != null) this.data = other.data.copy();
    }

    @Override public void writeNbt(CompoundTag tag) { tag.put("Data", this.data.copy()); }

    /** 这具身体的尸兄状态原始 NBT（渲染克隆人时用） */
    public CompoundTag getData() { return this.data; }

    /**
     * 按培育配方塑造这具克隆体的尸兄状态。
     * <p>
     * 尸水培育 → 尸兄化：继承度（完成度）越低，本体特征缺失越多，也可能自己长出变异；
     * 清水培育 → 干净的普通身体。
     */
    public void applyCloneFormula(net.minecraft.util.RandomSource random,
                                  boolean corpseClone, float completion) {
        CompoundTag tag = this.data.copy();

        if (!corpseClone) {
            tag.putBoolean("is_corpse", false);
            tag.putInt("extra_eye_count", 0);
            tag.putBoolean("has_wing", false);
            tag.putBoolean("has_tail", false);
            tag.putBoolean("is_disguised", false);
            tag.putBoolean("has_consciousness", true);
            tag.putBoolean("consciousness_restored", true);
            this.data = tag;
            return;
        }

        tag.putBoolean("is_corpse", true);
        tag.putBoolean("has_wing", rollFeature(random, tag.getBoolean("has_wing").orElse(false), completion));
        tag.putBoolean("has_tail", rollFeature(random, tag.getBoolean("has_tail").orElse(false), completion));
        tag.putBoolean("is_disguised", rollFeature(random, tag.getBoolean("is_disguised").orElse(false), completion));

        int eyes = tag.getInt("extra_eye_count").orElse(0);
        tag.putInt("extra_eye_count", Math.max(0, Math.round(eyes * completion)));

        int level = tag.getInt("evolution_level").orElse(1);
        if (random.nextFloat() > completion) {
            level = Math.max(1, level - 1);
        }
        tag.putInt("evolution_level", level);

        // 意识：完成度低的时候更可能是个只剩本能的空壳
        boolean hadMind = tag.getBoolean("has_consciousness").orElse(false);
        if (hadMind) {
            if (random.nextFloat() > completion) {
                tag.putBoolean("has_consciousness", false);
                tag.putBoolean("consciousness_restored", false);
            }
        } else if (random.nextFloat() < completion * 0.3F) {
            tag.putBoolean("has_consciousness", true);
        }
        this.data = tag;
    }

    /** 本体有则按完成度保留，本体没有则小概率变异长出来 */
    private static boolean rollFeature(net.minecraft.util.RandomSource random,
                                       boolean had, float completion) {
        if (had) {
            return random.nextFloat() <= completion;
        }
        return random.nextFloat() < (1.0F - completion) * 0.4F;
    }

    @Override public void readNbt(CompoundTag tag) {
        this.data = tag.getCompound("Data").orElse(new CompoundTag());
    }

    @Override
    public void applyTo(ServerPlayer player) {
        PlayerCorpseComponent.get(player).readNbt(this.data);
    }
}