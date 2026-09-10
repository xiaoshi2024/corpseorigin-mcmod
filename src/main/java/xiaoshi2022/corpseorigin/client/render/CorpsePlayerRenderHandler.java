package xiaoshi2022.corpseorigin.client.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import xiaoshi2022.corpseorigin.client.render.layer.ExoskeletonRenderLayer;

import java.util.Map;
import java.util.WeakHashMap;

@Environment(EnvType.CLIENT)
public final class CorpsePlayerRenderHandler {

    public static final Map<AvatarRenderer<?>, ExoskeletonRenderLayer> LAYER_MAP = new WeakHashMap<>();

    private CorpsePlayerRenderHandler() {
    }

    public static void register() {
        // ✅ 空实现，Mixin 已经处理
    }

    public static ExoskeletonRenderLayer getAnyLayer() {
        return LAYER_MAP.values().stream().findFirst().orElse(null);
    }

    public static void triggerSwingAll() {
        for (ExoskeletonRenderLayer layer : LAYER_MAP.values()) {
            layer.triggerSwing();
        }
    }
}