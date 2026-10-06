package xiaoshi2022.corpseorigin.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import xiaoshi2022.corpseorigin.entity.LowerLevelZbEntity;
import xiaoshi2022.corpseorigin.entity.evolution.ZbOrganGrowth;

import java.util.List;
import java.util.Set;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

/**
 * 导演指令：给召唤出来的尸兄实体直接装配器官，不走"临界突破随机突变"。
 * <ul>
 *   <li>{@code /zborgans <targets> add <organ> [joint]} —— 长出指定器官（海陆空靠它指派）</li>
 *   <li>{@code /zborgans <targets> clear} —— 清空器官（渲染层同步消失）</li>
 * </ul>
 * 常用配方：水肺 {@code aquatic_tail}；翅膀 {@code bat_black / feather_white}；
 * 自定义资源包的全身替换器官（joint=full_body，一只尸兄最多一个）。
 */
public final class ZbOrganCommand {

    /** 器官允许挂的关节（与 ZbOrganGrowth.JOINTS 保持一致）。 */
    private static final Set<String> JOINTS = Set.of(
            "body", "head", "left_arm", "right_arm", "left_leg", "right_leg", "full_body");

    private ZbOrganCommand() {}

    private static final net.minecraft.server.permissions.Permission OP_PERMISSION =
            new net.minecraft.server.permissions.Permission.HasCommandLevel(
                    net.minecraft.server.permissions.PermissionLevel.GAMEMASTERS);

    public static void register(com.mojang.brigadier.CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> root = literal("zborgans")
                .requires(source -> source.permissions().hasPermission(OP_PERMISSION))
                .then(argument("targets", EntityArgument.entities())
                        .then(literal("add")
                                .then(argument("organ", StringArgumentType.word())
                                        .executes(ctx -> add(ctx.getSource(),
                                                EntityArgument.getEntities(ctx, "targets"),
                                                StringArgumentType.getString(ctx, "organ"), null))
                                        .then(argument("joint", StringArgumentType.word())
                                                .executes(ctx -> add(ctx.getSource(),
                                                        EntityArgument.getEntities(ctx, "targets"),
                                                        StringArgumentType.getString(ctx, "organ"),
                                                        StringArgumentType.getString(ctx, "joint"))))))
                        .then(literal("clear")
                                .executes(ctx -> clear(ctx.getSource(),
                                        EntityArgument.getEntities(ctx, "targets")))));
        dispatcher.register(root);

        // ---- /zbfavorite：配置尸兄的"生前执念"物品（追掉落物/手持玩家，转移注意力）----
        LiteralArgumentBuilder<CommandSourceStack> favRoot = literal("zbfavorite")
                .requires(source -> source.permissions().hasPermission(OP_PERMISSION))
                .then(argument("targets", EntityArgument.entities())
                        .then(literal("clear")
                                .executes(ctx -> clearFavorite(ctx.getSource(),
                                        EntityArgument.getEntities(ctx, "targets"))))
                        .then(argument("items", StringArgumentType.greedyString())
                                .executes(ctx -> setFavorite(ctx.getSource(),
                                        EntityArgument.getEntities(ctx, "targets"),
                                        StringArgumentType.getString(ctx, "items")))));
        dispatcher.register(favRoot);

        // ---- /zbclone：复制一只尸兄的完整外貌（皮肤/变种/进化等级/器官/执念），批量召唤副本 ----
        var cloneRoot = literal("zbclone")
                .requires(source -> source.permissions().hasPermission(OP_PERMISSION));
        var cloneSrc = argument("source", EntityArgument.entity());
        cloneSrc.executes(ctx -> cloneZb(ctx.getSource(), EntityArgument.getEntity(ctx, "source"), 1));
        cloneSrc.then(argument("count", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 16))
                .executes(ctx -> cloneZb(ctx.getSource(), EntityArgument.getEntity(ctx, "source"),
                        com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "count"))));
        cloneRoot.then(cloneSrc);
        dispatcher.register(cloneRoot);
    }

    private static int cloneZb(CommandSourceStack source, net.minecraft.world.entity.Entity origin, int count) {
        if (!(origin instanceof LowerLevelZbEntity src)) {
            source.sendFailure(Component.translatable("message.corpseorigin.zbclone.not_zb"));
            return 0;
        }
        if (!(source.getLevel() instanceof net.minecraft.server.level.ServerLevel level)) return 0;
        String skinName = src.getPlayerSkinName();
        String customId = src.getCustomId();
        boolean corpseEye = src.hasCorpseEye();
        boolean cracked = src.isCracked();
        int evolution = src.getEvolutionLevel();
        String loadout = src.getOrganLoadout();
        String favorites = src.favoriteItems();

        var center = source.getPosition();
        int spawned = 0;
        for (int i = 0; i < count; i++) {
            var created = src.getType().create(level, net.minecraft.world.entity.EntitySpawnReason.EVENT);
            if (!(created instanceof LowerLevelZbEntity zb)) continue;
            // 26.2：setPos + setYRot（无 moveTo）；环形散布避免叠在一起
            double angle = (Math.PI * 2 * i) / count;
            zb.setPos(center.x + Math.cos(angle) * 2.0, center.y, center.z + Math.sin(angle) * 2.0);
            zb.setYRot(zb.getRandom().nextFloat() * 360.0F);
            // 外貌快照回填：皮肤走名字自动重载（Mojang/本地皮肤缓存都按名字命中）
            zb.setPlayerSkinName(skinName);
            zb.setCustomId(customId);
            zb.setCorpseEye(corpseEye);
            zb.setCracked(cracked);
            zb.setEvolutionLevel(evolution);
            zb.setOrganLoadout(loadout);
            zb.setFavoriteItems(favorites);
            level.addFreshEntity(zb);
            spawned++;
        }
        final int n = spawned;
        if (n == 0) {
            source.sendFailure(Component.translatable("message.corpseorigin.zbclone.fail"));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("message.corpseorigin.zbclone.done", n,
                skinName.isEmpty() ? src.getName().getString() : skinName), true);
        return n;
    }

    private static int setFavorite(CommandSourceStack source,
                                   java.util.Collection<? extends net.minecraft.world.entity.Entity> targets,
                                   String items) {
        // 逐个校验物品 id（要求带命名空间），无效整单拒绝
        for (String raw : items.split(",")) {
            String id = raw.trim();
            if (id.isEmpty()) continue;
            var key = net.minecraft.resources.Identifier.tryParse(id);
            if (key == null || !net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(key)) {
                source.sendFailure(Component.translatable("message.corpseorigin.zbfavorite.no_item", id));
                return 0;
            }
        }
        int count = 0;
        for (var e : targets) {
            if (e instanceof LowerLevelZbEntity zb) { zb.setFavoriteItems(items); count++; }
        }
        final int n = count;
        source.sendSuccess(() -> Component.translatable("message.corpseorigin.zbfavorite.set", n, items.trim()), true);
        return n;
    }

    private static int clearFavorite(CommandSourceStack source,
                                     java.util.Collection<? extends net.minecraft.world.entity.Entity> targets) {
        int count = 0;
        for (var e : targets) {
            if (e instanceof LowerLevelZbEntity zb) { zb.setFavoriteItems(null); count++; }
        }
        final int n = count;
        source.sendSuccess(() -> Component.translatable("message.corpseorigin.zbfavorite.cleared", n), true);
        return n;
    }

    private static int add(CommandSourceStack source, java.util.Collection<? extends net.minecraft.world.entity.Entity> targets,
                           String organId, String joint) {
        if (joint != null && !JOINTS.contains(joint)) {
            source.sendFailure(Component.translatable("message.corpseorigin.zborgans.invalid_joint", joint));
            return 0;
        }
        if (ZbOrganGrowth.allowedDefinitions().stream().noneMatch(d -> d.id().equals(organId))) {
            List<String> ids = ZbOrganGrowth.allowedDefinitions().stream().map(d -> d.id()).limit(12).toList();
            source.sendFailure(Component.translatable("message.corpseorigin.zborgans.no_organ",
                    organId, String.join(", ", ids)));
            return 0;
        }
        int ok = 0;
        for (var e : targets) {
            if (e instanceof LowerLevelZbEntity zb && ZbOrganGrowth.grantOrgan(zb, organId, joint)) ok++;
        }
        final int granted = ok;
        if (granted == 0) {
            source.sendFailure(Component.translatable("message.corpseorigin.zborgans.fail", organId));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("message.corpseorigin.zborgans.granted",
                granted, organId, joint == null ? "random" : joint), true);
        return granted;
    }

    private static int clear(CommandSourceStack source, java.util.Collection<? extends net.minecraft.world.entity.Entity> targets) {
        int cleared = 0, count = 0;
        for (var e : targets) {
            if (e instanceof LowerLevelZbEntity zb) {
                cleared += ZbOrganGrowth.clearOrgans(zb);
                count++;
            }
        }
        final int clearedF = cleared, countF = count;
        source.sendSuccess(() -> Component.translatable("message.corpseorigin.zborgans.cleared", countF, clearedF), true);
        return countF;
    }
}
