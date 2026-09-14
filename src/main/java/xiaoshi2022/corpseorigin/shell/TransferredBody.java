package xiaoshi2022.corpseorigin.shell;

import net.minecraft.server.level.ServerLevel;

import java.util.UUID;

public interface TransferredBody {

    UUID ownerUuid();

    ShellState snapshot();

    boolean ready();

    void consume(ServerLevel level);

    /** 旧身体存回这个容器（分身默认 noop，sync 会另生成新分身） */
    default void receiveOldBody(ServerLevel level, ShellState oldState) {}
}