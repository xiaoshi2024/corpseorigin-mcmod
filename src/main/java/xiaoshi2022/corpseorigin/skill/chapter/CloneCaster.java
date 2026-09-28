package xiaoshi2022.corpseorigin.skill.chapter;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import xiaoshi2022.corpseorigin.entity.CloneAvatarEntity;
import xiaoshi2022.corpseorigin.entity.ZombieKin;

import java.util.*;

/**
 * 克隆分身专用的"角色招式执行器"。
 * <p>
 * 玩家技能（{@link xiaoshi2022.corpseorigin.skill.ISkill}）强类型绑定 {@code ServerPlayer}，
 * 没法直接给生物用。这里把新角色那批已经 LivingEntity 友好的招式（鬼棍 / 黑白二将）
 * 用 {@link ChapterCombat} 的通用重载重新实现一遍，由 {@link xiaoshi2022.corpseorigin.entity.CloneSkillGoal}
 * 按分身保存的角色 id 选用。所有伤害/位移只在服务端结算，动作姿势走
 * {@link ChapterScenes#ACTION} 附件（对任意实体同步，客户端摆臂 Mixin 自动生效）。
 * <p>
 * 冷却、共振声场、白无常连斩的延迟段全部在本类自建状态（玩家那套强类型 ServerPlayer，
 * 不能混用），分身死亡 / 跨界 / 换角色时自检清除。
 */
public final class CloneCaster {
    private CloneCaster() {}

    private static final int SWEEP_COLOR = 0xe8e0c8;
    private static final int FURY_COLOR = 0xc01e2e;
    private static final int RESONANCE_COLOR = 0x7cc23f;
    private static final int CRUSH_COLOR = 0x4f8f2f;
    private static final int BLACK_COLOR = 0x3a3a44;
    private static final int WHITE_COLOR = 0xf2f2f0;
    private static final int RESONANCE_TICKS = 60;
    private static final double[] TWIN_YAW = {-18, 0, 18};

    /** 每具分身每招的冷却：分身 UUID → (招式名 → 可再次施放的 gameTime)。 */
    private static final Map<UUID, Map<String, Long>> COOLDOWNS = new HashMap<>();
    /** 鬼棍·尸兄的次声波持续声场。 */
    private record Channel(CloneAvatarEntity caster, ServerLevel level, long start, long end) {}
    private static final Map<UUID, Channel> CHANNELS = new HashMap<>();
    /** 白无常三段连斩的延迟段。 */
    private record Pend(CloneAvatarEntity caster, long at, int index) {}
    private static final List<Pend> PENDS = new ArrayList<>();

    public static void register() {
        ServerLifecycleEvents.SERVER_STOPPED.register(s -> {
            CloneRoleSkills.clearAll();
            COOLDOWNS.clear();
            CHANNELS.clear();
            PENDS.clear();
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            CHANNELS.values().removeIf(c -> {
                CloneAvatarEntity caster = c.caster;
                if (!caster.isAlive() || caster.isRemoved() || caster.level() != c.level
                        || !caster.getMainHandItem().is(xiaoshi2022.corpseorigin.registry.ModItems.GUIGUN_CLUB)
                        || c.level.getGameTime() >= c.end) {
                    pose(caster, "", 0);
                    return true;
                }
                int age = (int) (c.level.getGameTime() - c.start);
                // 一圈圈向外扩散的绿色气浪
                double radius = 1.2 + (age % 10) * .62;
                if (age % 4 == 0) {
                    QiEffects.cloud(c.level, caster.position().add(0, .25, 0), RESONANCE_COLOR, (float) radius, 10);
                }
                if (age % 10 == 0) {
                    resonancePulse(caster, c.level);
                }
                return false;
            });
            if (!PENDS.isEmpty()) {
                var it = PENDS.iterator();
                while (it.hasNext()) {
                    Pend pend = it.next();
                    if (pend.at > pend.caster.level().getGameTime()) continue;
                    it.remove();
                    CloneAvatarEntity caster = pend.caster;
                    if (caster.isAlive() && !caster.isRemoved()
                            && "bai_wusheng".equals(caster.getBodyRole())) {
                        twinStrike(caster, pend.index);
                    }
                }
            }
        });
    }

    /** 分身被移除时清掉它残留的全部施法状态。 */
    public static void clear(UUID uuid) {
        CloneRoleSkills.clear(uuid);
        COOLDOWNS.remove(uuid);
        CHANNELS.remove(uuid);
        PENDS.removeIf(p -> p.caster.getUUID().equals(uuid));
    }

    public static boolean resonating(UUID uuid) {
        return CHANNELS.containsKey(uuid);
    }

    /**
     * 尝试按当前角色释放一招。返回是否真的放了（给目标调整节奏用）。
     * 没有目标 / 冷却中 / 没角色时返回 false。
     */
    public static boolean cast(CloneAvatarEntity caster) {
        LivingEntity target = caster.getTarget();
        if (target == null || !target.isAlive()) {
            return false;
        }
        String role = caster.getBodyRole();
        if (role == null) {
            return false;
        }
        if (!ChapterCombat.canHit(caster, target) || !caster.hasLineOfSight(target)) return false;
        xiaoshi2022.corpseorigin.entity.CloneWeaponArts.face(caster, target);
        double distSq = caster.distanceToSqr(target);
        return switch (role) {
            case "guigun_human" -> castGuigunHuman(caster, target, distSq);
            case "guigun_corpse" -> castGuigunCorpse(caster, target, distSq);
            case "hei_wuchou" -> castHeiWuchou(caster, target, distSq);
            case "bai_wusheng" -> castBaiWusheng(caster, target, distSq);
            default -> CloneRoleSkills.cast(caster, target);
        };
    }

    public static boolean castWeapon(CloneAvatarEntity caster) {
        var target = caster.getTarget();
        if (target == null || !ChapterCombat.canHit(caster, target) || !caster.hasLineOfSight(target)) return false;
        double distance = caster.distanceToSqr(target);
        if (caster.getMainHandItem().is(xiaoshi2022.corpseorigin.registry.ModItems.GUIGUN_WEAP)) {
            if (distance > 4.5 * 4.5 || !ready(caster, "guigun_sweep", 60)) return false;
            ChapterCombat.arc(caster, caster.getLookAngle(), 4.2, 60, 12, .9);
            caster.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
            pose(caster, "guigun_sweep", 12);
            ChapterCombat.arcDust((ServerLevel) caster.level(), caster.getBoundingBox().getCenter(), caster.getLookAngle(), 2.9, 60, SWEEP_COLOR);
            return true;
        }
        return caster.getMainHandItem().is(xiaoshi2022.corpseorigin.registry.ModItems.GUIGUN_CLUB)
                && castGuigunCorpse(caster, target, distance);
    }

    // ==================== 鬼棍·人类 ====================

    private static boolean castGuigunHuman(CloneAvatarEntity caster, LivingEntity target, double distSq) {
        // 残血且没在狂暴：金针切痛觉
        if (caster.getHealth() < caster.getMaxHealth() * .6F
                && caster.getEffect(MobEffects.STRENGTH) == null && ready(caster, "guigun_guard", 240)) {
            caster.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 200, 1));
            caster.addEffect(new MobEffectInstance(MobEffects.SPEED, 200, 1));
            caster.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 200, 0));
            pose(caster, "guigun_guard", 40);
            QiEffects.aura(caster, "guigun_fury", FURY_COLOR, 2f, 50);
            QiEffects.cloud((ServerLevel) caster.level(), caster.position().add(0, .9, 0), FURY_COLOR, 1.8f, 14);
            caster.level().playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                    SoundEvents.ILLUSIONER_PREPARE_BLINDNESS, SoundSource.HOSTILE, 1.2f, .8f);
            return true;
        }
        // 三节棍横扫（玩家版 weaponOnly：手里必须有三节棍）
        if (distSq <= 4.5 * 4.5
                && holding(caster, xiaoshi2022.corpseorigin.registry.ModItems.GUIGUN_WEAP)
                && ready(caster, "guigun_sweep", 60)) {
            ServerLevel level = (ServerLevel) caster.level();
            Vec3 look = caster.getLookAngle();
            ChapterCombat.arc(caster, look, 4.2, 60, 12f, .9);
            caster.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
            pose(caster, "guigun_sweep", 12);
            ChapterCombat.arcDust(level, caster.getBoundingBox().getCenter().add(0, .15, 0),
                    look, 2.9, 60, SWEEP_COLOR);
            level.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                    SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1f, .85f);
            return true;
        }
        return false;
    }

    // ==================== 鬼棍·尸兄 ====================

    private static boolean castGuigunCorpse(CloneAvatarEntity caster, LivingEntity target, double distSq) {
        // 次声波共振：起手先震一发，之后持续 3 秒（玩家版 weaponOnly：手里必须有大木棍）
        if (distSq <= 7 * 7
                && holding(caster, xiaoshi2022.corpseorigin.registry.ModItems.GUIGUN_CLUB)
                && ready(caster, "guigun_resonance", 200)
                && !CHANNELS.containsKey(caster.getUUID())) {
            ServerLevel level = (ServerLevel) caster.level();
            long now = level.getGameTime();
            CHANNELS.put(caster.getUUID(), new Channel(caster, level, now, now + RESONANCE_TICKS));
            pose(caster, "guigun_resonance", RESONANCE_TICKS + 4);
            QiEffects.aura(caster, "guigun_resonance", RESONANCE_COLOR, 6.5f, 65);
            level.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                    SoundEvents.WARDEN_ROAR, SoundSource.HOSTILE, 1f, .7f);
            resonancePulse(caster, level);
            return true;
        }
        // 大木棍砸地：朝目标位置跳劈，落点 3 格震飞（玩家版 weaponOnly）
        if (distSq <= 5 * 5
                && holding(caster, xiaoshi2022.corpseorigin.registry.ModItems.GUIGUN_CLUB)
                && ready(caster, "guigun_crush", 80)) {
            ServerLevel level = (ServerLevel) caster.level();
            Vec3 start = caster.getEyePosition();
            Vec3 look = caster.getLookAngle();
            LivingEntity direct = ChapterCombat.aim(caster, 4.5);
            Vec3 impact;
            if (direct != null) {
                impact = direct.position().add(0, .5, 0);
            } else {
                impact = level.clip(new ClipContext(start, start.add(look.scale(4.5)),
                        ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster)).getLocation();
            }
            caster.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
            pose(caster, "guigun_crush", 18);
            // 朝落点跃一步，更有"跳劈"的分量感
            Vec3 lunge = impact.subtract(caster.position());
            if (lunge.lengthSqr() > 1.0E-4) {
                lunge = lunge.normalize().scale(.65);
                caster.setDeltaMovement(lunge.x, .42, lunge.z);
                caster.hurtMarked = true;
            }
            level.playSound(null, impact.x, impact.y, impact.z,
                    SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 1f, .85f);
            ChapterCombat.ring(level, impact, 1.6, CRUSH_COLOR, 24);
            ChapterCombat.ring(level, impact, 3, CRUSH_COLOR, 32);
            QiEffects.burst(level, impact.x, impact.y + .1, impact.z, CRUSH_COLOR, 2, .1);
            for (LivingEntity t : level.getEntitiesOfClass(LivingEntity.class,
                    new AABB(impact, impact).inflate(3),
                    t -> isFoe(caster, t) && impact.distanceToSqr(t.getBoundingBox().getCenter()) <= 9)) {
                if (!t.hurtServer(level, caster.damageSources().mobAttack(caster), 20f)) continue;
                Vec3 away = t.getBoundingBox().getCenter().subtract(impact);
                if (away.lengthSqr() < 1.0E-6) away = look;
                away = away.normalize();
                t.push(away.x * 1.15, .55, away.z * 1.15);
                t.hurtMarked = true;
            }
            return true;
        }
        return false;
    }

    /** 次声波脉冲：持续伤害 + 反胃 + 迟缓。 */
    private static void resonancePulse(CloneAvatarEntity caster, ServerLevel level) {
        level.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.HOSTILE, 1.5f, .5f);
        for (LivingEntity t : level.getEntitiesOfClass(LivingEntity.class, caster.getBoundingBox().inflate(6.5))) {
            if (!isFoe(caster, t) || caster.distanceToSqr(t) > 6.5 * 6.5) continue;
            t.hurtServer(level, caster.damageSources().mobAttack(caster), 5f);
            t.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 100, 0));
            t.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 0));
        }
    }

    // ==================== 黑·武丑 ====================

    private static boolean castHeiWuchou(CloneAvatarEntity caster, LivingEntity target, double distSq) {
        // 拉远距离时潜形急冲贴脸
        if (distSq > 5 * 5 && ready(caster, "wuchou_step", 120)) {
            ServerLevel level = (ServerLevel) caster.level();
            Vec3 face = horizontalLook(caster).scale(1.35);
            caster.setDeltaMovement(face.x, .22, face.z);
            caster.hurtMarked = true;
            caster.addEffect(new MobEffectInstance(MobEffects.SPEED, 100, 1));
            caster.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 40, 0));
            pose(caster, "wuchou_step", 12);
            QiEffects.burst(level, caster.getX(), caster.getY() + .2, caster.getZ(), 0x9aa4b0, 18, .25);
            level.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                    SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1f, 1.4f);
            return true;
        }
        // 贴地一刀斩
        if (distSq <= 6 * 6 && ready(caster, "wuchou_blade", 60)) {
            ServerLevel level = (ServerLevel) caster.level();
            Vec3 look = caster.getLookAngle();
            Vec3 face = horizontalLook(caster).scale(.55);
            caster.setDeltaMovement(face.x, caster.getDeltaMovement().y, face.z);
            caster.hurtMarked = true;
            ChapterCombat.arc(caster, look, 3.8, 55, 11f, .8);
            caster.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
            pose(caster, "wuchou_blade", 12);
            ChapterCombat.arcDust(level, caster.getBoundingBox().getCenter().add(0, .1, 0),
                    look, 2.6, 55, BLACK_COLOR);
            level.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                    SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.HOSTILE, 1f, .7f);
            return true;
        }
        return false;
    }

    // ==================== 白·武生 ====================

    private static boolean castBaiWusheng(CloneAvatarEntity caster, LivingEntity target, double distSq) {
        // 中远距离：十字刀气直线
        if (distSq > 3 * 3 && distSq <= 8 * 8 && ready(caster, "wusheng_cross", 90)) {
            ServerLevel level = (ServerLevel) caster.level();
            Vec3 origin = caster.getEyePosition();
            Vec3 dir = caster.getLookAngle();
            Vec3 end = level.clip(new ClipContext(origin, origin.add(dir.scale(8)),
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster)).getLocation();
            caster.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
            pose(caster, "wusheng_cross", 14);
            level.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                    SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.HOSTILE, 1f, .8f);
            for (LivingEntity t : level.getEntitiesOfClass(LivingEntity.class,
                    new AABB(origin, end).inflate(1.7),
                    t -> isFoe(caster, t))) {
                Vec3 offset = t.getBoundingBox().getCenter().subtract(origin);
                double along = offset.dot(dir);
                if (along < 0 || along > origin.distanceTo(end) + .5
                        || offset.subtract(dir.scale(along)).length() > 1.7 + along * .05) continue;
                if (level.clip(new ClipContext(origin, t.getBoundingBox().getCenter(),
                        ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster)).getType()
                        != HitResult.Type.MISS) continue;
                if (t.hurtServer(level, caster.damageSources().mobAttack(caster), 14f)) {
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
            return true;
        }
        // 贴脸：三段交叉连斩
        if (distSq <= 4 * 4 && ready(caster, "wusheng_twin", 70)) {
            long now = ((ServerLevel) caster.level()).getGameTime();
            twinStrike(caster, 0);
            PENDS.add(new Pend(caster, now + 6, 1));
            PENDS.add(new Pend(caster, now + 12, 2));
            return true;
        }
        return false;
    }

    private static void twinStrike(CloneAvatarEntity caster, int index) {
        ServerLevel level = (ServerLevel) caster.level();
        Vec3 dir = ChapterCombat.rotateY(caster.getLookAngle(), TWIN_YAW[index]);
        ChapterCombat.arc(caster, dir, 3.6, 50, 6f, .45);
        caster.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
        caster.swing(net.minecraft.world.InteractionHand.OFF_HAND, true);
        pose(caster, "wusheng_twin", 10);
        ChapterCombat.arcDust(level, caster.getBoundingBox().getCenter().add(0, .1, 0),
                dir, 2.5, 50, WHITE_COLOR);
        level.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, .9f, 1.1f);
    }

    // ==================== 其他有角色的分身（龙右等） ====================

    private static boolean castGeneric(CloneAvatarEntity caster, LivingEntity target, double distSq) {
        if (distSq <= 4 * 4 && ready(caster, "clone_claw", 80)) {
            ServerLevel level = (ServerLevel) caster.level();
            Vec3 look = caster.getLookAngle();
            ChapterCombat.arc(caster, look, 3.8, 70, 10f, .8);
            caster.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
            pose(caster, "attack", 14);
            ChapterCombat.arcDust(level, caster.getBoundingBox().getCenter().add(0, .15, 0),
                    look, 2.6, 70, FURY_COLOR);
            level.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                    SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.HOSTILE, 1f, .8f);
            return true;
        }
        // 拉开距离时爆发追身
        if (distSq > 6 * 6 && ready(caster, "clone_chase", 140)) {
            ServerLevel level = (ServerLevel) caster.level();
            Vec3 face = horizontalLook(caster).scale(.9);
            caster.setDeltaMovement(face.x, .25, face.z);
            caster.hurtMarked = true;
            caster.addEffect(new MobEffectInstance(MobEffects.SPEED, 80, 0));
            pose(caster, "charge", 16);
            QiEffects.burst(level, caster.getX(), caster.getY() + .2, caster.getZ(), FURY_COLOR, 14, .3);
            return true;
        }
        return false;
    }

    // ==================== 工具 ====================

    /** 分身主手或副手是否持有指定武器（玩家的 weaponOnly 技能在分身上的对应判定）。 */
    private static boolean holding(CloneAvatarEntity caster, net.minecraft.world.item.Item item) {
        return caster.getMainHandItem().is(item) || caster.getOffhandItem().is(item);
    }

    /** 技能冷却判定（冷却单位与玩家技能一致，用 tick 数表示）。 */
    private static boolean ready(CloneAvatarEntity caster, String path, int cooldownTicks) {
        long now = caster.level().getGameTime();
        var body = caster.getAttachedOrCreate(xiaoshi2022.corpseorigin.growth.SurvivalGrowth.BODY);
        var cost = xiaoshi2022.corpseorigin.skill.SkillResourceRules.cost(path, 0);
        if (body.getLongOr("clone_skill_cd:" + path, 0) > now || !CloneRoleSkills.affordable(caster, cost)) {
            return false;
        }
        body = body.copy();
        body.putLong("clone_skill_cd:" + path, now + cooldownTicks);
        caster.setAttached(xiaoshi2022.corpseorigin.growth.SurvivalGrowth.BODY, body);
        CloneRoleSkills.pay(caster, cost);
        return true;
    }

    /** 写动作姿势附件（任意 LivingEntity 都能同步，客户端摆臂 Mixin 按 UNTIL 自动生效/过期）。 */
    private static void pose(LivingEntity actor, String action, int ticks) {
        actor.setAttached(ChapterScenes.ACTION, action);
        actor.setAttached(ChapterScenes.UNTIL, actor.level().getGameTime() + ticks);
    }

    private static Vec3 horizontalLook(CloneAvatarEntity caster) {
        Vec3 look = caster.getLookAngle();
        Vec3 face = new Vec3(look.x, 0, look.z);
        return face.lengthSqr() < 1.0E-6 ? Vec3.directionFromRotation(0, caster.getYRot()) : face.normalize();
    }

    /**
     * 分身的可攻击目标：通用规则之外，永远不能打自己的本体和主人驯服的仆从；
     * 尸族内部也不互伤（和分身近战索敌的口径一致）。
     */
    private static boolean isFoe(CloneAvatarEntity caster, LivingEntity target) {
        if (!ChapterCombat.canHit(caster, target)) {
            return false;
        }
        UUID owner = caster.getOwnerUuid();
        if (owner != null && owner.equals(target.getUUID())) {
            return false;
        }
        if (target instanceof TamableAnimal pet && owner != null && pet.getOwnerReference() != null
                && owner.equals(pet.getOwnerReference().getUUID())) {
            return false;
        }
        return !(target instanceof ZombieKin);
    }
}
