package xiaoshi2022.corpseorigin.mixin;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xiaoshi2022.corpseorigin.entity.LowerLevelZbEntity;
import xiaoshi2022.corpseorigin.growth.CorpseHorror;
@Mixin(LivingEntity.class)
public abstract class CorpseHorrorDeathMixin {
    @Inject(method="tickDeath",at=@At("HEAD"),cancellable=true)
    private void corpseorigin$horrorDeath(CallbackInfo ci){
        if((Object)this instanceof LowerLevelZbEntity z && CorpseHorror.applies(z)){CorpseHorror.deathTick(z);ci.cancel();}
    }
}
