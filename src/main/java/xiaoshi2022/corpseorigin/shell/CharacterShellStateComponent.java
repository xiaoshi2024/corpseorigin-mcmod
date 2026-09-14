package xiaoshi2022.corpseorigin.shell;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
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
    public void applyTo(ServerPlayer player) {
        if (this.playerUuid == null) this.playerUuid = player.getUUID();
        PlayerCharacterData.get(player).readNbt(this.playerUuid, this.data);
    }
}