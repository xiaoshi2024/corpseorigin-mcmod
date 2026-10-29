package xiaoshi2022.corpseorigin.skill.lanhai;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.network.CorpseNetwork;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 蓝海「灵魂出窍」的服务端状态机。
 * <p>
 * 服务端只负责"躯壳"这一边，灵魂本身是客户端摄像机（见
 * {@code PersistentCameraEntity#startSoulProjection}）：
 * <ol>
 *   <li>本体被锁在出窍点，每 tick 拽回原位并把速度清零，看上去像昏迷；</li>
 *   <li>本体像睡着一样躺在地上（{@link Pose#SLEEPING}），别人看到的是一具躺着的躯壳；</li>
 *   <li>{@link #MAX_DURATION} 后自动回窍（兜底，避免玩家忘了回来被永久定住）；</li>
 *   <li>死亡 / 下线自动清理；</li>
 *   <li>被龙右的次声波震到（见 {@code InfrasoundFieldHandler}）会被强行挣回体内。</li>
 * </ol>
 * 客户端的出窍 / 回窍过场与灵魂模型都由 S2C 包 {@code SoulProjectionS2C} 驱动。
 */
public final class SoulProjectionHandler {

    /**
     * 出窍状态同步标记。
     * <p>
     * 和 {@code HeartImplant.FEIGNING} 一样同步给所有客户端：别的玩家那边也要看到本体
     * 像睡着一样躺着，而 {@link Pose} 每 tick 都会被客户端自己重算，只靠状态包盖不住。
     */
    public static final AttachmentType<Boolean> PROJECTING = AttachmentRegistry.create(
            CorpseOrigin.id("soul_projection"),
            b -> b.initializer(() -> false).syncWith(ByteBufCodecs.BOOL, AttachmentSyncPredicate.all()));

    /** 这个玩家（无论服务端还是客户端）是否正在灵魂出窍 */
    public static boolean active(Player player) {
        return player.getAttachedOrCreate(PROJECTING);
    }

    /** 最长出窍时间（tick）：30 秒，到点自动回窍 */
    public static final int MAX_DURATION = 600;

    /** 胸口位置占身高的比例（起手粒子从这里冒出来） */
    private static final double CHEST_RATIO = 0.55;

    /** 本体允许偏离出窍点的距离平方（超过就拽回来） */
    private static final double MOVE_EPSILON_SQR = 1.0E-6;

    private record State(Vec3 origin, Identifier world, int until) {
    }

    /** 出窍中的玩家 uuid → 状态 */
    private static final Map<UUID, State> ACTIVE = new ConcurrentHashMap<>();

    private SoulProjectionHandler() {
    }

    public static boolean isActive(UUID uuid) {
        return uuid != null && ACTIVE.containsKey(uuid);
    }

    /**
     * 启动时调用一次，确保 {@link #PROJECTING} 这个同步附件在客户端与服务端都已注册
     *（调用静态方法即会触发类初始化；附件类型必须两边都有，否则跨端同步收不到）。
     */
    public static void register() {
    }

    /** 技能按一下切换：没出窍就出窍，正在出窍就回窍 */
    public static void toggle(ServerPlayer player) {
        if (isActive(player.getUUID())) {
            end(player, "off");
        } else {
            start(player);
        }
    }

    // ==================== 出窍 ====================

    public static void start(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level) || !player.isAlive() || player.isSpectator()) {
            return;
        }
        Vec3 origin = player.position();
        ACTIVE.put(player.getUUID(), new State(origin,
                level.dimension().identifier(), player.tickCount + MAX_DURATION));
        player.setAttached(PROJECTING, true);

        // 躯壳昏迷：不能动、不刷重力、像睡着一样躺在地上
        player.setDeltaMovement(Vec3.ZERO);
        player.setNoGravity(true);
        player.setSprinting(false);
        player.stopSleeping();
        refreshComaEffects(player);

        // 起手：胸口炸开一圈蓝色灵魂粒子
        Vec3 chest = chestOf(player);
        level.sendParticles(ParticleTypes.SOUL, chest.x, chest.y, chest.z,
                24, 0.28, 0.35, 0.28, 0.02);
        level.playSound(null, origin.x, origin.y, origin.z,
                SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 1.0F, 1.0F);

        player.sendOverlayMessage(Component.translatable(msgKey("on")));
        CorpseNetwork.sendSoulProjection(player, true);

        // ★ 必须在起手粒子（按站立身高算胸口）之后再躺下：躺姿的身高只有 0.2 格
        player.setPose(Pose.SLEEPING);
    }

    // ==================== 回窍 ====================

    public static void end(ServerPlayer player, String reason) {
        State state = ACTIVE.remove(player.getUUID());
        if (state == null) {
            return;
        }
        releaseComa(player);

        if (player.level() instanceof ServerLevel level) {
            Vec3 chest = chestOf(player);
            level.sendParticles(ParticleTypes.SOUL, chest.x, chest.y, chest.z,
                    18, 0.25, 0.3, 0.25, 0.01);
            level.playSound(null, chest.x, chest.y, chest.z,
                    SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 0.7F, 0.7F);
        }

        player.sendOverlayMessage(Component.translatable("skill.corpseorigin."
                + SoulProjectionSkill.PATH + "." + reason));
        CorpseNetwork.sendSoulProjection(player, false);
    }

    /** 被次声波震回体内（见 {@code InfrasoundFieldHandler}） */
    public static void forceReturn(ServerPlayer player) {
        if (isActive(player.getUUID())) {
            end(player, "expelled");
        }
    }

    // ==================== 每 tick 推进 ====================

    public static void tick(MinecraftServer server) {
        if (ACTIVE.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<UUID, State>> iterator = ACTIVE.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, State> entry = iterator.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null) {
                iterator.remove();   // 已下线：只清状态
                continue;
            }
            if (!player.isAlive()) {
                end(player, "dead");
                continue;
            }
            State state = entry.getValue();
            if (player.tickCount >= state.until()) {
                end(player, "timeout");
                continue;
            }

            // 本体锁死在出窍点：客户端哪怕有输入也推不动这具躯壳
            if (player.position().distanceToSqr(state.origin()) > MOVE_EPSILON_SQR) {
                player.setPos(state.origin().x, state.origin().y, state.origin().z);
            }
            player.setDeltaMovement(Vec3.ZERO);
            if (player.tickCount % 20 == 0) {
                refreshComaEffects(player);
            }
        }
    }

    /** 下线时清掉状态 + 摘掉昏迷效果，避免重登后还隐身 */
    public static void clearOnDisconnect(ServerPlayer player) {
        if (ACTIVE.remove(player.getUUID()) != null) {
            releaseComa(player);
        }
    }

    // ==================== 躯壳表现 ====================

    private static void refreshComaEffects(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 6, false, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 3, false, false, false));
    }

    private static void releaseComa(ServerPlayer player) {
        player.setAttached(PROJECTING, false);
        player.removeEffect(MobEffects.SLOWNESS);
        player.removeEffect(MobEffects.WEAKNESS);
        player.setNoGravity(false);
        player.setDeltaMovement(Vec3.ZERO);
        // 躺姿解除；updatePlayerPose 会按实际状态重新计算
        player.setPose(Pose.STANDING);
    }

    private static Vec3 chestOf(ServerPlayer player) {
        return new Vec3(player.getX(), player.getY() + player.getBbHeight() * CHEST_RATIO, player.getZ());
    }

    private static String msgKey(String suffix) {
        return "skill.corpseorigin." + SoulProjectionSkill.PATH + "." + suffix;
    }
}
