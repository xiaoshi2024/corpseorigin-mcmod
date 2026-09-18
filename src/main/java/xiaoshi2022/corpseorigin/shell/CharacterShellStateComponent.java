package xiaoshi2022.corpseorigin.shell;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import xiaoshi2022.corpseorigin.character.MortalCharacter;
import xiaoshi2022.corpseorigin.character.PlayerCharacterData;

import java.util.UUID;

public class CharacterShellStateComponent extends ShellStateComponent {

    private UUID playerUuid;
    private CompoundTag data = new CompoundTag();

    public CharacterShellStateComponent() {}

    public CharacterShellStateComponent(ServerPlayer player) {
        this.playerUuid = player.getUUID();
        this.data = PlayerCharacterData.get(player).writeNbt(player.getUUID());
    }

    @Override
    public String getId() { return "corpseorigin:character"; }

    /** 这具身体扮演的角色 id（没记录过就是凡人） */
    public String getCharacterId() {
        return this.data.getStringOr("CharacterId", MortalCharacter.ID);
    }

    /**
     * 把角色身份清成凡人，但<b>保留</b>进化点与已学技能。
     * <p>
     * 克隆仓培育出来的身体是"白纸"：肉体的东西（尸兄体质 / 内力 / 进化点）照抄本体，
     * 但"我是谁"不该跟着被复制 —— 否则克隆一具身体就能白嫖本体的整套角色身份。
     * 换进这具身体后，用角色选择书重新选角色即可。
     */
    public void clearCharacterId() {
        CompoundTag tag = this.data.copy();
        tag.putString("CharacterId", MortalCharacter.ID);
        this.data = tag;
    }

    @Override
    public void clone(ShellStateComponent component) {
        CharacterShellStateComponent other = component.as(CharacterShellStateComponent.class);
        if (other != null) {
            this.playerUuid = other.playerUuid;
            this.data = other.data.copy();
        }
    }

    @Override
    public void writeNbt(CompoundTag tag) {
        if (this.playerUuid != null) {
            tag.putString("Uuid", this.playerUuid.toString());
        }
        tag.put("Data", this.data.copy());
    }

    @Override
    public void readNbt(CompoundTag tag) {
        tag.getString("Uuid").ifPresent(s -> {
            try { this.playerUuid = UUID.fromString(s); } catch (IllegalArgumentException ignored) {}
        });
        this.data = tag.getCompound("Data").orElse(new CompoundTag());
    }

    /** ★ 应用回玩家 */
    @Override
    public void applyTo(ServerPlayer player) {
        if (this.playerUuid == null) this.playerUuid = player.getUUID();
        // ★ 这具身体从没记录过角色数据（空组件：还没写过的壳、老存档里的空壳）：
        //   绝不能拿一份空数据去 readNbt —— 那会把玩家的角色身份、已学技能、进化点一起抹平。
        //   空 = "这具身体没记过我是谁"，那就保持玩家现在的身份不动。
        if (!this.data.contains("CharacterId") && !this.data.contains("LearnedSkills")) {
            return;
        }
        PlayerCharacterData.get(player).readNbt(this.playerUuid, this.data);
    }

    /**
     * 记忆是残缺的：完成度（继承度）越低，越可能忘掉学过的技能，进化点数也按比例缩水。
     */
    public void applyCloneFormula(net.minecraft.util.RandomSource random, float ratio) {
        if (ratio >= 0.999F) {
            return;
        }
        CompoundTag tag = this.data.copy();

        net.minecraft.nbt.ListTag kept = new net.minecraft.nbt.ListTag();
        tag.getList("LearnedSkills").ifPresent(list -> {
            for (Tag skill : list) {
                if (random.nextFloat() <= ratio) {
                    kept.add(skill);
                }
            }
        });
        tag.put("LearnedSkills", kept);
        tag.putInt("Earned", Math.round(tag.getIntOr("Earned", 0) * ratio));
        tag.putInt("Available", Math.round(tag.getIntOr("Available", 0) * ratio));
        this.data = tag;
    }
}