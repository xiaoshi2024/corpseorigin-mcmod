package xiaoshi2022.corpseorigin.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xiaoshi2022.corpseorigin.character.CharacterManager;

/** The bug mother and her insect swarm can climb walls while selected. */
@Mixin(LivingEntity.class)
public abstract class SwarmClimbMixin {
    @Inject(method = "onClimbable", at = @At("HEAD"), cancellable = true)
    private void corpseorigin$swarmClimbs(CallbackInfoReturnable<Boolean> cir) {
        if (!((Object)this instanceof Player player)
                || !player.horizontalCollision || player.isSpectator()) return;
        String role = player.level().isClientSide()
                ? player.getAttachedOrCreate(xiaoshi2022.corpseorigin.skill.chapter.ChapterActorState.ROLE)
                : CharacterManager.getInstance().getPlayerCharacterId(player);
        if ("chongmu".equals(role) || "chongqun".equals(role)
                || xiaoshi2022.corpseorigin.skill.chapter.GourdInheritance.climbing(player)) {
            cir.setReturnValue(true);
        }
    }
}
