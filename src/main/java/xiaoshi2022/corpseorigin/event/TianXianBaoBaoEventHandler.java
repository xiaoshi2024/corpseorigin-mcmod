package xiaoshi2022.corpseorigin.event;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.character.CharacterManager;
import xiaoshi2022.corpseorigin.character.PlayerCharacterData;
import xiaoshi2022.corpseorigin.character.TianXianBaoBaoZb;
import xiaoshi2022.corpseorigin.item.armor.AntennaZBRitem;
import xiaoshi2022.corpseorigin.network.CorpseNetwork;
import xiaoshi2022.corpseorigin.skill.tianxianbaobao_zb.AntennaBlockSkill;
import xiaoshi2022.corpseorigin.skill.tianxianbaobao_zb.EntityAntennaSuckHandler;

/**
 * 天线宝宝尸兄专属事件处理。
 * <p>
 * 承载两件事：
 * <ul>
 *   <li><b>「天线格挡」</b>：挨到<b>斧头</b>或<b>箭矢/投掷物</b>时 ——
 *       <ul>
 *         <li><b>被动（常驻）</b>：{@value AntennaBlockSkill#PASSIVE_BLOCK_CHANCE_PERCENT}% 概率
 *             把这一击整个挡掉；</li>
 *         <li><b>主动（手动触发）</b>：{@link AntennaBlockSkill#isGuarding} 为真期间<b>必定</b>挡下。</li>
 *       </ul>
 *       两种都是"整下挡掉"（不是减伤），挡下时会广播动画信号给客户端。
 *       和尸水之源、黑金心脏一个套路 —— 要该玩家是天线宝宝尸兄、且已学会这个技能才会触发；
 *       想要"是天线宝宝尸兄就常驻"的话，把 {@link #hasAntennaBlock} 里的 {@code hasLearned} 去掉即可。</li>
 *   <li><b>「吸食」（生物）</b>：非玩家生物只要穿戴了天线宝宝套装，攻击命中时就能发动吸食 ——
 *       抓取距最近的目标持续吸血，并让身上的盔甲播 {@code absorb} 动画。
 *       逻辑与玩家的技能入口完全共用（{@link EntityAntennaSuckHandler}）。</li>
 * </ul>
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

        // ★ 生物穿戴天线宝宝套装 → 攻击命中时发动吸食（玩家走技能入口，这里跳过）
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(source.getEntity() instanceof LivingEntity attacker)) {
                return true;
            }
            if (attacker instanceof ServerPlayer) {
                return true;
            }
            if (!wearsAntennaSet(attacker)) {
                return true;
            }
            // 已经在吸 / 附近没目标时内部直接返回 false，不会重复触发
            EntityAntennaSuckHandler.start(attacker);
            return true;
        });

        CorpseOrigin.LOGGER.info("TianXianBaoBao events registered");
    }

    /**
     * 这个生物身上有没有穿戴天线宝宝套装（头盔 / 胸甲 / 护腿任意一件）。
     * <p>
     * 判定用 {@code instanceof AntennaZBRitem}，所以不依赖具体是哪个槽位、也不受以后
     * 新增部件影响；玩家由技能入口负责，不走这条。
     */
    private static boolean wearsAntennaSet(LivingEntity entity) {
        for (EquipmentSlot slot : new EquipmentSlot[]{
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS}) {
            if (entity.getItemBySlot(slot).getItem() instanceof AntennaZBRitem) {
                return true;
            }
        }
        return false;
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
