package xiaoshi2022.corpseorigin.mixin;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.golem.AbstractGolem;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xiaoshi2022.corpseorigin.entity.ZombieKin;

@Mixin(IronGolem.class)
public abstract class IronGolemCorpseTargetMixin extends AbstractGolem {
    protected IronGolemCorpseTargetMixin(EntityType<? extends AbstractGolem> type, Level level) {
        super(type, level);
    }

    @Inject(method = "registerGoals", at = @At("TAIL"))
    private void corpseorigin$targetCorpseCreatures(CallbackInfo ci) {
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, LivingEntity.class,
                10, true, false, (target, level) ->
                        target instanceof ZombieKin && ZombieKin.isZombieKin(target)));
    }
}
