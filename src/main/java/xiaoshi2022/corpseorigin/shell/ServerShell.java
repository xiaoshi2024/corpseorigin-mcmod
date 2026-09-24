package xiaoshi2022.corpseorigin.shell;
import net.minecraft.network.chat.Component;

import com.mojang.datafixers.util.Either;
import net.minecraft.server.level.ServerPlayer;

import java.util.stream.Stream;

/**
 * 服务端玩家作为"可迁移身体"的接口，由 ServerPlayerShellMixin 实现。
 */
public interface ServerShell {

    /** 当前玩家所有可转移的身体（存储仓 + 分身） */
    Stream<TransferredBody> getAvailableBodies();

    /** 死亡时自动寻找最近的可转移身体 */
    TransferredBody findNearestBody();

    /**
     * 把自己当前身体存进 storedState，再把 target 的身体 apply 到玩家。
     * 返回：成功时返回旧身体的 ShellState；失败时返回错误信息。
     */
    Either<ShellState, Component> sync(TransferredBody target);

    /**
     * 死亡夺舍：旧身体只回收进"身边"的空克隆仓，附近没有就直接丢弃（掉落物品）。
     * 与手动转移不同——手动转移会把旧身体留在取走身体的那座仓里，以便随时换回来。
     */
    default Either<ShellState, Component> syncFromDeath(TransferredBody target) {
        return sync(target);
    }

    /** 把目标身体写到玩家身上 */
    void apply(ShellState state);

    static ServerShell of(ServerPlayer player) {
        return (ServerShell) player;
    }
}