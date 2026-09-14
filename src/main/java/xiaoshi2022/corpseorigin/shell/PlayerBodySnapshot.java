package xiaoshi2022.corpseorigin.shell;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * 玩家身体快照。
 */
public final class PlayerBodySnapshot {

    private CompoundTag tag;

    private PlayerBodySnapshot(CompoundTag tag) {
        this.tag = tag == null ? new CompoundTag() : tag;
    }

    public static PlayerBodySnapshot of(ServerPlayer player) {
        TagValueOutput output = TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING);
        player.saveWithoutId(output);
        return new PlayerBodySnapshot(output.buildResult());
    }

    /** ★ 培育用空壳：空背包、满血、饱食 20、经验归零 */
    public static PlayerBodySnapshot blank(ServerPlayer player) {
        TagValueOutput output = TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING);
        player.saveWithoutId(output);
        CompoundTag tag = output.buildResult();

        // 空背包（必须显式写，否则 load 不会清背包）
        tag.put("Inventory", new ListTag());

        // 满血、饱食 20、经验归零
        tag.putFloat("Health", player.getMaxHealth());
        tag.putInt("foodLevel", 20);
        tag.putFloat("foodSaturationLevel", 5.0F);
        tag.putFloat("foodExhaustionLevel", 0.0F);
        tag.putInt("XpLevel", 0);
        tag.putFloat("XpP", 0.0F);
        tag.putInt("XpTotal", 0);
        tag.put("ActiveEffects", new ListTag());
        tag.putFloat("AbsorptionAmount", 0.0F);

        // 剥离身份/位置/载具
        tag.remove("UUID");
        tag.remove("Pos");
        tag.remove("Dimension");
        tag.remove("RootVehicle");
        tag.remove("Passengers");
        tag.remove("PortalCooldown");
        tag.remove("LastDeathLocation");

        ListTag rotation = new ListTag();
        rotation.add(FloatTag.valueOf(0.0F));
        rotation.add(FloatTag.valueOf(0.0F));
        tag.put("Rotation", rotation);

        return new PlayerBodySnapshot(tag);
    }

    public static PlayerBodySnapshot empty() {
        return new PlayerBodySnapshot(new CompoundTag());
    }

    public void writeTo(ValueOutput out) {
        out.store("Body", CompoundTag.CODEC, this.tag);
    }

    public static PlayerBodySnapshot read(ValueInput in) {
        return new PlayerBodySnapshot(
                in.read("Body", CompoundTag.CODEC).orElseGet(CompoundTag::new));
    }

    public CompoundTag asCompoundTag() {
        return this.tag;
    }

    public static PlayerBodySnapshot fromCompoundTag(CompoundTag tag) {
        return new PlayerBodySnapshot(tag);
    }

    public void applyTo(ServerPlayer player) {
        ValueInput input = TagValueInput.create(
                ProblemReporter.DISCARDING,
                player.level().registryAccess(),
                this.tag.copy());
        player.load(input);
    }

    public boolean isEmpty() {
        return this.tag.isEmpty();
    }
}