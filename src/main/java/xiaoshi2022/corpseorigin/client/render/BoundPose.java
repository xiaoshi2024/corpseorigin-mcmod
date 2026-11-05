package xiaoshi2022.corpseorigin.client.render;

import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import xiaoshi2022.corpseorigin.skill.chapter.ChapterScenes;

/**
 * 玩家被捆绑姿态（{@code BoundPose}）—— 穆博士把晓佳/小惠捆住时的渲染驱动。
 * <p>
 * 剧本桥段：穆博士把晓佳和小惠捆起来叫嚣 "让我们一起建立一个新的国度吧！"。
 * 此时这两个角色的玩家渲染需要切换到整身 xiaohui_scene 场景模型（自带 ropes + fear），
 * 双手被 Mixin 锁在胸前，动作像被绳子捆住。
 * <p>
 * 触发：
 * <pre>
 *   /zb_bind @a          ← 永久捆住（直到 /zb_unbind）
 *   /zb_bind @s 6000     ← 捆 5 分钟
 *   /zb_struggle @a      ← 挣扎态（双手抖动 + fear clip）
 *   /zb_unbind @a        ← 解捆
 * </pre>
 * <p>
 * 视觉层在 CreaturePlayerRenderer.extract() 里：
 * BoundPose.active(player) == true 时 → 切 xiaohui_scene 场景模型。
 * 姿态层在 PlayerModelLimbMixin.TAIL：双手锁在胸前。
 */
public final class BoundPose {
    public static final String ACTION_BOUND       = "bound";
    public static final String ACTION_STRUGGLE    = "bound_struggle";
    public static final String ACTION_BOUND_FULL  = "bound_full";

    private BoundPose() {}

    // ==================== 查询 ====================

    public static boolean active(Entity entity) {
        String action = entity.getAttachedOrCreate(ChapterScenes.ACTION);
        if (action.isEmpty()) return false;
        long until = entity.getAttachedOrCreate(ChapterScenes.UNTIL);
        if (until > 0 && until <= entity.level().getGameTime()) return false;
        return action.startsWith("bound");
    }

    public static String pose(Entity entity) {
        if (!active(entity)) return "";
        return entity.getAttachedOrCreate(ChapterScenes.ACTION);
    }

    // ==================== 服务端 API ====================

    public static void bind(Entity player, int ticks) {
        player.setAttached(ChapterScenes.ACTION, ACTION_BOUND);
        player.setAttached(ChapterScenes.UNTIL, ticks > 0
                ? player.level().getGameTime() + ticks : 0L);
    }

    public static void struggle(Entity player, int ticks) {
        player.setAttached(ChapterScenes.ACTION, ACTION_STRUGGLE);
        player.setAttached(ChapterScenes.UNTIL, ticks > 0
                ? player.level().getGameTime() + ticks : 0L);
    }

    public static void bindFull(Entity player, int ticks) {
        player.setAttached(ChapterScenes.ACTION, ACTION_BOUND_FULL);
        player.setAttached(ChapterScenes.UNTIL, ticks > 0
                ? player.level().getGameTime() + ticks : 0L);
    }

    public static void unbind(Entity player) {
        player.setAttached(ChapterScenes.ACTION, "");
        player.setAttached(ChapterScenes.UNTIL, 0L);
    }

    // ==================== 指令注册 ====================

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registry, env) -> {
            var bind = Commands.literal("zb_bind").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                    .then(Commands.argument("targets", EntityArgument.players())
                            .then(Commands.literal("struggle")
                                    .executes(ctx -> applyBind(ctx, "struggle")))
                            .then(Commands.literal("full")
                                    .executes(ctx -> applyBind(ctx, "full")))
                            .executes(ctx -> applyBind(ctx, "bound")));

            var unbind = Commands.literal("zb_unbind").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                    .then(Commands.argument("targets", EntityArgument.players())
                            .executes(ctx -> {
                                int count = 0;
                                for (ServerPlayer p : EntityArgument.getPlayers(ctx, "targets")) {
                                    unbind(p);
                                    count++;
                                }
                                final int applied = count;
                                ctx.getSource().sendSuccess(() ->
                                        Component.translatable("command.corpseorigin.zb_unbind.applied", applied), false);
                                return count;
                            }));

            dispatcher.register(bind);
            dispatcher.register(unbind);
        });

        // 定时自动解捆（UNTIL 到期）
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                long until = p.getAttachedOrCreate(ChapterScenes.UNTIL);
                String action = p.getAttachedOrCreate(ChapterScenes.ACTION);
                if (action.startsWith("bound") && until > 0 && until <= p.level().getGameTime()) {
                    unbind(p);
                }
            }
        });
    }

    private static int applyBind(CommandContext<CommandSourceStack> ctx, String mode) {
        int count = 0;
        try {
            for (ServerPlayer p : EntityArgument.getPlayers(ctx, "targets")) {
                switch (mode) {
                    case "struggle" -> struggle(p, 0);
                    case "full"    -> bindFull(p, 0);
                    default        -> bind(p, 0);
                }
                count++;
            }
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException ignored) {}
        final int applied = count;
        ctx.getSource().sendSuccess(() ->
                Component.translatable("command.corpseorigin.zb_bind.applied", applied, mode), false);
        return count;
    }
}
