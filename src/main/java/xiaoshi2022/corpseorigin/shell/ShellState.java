package xiaoshi2022.corpseorigin.shell;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class ShellState {

    public static final float PROGRESS_START = 0.0F;
    public static final float PROGRESS_DONE = 1.0F;
    public static final float PROGRESS_PRINTING = 0.75F;

    private UUID uuid;
    private UUID ownerUuid;
    private String ownerName;
    private float progress;
    private boolean artificial;

    private Identifier world;
    private BlockPos pos;

    private PlayerBodySnapshot body;
    private ShellStateComponent component;

    private ShellState() {
    }

    /** ★ 培育用空壳：供玩家调用（培育开始） */
    public static ShellState blank(ServerPlayer player, BlockPos pos) {
        ShellState state = new ShellState();
        state.uuid = UUID.randomUUID();
        state.ownerUuid = player.getUUID();
        state.ownerName = player.getScoreboardName();
        state.progress = PROGRESS_START;
        state.artificial = true;
        state.world = player.level().dimension().registry();
        state.pos = pos;
        state.body = PlayerBodySnapshot.blank(player);
        state.component = ShellStateComponent.empty();
        return state;
    }

    /** ★ 培育用空壳：供 BE 调用（不依赖 ServerPlayer） */
    public static ShellState blank(UUID owner, String ownerName, Identifier world, BlockPos pos) {
        ShellState state = new ShellState();
        state.uuid = UUID.randomUUID();
        state.ownerUuid = owner;
        state.ownerName = ownerName;
        state.progress = PROGRESS_START;
        state.artificial = true;
        state.world = world;
        state.pos = pos;
        state.body = PlayerBodySnapshot.empty();
        state.component = ShellStateComponent.empty();
        return state;
    }

    /** ★ 夺舍时抓玩家当前身体（带物品 + 组件） */
    public static ShellState of(ServerPlayer player, BlockPos pos) {
        ShellState state = new ShellState();
        state.uuid = UUID.randomUUID();
        state.ownerUuid = player.getUUID();
        state.ownerName = player.getScoreboardName();
        state.progress = PROGRESS_DONE;
        state.artificial = true;
        state.world = player.level().dimension().registry();
        state.pos = pos;
        state.body = PlayerBodySnapshot.of(player);

        if (player.getHealth() <= 0.0F) {
            CompoundTag tag = state.body.asCompoundTag().copy();
            tag.putFloat("Health", 1.0F);
            state.body = PlayerBodySnapshot.fromCompoundTag(tag);
        }

        state.component = ShellStateComponent.of(player);
        return state;
    }

    public void writeTo(ValueOutput out) {
        out.putString("Uuid", this.uuid.toString());
        out.putString("Owner", this.ownerUuid.toString());
        if (this.ownerName != null) {
            out.putString("OwnerName", this.ownerName);
        }
        out.putFloat("Progress", this.progress);
        out.putBoolean("Artificial", this.artificial);
        if (this.world != null) {
            out.putString("World", this.world.toString());
        }
        if (this.pos != null) {
            out.store("Pos", BlockPos.CODEC, this.pos);
        }
        if (this.body != null) {
            this.body.writeTo(out.child("Body"));
        }
        if (this.component != null) {
            CompoundTag compTag = new CompoundTag();
            this.component.writeNbt(compTag);
            out.store("Component", CompoundTag.CODEC, compTag);
        }
    }

    public static ShellState read(ValueInput in) {
        ShellState state = new ShellState();
        state.uuid = UUID.fromString(in.getStringOr("Uuid", new UUID(0L, 0L).toString()));
        state.ownerUuid = UUID.fromString(in.getStringOr("Owner", new UUID(0L, 0L).toString()));
        state.ownerName = in.getString("OwnerName").orElse(null);
        state.progress = in.getFloatOr("Progress", 0.0F);
        state.artificial = in.getBooleanOr("Artificial", false);
        state.world = in.getString("World").map(Identifier::tryParse).orElse(null);
        state.pos = in.read("Pos", BlockPos.CODEC).orElse(null);
        state.body = in.child("Body").map(PlayerBodySnapshot::read).orElse(null);
        state.component = ShellStateComponent.empty();
        in.read("Component", CompoundTag.CODEC).ifPresent(tag -> state.component.readNbt(tag));
        return state;
    }

    public UUID getUuid() { return this.uuid; }
    public UUID getOwnerUuid() { return this.ownerUuid; }
    public String getOwnerName() { return this.ownerName; }
    public float getProgress() { return this.progress; }
    public void setProgress(float progress) { this.progress = progress; }
    public boolean isArtificial() { return this.artificial; }
    public Identifier getWorld() { return this.world; }
    public void setWorld(Identifier world) { this.world = world; }
    public BlockPos getPos() { return this.pos; }
    public void setPos(BlockPos pos) { this.pos = pos; }
    @Nullable
    public PlayerBodySnapshot getBody() { return this.body; }
    public void setBody(@Nullable PlayerBodySnapshot body) { this.body = body; }
    public ShellStateComponent getComponent() { return this.component; }
    public void setComponent(ShellStateComponent component) { this.component = component; }

    public boolean isReady() {
        return this.progress >= PROGRESS_DONE && this.body != null && !this.body.isEmpty();
    }
}