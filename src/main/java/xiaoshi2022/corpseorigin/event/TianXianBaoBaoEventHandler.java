package xiaoshi2022.corpseorigin.event;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.character.CharacterManager;
import xiaoshi2022.corpseorigin.character.PlayerCharacterData;
import xiaoshi2022.corpseorigin.character.TianXianBaoBaoZb;
import xiaoshi2022.corpseorigin.skill.tianxianbaobao_zb.AntennaBlockSkill;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 天线宝宝尸兄专属事件处理。
 * <p>
 * 目前承载「天线格挡」被动：受到的<b>斧头</b>与<b>箭矢/投掷物</b>伤害降低 30%。
 * 和尸水之源、黑金心脏一个套路 —— 要该玩家是天线宝宝尸兄、且已学会这个技能才会触发；
 * 想要"是天线宝宝尸兄就常驻"的话，把 {@link #hasAntennaBlock} 里的 {@code hasLearned} 去掉即可。
 */
public final class TianXianBaoBaoEventHandler {

    /**
     * 正在被本处理器"按减免后的数值重打一次"的玩家。
     * <p>
     * 为什么需要这个标记：{@code ALLOW_DAMAGE} 只能返回布尔（放行 / 取消），<b>改不了数值</b>，
     * 所以减免的做法是"取消原来那份 + 自己按 70% 重打一次"。而重打的那一次又会进这个事件 ——
     * 没有标记就会 70%、49%、34%… 一路递归下去。
     */
    private static final Set<UUID> REAPPLYING = ConcurrentHashMap.newKeySet();

    private TianXianBaoBaoEventHandler() {
    }

    public static void register() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayer player) || REAPPLYING.contains(player.getUUID())) {
                return true;
            }
            if (!hasAntennaBlock(player) || !isBlockedDamage(source)) {
                return true;
            }
            if (!(player.level() instanceof ServerLevel level)) {
                return true;
            }

            float reduced = amount * (1.0F - AntennaBlockSkill.DAMAGE_REDUCTION);
            if (reduced >= amount) {
                return true;
            }

            REAPPLYING.add(player.getUUID());
            try {
                player.hurtServer(level, source, reduced);
            } finally {
                REAPPLYING.remove(player.getUUID());
            }

            // 原来那份整额伤害取消掉——上面已经按减免后的数值打过了
            return false;
        });

        CorpseOrigin.LOGGER.info("TianXianBaoBao events registered");
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
