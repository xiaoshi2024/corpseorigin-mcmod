package xiaoshi2022.corpseorigin.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xiaoshi2022.corpseorigin.entity.ZombieKin;
import xiaoshi2022.corpseorigin.entity.ZuoFloodLongEntity;

/**
 * 尸王威严：尸族生物不敢对龙右不敬。
 * <p>
 * 挂在 {@link Mob#canAttack(LivingEntity)} 这一个点上就能同时封掉两条路：
 * <ul>
 *   <li>{@code NearestAttackableTargetGoal} —— 索敌时走
 *       {@code TargetingConditions.test → LivingEntity.canAttack}，于是不会把龙右选成目标；</li>
 *   <li>{@code HurtByTargetGoal} —— 反击时走 {@code TargetGoal.canAttack → Mob.canAttack}，
 *       于是被龙右打了也不会记仇还手。</li>
 * </ul>
 * 放这里而不是逐个改每个尸族生物的 targetSelector：以后新加的尸族生物只要
 * {@code implements ZombieKin} 就自动继承这条规矩。
 * <p>
 * 伤害层还有 {@link ZombieKin#canAttack} 兜底（打不进去伤害），两层一起才是"不敢不敬"。
 */
@Mixin(Mob.class)
public class MobZombieKingRespectMixin {

    @Inject(method = "canAttack(Lnet/minecraft/world/entity/LivingEntity;)Z",
            at = @At("HEAD"), cancellable = true)
    private void corpseorigin$respectCorpseKing(LivingEntity target, CallbackInfoReturnable<Boolean> cir) {
        // 例外：左护法的尸蛟龙（神宠打手）—— 主人砍谁它就咬谁，哪怕目标是尸王
        // （它仍 implements ZombieKin，所以尸族不会主动围殴它，只是它自己不敬尸王）
        // this 经 Object 过渡：mixin 编译期 this 是 Mob，与 ZuoFloodLongEntity 是不可直接转换的兄弟类型
        if ((Object) this instanceof ZuoFloodLongEntity) {
            return;
        }
        if (this instanceof ZombieKin && ZombieKin.isZombieKing(target)) {
            cir.setReturnValue(false);
        }
    }
}
