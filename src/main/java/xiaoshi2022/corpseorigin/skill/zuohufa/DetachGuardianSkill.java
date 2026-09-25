package xiaoshi2022.corpseorigin.skill.zuohufa;

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
import xiaoshi2022.corpseorigin.skill.chapter.QiEffects;

/**
 * 左护法·脱离 —— 把身上那条蛟龙蜕下来。
 * <p>
 * 原著里左护法是人兽合体的形态（人 + 青龙，作为尸王四大神宠之一"青龙"）。
 * 这一招就是"人兽分离"：<b>人恢复人形、蛟龙单独落地</b>，
 * 变成一只听命于本体的宠物 BOSS（{@code zuo_flood_long}），之后还可以用「合体」再收回身上。
 * <p>
 * 注意：脱的是蛟龙，<b>不是</b>尸兄身份 —— 用完仍是尸族（外观变种清 0，回到普通尸兄那套）。
 * 冷却 10 秒。
 */
public class DetachGuardianSkill implements ISkill {

    public static final String PATH = "detach_guardian";

    /** 附近已有自己的蛟龙时不再重复放 */
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
        if (!PlayerCorpseComponent.isMutantVariant(player)) {
            return Component.translatable("skill.corpseorigin." + PATH + ".need_form");
        }
        if (ZuoFloodLongEntity.findOwned(player, DUPLICATE_CHECK_RADIUS) != null) {
            return Component.translatable("skill.corpseorigin." + PATH + ".already_out");
        }
        return null;
    }

    @Override
    public void onActivate(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        // ① 先把蛟龙造出来再脱 —— 造不出来就保持龙身，免得"人形了、龙却没落地"
        ZuoFloodLongEntity dragon = ZuoFloodLongEntity.spawnFor(player, level);
        if (dragon == null) {
            player.sendOverlayMessage(Component.translatable("skill.corpseorigin." + PATH + ".failed"));
            return;
        }

        // ② 人恢复人形：只清掉"蛟龙外观变种"，尸兄身份照旧
        PlayerCorpseComponent.get(player).setVariant(0);
        // 数值跟着形态换档：分离后是人形那一档（比合体少 5 颗心）
        ZuoHuFa.applyIfZuoHuFa(player);
        CorpseNetwork.broadcastPlayerCorpseSync(player);

        // ③ 蜕皮表现：血雾 + 黏液（各塌缩成一朵气团）
        Vec3 center = player.position().add(0, player.getBbHeight() * 0.5, 0);
        QiEffects.burst(level, center.x, center.y, center.z, 0x8ce06a, 40, 0.8);
        QiEffects.burst(level, center.x, center.y, center.z, 0xc0182a, 20, 0.7);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.SLIME_BLOCK_BREAK, SoundSource.PLAYERS, 1.2F, 0.6F);

        player.sendOverlayMessage(Component.translatable("skill.corpseorigin." + PATH + ".done"));
    }
}
