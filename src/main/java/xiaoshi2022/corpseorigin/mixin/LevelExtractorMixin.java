package xiaoshi2022.corpseorigin.mixin;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xiaoshi2022.corpseorigin.client.camera.PersistentCameraEntity;

/**
 * 灵魂出窍时让玩家看得见自己的本体。
 * <p>
 * 原版 {@code extractVisibleEntities} 的实体循环里有一句
 * 「{@code entity instanceof LocalPlayer && camera.entity() != entity} 就跳过」——
 * 只要镜头不在本体上（灵魂出窍把镜头交给了 {@link PersistentCameraEntity}），
 * 自己的躯壳就永远不进渲染队列，而别的客户端看到的仍是普通远端玩家，所以照常渲染。
 * <p>
 * 这里只把那处比较用的 {@code camera.entity()} 换成出窍中的本体：
 * 比较相等 → 不跳过；其他三处 {@code Camera.entity()}（相机实体自身的跳过判定）
 * 不动，用 {@code ordinal = 3} 精确落在该分支上。
 */
@Mixin(LevelExtractor.class)
abstract class LevelExtractorMixin {

    @Redirect(
            method = "extractVisibleEntities",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/Camera;entity()Lnet/minecraft/world/entity/Entity;",
                    ordinal = 3),
            require = 1)
    private Entity corpseorigin$soulBodyCameraEntity(Camera camera) {
        if (PersistentCameraEntity.isSoulProjectionActive()) {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null) {
                return player;
            }
        }
        return camera.entity();
    }
}
