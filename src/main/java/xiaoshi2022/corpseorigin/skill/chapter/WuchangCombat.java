package xiaoshi2022.corpseorigin.skill.chapter;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.Vec3;
import xiaoshi2022.corpseorigin.character.CharacterManager;
import xiaoshi2022.corpseorigin.skill.SkillType;

import java.util.ArrayList;
import java.util.List;

/**
 * 黑白二将（尸王麾下京剧双煞）的经典招式：
 * <ul>
 *   <li>黑·武丑：武丑刀法（{@code wuchou_blade}）——贴地突进的一刀斩；武丑身法（{@code wuchou_step}）——潜形急冲；</li>
 *   <li>白·武生：武生双刀（{@code wusheng_twin}）——三段交叉连斩；双刀交斩（{@code wusheng_cross}）——放出十字刀气。</li>
 * </ul>
 */
public final class WuchangCombat {
    private WuchangCombat() {}
    private static final int BLACK_COLOR = 0x3a3a44;
    private static final int WHITE_COLOR = 0xf2f2f0;

    /** 武生双刀的延迟连斩段（第 1、2 段延迟到 6/12 tick）。 */
    private record Pending(ServerPlayer player, int atTick, int index) {}
    private static final List<Pending> PENDING = new ArrayList<>();
    private static final double[] TWIN_YAW = {-18, 0, 18};

    public static void register() {
        ServerLifecycleEvents.SERVER_STOPPED.register(s -> PENDING.clear());
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (PENDING.isEmpty()) return;
            int now = server.getTickCount();
            var it = PENDING.iterator();
            while (it.hasNext()) {
                Pending p = it.next();
                if (p.atTick > now) continue;
                it.remove();
                ServerPlayer player = p.player;
                if (player.isAlive() && !player.isRemoved()
                        && "bai_wusheng".equals(CharacterManager.getInstance().getPlayerCharacterId(player)))
                    twinStrike(player, p.index);
            }
        });
    }

    /** 武丑刀法：借势前扑一步，双刀在身前划出黑色刀弧。 */
    public static final class WuchouBlade extends RoleChapterSkill {
        public WuchouBlade() { super("wuchou_blade", SkillType.COMBAT, 60, "hei_wuchou"); }
        @Override public void onActivate(ServerPlayer p) {
            var level = (ServerLevel) p.level();
            Vec3 look = p.getLookAngle();
            Vec3 face = new Vec3(look.x, 0, look.z);
            if (face.lengthSqr() > 1.0E-6) {
                face = face.normalize().scale(.55);
                p.setDeltaMovement(face.x, p.getDeltaMovement().y, face.z);
                p.hurtMarked = true;
            }
            ChapterCombat.arc(p, look, 3.8, 55, 11f, .8);
            p.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
            ChapterScenes.action(p, "wuchou_blade", 12);
            ChapterCombat.arcDust(level, p.getBoundingBox().getCenter().add(0, .1, 0), look, 2.6, 55, BLACK_COLOR);
            level.playSound(null, p.getX(), p.getY(), p.getZ(),
                    SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1f, .7f);
        }
    }

    /** 武丑身法：矮身贴地急冲，短暂隐去身形，专打偷袭。 */
    public static final class WuchouStep extends RoleChapterSkill {
        public WuchouStep() { super("wuchou_step", SkillType.COMBAT, 120, "hei_wuchou"); }
        @Override public void onActivate(ServerPlayer p) {
            var level = (ServerLevel) p.level();
            Vec3 look = p.getLookAngle();
            Vec3 face = new Vec3(look.x, 0, look.z);
            if (face.lengthSqr() > 1.0E-6) {
                face = face.normalize().scale(1.35);
                p.setDeltaMovement(face.x, .22, face.z);
                p.hurtMarked = true;
            }
            p.addEffect(new MobEffectInstance(MobEffects.SPEED, 100, 1));
            p.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 40, 0));
            ChapterScenes.action(p, "wuchou_step", 12);
            QiEffects.burst(level, p.getX(), p.getY() + .2, p.getZ(), 0x9aa4b0, 18, .25);
            level.playSound(null, p.getX(), p.getY(), p.getZ(),
                    SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1f, 1.4f);
        }
    }

    /** 武生双刀：左、中、右三段白色刀光连斩，两息之内尽数劈在敌人身上。 */
    public static final class WushengTwin extends RoleChapterSkill {
        public WushengTwin() { super("wusheng_twin", SkillType.COMBAT, 70, "bai_wusheng"); }
        @Override public void onActivate(ServerPlayer p) {
            twinStrike(p, 0);
            PENDING.add(new Pending(p, p.tickCount + 6, 1));
            PENDING.add(new Pending(p, p.tickCount + 12, 2));
        }
    }

    private static void twinStrike(ServerPlayer p, int index) {
        var level = (ServerLevel) p.level();
        Vec3 dir = ChapterCombat.rotateY(p.getLookAngle(), TWIN_YAW[index]);
        ChapterCombat.arc(p, dir, 3.6, 50, 6f, .45);
        p.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
        p.swing(net.minecraft.world.InteractionHand.OFF_HAND, true);
        ChapterScenes.action(p, "wusheng_twin", 10);
        ChapterCombat.arcDust(level, p.getBoundingBox().getCenter().add(0, .1, 0), dir, 2.5, 50, WHITE_COLOR);
        level.playSound(null, p.getX(), p.getY(), p.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, .9f, 1.1f);
    }

    /** 双刀交斩：双刀十字交错，向 8 格内放出一道直线十字刀气。 */
    public static final class WushengCross extends RoleChapterSkill {
        public WushengCross() { super("wusheng_cross", SkillType.COMBAT, 90, "bai_wusheng"); }
        @Override public void onActivate(ServerPlayer p) {
            var level = (ServerLevel) p.level();
            Vec3 origin = p.getEyePosition();
            Vec3 dir = p.getLookAngle();
            Vec3 end = level.clip(new ClipContext(origin, origin.add(dir.scale(8)),
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p)).getLocation();
            p.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
            ChapterScenes.action(p, "wusheng_cross", 14);
            level.playSound(null, p.getX(), p.getY(), p.getZ(),
                    SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1f, .8f);
            for (LivingEntity t : level.getEntitiesOfClass(LivingEntity.class,
                    new net.minecraft.world.phys.AABB(origin, end).inflate(1.7),
                    t -> ChapterCombat.canHit(p, t))) {
                Vec3 offset = t.getBoundingBox().getCenter().subtract(origin);
                double along = offset.dot(dir);
                if (along < 0 || along > origin.distanceTo(end) + .5
                        || offset.subtract(dir.scale(along)).length() > 1.7 + along * .05) continue;
                if (level.clip(new ClipContext(origin, t.getBoundingBox().getCenter(),
                        ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p)).getType()
                        != net.minecraft.world.phys.HitResult.Type.MISS) continue;
                if (t.hurtServer(level, p.damageSources().playerAttack(p), 14f)) {
                    t.push(dir.x * .9, .3, dir.z * .9);
                    t.hurtMarked = true;
                }
            }
            Vec3 right = new Vec3(-dir.z, 0, dir.x).normalize();
            for (int i = 0; i < 20; i++) {
                Vec3 at = origin.lerp(end, i / 19.0);
                ChapterCombat.dust(level, at, WHITE_COLOR, 1.1f);
                ChapterCombat.dust(level, at.add(right.scale(.5)).add(0, .35, 0), WHITE_COLOR, 1f);
                ChapterCombat.dust(level, at.add(right.scale(-.5)).subtract(0, .35, 0), WHITE_COLOR, 1f);
            }
        }
    }
}
