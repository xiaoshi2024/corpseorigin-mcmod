package xiaoshi2022.corpseorigin.client.gui;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import xiaoshi2022.corpseorigin.client.CorpseOriginClient;
import xiaoshi2022.corpseorigin.network.SynchronizationRequestPacket;

public class CloneChamberScreen extends Screen {

    private final BlockPos pos;

    public CloneChamberScreen(BlockPos pos) {
        super(Component.translatable("gui.corpseorigin.clone_chamber"));
        this.pos = pos;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int y = this.height / 2 - 40;

        addRenderableWidget(Button.builder(
                Component.translatable("gui.corpseorigin.clone_chamber.transfer"),
                b -> {
                    // 缓存条目只用来带 UUID；坐标才是服务端解析目标的依据（列表可能已过期）
                    CorpseOriginClient.ClientShellEntry target =
                            CorpseOriginClient.clientShellEntries.stream()
                                    .filter(e -> e.x() == pos.getX()
                                            && e.y() == pos.getY()
                                            && e.z() == pos.getZ())
                                    .findFirst().orElse(null);
                    net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(
                            new SynchronizationRequestPacket(
                                    target == null ? new java.util.UUID(0L, 0L) : target.uuid(),
                                    pos));
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
