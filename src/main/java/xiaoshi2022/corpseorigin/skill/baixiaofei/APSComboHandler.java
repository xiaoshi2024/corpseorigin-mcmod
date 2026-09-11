package xiaoshi2022.corpseorigin.skill.baixiaofei;

import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import xiaoshi2022.corpseorigin.registry.ModDataAttachments;

/**
 * 古仙剑·连招模式：左键攻击推进段数
 */
public class APSComboHandler {

    public static void register() {
        // 左键攻击实体
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClientSide()) return InteractionResult.PASS;
            if (player instanceof ServerPlayer sp) {
                tryAdvance(sp);
            }
            return InteractionResult.PASS;
        });

        // 左键攻击方块
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            if (world.isClientSide()) return InteractionResult.PASS;
            if (player instanceof ServerPlayer sp) {
                tryAdvance(sp);
            }
            return InteractionResult.PASS;
        });
    }

    private static void tryAdvance(ServerPlayer player) {
        ServerLevel level = player.level();
        long now = level.getGameTime();

        CompoundTag state = player.getAttachedOrCreate(ModDataAttachments.APS_STATE).copy();

        // 不在连招模式 → 不处理
        if (!state.getBoolean(AncientPoetrySwordSkill.ACTIVE_KEY).orElse(false)) {
            return;
        }

        // 超时 → 关闭连招模式
        long lastCast = state.getLong(AncientPoetrySwordSkill.CD_KEY).orElse(0L);
        if (now - lastCast > AncientPoetrySwordSkill.STAGE_RESET_TICKS) {
            state.putBoolean(AncientPoetrySwordSkill.ACTIVE_KEY, false);
            state.putInt(AncientPoetrySwordSkill.STAGE_KEY, 0);
            player.setAttached(ModDataAttachments.APS_STATE, state);
            player.sendSystemMessage(Component.translatable(
                    "skill.corpseorigin.ancient_poetry_sword.timeout"));
            return;
        }

        // 推进段数
        int stage = state.getInt(AncientPoetrySwordSkill.STAGE_KEY).orElse(0);
        stage = (stage % 4) + 1;
        state.putInt(AncientPoetrySwordSkill.STAGE_KEY, stage);
        state.putLong(AncientPoetrySwordSkill.CD_KEY, now);
        player.setAttached(ModDataAttachments.APS_STATE, state);

        // 触发对应段
        switch (stage) {
            case 1 -> APSSubSkills.castZhaoCiBaiDi(player, level);
            case 2 -> APSSubSkills.castQianLiJiangLing(player, level);
            case 3 -> APSSubSkills.castLiangAnYuanSheng(player, level);
            case 4 -> {
                APSSubSkills.castQingZhouYiGuo(player, level);
                // 第 4 段后关闭连招模式
                CompoundTag s = player.getAttachedOrCreate(ModDataAttachments.APS_STATE).copy();
                s.putBoolean(AncientPoetrySwordSkill.ACTIVE_KEY, false);
                s.putInt(AncientPoetrySwordSkill.STAGE_KEY, 0);
                player.setAttached(ModDataAttachments.APS_STATE, s);
            }
        }
    }
}