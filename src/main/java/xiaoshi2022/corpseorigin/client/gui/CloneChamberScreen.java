package xiaoshi2022.corpseorigin.client.gui;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import xiaoshi2022.corpseorigin.client.CorpseOriginClient;
import xiaoshi2022.corpseorigin.network.SynchronizationRequestPacket;

import java.util.List;

/**
 * 克隆仓面板：列出该玩家所有可转移的身体（各座仓、各个维度、走动的分身），点哪一具就转移过去。
 * <p>
 * 之所以列全量而不是只认眼前这一座仓：身体经常不在"当前仓"里——
 * 上一次转移时旧身体按规则存进了身边的另一座仓，或者被激活成了走动的分身。
 * 只按当前仓解析的话，会一直提示"这座克隆仓里没有可转移的身体"，手动就换不回来了。
 */
public class CloneChamberScreen extends Screen {

    private static final int ROW_HEIGHT = 22;
    /** 行数上限，身体很多时不至于顶出屏幕 */
    private static final int MAX_ROWS = 9;

    private final BlockPos pos;

    public CloneChamberScreen(BlockPos pos) {
        super(Component.translatable("gui.corpseorigin.clone_chamber"));
        this.pos = pos;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        List<CorpseOriginClient.ClientShellEntry> entries = CorpseOriginClient.clientShellEntries;
        int rows = Math.min(entries.size(), MAX_ROWS);
        int listTop = this.height / 2 - rows * ROW_HEIGHT / 2 - 14;

        for (int i = 0; i < rows; i++) {
            CorpseOriginClient.ClientShellEntry entry = entries.get(i);
            addRenderableWidget(Button.builder(label(entry), b -> transfer(entry))
                    .bounds(cx - 150, listTop + i * ROW_HEIGHT, 300, 20)
                    .build());
        }

        if (entries.isEmpty()) {
            addRenderableWidget(Button.builder(
                            Component.translatable("gui.corpseorigin.clone_chamber.no_bodies"),
                            b -> onClose())
                    .bounds(cx - 150, listTop-16, 300, 20)
                    .build());
        }

        addRenderableWidget(Button.builder(
                        Component.translatable("gui.corpseorigin.close"),
                        b -> onClose())
                .bounds(cx - 60, listTop + rows * ROW_HEIGHT + 10, 120, 20)
                .build());
    }

    private Component label(CorpseOriginClient.ClientShellEntry entry) {
        boolean here = entry.x() == this.pos.getX()
                && entry.y() == this.pos.getY()
                && entry.z() == this.pos.getZ();
        int percent = Math.min(100, Math.round(entry.progress() * 100.0F));
        String text = worldName(entry.world())
                + " (" + entry.x() + ", " + entry.y() + ", " + entry.z() + ") " + percent + "%";
        if (here) {
            text = text + " ← 本仓";
        }
        return Component.literal(text);
    }

    private static String worldName(String world) {
        if (world == null) {
            return "";
        }
        return switch (world) {
            case "minecraft:overworld" -> "主世界";
            case "minecraft:the_nether" -> "下界";
            case "minecraft:the_end" -> "末地";
            default -> world;
        };
    }

    /** 坐标给服务端做实时解析（仓按坐标定位），UUID 用于兜底（分身按 UUID 定位） */
    private void transfer(CorpseOriginClient.ClientShellEntry entry) {
        ClientPlayNetworking.send(new SynchronizationRequestPacket(
                entry.uuid(), new BlockPos(entry.x(), entry.y(), entry.z())));
        onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
