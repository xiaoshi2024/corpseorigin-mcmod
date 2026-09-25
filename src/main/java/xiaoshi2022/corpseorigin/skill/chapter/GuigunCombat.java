package xiaoshi2022.corpseorigin.skill.chapter;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.chat.Component;
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

/**
 * 鬼棍的经典招式（人类·地级 与 尸兄·天级 两套）。
 * <ul>
 *   <li>人类：三节棍横扫（{@code guigun_sweep}）、金针刺穴切痛觉入恶鬼形态（{@code guigun_guard}）；</li>
 *   <li>尸兄：次声波尸棍持续共振（{@code guigun_resonance}）、大木棍砸地重击（{@code guigun_crush}）。</li>
 * </ul>
 * 所有判定只在服务端进行，表现走 {@link ChapterScenes}/{@link QiEffects}/{@link ChapterCombat} 的既有通道。
 */
public final class GuigunCombat {
    private GuigunCombat() {}
    private static final int SWEEP_COLOR = 0xe8e0c8;
    private static final int FURY_COLOR = 0xc01e2e;
    private static final int RESONANCE_COLOR = 0x7cc23f;
    private static final int CRUSH_COLOR = 0x4f8f2f;

    /** 次声波持续声场：鬼棍·尸兄专属，死亡/换角/跨界立即中断。 */
    private record Channel(ServerPlayer player, ServerLevel level, int endTick) {}
    private static final Map<UUID, Channel> CHANNELS = new HashMap<>();

    public static boolean resonating(ServerPlayer p) { return CHANNELS.containsKey(p.getUUID()); }

    public static void register() {
        ServerLifecycleEvents.SERVER_STOPPED.register(s -> CHANNELS.clear());
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            CHANNELS.values().removeIf(c -> {
                ServerPlayer p = c.player;
                if (!p.isAlive() || p.isRemoved() || p.level() != c.level
                        || !"guigun_corpse".equals(CharacterManager.getInstance().getPlayerCharacterId(p))
                        || p.tickCount >= c.endTick) {
                    p.setAttached(ChapterScenes.ACTION, "");
                    return true;
                }
                int age = RESONANCE_TICKS - (c.endTick - p.tickCount);
                ServerLevel level = (ServerLevel) p.level();
                // 一圈圈向外扩散的绿色气浪（整圈一朵，每 4 tick 推一圈，不再逐点撒粒子）。
                double radius = 1.2 + (age % 10) * .62;
                if (age % 4 == 0) QiEffects.cloud(level, p.position().add(0, .25, 0), RESONANCE_COLOR, (float) radius, 10);
                if (age % 10 == 0) pulse(p, level);
                return false;
            });
        });
    }

    /** 一次次声波脉冲：内脏共振——持续伤害 + 反胃 + 迟缓。 */
    private static void pulse(ServerPlayer p, ServerLevel level) {
        level.playSound(null, p.getX(), p.getY(), p.getZ(),
                SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.5f, .5f);
        for (LivingEntity t : level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(6.5))) {
            if (!ChapterCombat.canHit(p, t) || p.distanceToSqr(t) > 6.5 * 6.5) continue;
            t.hurtServer(level, p.damageSources().playerAttack(p), 5f);
            t.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 100, 0));
            t.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 0));
        }
    }

    /** 棍术横扫（鬼影棍）：三节棍在身前扫出 120 度弧光。 */
    public static final class Sweep extends RoleChapterSkill {
        public Sweep() { super("guigun_sweep", SkillType.COMBAT, 60, "guigun_human"); }
        @Override public void onActivate(ServerPlayer p) {
            var level = (ServerLevel) p.level();
            Vec3 look = p.getLookAngle();
            int hit = ChapterCombat.arc(p, look, 4.2, 60, 12f, .9);
            p.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
            ChapterScenes.action(p, "guigun_sweep", 12);
            Vec3 origin = p.getBoundingBox().getCenter().add(0, .15, 0);
            ChapterCombat.arcDust(level, origin, look, 2.9, 60, SWEEP_COLOR);
            level.playSound(null, p.getX(), p.getY(), p.getZ(),
                    SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1f, .85f);
            if (hit == 0) p.sendOverlayMessage(Component.translatable("skill.corpseorigin.guigun_sweep.miss"));
        }
    }

    /** 金针刺穴：金针切断全部痛觉，短时化为力量速度暴涨的恶鬼形态。 */
    public static final class Guard extends RoleChapterSkill {
        public Guard() { super("guigun_guard", SkillType.COMBAT, 240, "guigun_human"); }
        @Override public void onActivate(ServerPlayer p) {
            p.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 200, 1));
            p.addEffect(new MobEffectInstance(MobEffects.SPEED, 200, 1));
            p.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 200, 0));
            ChapterScenes.action(p, "guigun_guard", 40);
            QiEffects.aura(p, "guigun_fury", FURY_COLOR, 2f, 50);
            // 周身炸开一圈恶鬼之气（原来是一圈 24 颗粒子，现在整圈一朵）。
            QiEffects.cloud((ServerLevel) p.level(), p.position().add(0, .9, 0), FURY_COLOR, 1.8f, 14);
            p.level().playSound(null, p.getX(), p.getY(), p.getZ(),
                    SoundEvents.ILLUSIONER_PREPARE_BLINDNESS, SoundSource.PLAYERS, 1.2f, .8f);
            p.sendOverlayMessage(Component.translatable("skill.corpseorigin.guigun_guard.fury"));
        }
    }

    /** 次声波尸棍：高举布满骷髅的尸棍，3 秒内向周围持续扫出次声波。 */
    public static final class Resonance extends RoleChapterSkill {
        public Resonance() { super("guigun_resonance", SkillType.COMBAT, 200, "guigun_corpse"); }
        @Override public Component checkUsable(ServerPlayer p) {
            Component blocked = super.checkUsable(p);
            if (blocked != null) return blocked;
            if (resonating(p)) return Component.translatable("skill.corpseorigin.guigun_resonance.busy");
            return null;
        }
        @Override public void onActivate(ServerPlayer p) {
            CHANNELS.put(p.getUUID(), new Channel(p, (ServerLevel) p.level(), p.tickCount + RESONANCE_TICKS));
            ChapterScenes.action(p, "guigun_resonance", RESONANCE_TICKS + 4);
            QiEffects.aura(p, "guigun_resonance", RESONANCE_COLOR, 6.5f, 65);
            p.level().playSound(null, p.getX(), p.getY(), p.getZ(),
                    SoundEvents.WARDEN_ROAR, SoundSource.PLAYERS, 1f, .7f);
        }
    }
    private static final int RESONANCE_TICKS = 60;

    /** 尸棍重击（打狗棍法）：大木棍高高跃起砸地，落点 3 格内全部震飞。 */
    public static final class Crush extends RoleChapterSkill {
        public Crush() { super("guigun_crush", SkillType.COMBAT, 80, "guigun_corpse"); }
        @Override public void onActivate(ServerPlayer p) {
            var level = (ServerLevel) p.level();
            Vec3 start = p.getEyePosition();
            Vec3 look = p.getLookAngle();
            LivingEntity direct = ChapterCombat.aim(p, 4.5);
            Vec3 impact;
            if (direct != null) {
                impact = direct.position().add(0, .5, 0);
            } else {
                impact = level.clip(new ClipContext(start, start.add(look.scale(4.5)),
                        ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p)).getLocation();
            }
            p.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
            ChapterScenes.action(p, "guigun_crush", 18);
            level.playSound(null, impact.x, impact.y, impact.z,
                    SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1f, .85f);
            ChapterCombat.ring(level, impact, 1.6, CRUSH_COLOR, 24);
            ChapterCombat.ring(level, impact, 3, CRUSH_COLOR, 32);
            QiEffects.burst(level, impact.x, impact.y + .1, impact.z, CRUSH_COLOR, 2, .1);
            for (LivingEntity t : level.getEntitiesOfClass(LivingEntity.class,
                    new net.minecraft.world.phys.AABB(impact, impact).inflate(3),
                    t -> ChapterCombat.canHit(p, t) && impact.distanceToSqr(t.getBoundingBox().getCenter()) <= 9)) {
                if (!t.hurtServer(level, p.damageSources().playerAttack(p), 20f)) continue;
                Vec3 away = t.getBoundingBox().getCenter().subtract(impact);
                if (away.lengthSqr() < 1.0E-6) away = look;
                away = away.normalize();
                t.push(away.x * 1.15, .55, away.z * 1.15);
                t.hurtMarked = true;
            }
        }
    }
}
