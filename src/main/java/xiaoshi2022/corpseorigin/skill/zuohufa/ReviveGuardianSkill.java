package xiaoshi2022.corpseorigin.skill.zuohufa;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.character.PlayerCharacterData;
import xiaoshi2022.corpseorigin.entity.ZuoFloodLongEntity;
import xiaoshi2022.corpseorigin.skill.ISkill;
import xiaoshi2022.corpseorigin.skill.SkillType;
import xiaoshi2022.corpseorigin.skill.longyou.BloodReserve;

/**
 * 左护法·唤龙 —— 以气血为引，重新凝聚一条被击杀的青龙宠物。
 * <p>
 * 原著里青龙是尸王四大神宠之一，血肉可散而神魂不灭。放出去的蛟龙被打死后，
 * 左护法可以耗自己的<b>100 点气血</b>把它重新凝聚出来（落在身前）。
 * <p>
 * 判定：
 * <ul>
 *   <li>只有左护法本人能用；</li>
 *   <li>必须真的有一条青龙"殒落"（{@link PlayerCharacterData#isGuardianLost}，
 *       宠物死亡事件里置位，随世界存档保存）；活着收回身上（合体）不算；</li>
 *   <li>身边不能已经有一条自己的蛟龙（防重复）；</li>
 *   <li>气血储备 ≥ {@value #REVIVE_COST}（打普通尸兄 / Shift 右键吃尸肉积攒）。</li>
 * </ul>
 * 复活后玩家<b>保持当前形态</b>（通常是人形），想要龙躯再用一次「合体」即可。冷却 30 秒。
 */
public class ReviveGuardianSkill implements ISkill {

    public static final String PATH = "revive_guardian";

    /** 复活一条青龙的气血消耗 */
    public static final int REVIVE_COST = 100;
    /** 防重复：这个半径内已有自己的蛟龙就不再凝聚 */
    private static final double DUPLICATE_CHECK_RADIUS = 64.0;

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
        return Component.translatable("skill.corpseorigin." + PATH + ".desc", REVIVE_COST);
    }

    @Override
    public SkillType getSkillType() {
        return SkillType.UTILITY;
    }

    @Override
    public boolean isActivatable() {
        return true;
    }

    @Override
    public int getCooldownTicks() {
        return 600;   // 30s
    }

    @Override
    public Component checkUsable(ServerPlayer player) {
        PlayerCharacterData data = PlayerCharacterData.get(player);
        if (!data.isGuardianLost(player.getUUID())) {
            return Component.translatable("skill.corpseorigin." + PATH + ".not_lost");
        }
        if (ZuoFloodLongEntity.findOwned(player, DUPLICATE_CHECK_RADIUS) != null) {
            return Component.translatable("skill.corpseorigin." + PATH + ".already_out");
        }
        if (BloodReserve.get(player) < REVIVE_COST) {
            return Component.translatable("skill.corpseorigin." + PATH + ".no_blood",
                    REVIVE_COST, BloodReserve.get(player));
        }
        return null;
    }

    @Override
    public void onActivate(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        PlayerCharacterData data = PlayerCharacterData.get(player);

        // ① 先扣气血 —— 扣不动直接中止（checkUsable 已拦，这里兜底）
        if (!BloodReserve.spend(player, REVIVE_COST)) {
            player.sendOverlayMessage(Component.translatable(
                    "skill.corpseorigin." + PATH + ".no_blood",
                    REVIVE_COST, BloodReserve.get(player)));
            return;
        }

        // ② 凝聚青龙（与「脱离」共用生成入口，保证状态一致）
        ZuoFloodLongEntity dragon = ZuoFloodLongEntity.spawnFor(player, level);
        if (dragon == null) {
            // 造不出来：把气血退回去，别让玩家白花
            BloodReserve.add(player, REVIVE_COST);
            player.sendOverlayMessage(Component.translatable("skill.corpseorigin." + PATH + ".failed"));
            return;
        }

        // ③ 清掉"青龙已殒"标记（随存档持久化）
        data.setGuardianLost(player.getUUID(), false);

        // ④ 重生特效：血气汇聚 + 龙吟
        Vec3 center = dragon.position().add(0, dragon.getBbHeight() * 0.5, 0);
        for (int i = 0; i < 36; i++) {
            double a = Math.random() * Math.PI * 2;
            double r = 3.0 * (1.0 - i / 36.0);
            double px = center.x + Math.cos(a) * r;
            double pz = center.z + Math.sin(a) * r;
            double py = center.y + (Math.random() - 0.5) * 2.0;
            level.sendParticles(ParticleTypes.SOUL, px, py, pz, 1, 0, 0, 0, 0.02);
        }
        level.sendParticles(ParticleTypes.END_ROD, center.x, center.y, center.z,
                24, 0.8, 0.8, 0.8, 0.02);
        level.sendParticles(ParticleTypes.ITEM_SLIME, center.x, center.y, center.z,
                30, 0.8, 0.8, 0.8, 0.08);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 0.8F, 0.6F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.SLIME_BLOCK_PLACE, SoundSource.PLAYERS, 1.2F, 0.5F);

        player.sendOverlayMessage(Component.translatable("skill.corpseorigin." + PATH + ".done", REVIVE_COST));
    }

    /** 技能是左护法专属：挂在 ZuoHuFa 的技能表里，只有该角色能在技能树里学到 */
}
