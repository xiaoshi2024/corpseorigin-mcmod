package xiaoshi2022.corpseorigin.shell;

import net.minecraft.server.level.ServerLevel;

import java.util.UUID;

public interface TransferredBody {

    UUID ownerUuid();

    ShellState snapshot();

    boolean ready();

    void consume(ServerLevel level);

    /** 旧身体存回这个容器（由 ServerShell.sync 调用：仅当玩家正站在该克隆仓里） */
    default void receiveOldBody(ServerLevel level, ShellState oldState) {}
}