package xiaoshi2022.corpseorigin.shell;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import xiaoshi2022.corpseorigin.character.CharacterManager;
import xiaoshi2022.corpseorigin.character.ICharacter;
import xiaoshi2022.corpseorigin.character.InnerPowerManager;

/**
 * 内力：跟着身体走。
 * <p>
 * 内力存储在 {@link InnerPowerManager} 的静态表里（按玩家 UUID），
 * 本组件负责在身体切换时搬运：
 * <ul>
 *     <li>{@link #of(ServerPlayer)} — 把当前玩家内力抓进这具身体的快照</li>
 *     <li>{@link #applyTo(ServerPlayer)} — 把这具身体自带的内力写回玩家缓存并同步客户端</li>
 * </ul>
 * 新培育的空身体内力值为 -1，表示"未初始化"，应用时按角色内力上限补满。
 */
public class InnerPowerShellStateComponent extends ShellStateComponent {

    private static final int UNINITIALIZED = -1;

    private int innerPower = UNINITIALIZED;

    public InnerPowerShellStateComponent() {}

    public InnerPowerShellStateComponent(ServerPlayer player) {
        this.innerPower = InnerPowerManager.getInnerPower(player);
    }

    @Override
    public String getId() { return "corpseorigin:inner_power"; }

    @Override
    public void clone(ShellStateComponent component) {
        InnerPowerShellStateComponent other = component.as(InnerPowerShellStateComponent.class);
        if (other != null) {
            this.innerPower = other.innerPower;
        }
    }

    @Override
    public void writeNbt(CompoundTag tag) {
        tag.putInt("InnerPower", this.innerPower);
    }

    @Override
    public void readNbt(CompoundTag tag) {
        this.innerPower = tag.getInt("InnerPower").orElse(UNINITIALIZED);
    }

    @Override
    public void applyTo(ServerPlayer player) {
        ICharacter character = CharacterManager.getInstance().getPlayerCharacter(player);
        int max = character.getMaxInnerPower();
        if (max <= 0) {
            // 无内力角色：清空缓存
            InnerPowerManager.set(player, 0);
            return;
        }
        if (this.innerPower <= UNINITIALIZED) {
            // 新培育的空身体：满内力
            InnerPowerManager.set(player, max);
        } else {
            InnerPowerManager.set(player, this.innerPower);
        }
    }
}
