package xiaoshi2022.corpseorigin.mixin;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xiaoshi2022.corpseorigin.item.ZbWormItem;
import xiaoshi2022.corpseorigin.registry.ModItems;

/**
 * 尸兄虫丢出去 → 变回活体虫子。
 * <p>
 * 1.21.1 那边是 NeoForge 的 {@code Item#onDroppedByPlayer}（能在掉落物生成<b>之前</b>拦下来）；
 * Fabric 既没有这个钩子，26.2 的 fabric-entity-events 也把 {@code ENTITY_LOAD} 删了，
 * 所以改成挂在掉落物实体自己的 tick 上：出生后第一帧服务端 tick 检查一次，
 * 命中就换成虫子（放在 tick 里做，避开"在世界正在添加实体时又添加实体"这种时序问题）。
 * <p>
 * 每个掉落物一辈子只检查一次：{@code corpseorigin$wormChecked} 打完标记就不再进这个方法体，
 * 免得每个掉落物每 tick 都去比对物品。
 */
@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin {

    @Unique
    private boolean corpseorigin$wormChecked;

    @Inject(method = "tick", at = @At("HEAD"))
    private void corpseorigin$releaseWorm(CallbackInfo ci) {
        if (this.corpseorigin$wormChecked) {
            return;
        }

        ItemEntity self = (ItemEntity) (Object) this;
        if (self.level().isClientSide()) {
            return;
        }
        this.corpseorigin$wormChecked = true;

        ItemStack stack = self.getItem();
        if (stack.is(ModItems.ZB_WORM_ITEM)) {
            ZbWormItem.tryReleaseWorm(self);
        }
    }
}
