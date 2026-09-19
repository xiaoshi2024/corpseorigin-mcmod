package xiaoshi2022.corpseorigin.event;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.phys.Vec3;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.character.CharacterManager;
import xiaoshi2022.corpseorigin.character.KaiWeiNai;
import xiaoshi2022.corpseorigin.skill.kaiweinai.ChrysanthemumShieldSkill;

/**
 * 开胃奶专属事件处理 —— 菊花盾的格挡与箭矢反弹。
 * <p>
 * 判定与动画是分开的：动画窗口由技能自己广播（{@link ChrysanthemumShieldSkill#onActivate}
 * → {@code CorpseNetwork.broadcastNiunaiParry}），这里只认窗口本身：
 * <ul>
 *   <li><b>正面伤害</b>：盾窗口内整下挡掉（是格挡，不是减伤，也不是无敌帧）；</li>
 *   <li><b>迎面箭矢</b>：{@link AbstractArrow}（弓 / 弩 / 三叉戟）会被原样弹回去，
 *       并认这具身体为新的发射者 —— 所以能反过来伤到原射手，正是"反弹箭矢类似于盾牌"。</li>
 * </ul>
 * 判定用"是不是开胃奶 + 在不在窗口内"，和技能入口（只有开胃奶的技能列表里有这招）一致。
 */
public final class KaiWeiNaiEventHandler {

    private KaiWeiNaiEventHandler() {
    }

    public static void register() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayer player)) {
                return true;
            }
            if (!hasShield(player) || !isFrontal(player, source)) {
                return true;
            }

            // 迎面飞来的箭矢 / 弩矢 / 三叉戟 → 原样弹回去
            if (source.getDirectEntity() instanceof AbstractArrow arrow && !arrow.isRemoved()) {
                reflect(player, arrow);
            }

            playBlockFeedback(player);
            // 整下挡掉：返回 false 直接取消这份伤害，不做任何重打（所以也不涉及递归）
            return false;
        });

        CorpseOrigin.LOGGER.info("KaiWeiNai events registered");
    }

    /** 该玩家现在是不是"开胃奶"且处于菊花盾窗口内 */
    private static boolean hasShield(ServerPlayer player) {
        if (!KaiWeiNai.ID.equals(CharacterManager.getInstance().getPlayerCharacterId(player))) {
            return false;
        }
        // 战斗中每一下都被调一次，所以放在角色判定之后（getPlayerCharacterId 更便宜）
        return ChrysanthemumShieldSkill.isShielding(player);
    }

    /**
     * 这记伤害是不是从<b>正面</b>来的（盾只护正面，背后挨打照样吃伤害）。
     * <ul>
     *   <li><b>投掷物</b>：命中那一刻它已经贴到身上了，用位置算不出方向 —— 改用"它朝哪飞"判断：
     *       迎面飞来的速度方向与我的朝向往相反；速度取不到时保守放行，别让盾漏；</li>
     *   <li><b>近战 / 其他直接来源</b>：用攻击者相对我的方位；</li>
     *   <li><b>没有直接来源</b>（摔落 / 着火 / 中毒这类环境伤害）：不吃盾。</li>
     * </ul>
     */
    private static boolean isFrontal(ServerPlayer player, DamageSource source) {
        Vec3 look = player.getLookAngle();
        Entity direct = source.getDirectEntity();

        if (direct instanceof Projectile projectile) {
            Vec3 motion = projectile.getDeltaMovement();
            if (motion.lengthSqr() > 1.0E-6) {
                return motion.normalize().dot(look) < 0.0;
            }
            return true;
        }

        if (direct != null) {
            Vec3 toSource = direct.position().subtract(player.position());
            if (toSource.lengthSqr() > 1.0E-6) {
                return look.dot(toSource.normalize()) > 0.0;
            }
        }
        return false;
    }

    /**
     * 把箭矢原样弹回去。
     * <p>
     * 掉头之后略微加速：按原速掉头的话，重力会很快把它拉到地上，看着不像"弹回去"。
     * 同时把发射者改成这具身体 —— 弹回去的箭才算我们的战果，也不会再判定打到自己。
     */
    private static void reflect(ServerPlayer player, AbstractArrow arrow) {
        arrow.setDeltaMovement(arrow.getDeltaMovement().scale(-1.15));
        arrow.setOwner(player);
        // 速度由服务端权威：不标这一下，客户端看不到这次掉头（同 APSSubSkills 的做法）
        arrow.hurtMarked = true;
    }

    /**
     * 挡下之后的反馈：一声闷响。
     * <p>
     * 盾张开的动画不在这里发 —— 它由技能开盾时广播的那条窗口覆盖整个持续时间，
     * 挨打时再发一遍只会把已经张开的花瓣每帧拽回第 0 帧重播。
     */
    private static void playBlockFeedback(ServerPlayer player) {
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.7F, 1.2F);
    }
}
