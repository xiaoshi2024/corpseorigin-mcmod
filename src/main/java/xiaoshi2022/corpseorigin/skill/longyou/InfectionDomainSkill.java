package xiaoshi2022.corpseorigin.skill.longyou;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import xiaoshi2022.corpseorigin.character.PlayerCharacterData;
import xiaoshi2022.corpseorigin.skill.AbstractSkill;
import xiaoshi2022.corpseorigin.skill.EvolutionManager;
import xiaoshi2022.corpseorigin.skill.EvolutionTier;
import xiaoshi2022.corpseorigin.skill.SkillType;

import java.util.UUID;

/**
 * 龙右·感染领域 —— <b>主动开关</b>技能。
 * <p>
 * 按下展开一个可调半径的"感染领域"，领域内的原版怪物（僵尸/末影人/苦力怕等）
 * 会被周期性施加 {@link xiaoshi2022.corpseorigin.effect.BYeffect#applyInfection}，
 * 感染期满后转换为 {@link xiaoshi2022.corpseorigin.entity.LowerLevelZbEntity}，
 * 同时通过 {@link xiaoshi2022.corpseorigin.entity.HybridZombie#tagAsHybrid} 写入"半尸兄"标签
 * （original_species / hybrid_grade / infection_generation），形成"尸兄小黑/尸兄苦力怕"等混血概念。
 * <p>
 * <b>释放门槛</b>（用户 2026-10-08 确认）：
 * <ul>
 *   <li><b>创造模式</b>：管理员/导演可随时释放，用于服务器录制/演出</li>
 *   <li><b>超神级别</b>：境界绝对等级 ≥ 13（超神SSS），凡人/人/地/天/神级龙右都不能释放</li>
 *   <li>门槛通过 {@link #checkUsable} 与 {@link #getRequiredLevel()} 双重卡住</li>
 * </ul>
 * <p>
 * <b>两段式领域形态</b>（用户已确认）：
 * <ul>
 *   <li><b>移动跟随</b>（默认）：领域以施术者为中心跟随移动，适合"尸王巡视"</li>
 *   <li><b>固定锚点</b>（潜行+激活）：领域钉死在释放位置，施术者可离开，适合"笼罩整座城市后离开"</li>
 * </ul>
 * 同一施术者同时只允许一个领域，再次激活会顶替旧的（切换模式也走顶替）。
 * <p>
 * <b>代价</b>：开启期间持续消耗饱食度（{@link #HUNGER_INTERVAL} / {@link #HUNGER_EXHAUSTION}），
 * 饿到见底自动中断 —— 与 {@link WaterPollutionSkill} 同模式。
 * <p>
 * <b>不死髅体离体自动释放</b>：玩家本体离体（{@link UndeadBodyState#STATE} == 1，
 * 即夺舍/披甲状态）时，肉体会自动开启感染领域作为"感染源泉"，
 * 不受境界门槛限制 —— 见 {@link UndeadBodyState#tickAutoInfection}。
 * <p>
 * <b>Lost City 兼容</b>：若检测到 Lost Cities 模组加载且配置 {@code enableLostCityIntegration=true}，
 * 领域范围自动按 {@code cityCoverageMultiplier} 扩展，优先取 LostCityAPI 提供的城市边界。
 * <p>
 * 实际 tick 推进见 {@link InfectionDomainHandler}，由 {@code LongYouEventHandler} 注册到
 * {@code ServerTickEvents.END_SERVER_TICK}。
 */
public class InfectionDomainSkill extends AbstractSkill {

    public static final String PATH = "infection_domain";

    /** 开启中每多少 tick 扣一次饱食度（1 秒一次） */
    public static final int HUNGER_INTERVAL = 20;
    /** 每次扣掉的饥饿消耗量：约 8 秒掉 1 点饥饿（与尸水之源同档） */
    public static final float HUNGER_EXHAUSTION = 0.5F;

    /** 释放门槛：超神SSS（绝对等级 13）—— 凡人/人/地/天/神级龙右都不能释放 */
    public static final int REQUIRED_REALM = 13;

    public InfectionDomainSkill() {
        // 主动技能 + 冷却 0 + 不消耗内力：开关不该有冷却，代价是饱食度
        super(PATH, SkillType.UTILITY, 0);
    }

    /**
     * 学习门槛：超神SSS（13 级）。
     * <p>
     * 技能树用这个数值卡住"未到超神不能学"；
     * 释放时再走 {@link #checkUsable} 双重校验（创造模式豁免）。
     */
    @Override
    public int getRequiredLevel() {
        return REQUIRED_REALM;
    }

    /**
     * 释放前校验：创造模式豁免，否则要求境界 ≥ {@link #REQUIRED_REALM}。
     * <p>
     * 不满足返回失败提示，技能不生效、不进冷却、不扣内力。
     */
    @Override
    public Component checkUsable(ServerPlayer player) {
        // 创造模式豁免：管理员/导演录制用
        if (player.isCreative()) {
            return null;
        }
        int earned = PlayerCharacterData.get(player).getEarnedPoints(player.getUUID());
        int realm = EvolutionManager.getLevel(earned);
        if (realm < REQUIRED_REALM) {
            return Component.translatable("skill.corpseorigin." + PATH + ".need_realm",
                    EvolutionTier.formatShortName(REQUIRED_REALM),
                    EvolutionTier.formatShortName(realm));
        }
        return null;
    }

    /** 按一下切换开关状态；潜行激活切换为固定锚点模式 */
    @Override
    public void onActivate(ServerPlayer player) {
        boolean anchored = player.isShiftKeyDown();
        boolean nowOn = InfectionDomainHandler.toggle(player, anchored);

        String suffix;
        if (nowOn) {
            suffix = anchored ? "anchored" : "on";
        } else {
            suffix = "off";
        }
        player.sendOverlayMessage(Component.translatable(msgKey(suffix)));
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                nowOn ? SoundEvents.WARDEN_SONIC_BOOM : SoundEvents.BUCKET_EMPTY,
                SoundSource.PLAYERS, 0.8F, nowOn ? 0.7F : 1.4F);
    }

    /** 玩家断开连接时清掉领域 —— 免得重登之后领域还赖在原地 */
    public static void clearOnDisconnect(UUID uuid) {
        InfectionDomainHandler.release(uuid);
    }

    private static String msgKey(String suffix) {
        return "skill.corpseorigin." + PATH + "." + suffix;
    }
}
