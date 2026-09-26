package xiaoshi2022.corpseorigin.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xiaoshi2022.corpseorigin.block.entity.CNChessZbrsBlockEntity;

/**
 * 把"左键挖方块"在象棋尸兄身上转成近战攻击。
 * <p>
 * 26.2 已删除旧的 {@code Block.attack} 回调，左键开始破坏的唯一服务端入口是
 * {@link ServerPlayerGameMode#handleBlockBreakAction}。拦截 START_DESTROY_BLOCK：
 * 目标是象棋尸兄时，按玩家攻击力 × 攻击冷却造伤（和近战打实体一致），
 * 并取消原版的破坏流程（方块本身也不可挖掘）。
 */
@Mixin(ServerPlayerGameMode.class)
public class ServerPlayerGameModeMixin {

    @Shadow
    protected ServerLevel level;

    @Shadow
    protected ServerPlayer player;

    @Inject(method = "handleBlockBreakAction", at = @At("HEAD"), cancellable = true)
    private void corpseorigin$meleeChess(BlockPos pos, ServerboundPlayerActionPacket.Action action,
                                          Direction direction, int i, int j, CallbackInfo ci) {
        if (action != ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK) {
            return;
        }
        if (!(this.level.getBlockEntity(pos) instanceof CNChessZbrsBlockEntity zbrs)) {
            return;
        }

        // 无论如何都取消破坏流程：方块不可挖掘，它是"生物"不是矿
        ci.cancel();

        if (this.player.isSpectator()) {
            return;
        }

        float cooldownScale = this.player.getAttackStrengthScale(0.5F);
        if (cooldownScale <= 0.1F) {
            return;
        }

        float baseDamage = (float) this.player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        float amount = baseDamage * cooldownScale;
        if (amount <= 0.0F) {
            return;
        }

        DamageSource source = this.player.damageSources().playerAttack(this.player);
        zbrs.hurt(this.level, source, amount);
        this.player.resetAttackStrengthTicker();
    }
}
