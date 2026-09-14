package xiaoshi2022.corpseorigin.client.gui;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import xiaoshi2022.corpseorigin.client.CorpseOriginClient;
import xiaoshi2022.corpseorigin.network.SynchronizationRequestPacket;

public class ShellStorageScreen extends Screen {

    private final BlockPos pos;

    public ShellStorageScreen(BlockPos pos) {
        super(Component.translatable("gui.corpseorigin.shell_storage"));
        this.pos = pos;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int y = this.height / 2 - 40;

        addRenderableWidget(Button.builder(
                Component.translatable("gui.corpseorigin.shell_storage.transfer"),
                b -> {
                    // 从客户端缓存里找这个 pos 对应的 entry
                    CorpseOriginClient.ClientShellEntry target =
                            CorpseOriginClient.clientShellEntries.stream()
                                    .filter(e -> e.x() == pos.getX()
                                            && e.y() == pos.getY()
                                            && e.z() == pos.getZ())
                                    .findFirst().orElse(null);
                    if (target != null) {
                        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(
                                new SynchronizationRequestPacket(target.uuid()));
                    }
                    onClose();
                }).bounds(cx - 100, y, 200, 20).build());

        addRenderableWidget(Button.builder(
                        Component.translatable("gui.corpseorigin.close"),
                        b -> onClose())
                .bounds(cx - 100, y + 24, 200, 20).build());
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}