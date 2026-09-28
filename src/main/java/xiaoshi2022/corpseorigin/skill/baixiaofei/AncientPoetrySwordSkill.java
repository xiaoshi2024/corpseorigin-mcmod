package xiaoshi2022.corpseorigin.skill.baixiaofei;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.registry.ModDataAttachments;
import xiaoshi2022.corpseorigin.skill.ISkill;
import xiaoshi2022.corpseorigin.skill.SkillType;
import xiaoshi2022.corpseorigin.skill.baixiaofei.aps.APSTerrainManager;

public class AncientPoetrySwordSkill implements ISkill {

    public static final String STAGE_KEY = "aps_stage";
    public static final String CD_KEY = "aps_cd";
    public static final String ACTIVE_KEY = "aps_active";
    public static final int STAGE_RESET_TICKS = 200;  // 10 秒

    @Override
    public Identifier getId() {
        return Identifier.fromNamespaceAndPath(CorpseOrigin.MOD_ID, "ancient_poetry_sword");
    }

    @Override
    public Component getName() {
        return Component.translatable("skill.corpseorigin.ancient_poetry_sword");
    }

    @Override
    public Component getDescription() {
        return Component.translatable("skill.corpseorigin.ancient_poetry_sword.desc");
    }

    @Override
    public SkillType getSkillType() {
        return SkillType.COMBAT;
    }

    @Override
    public boolean isActivatable() {
        return true;
    }

    @Override
    public int getCooldownTicks() {
        return 1200;
    }

    /**
     * 诗仙剑是原著<b>大结局</b>才登场的招式，所以门槛抬到「神级 + 16 点」：
     * <ul>
     *   <li>{@link #getRequiredLevel()} = <b>10</b> —— 绝对进化等级 10，也就是
     *       {@code EvolutionTier.SHEN}（神·前期）；</li>
     *   <li>{@link #getCost()} = <b>16</b> —— 学会要花 16 点进化点。</li>
     * </ul>
     * 这两条同时管住技能树与自由角色（凡人 / 尸兄）的「发现」：
     * {@code BalanceRules.discoveryLevel} 取 requiredLevel 与 9 的较大值，
     * 所以神级之前连随机机遇都刷不出这条技能。
     * <p>
     * 注意本类直接实现 {@link ISkill}（不是 AbstractSkill），不写这两个方法就走接口默认的 1 点 / 1 级。
     */
    @Override
    public int getRequiredLevel() {
        return 10;
    }

    @Override
    public int getCost() {
        return 16;
    }

    public static boolean isRunning(ServerPlayer player) {
        return APSTerrainManager.hasActiveAPS(player)
                || player.getAttachedOrCreate(ModDataAttachments.APS_STATE).getBoolean(ACTIVE_KEY).orElse(false);
    }

    public static void stop(ServerPlayer player) {
        var state = player.getAttachedOrCreate(ModDataAttachments.APS_STATE).copy();
        state.putBoolean(ACTIVE_KEY, false);
        state.putInt(STAGE_KEY, 0);
        player.setAttached(ModDataAttachments.APS_STATE, state);
        if (APSTerrainManager.hasActiveAPS(player)) APSTerrainManager.forceRestore(player, player.level());
    }

    @Override
    public void onActivate(ServerPlayer player) {
        ServerLevel level = player.level();

        if (APSTerrainManager.hasActiveAPS(player)) {
            var stopped = player.getAttachedOrCreate(ModDataAttachments.APS_STATE).copy();
            stopped.putBoolean(ACTIVE_KEY, false);
            stopped.putInt(STAGE_KEY, 0);
            player.setAttached(ModDataAttachments.APS_STATE, stopped);
            boolean started = APSTerrainManager.forceRestore(player, level);
            if (started) {
                player.sendOverlayMessage(Component.translatable(
                        "skill.corpseorigin.ancient_poetry_sword.restoring"));
            }
            return;
        }

        CompoundTag state = player.getAttachedOrCreate(ModDataAttachments.APS_STATE).copy();
        boolean active = state.getBoolean(ACTIVE_KEY).orElse(false);

        if (active) {
            state.putBoolean(ACTIVE_KEY, false);
            state.putInt(STAGE_KEY, 0);
            player.setAttached(ModDataAttachments.APS_STATE, state);
            player.sendOverlayMessage(Component.translatable(
                    "skill.corpseorigin.ancient_poetry_sword.off"));
        } else {
            state.putBoolean(ACTIVE_KEY, true);
            state.putInt(STAGE_KEY, 0);
            state.putLong(CD_KEY, level.getGameTime());
            player.setAttached(ModDataAttachments.APS_STATE, state);
            player.sendOverlayMessage(Component.translatable(
                    "skill.corpseorigin.ancient_poetry_sword.on"));
        }
    }
}
