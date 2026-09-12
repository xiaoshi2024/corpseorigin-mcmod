package xiaoshi2022.corpseorigin.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xiaoshi2022.corpseorigin.item.weapon.BloodLotusLamp;

@Mixin(Mob.class)
public class MobPeacefulMixin {

    @Inject(method = "setTarget", at = @At("HEAD"), cancellable = true)
    private void corpseorigin$noAggro(LivingEntity target, CallbackInfo ci) {
        Mob self = (Mob) (Object) this;

        if (self.getMainHandItem().getItem() instanceof BloodLotusLamp
                && target instanceof Player player) {
            // ✅ 检查这个玩家是否最近攻击过自己
            long lastHurtTime = self.getLastHurtByMobTimestamp();
            LivingEntity lastAttacker = self.getLastHurtByMob();

            // 如果最近 10 秒（200 tick）内没有被这个玩家攻击过，就不主动攻击
            if (lastAttacker != player
                    || self.tickCount - lastHurtTime > 200) {
                ci.cancel();
            }
        }
    }
}
