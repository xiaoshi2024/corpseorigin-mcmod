package com.phagens.corpseorigin.skill.special;

import com.phagens.corpseorigin.CorpseOrigin;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.UUID;

/**
 * 尸王控制处理器 - 处理被控制玩家的行为
 *
 * 【功能说明】
 * 1. 每tick更新控制状态（清理过期控制）
 * 2. 强制被控制玩家看向控制者方向
 * 3. 处理定身效果（完全锁定移动）
 * 4. 提供接口检查玩家是否可以自主行动
 *
 * 【控制效果】
 * - 视角锁定：被控制玩家强制看向控制者看向的方向
 * - 移动锁定：定身状态下完全禁止移动
 * - 状态同步：通过hurtMarked标记强制同步到客户端
 *
 * 【事件处理】
 * - PlayerTickEvent.Pre: 每tick处理控制逻辑
 *
 * 【关联系统】
 * - CorpseKingPowerSkill: 提供控制状态查询和更新
 * - CorpseKingData: 控制数据持久化
 *
 * @author Phagens
 * @version 1.0
 */
@EventBusSubscriber(modid = CorpseOrigin.MODID)
public class CorpseKingControlHandler {

    /**
     * 玩家tick事件 - 处理被控制玩家的强制行为
     */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Pre event) {
        Player player = event.getEntity();

        if (player.level().isClientSide) return;

        // 更新控制状态
        if (player.level() instanceof ServerLevel level) {
            CorpseKingPowerSkill.updateControls(level);
        }

        // 检查玩家是否被控制
        if (CorpseKingPowerSkill.isPlayerControlled(player)) {
            handleControlledPlayer(player);
        }
    }

    /**
     * 处理被控制玩家的行为
     */
    private static void handleControlledPlayer(Player controlled) {
        // 获取控制者的UUID
        UUID controllerUUID = CorpseKingPowerSkill.getControllerUUID(controlled);
        if (controllerUUID == null) return;

        // 获取控制者实体
        if (!(controlled.level() instanceof ServerLevel level)) return;
        Player controller = level.getServer().getPlayerList().getPlayer(controllerUUID);
        if (controller == null) return;

        // 强制被控制玩家看向控制者看向的方向
        Vec3 controllerLook = controller.getLookAngle();
        double yaw = Math.toDegrees(Math.atan2(controllerLook.x, controllerLook.z));
        double pitch = Math.toDegrees(Math.asin(-controllerLook.y));

        controlled.setYRot((float) yaw);
        controlled.setXRot((float) pitch);
        controlled.setYHeadRot((float) yaw);

        // 如果被定身，强制停止移动（完全锁定）
        if (CorpseKingPowerSkill.isPlayerFrozen(controlled)) {
            controlled.setDeltaMovement(0, 0, 0);
            controlled.hurtMarked = true; // 标记需要同步到客户端
        }
    }

    /**
     * 检查玩家是否可以自主行动
     */
    public static boolean canPlayerActFreely(Player player) {
        return !CorpseKingPowerSkill.isPlayerControlled(player);
    }
}
