package xiaoshi2022.corpseorigin.skill.heixiaofei;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.network.CorpseNetwork;
import xiaoshi2022.corpseorigin.skill.ISkill;
import xiaoshi2022.corpseorigin.skill.SkillType;

/**
 * 黑小飞·杀戮化形·身外化身 - 召唤血色分身。
 * <p>
 * TODO 待办（留给后续单独实现）：等专用的“血色分身”实体写好之后，
 *  在 {@link #onActivate} 中替换掉占位表现：
 *  1. 生成 1~2 具血色分身实体（继承玩家外观、自动攻击施术者的目标）；
 *  2. 持续 {@value #MINION_DURATION_TICKS} tick 后消散；
 *  3. 冷却改回正式值 {@link #FORMAL_COOLDOWN_TICKS}。
 *  目前仅播放提示音、提示文本与红眼特效，不生成实体。
 */
public class KillingIncarnationSkill implements ISkill {

    public static final String PATH = "killing_incarnation";

    /** 正式冷却（120 秒），分身实装后启用 */
    public static final int FORMAL_COOLDOWN_TICKS = 2400;
    /** 分身存在时长，供后续实体实现使用 */
    public static final int MINION_DURATION_TICKS = 400;
    /** 分身数量，供后续实体实现使用 */
    public static final int MINION_COUNT = 2;

    /** 占位冷却（5 秒）：分身未实装，避免长时间冷却惩罚 */
    private static final int PLACEHOLDER_COOLDOWN_TICKS = 100;

    @Override
    public Identifier getId() {
        return Identifier.fromNamespaceAndPath(CorpseOrigin.MOD_ID, PATH);
    }

    @Override
    public Component getName() {
        return Component.translatable("skill.corpseorigin." + PATH);
    }

    @Override
    public Component getDescription() {
        return Component.translatable("skill.corpseorigin." + PATH + ".desc");
    }

    @Override
    public SkillType getSkillType() {
        return SkillType.ULTIMATE;
    }

    @Override
    public int getCost() {
        return 3;
    }

    @Override
    public int getRequiredLevel() {
        return 1;
    }

    @Override
    public boolean isActivatable() {
        return true;
    }

    @Override
    public int getCooldownTicks() {
        // TODO 分身实装后改为 FORMAL_COOLDOWN_TICKS
        return PLACEHOLDER_COOLDOWN_TICKS;
    }

    @Override
    public void onActivate(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        // ==================== 占位表现（分身实体待补） ====================
        CorpseNetwork.broadcastTempRedEye(player, MINION_DURATION_TICKS);
        for (int i = 0; i < 40; i++) {
            double angle = Math.random() * Math.PI * 2;
            double radius = 0.8 + Math.random() * 1.8;
            level.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
                    player.getX() + Math.cos(angle) * radius,
                    player.getY() + 0.3 + Math.random() * 1.6,
                    player.getZ() + Math.sin(angle) * radius,
                    1, 0, 0.02, 0, 0.0);
        }
        player.sendOverlayMessage(Component.translatable(
                "skill.corpseorigin." + PATH + ".not_ready"));
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.6F, 1.6F);
    }
}
