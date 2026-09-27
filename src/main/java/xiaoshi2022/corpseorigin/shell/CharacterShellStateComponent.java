package xiaoshi2022.corpseorigin.shell;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import xiaoshi2022.corpseorigin.character.MortalCharacter;
import xiaoshi2022.corpseorigin.character.PlayerCharacterData;

import java.util.UUID;

public class CharacterShellStateComponent extends ShellStateComponent {

    public static final String ORGAN_ROLE_KEY = "clone_organ_role";

    private UUID playerUuid;
    private CompoundTag data = new CompoundTag();

    public CharacterShellStateComponent() {}

    public CharacterShellStateComponent(ServerPlayer player) {
        this.playerUuid = player.getUUID();
        this.data = PlayerCharacterData.get(player).writeNbt(player.getUUID());
        this.data.putInt("CloneInnerCapacity", xiaoshi2022.corpseorigin.character.InnerPowerManager.getMaxInnerPower(player));
        this.data.putBoolean("JingangInfant",player.getAttachedOrCreate(xiaoshi2022.corpseorigin.skill.chapter.CreatureAbilities.INFANT));
        this.data.putInt("BearArms",player.getAttachedOrCreate(xiaoshi2022.corpseorigin.skill.chapter.CreatureAbilities.BEAR_ARMS));
        this.data.put("EvolutionParts", player.getAttachedOrCreate(xiaoshi2022.corpseorigin.growth.SurvivalGrowth.BODY).copy());
    }

    @Override
    public String getId() { return "corpseorigin:character"; }

    /** 这具身体扮演的角色 id（没记录过就是凡人） */
    public String getCharacterId() {
        return this.data.getStringOr("CharacterId", MortalCharacter.ID);
    }

    /** 这具身体保存的进化部件 NBT（翅膀 / 鱼鳃 / 吸血体质，渲染分身附加骨骼时用） */
    public void setEvolutionParts(CompoundTag parts) { this.data.put("EvolutionParts", parts.copy()); }

    public int getCloneInnerCapacity() { return Math.max(0, data.getIntOr("CloneInnerCapacity", 0)); }

    public int getEvolutionLevel() {
        return xiaoshi2022.corpseorigin.skill.EvolutionManager.getLevel(data.getIntOr("Earned", 0));
    }

    public boolean hasLearnedSkill(String path) {
        return data.getList("LearnedSkills").map(list -> list.stream().anyMatch(tag ->
                tag instanceof net.minecraft.nbt.StringTag text
                        && (text.value().equals(path) || text.value().equals("corpseorigin:" + path)))).orElse(false);
    }

    public void setCreatureState(boolean infant, int bearArms) {
        this.data.putBoolean("JingangInfant", infant);
        this.data.putInt("BearArms", Math.clamp(bearArms, 0, 6));
    }

    public CompoundTag getEvolutionParts() {
        return this.data.getCompound("EvolutionParts").orElseGet(CompoundTag::new);
    }

    /** 这具身体保存时是不是金刚婴儿形态 */
    public boolean isInfant() {
        return this.data.getBooleanOr("JingangInfant", false);
    }

    /** 这具身体保存时的巨熊臂层数（0~6） */
    public int getBearArms() {
        return Math.max(0, Math.min(6, this.data.getIntOr("BearArms", 0)));
    }

    /**
     * 把角色身份清成凡人，<b>连同该角色的技能一起</b>，只保留进化点。
     * <p>
     * 克隆仓培育出来的身体是"白纸"：肉体的东西（尸兄体质 / 内力 / 进化点）照抄本体，
     * 但"我是谁、我会什么"不该跟着被复制 —— 否则克隆一具身体就能白嫖本体的整套角色身份，
     * 外观也会跟着变（培育出来的是凡人，就该长凡人样）。
     * <p>
     * 想留下带角色外观的身体，正确做法是把<b>旧身体存进克隆仓</b>
     * （{@code CloneChamberBlockEntity#receiveOldBody}）—— 那具身体本来就带着原角色的身份与外观。
     * <p>
     * ⚠️ 技能必须跟角色身份一起清。技能本来就是<b>绑定角色</b>的（{@code CharacterManager}
     * 换角色时会 {@code clearLearnedSkills}），只清身份不清技能的话，换进克隆体的玩家会变成
     * "凡人身带着上一个角色的一整套招式" —— 那正是"死亡夺舍凡人克隆体后技能还在"的来源。
     * 凡人 / 尸兄这类<b>自由路线</b>的招式是探索得来的，与角色身份无关，照旧保留。
     */
    public void clearCharacterId() {
        CompoundTag tag = this.data.copy();
        String previousRole = tag.getStringOr("CharacterId", MortalCharacter.ID);
        tag.putString("CharacterId", MortalCharacter.ID);
        if (!xiaoshi2022.corpseorigin.growth.FreeGrowth.isFree(previousRole)) {
            tag.put("LearnedSkills", new net.minecraft.nbt.ListTag());
        }
        this.data = tag;
    }

    public void applyEvolutionPartsClone(net.minecraft.util.RandomSource random, boolean corpseClone, float ratio) {
        CompoundTag parts = data.getCompound("EvolutionParts").orElseGet(CompoundTag::new).copy();
        String organRole = parts.getStringOr(ORGAN_ROLE_KEY, getCharacterId());
        String loadout = parts.getStringOr(xiaoshi2022.corpseorigin.growth.OrganLibrary.BODY_KEY, "");
        if (!corpseClone) {
            parts = new CompoundTag();
            // Equipped geometry belongs to the physical body, not its learned skills or mutations.
            if (!loadout.isEmpty()) parts.putString(xiaoshi2022.corpseorigin.growth.OrganLibrary.BODY_KEY, loadout);
        }
        else for (String trait : xiaoshi2022.corpseorigin.growth.SurvivalGrowth.TRAITS) {
            if (random.nextFloat() > ratio) {
                parts.remove(trait);
                parts.remove(trait + "_progress");
            }
        }
        if ("xiaojingang".equals(organRole) || "kaiweinai".equals(organRole))
            parts.putString(ORGAN_ROLE_KEY, organRole);
        data.put("EvolutionParts", parts);
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
        player.setAttached(xiaoshi2022.corpseorigin.skill.chapter.CreatureAbilities.INFANT,this.data.getBooleanOr("JingangInfant",false));
        player.setAttached(xiaoshi2022.corpseorigin.skill.chapter.CreatureAbilities.BEAR_ARMS,Math.max(0,Math.min(6,this.data.getIntOr("BearArms",0))));
        player.setAttached(xiaoshi2022.corpseorigin.growth.SurvivalGrowth.BODY,
                this.data.getCompound("EvolutionParts").orElseGet(CompoundTag::new).copy());
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
