package xiaoshi2022.corpseorigin.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xiaoshi2022.corpseorigin.event.WorldThreatManager;

/**
 * 世界威胁等级入口 —— 所有实体进入服务器世界的必经点。
 * <p>
 * {@code ServerLevel.addFreshEntity} 覆盖自然生成、事件召唤、刷怪蛋、/summon、
 * 区块读档与跨维度：任何路径下的尸兄（白名单见 {@link WorldThreatManager}）
 * 都会在加入世界时被挂上威胁加成。{@code apply} 内部用固定 id 的
 * 永久修饰符做幂等覆盖，读档重复触发绝不叠加。
 */
@Mixin(ServerLevel.class)
public class WorldThreatMixin {

    @Inject(method = "addFreshEntity", at = @At("HEAD"))
    private void corpseorigin$threatOnEntityAdd(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (entity instanceof LivingEntity living) {
            WorldThreatManager.apply(living, (ServerLevel) (Object) this);
        }
    }
}
