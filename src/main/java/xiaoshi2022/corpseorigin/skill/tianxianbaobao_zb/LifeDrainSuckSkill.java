package xiaoshi2022.corpseorigin.skill.tianxianbaobao_zb;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import xiaoshi2022.corpseorigin.character.CharacterManager;
import xiaoshi2022.corpseorigin.character.ICharacter;
import xiaoshi2022.corpseorigin.character.MortalCharacter;
import xiaoshi2022.corpseorigin.network.CorpseNetwork;
import xiaoshi2022.corpseorigin.skill.AbstractSkill;
import xiaoshi2022.corpseorigin.skill.SkillType;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 天线宝宝尸兄·吸食——近战抓住玩家持续吸血，可用打断技解救。
 * <p>
 * 主动技能，无冷却（0 ticks）。
 * <p>
 * 实装效果：抓住身边<b>任意方向</b>最近的目标 → 压制其移动并持续吸血（自身回血）→
 * 目标死亡 / 被拉开距离 / 自身受击时中断。
 * <p>
 * 吸食期间盔甲播 {@code absorb} 动画，并靠动画里的 {@code query.target_*_rotation}
 * 把触手转向目标：状态与目标实体 id 由 {@link CorpseNetwork#broadcastAntennaSuck}
 * 同步给所有客户端，渲染时读它算朝向。
 */
public class LifeDrainSuckSkill extends AbstractSkill {

    public static final String PATH = "life_drain_suck";

    /**
     * 起手抓取距离（不分方向：前后左右上下都算）。
     * <p>
     * 判定给到多远，触手就能伸到多远：{@code absorb} 的伸长量是按<b>实际距离</b>反算的
     * （渲染器里的 {@code query.target_stretch}，基准是 {@code CHAIN_ABOVE_BONE6}），
     * 动画里那个 5.76 只是"基准长度"，不是硬上限。
     */
    private static final double GRAB_RANGE = 8.0;
    /** 单次吸食持续时长（tick）：6 秒 */
    private static final int DURATION = 120;
    /** 吸一口的间隔与数值：每 0.3 秒一口、每口 4 点 */
    private static final int DRAIN_INTERVAL = 6;
    private static final float DRAIN_DAMAGE = 4.0f;
    /** 吸血转回自身的比例 */
    private static final float HEAL_RATIO = 0.8f;
    /** 目标拉开到这个距离就算挣脱（要比抓取距离大一截，否则刚抓住就断） */
    private static final double BREAK_DISTANCE = 9.5;

    /**
     * 缓慢的触发概率：<b>每次吸血（每 {@link #DRAIN_INTERVAL} tick）</b>掷一次骰子，
     * 命中才给目标压上 {@link #SLOWNESS_DURATION} tick 的缓慢 III。
     * <p>
     * 也就是"概率版"的减速：不再全程按死，而是断断续续 —— 实际覆盖率由概率与效果时长共同决定
     * （0.4 命中 + 24 tick 效果 ≈ 六成左右的时间是慢的）。想让它更黏人就调高概率，
     * 想让它偶尔才生效就调低；概率给 1.0 就退回"每口必中"。
     */
    private static final double SLOWNESS_CHANCE = 0.4;
    /** 命中一次缓慢给多久：24 tick —— 一轮吸血 6 tick，所以能盖住接下来几口 */
    private static final int SLOWNESS_DURATION = 24;
    /** 缓慢等级：III（amplifier 2） */
    private static final int SLOWNESS_AMPLIFIER = 2;

    /** 吸血带来的强化：普通目标给这么多 tick（5 秒） */
    private static final int BUFF_DURATION = 100;
    /** 特殊血液目标（本模组角色玩家）给更久（15 秒）—— 同类血液更补 */
    private static final int SPECIAL_BLOOD_BUFF_DURATION = 300;
    /** 特殊血液目标额外提升的等级 */
    private static final int SPECIAL_BLOOD_AMPLIFIER_BONUS = 1;

    /** 施术者 uuid → 正在进行的吸食 */
    private static final Map<UUID, Suck> ACTIVE = new ConcurrentHashMap<>();

    public LifeDrainSuckSkill() {
        super(PATH, SkillType.COMBAT, 0);
    }

    @Override
    public void onActivate(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        if (ACTIVE.containsKey(player.getUUID())) {
            return;
        }

        LivingEntity target = findGrabbableTarget(player, level);
        if (target == null) {
            player.sendOverlayMessage(Component.translatable(msgKey("no_target")));
            return;
        }

        ACTIVE.put(player.getUUID(),
                new Suck(target, player.tickCount + DURATION, player.tickCount + DRAIN_INTERVAL));
        // 抓取瞬间也走同一套概率（想看"抓住必定减速"的话，这里改回直接 addEffect 就行）
        tryApplySlowness(target);

        player.sendOverlayMessage(Component.translatable(msgKey("grab"), target.getName()));
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 0.9F, 0.6F);

        // 告诉所有客户端"他在吸食谁"，盔甲据此播 absorb 并把触手转向目标
        CorpseNetwork.broadcastAntennaSuck(player, target.getId(), DURATION);
    }

    /** 服务端每 tick 推进所有进行中的吸食（由 {@code ServerEvents} 调用） */
    public static void tick(MinecraftServer server) {
        if (ACTIVE.isEmpty()) {
            return;
        }

        Iterator<Map.Entry<UUID, Suck>> iterator = ACTIVE.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Suck> entry = iterator.next();
            ServerPlayer caster = server.getPlayerList().getPlayer(entry.getKey());
            if (caster == null || !(caster.level() instanceof ServerLevel level)) {
                iterator.remove();
                continue;
            }
            if (!tickOne(caster, level, entry.getValue())) {
                iterator.remove();
            }
        }
    }

    /** @return false 表示这次吸食到此为止 */
    private static boolean tickOne(ServerPlayer caster, ServerLevel level, Suck suck) {
        LivingEntity target = suck.target;

        if (!caster.isAlive() || !target.isAlive() || target.isRemoved() || target.level() != level) {
            stop(caster, suck, "loose");
            return false;
        }
        if (caster.distanceTo(target) > BREAK_DISTANCE) {
            stop(caster, suck, "loose");
            return false;
        }
        // 自己挨打就松手——这就是"可用打断技解救"的判定入口
        if (caster.hurtTime > 0) {
            stop(caster, suck, "interrupted");
            return false;
        }

        // 血色粒子：从目标流回施术者
        Vec3 from = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0);
        Vec3 to = caster.position().add(0.0, caster.getBbHeight() * 0.5, 0.0);
        for (int i = 0; i < 4; i++) {
            double t = i / 4.0;
            level.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
                    from.x + (to.x - from.x) * t,
                    from.y + (to.y - from.y) * t,
                    from.z + (to.z - from.z) * t,
                    1, 0.0, 0.0, 0.0, 0.0);
        }

        // 每隔一段时间吸一口
        if (caster.tickCount >= suck.nextDrain) {
            target.hurtServer(level, level.damageSources().playerAttack(caster), DRAIN_DAMAGE);
            caster.heal(DRAIN_DAMAGE * HEAL_RATIO);
            // 概率版缓慢 III：每吸一口掷一次骰子（玩家目标就是靠这个被拖住的）
            tryApplySlowness(target);
            applyBloodBuff(caster, target, suck);
            suck.nextDrain = caster.tickCount + DRAIN_INTERVAL;
            level.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                    SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 0.6F, 0.8F);
        }

        if (caster.tickCount >= suck.until) {
            stop(caster, suck, null);
            return false;
        }
        return true;
    }

    /** 结束一次吸食：通知客户端撤掉 absorb 动画（reasonKey 非空时顺带提示一下） */
    private static void stop(ServerPlayer caster, Suck suck, String reasonKey) {
        if (reasonKey != null) {
            caster.sendOverlayMessage(Component.translatable(msgKey(reasonKey), suck.target.getName()));
        }
        CorpseNetwork.broadcastAntennaSuck(caster, -1, 0);
    }

    /**
     * 掷一次骰子决定这次要不要给目标压上缓慢 III（概率见 {@link #SLOWNESS_CHANCE}）。
     * <p>
     * 抓取瞬间与每一次吸血都调它，所以减速是"断断续续"的：命中就重新刷新成
     * {@link #SLOWNESS_DURATION} tick，没命中就让它自然走完。
     */
    private static void tryApplySlowness(LivingEntity target) {
        if (target.getRandom().nextDouble() >= SLOWNESS_CHANCE) {
            return;
        }
        target.addEffect(new MobEffectInstance(
                MobEffects.SLOWNESS, SLOWNESS_DURATION, SLOWNESS_AMPLIFIER, false, false, false));
    }

    /**
     * 吸食带来的强化：吸到"好血"就让施术者短暂变强。
     * <p>
     * 普通目标：力量 / 迅捷 / 再生 I，{@value #BUFF_DURATION} tick（5 秒）。
     * <p>
     * <b>特殊血液</b>目标（被吸的是本模组的角色玩家，比如黑小飞、牛奶这类）：
     * 等级再 +1、时长 {@value #SPECIAL_BLOOD_BUFF_DURATION} tick（15 秒）—— 同类血液更补。
     * <p>
     * 每吸一口刷新一次，所以吸食期间会一直挂着，松手后还能留一段时间。
     */
    private static void applyBloodBuff(ServerPlayer caster, LivingEntity target, Suck suck) {
        boolean special = isSpecialBlood(target);
        int duration = special ? SPECIAL_BLOOD_BUFF_DURATION : BUFF_DURATION;
        int bonus = special ? SPECIAL_BLOOD_AMPLIFIER_BONUS : 0;

        caster.addEffect(new MobEffectInstance(MobEffects.STRENGTH, duration, bonus, false, false, true));
        caster.addEffect(new MobEffectInstance(MobEffects.SPEED, duration, bonus, false, false, true));
        caster.addEffect(new MobEffectInstance(MobEffects.REGENERATION, duration, bonus, false, false, true));

        // 一次吸食只提示一次"吸到好血"，别每 10 tick 刷屏
        if (special && !suck.bloodHintShown) {
            suck.bloodHintShown = true;
            caster.sendOverlayMessage(Component.translatable(msgKey("special_blood"), target.getName()));
        }
    }

    /**
     * 是否"特殊血液"：被吸的目标是<b>本模组的角色玩家</b>（黑小飞、牛奶这类），
     * 而不是凡人、也不是普通生物。
     * <p>
     * 之所以用"有没有角色"而不是白名单：这类角色以后还会加，一个个列进集合容易漏。
     */
    private static boolean isSpecialBlood(LivingEntity target) {
        if (!(target instanceof ServerPlayer player)) {
            return false;
        }
        ICharacter character = CharacterManager.getInstance().getPlayerCharacter(player);
        return character != null && !MortalCharacter.ID.equals(character.getId());
    }

    /**
     * 找身边<b>最近</b>的目标 —— 不看方向。
     * <p>
     * 触手是靠动画里的 {@code query.target_*_rotation} 自己转过去对准目标的，
     * 所以这里不需要"必须正对"的前方锥形判定：前后左右、头上脚下都能抓。
     * <p>
     * 玩家（包括尸兄玩家）同样是合法目标：受影响的是缓慢压制 + 吸血，
     * 唯一的例外是旁观者 —— 抓了也没伤害、动画却挂在身上，所以跳过。
     */
    private static LivingEntity findGrabbableTarget(ServerPlayer player, ServerLevel level) {
        Vec3 center = player.position().add(0.0, player.getBbHeight() * 0.5, 0.0);

        LivingEntity best = null;
        double bestDistance = GRAB_RANGE;

        for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(GRAB_RANGE),
                e -> e != player && e.isAlive() && !(e instanceof Player p && p.isSpectator()))) {
            double distance = center.distanceTo(
                    candidate.position().add(0.0, candidate.getBbHeight() * 0.5, 0.0));
            if (distance < bestDistance) {
                bestDistance = distance;
                best = candidate;
            }
        }
        return best;
    }

    private static String msgKey(String suffix) {
        return "skill.corpseorigin." + PATH + "." + suffix;
    }

    /** 一次进行中的吸食 */
    private static final class Suck {
        private final LivingEntity target;
        private final int until;
        private int nextDrain;
        /** "吸到特殊血液"的提示是否已经发过（一次吸食只发一条） */
        private boolean bloodHintShown;

        private Suck(LivingEntity target, int until, int nextDrain) {
            this.target = target;
            this.until = until;
            this.nextDrain = nextDrain;
        }
    }
}
