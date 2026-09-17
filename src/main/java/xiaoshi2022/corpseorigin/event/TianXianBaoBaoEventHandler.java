package xiaoshi2022.corpseorigin.event;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.character.CharacterManager;
import xiaoshi2022.corpseorigin.character.PlayerCharacterData;
import xiaoshi2022.corpseorigin.character.TianXianBaoBaoZb;
import xiaoshi2022.corpseorigin.network.CorpseNetwork;
import xiaoshi2022.corpseorigin.skill.tianxianbaobao_zb.AntennaBlockSkill;

/**
 * 天线宝宝尸兄专属事件处理。
 * <p>
 * 承载「天线格挡」：挨到<b>斧头</b>或<b>箭矢/投掷物</b>时 ——
 * <ul>
 *   <li><b>被动（常驻）</b>：{@value AntennaBlockSkill#PASSIVE_BLOCK_CHANCE_PERCENT}% 概率
 *       把这一击整个挡掉；</li>
 *   <li><b>主动（手动触发）</b>：{@link AntennaBlockSkill#isGuarding} 为真期间<b>必定</b>挡下。</li>
 * </ul>
 * 两种都是"整下挡掉"（不是减伤），挡下时会广播动画信号给客户端。
 * 和尸水之源、黑金心脏一个套路 —— 要该玩家是天线宝宝尸兄、且已学会这个技能才会触发；
 * 想要"是天线宝宝尸兄就常驻"的话，把 {@link #hasAntennaBlock} 里的 {@code hasLearned} 去掉即可。
 */
public final class TianXianBaoBaoEventHandler {

    private TianXianBaoBaoEventHandler() {
    }

    public static void register() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayer player)) {
                return true;
            }
            if (!hasAntennaBlock(player) || !isBlockedDamage(source)) {
                return true;
            }

            // 主动 → 窗口内必定挡下；被动 → 每次挨打现掷骰子。
            // 两者都是"整下挡掉"：返回 false 直接取消这份伤害，不做任何重打（所以也不涉及递归）。
            boolean blocked = AntennaBlockSkill.isGuarding(player)
                    || player.getRandom().nextDouble() < AntennaBlockSkill.PASSIVE_BLOCK_CHANCE;
            if (!blocked) {
                return true;
            }

            playBlockFeedback(player);
            return false;
        });

        CorpseOrigin.LOGGER.info("TianXianBaoBao events registered");
    }

    /**
     * 挡下之后的反馈：广播格挡动画 + 一声闷响。
     * <p>
     * 动画只给 {@link AntennaBlockSkill#PASSIVE_ANIM_TICKS} tick —— 够播完借用的那条
     * {@code special_attack}（1.25 秒）；连着挡住好几下时会从头重播，看起来就是"一直在挡"。
     */
    private static void playBlockFeedback(ServerPlayer player) {
        CorpseNetwork.broadcastAntennaBlock(player, AntennaBlockSkill.PASSIVE_ANIM_TICKS);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.7F, 1.2F);
    }

    /** 该玩家是否为已学会「天线格挡」的天线宝宝尸兄 */
    private static boolean hasAntennaBlock(ServerPlayer player) {
        if (!TianXianBaoBaoZb.ID.equals(CharacterManager.getInstance().getPlayerCharacterId(player))) {
            return false;
        }
        return PlayerCharacterData.get(player)
                .hasLearned(player.getUUID(), AntennaBlockSkill.PATH);
    }

    /**
     * 是不是"斧头"或"箭矢/投掷物"造成的伤害。
     * <ul>
     *   <li><b>斧头</b>：直接攻击者的主手是斧 —— 用 {@code minecraft:axes} 标签判定，
     *       所以附魔斧、模组斧同样算；</li>
     *   <li><b>箭矢/投掷物</b>：直接来源是 {@link AbstractArrow} —— 弓、弩、光灵箭，
     *       以及三叉戟（{@code ThrownTrident} 也是它的子类）都算；
     *       雪球、火球、药水这类不算（那是"所有投射物"的范畴）。</li>
     * </ul>
     */
    private static boolean isBlockedDamage(DamageSource source) {
        if (source.getDirectEntity() instanceof AbstractArrow) {
            return true;
        }

        Entity attacker = source.getEntity();
        return attacker instanceof LivingEntity living && living.getMainHandItem().is(ItemTags.AXES);
    }
}
