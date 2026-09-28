package xiaoshi2022.corpseorigin.mixin;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xiaoshi2022.corpseorigin.client.render.laser.TianGangBeamState;
import xiaoshi2022.corpseorigin.item.weapon.TianGangKeyItem;
import xiaoshi2022.corpseorigin.network.CorpsePayloads;
import xiaoshi2022.corpseorigin.network.TianGangSwingPayload;
import xiaoshi2022.corpseorigin.skill.zhaoritian.TianGangKeySkill;

/** Route attacks to the active weapon hand without also attacking with the other hand. */
@Mixin(Minecraft.class)
public abstract class TianGangControlsMixin {
    @Unique private boolean corpseorigin$keyUseHeld;
    @Unique private long corpseorigin$lastSwing = Long.MIN_VALUE / 2;
    @Unique private Object corpseorigin$controlWorld;

    @Unique private boolean corpseorigin$leftKey() {
        var mc = (Minecraft)(Object)this;
        return mc.player != null && mc.level != null && !mc.player.isSpectator()
                && TianGangKeyItem.weaponHand(mc.player) != null;
    }
    @Unique private boolean corpseorigin$active() {
        var mc = (Minecraft)(Object)this;
        if (!corpseorigin$leftKey()) return false;
        var beam = TianGangBeamState.get(mc.player.getUUID());
        return beam != null && mc.player.getItemInHand(beam.hand()).getItem() instanceof TianGangKeyItem;
    }
    @Inject(method = "tick", at = @At("HEAD"))
    private void corpseorigin$releaseLatch(CallbackInfo ci) {
        var mc = (Minecraft)(Object)this;
        if (corpseorigin$controlWorld != mc.level) {
            corpseorigin$controlWorld = mc.level;
            corpseorigin$keyUseHeld = false;
            corpseorigin$lastSwing = Long.MIN_VALUE / 2;
        }
        if (!mc.options.keyUse.isDown() || mc.gui.screen() != null || !corpseorigin$leftKey()) corpseorigin$keyUseHeld = false;
    }
    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
    private void corpseorigin$toggleBlade(CallbackInfo ci) {
        if (!corpseorigin$leftKey()) return;
        ci.cancel();
        if (corpseorigin$keyUseHeld) return;
        corpseorigin$keyUseHeld = true;
        ClientPlayNetworking.send(new CorpsePayloads.ActivateSkillC2S(TianGangKeySkill.PATH));
    }
    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void corpseorigin$leftSwing(CallbackInfoReturnable<Boolean> cir) {
        if (!corpseorigin$active()) return;
        cir.setReturnValue(false);
        var mc = (Minecraft)(Object)this;
        var beam = TianGangBeamState.get(mc.player.getUUID());
        long now = mc.level.getGameTime();
        if (!beam.firing() || now - corpseorigin$lastSwing < 6) return;
        corpseorigin$lastSwing = now;
        mc.player.swing(beam.hand());
        ClientPlayNetworking.send(new TianGangSwingPayload());
    }
    @Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
    private void corpseorigin$noMainHandMining(boolean held, CallbackInfo ci) {
        if (corpseorigin$active()) ci.cancel();
    }
}
