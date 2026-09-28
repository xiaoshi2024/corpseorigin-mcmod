package xiaoshi2022.corpseorigin.skill.longyou;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import xiaoshi2022.corpseorigin.entity.CloneAvatarEntity;
import xiaoshi2022.corpseorigin.network.CorpseNetwork;
import xiaoshi2022.corpseorigin.skill.chapter.QiEffects;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * 尸王雷电系技能的服务端实现。
 * <p>
 * 三个技能共用这一套判定与特效：
 * <ul>
 *   <li><b>雷电之力</b>（被动）→ {@link #meleeZap}：近战命中附带雷伤与麻痹；</li>
 *   <li><b>雷鳗</b>（主动）→ {@link #eelStrike}：一道巨大紫色天雷砸在目标头上，<b>击碎地形</b>；</li>
 *   <li><b>自然审判·真·球状闪电</b>（终极）→ {@link #ballLightning}：飞出球状闪电，
 *       一路放电吸引，落地/到期后炸成一片废墟。</li>
 * </ul>
 * 紫色雷电的表现走 {@link CorpseNetwork#broadcastThunderBolt}（客户端自己画），
 * 服务端这边只负责几何与数值。
 */
public final class ThunderStrikeHandler {

    private ThunderStrikeHandler() {
    }

    // ==================== 雷电之力（被动） ====================

    /** 近战附带的额外雷伤 */
    public static final float MELEE_ZAP_DAMAGE = 4.0F;
    /** 近战麻痹时长（tick）：1 秒 */
    private static final int MELEE_STUN_TICKS = 20;

    // ==================== 雷鳗（主动） ====================

    /** 找敌人的半径 */
    public static final double EEL_RANGE = 24.0;
    /** 落雷伤害 */
    public static final float EEL_DAMAGE = 26.0F;
    /** 落雷击碎地形的半径（横向） */
    public static final double EEL_SMASH_RADIUS = 4.0;
    /** 落雷击碎地形的深度 */
    public static final double EEL_SMASH_DEPTH = 8.0;
    /** 麻痹时长（tick）：3 秒 */
    private static final int EEL_STUN_TICKS = 60;

    // ==================== 自然审判·真·球状闪电（终极） ====================

    /** 球状闪电存活时长（tick）：4 秒 */
    private static final int ORB_LIFETIME = 80;
    /** 球状闪电飞行速度（格/tick） */
    private static final double ORB_SPEED = 0.45;
    /** 每隔多少 tick 电一次身边的敌人 */
    private static final int ORB_ZAP_INTERVAL = 10;
    private static final float ORB_ZAP_DAMAGE = 6.0F;
    /** 爆炸半径与伤害 */
    private static final double BLAST_RADIUS = 14.0;
    private static final double BLAST_DEPTH = 10.0;
    private static final float BLAST_DAMAGE = 60.0F;
    /** 爆炸时往外炸开的电弧数量 */
    private static final int BLAST_ARCS = 12;

    /** 破坏方块用的更新标志：发客户端 + 不掉落（和剑意地形那套保持一致） */
    private static final int SMASH_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_SUPPRESS_DROPS;

    /** 正在飞的球状闪电 */
    private static final List<Orb> ORBS = new ArrayList<>();

    // ==================== 雷鳗 ====================

    /**
     * 雷鳗：向视线上的敌人（没有就打视线落点）召唤一道巨大的紫色天雷。
     *
     * @return true = 打出去了
     */
    public static boolean eelStrike(ServerPlayer caster) {
        if (!(caster.level() instanceof ServerLevel level)) {
            return false;
        }
        Vec3 point = findStrikePoint(caster, level);
        if (point == null) {
            return false;
        }

        // 视觉：从天而降的紫色雷电 + 雷声
        Vec3 sky = point.add(0.0, 26.0, 0.0);
        CorpseNetwork.broadcastThunderBolt(level, point, sky, point, 12, 3.2F);
        level.playSound(null, point.x, point.y, point.z,
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 5.0F, 0.9F);
        level.playSound(null, point.x, point.y, point.z,
                SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.WEATHER, 2.0F, 1.0F);
        QiEffects.burst(level, point.x, point.y + 0.5, point.z, 0x88aaff, 70, 1.8);
        QiEffects.burst(level, point.x, point.y + 0.5, point.z, 0x7a5cff, 1, 0.0);

        // 伤害 + 击飞 + 麻痹
        hurtArea(level, caster, point, EEL_SMASH_RADIUS + 2.0, EEL_DAMAGE, 1.8, EEL_STUN_TICKS, false);
        // 击碎地形
        smash(level, point, EEL_SMASH_RADIUS, EEL_SMASH_DEPTH);
        return true;
    }

    /** 视线锥里最近的敌人；没有就打视线落点 */
    private static Vec3 findStrikePoint(ServerPlayer caster, ServerLevel level) {
        Vec3 eye = caster.getEyePosition();
        Vec3 look = caster.getLookAngle();

        LivingEntity enemy = findLookTarget(caster, level, eye, look);
        if (enemy != null) {
            return enemy.position().add(0.0, enemy.getBbHeight() * 0.5, 0.0);
        }

        Vec3 end = eye.add(look.scale(EEL_RANGE));
        HitResult hit = level.clip(new ClipContext(
                eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, caster));
        return hit.getType() == HitResult.Type.MISS ? end : hit.getLocation();
    }

    private static LivingEntity findLookTarget(ServerPlayer caster, ServerLevel level, Vec3 eye, Vec3 look) {
        LivingEntity best = null;
        double bestScore = Double.MAX_VALUE;

        for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class,
                caster.getBoundingBox().inflate(EEL_RANGE), e -> canBeHit(caster, e))) {
            Vec3 to = candidate.position()
                    .add(0.0, candidate.getBbHeight() * 0.5, 0.0)
                    .subtract(eye);
            double distance = to.length();
            if (distance > EEL_RANGE) {
                continue;
            }
            double facing = to.normalize().dot(look);
            if (facing < 0.5) {
                continue;   // 约 ±60° 的锥内才算"瞄准"
            }
            double score = distance * (2.0 - facing);
            if (score < bestScore) {
                bestScore = score;
                best = candidate;
            }
        }
        return best;
    }

    // ==================== 雷电之力（近战附雷） ====================

    /**
     * 近战命中时补一发雷电：额外雷伤 + 短暂麻痹 + 一小段电弧。
     * <p>
     * ⚠️ 这个方法是在 AFTER_DAMAGE 里调的，而它自己又会造成一次伤害 ——
     * 调用方必须做好防重入，否则会无限递归（见 {@code LongYouEventHandler}）。
     */
    public static void meleeZap(ServerPlayer caster, LivingEntity target) {
        if (!(caster.level() instanceof ServerLevel level)) {
            return;
        }
        target.hurtServer(level, level.damageSources().playerAttack(caster), MELEE_ZAP_DAMAGE);
        paralyze(target, MELEE_STUN_TICKS);

        Vec3 from = caster.getEyePosition().add(caster.getLookAngle().scale(0.4));
        Vec3 to = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0);
        CorpseNetwork.broadcastThunderBolt(level, to, from, to, 6, 0.7F);
        level.playSound(null, to.x, to.y, to.z,
                SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.6F, 1.6F);
    }

    // ==================== 自然审判·真·球状闪电 ====================

    /** 释放球状闪电：从眼前飞出去，一路放电，最后炸成废墟 */
    public static boolean ballLightning(ServerPlayer caster) {
        if (!(caster.level() instanceof ServerLevel level)) {
            return false;
        }
        Vec3 look = caster.getLookAngle();
        Vec3 start = caster.getEyePosition().add(look.scale(1.2));

        ORBS.add(new Orb(level, caster.getUUID(), start, look.scale(ORB_SPEED), ORB_LIFETIME));

        level.playSound(null, start.x, start.y, start.z,
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 6.0F, 0.6F);
        QiEffects.burst(level, start.x, start.y, start.z, 0x7a5cff, 1, 0.0);
        return true;
    }

    /** 服务端每 tick 推进所有球状闪电 */
    public static void tick(MinecraftServer server) {
        if (ORBS.isEmpty()) {
            return;
        }
        Iterator<Orb> iterator = ORBS.iterator();
        while (iterator.hasNext()) {
            Orb orb = iterator.next();
            if (tickOrb(orb)) {
                detonate(orb);
                iterator.remove();
            }
        }
    }

    /** @return true = 该炸了 */
    private static boolean tickOrb(Orb orb) {
        if (orb.age++ >= orb.lifetime) {
            return true;
        }

        // 飞行：略带上飘，像一颗悬浮的等离子球
        orb.velocity = orb.velocity.scale(0.985).add(0.0, 0.004, 0.0);
        orb.position = orb.position.add(orb.velocity);

        // 撞到实体方块就提前炸
        BlockPos pos = BlockPos.containing(orb.position);
        if (orb.level.isLoaded(pos) && orb.level.getBlockState(pos).isSolid()) {
            return true;
        }

        // 球体本身：电火花 + 紫色焰 + 白光合成一朵气团，并降到每 2 tick 一朵（球在飞，气团跟着留痕）
        if (orb.age % 2 == 0) {
            QiEffects.cloud(orb.level, orb.position, 0x88aaff, 1.1f, 6);
        }

        // 周期性放电：电弧 + 伤害 + 吸过来
        ServerPlayer caster = orb.level.getServer() == null ? null
                : orb.level.getServer().getPlayerList().getPlayer(orb.caster);
        if (caster == null) {
            return true;   // 施术者掉线了，别留一颗没人管的球
        }
        if (orb.age % ORB_ZAP_INTERVAL == 0) {
            zapAround(orb, caster);
        }
        return false;
    }

    /** 球状闪电放电：电弧 + 伤害 + 把周围敌人往球心拽 */
    private static void zapAround(Orb orb, ServerPlayer caster) {
        double radius = 6.0;
        for (LivingEntity target : orb.level.getEntitiesOfClass(LivingEntity.class,
                new AABB(orb.position, orb.position).inflate(radius), e -> canBeHit(caster, e))) {
            Vec3 to = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0);
            CorpseNetwork.broadcastThunderBolt(orb.level, to, orb.position, to, 5, 0.9F);
            target.hurtServer(orb.level, xiaoshi2022.corpseorigin.skill.QiSkillDamageSource.wrap(orb.level.damageSources().playerAttack(caster)), ORB_ZAP_DAMAGE);
            paralyze(target, 20);

            Vec3 pull = orb.position.subtract(to).normalize().scale(0.35);
            target.setDeltaMovement(target.getDeltaMovement().add(pull));
            target.hurtMarked = true;
        }
        orb.level.playSound(null, orb.position.x, orb.position.y, orb.position.z,
                SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.WEATHER, 1.2F, 1.4F);
    }

    /** 炸开：一圈电弧 + 大范围伤害 + 击碎地形 */
    private static void detonate(Orb orb) {
        ServerLevel level = orb.level;
        Vec3 center = orb.position;

        // 视觉：一圈向外炸开的紫色电弧
        for (int i = 0; i < BLAST_ARCS; i++) {
            double angle = (double) i / BLAST_ARCS * Math.PI * 2;
            Vec3 to = center.add(
                    Math.cos(angle) * BLAST_RADIUS * 0.85,
                    (Math.random() - 0.5) * 6.0,
                    Math.sin(angle) * BLAST_RADIUS * 0.85);
            CorpseNetwork.broadcastThunderBolt(level, center, center, to, 14, 2.4F);
        }
        level.playSound(null, center.x, center.y, center.z,
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 9.0F, 0.5F);
        QiEffects.burst(level, center.x, center.y, center.z, 0xd8552c, 6, 3.0);
        QiEffects.burst(level, center.x, center.y, center.z, 0x88aaff, 200, 6.0);

        ServerPlayer caster = level.getServer() == null ? null
                : level.getServer().getPlayerList().getPlayer(orb.caster);
        if (caster != null) {
            hurtArea(level, caster, center, BLAST_RADIUS, BLAST_DAMAGE, 3.0, 100, true);
        } else {
            // 施术者不在了也要把地形和怪炸掉（不然球白飞一趟）
            for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                    new AABB(center, center).inflate(BLAST_RADIUS), LivingEntity::isAlive)) {
                target.hurtServer(level, level.damageSources().lightningBolt(), BLAST_DAMAGE);
            }
        }
        smash(level, center, BLAST_RADIUS, BLAST_DEPTH);
    }

    // ==================== 公共小工具 ====================

    /**
     * 能不能被龙右的雷电打到。
     * <p>
     * 只排除施术者本人和他的克隆分身（打自己没意义）；尸族打得到 ——
     * 尸王动怒的时候，手下也得挨着。
     */
    private static boolean canBeHit(ServerPlayer caster, LivingEntity candidate) {
        if (candidate == caster || !candidate.isAlive() || candidate.isSpectator()) {
            return false;
        }
        return !(candidate instanceof CloneAvatarEntity avatar
                && caster.getUUID().equals(avatar.getOwnerUuid()));
    }

    /** 范围伤害 + 击飞 + 麻痹 */
    private static void hurtArea(ServerLevel level, ServerPlayer caster, Vec3 center,
                                 double radius, float damage, double knockback, int stunTicks, boolean qiSkill) {
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                new AABB(center, center).inflate(radius), e -> canBeHit(caster, e))) {
            target.hurtServer(level, qiSkill ? xiaoshi2022.corpseorigin.skill.QiSkillDamageSource.wrap(level.damageSources().playerAttack(caster))
                    : level.damageSources().playerAttack(caster), damage);
            paralyze(target, stunTicks);

            Vec3 push = target.position().subtract(center);
            if (push.lengthSqr() > 1.0E-4) {
                push = push.normalize().scale(knockback);
            }
            target.setDeltaMovement(target.getDeltaMovement().add(push.x, knockback * 0.35, push.z));
            target.hurtMarked = true;
        }
    }

    /** 麻痹：几乎定住 + 用不出力气 */
    private static void paralyze(LivingEntity target, int ticks) {
        target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, ticks, 6, false, false, true));
        target.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, ticks, 3, false, false, true));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, 2, false, false, true));
    }

    /**
     * 击碎地形（这就是"雷鳗击碎大楼 / 球状闪电毁灭京城"的实装）。
     * <p>
     * 挖一个略微下扎的椭球：不可破坏的方块（基岩/屏障，破坏速度 &lt; 0）、流体、
     * 未加载的区块都跳过，避免把水凿出洞或者拖垮服务器。
     *
     * @return 打掉的方块数
     */
    public static int smash(ServerLevel level, Vec3 center, double radius, double depth) {
        int horizontal = Mth.ceil(radius);
        int down = Mth.ceil(depth);
        BlockPos origin = BlockPos.containing(center);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int broken = 0;

        for (int dx = -horizontal; dx <= horizontal; dx++) {
            for (int dz = -horizontal; dz <= horizontal; dz++) {
                for (int dy = -down; dy <= 2; dy++) {
                    double norm = Math.sqrt((double) (dx * dx + dz * dz) + dy * dy * 2.0);
                    if (norm > radius) {
                        continue;
                    }
                    pos.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    if (level.isOutsideBuildHeight(pos) || !level.isLoaded(pos)) {
                        continue;
                    }
                    BlockState state = level.getBlockState(pos);
                    if (state.isAir() || !state.getFluidState().isEmpty()) {
                        continue;
                    }
                    if (state.getDestroySpeed(level, pos) < 0.0F) {
                        continue;   // 基岩 / 屏障这类不可破坏的
                    }
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), SMASH_FLAGS);
                    broken++;
                }
            }
        }

        if (broken > 0) {
            QiEffects.burst(level, center.x, center.y + 0.5, center.z, 0x9aa4b0, 40,
                    Math.max(radius * 0.35, 1.0));
        }
        return broken;
    }

    /** 正在飞的一颗球状闪电 */
    private static final class Orb {
        private final ServerLevel level;
        private final UUID caster;
        private final int lifetime;
        private Vec3 position;
        private Vec3 velocity;
        private int age;

        private Orb(ServerLevel level, UUID caster, Vec3 position, Vec3 velocity, int lifetime) {
            this.level = level;
            this.caster = caster;
            this.position = position;
            this.velocity = velocity;
            this.lifetime = lifetime;
        }
    }
}
