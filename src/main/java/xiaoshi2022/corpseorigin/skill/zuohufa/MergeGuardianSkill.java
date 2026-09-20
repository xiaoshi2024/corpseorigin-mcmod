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
import xiaoshi2022.corpseorigin.character.ZuoHuFa;
import xiaoshi2022.corpseorigin.component.PlayerCorpseComponent;
import xiaoshi2022.corpseorigin.entity.ZuoFloodLongEntity;
import xiaoshi2022.corpseorigin.network.CorpseNetwork;
import xiaoshi2022.corpseorigin.skill.ISkill;
import xiaoshi2022.corpseorigin.skill.SkillType;

/**
 * 左护法·合体 —— 「脱离」的反向操作：把身边的蛟龙收回来，重新变成蛟龙形态。
 * <p>
 * 条件和「脱离」对称：<b>必须有自己的蛟龙在身边</b>（默认 16 格内），收回后：
 * 蛟龙实体消失、玩家外观变种切回蛟龙（{@link PlayerCorpseComponent#VARIANT_ZUO_GUARDIAN}），
 * 多段碰撞箱会自动跟着回来。冷却 10 秒。
 * <p>
 * 万一蛟龙不在身边（在别的维度 / 离得太远），走到它附近再合体即可；
 * 蛟龙现在是永久宠物，只有被打死才会消失。
 */
public class MergeGuardianSkill implements ISkill {

    public static final String PATH = "merge_guardian";

    /** 能收回的距离（格） */
    private static final double MERGE_RADIUS = 16.0;

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
        return SkillType.UTILITY;
    }

    @Override
    public boolean isActivatable() {
        return true;
    }

    @Override
    public int getCooldownTicks() {
        return 200;   // 10s
    }

    @Override
    public Component checkUsable(ServerPlayer player) {
        if (PlayerCorpseComponent.isMutantVariant(player)) {
            return Component.translatable("skill.corpseorigin." + PATH + ".already_merged");
        }
        if (ZuoFloodLongEntity.findOwned(player, MERGE_RADIUS) == null) {
            return Component.translatable("skill.corpseorigin." + PATH + ".no_pet",
                    (int) MERGE_RADIUS);
        }
        return null;
    }

    @Override
    public void onActivate(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        ZuoFloodLongEntity dragon = ZuoFloodLongEntity.findOwned(player, MERGE_RADIUS);
        if (dragon == null) {
            return;   // checkUsable 已经拦过，这里只是兜底
        }

        // ① 蛟龙回到身上
        Vec3 center = dragon.position().add(0, dragon.getBbHeight() * 0.5, 0);
        dragon.discard();

        // ② 人变回蛟龙形态（外观变种切回去；多段碰撞箱由 MutantHitboxHandler 下一 tick 自动生成）
        float maxHealthBefore = player.getMaxHealth();
        PlayerCorpseComponent.get(player).setVariant(PlayerCorpseComponent.VARIANT_ZUO_GUARDIAN);
        // 数值跟着形态换档：合体这一档血更厚（比人形多 5 颗心）
        ZuoHuFa.applyIfZuoHuFa(player);
        // 合体白捡的那部分上限直接补满，否则满血的人形合体后会顶着一截空血条；
        // 只补"新增的差值"，所以反复脱离/合体不能当回血手段
        float gained = player.getMaxHealth() - maxHealthBefore;
        if (gained > 0.0F) {
            player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + gained));
        }
        CorpseNetwork.broadcastPlayerCorpseSync(player);

        // ③ 吸回去的表现：血雾往身上收
        Vec3 to = player.position().add(0, player.getBbHeight() * 0.5, 0);
        for (int i = 0; i < 24; i++) {
            double t = i / 24.0;
            double x = center.x + (to.x - center.x) * t;
            double y = center.y + (to.y - center.y) * t;
            double z = center.z + (to.z - center.z) * t;
            level.sendParticles(ParticleTypes.SOUL, x, y, z, 2, 0.15, 0.15, 0.15, 0.0);
        }
        level.sendParticles(ParticleTypes.ITEM_SLIME, to.x, to.y, to.z, 24, 0.6, 0.6, 0.6, 0.05);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.SLIME_BLOCK_PLACE, SoundSource.PLAYERS, 1.2F, 0.5F);

        player.sendOverlayMessage(Component.translatable("skill.corpseorigin." + PATH + ".done"));
    }
}
