package com.xiaoshi2022.corpseorigin.command;

import com.mojang.brigadier.CommandDispatcher;
import com.xiaoshi2022.corpseorigin.growth.EvolutionTier;
import com.xiaoshi2022.corpseorigin.growth.NativeCorpseState;
import com.xiaoshi2022.corpseorigin.growth.ZbEvolution;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;

import java.util.Comparator;

/**
 * 尸兄境界查询指令。
 * <ul>
 *   <li>{@code /zbrealm} —— 查看 32 格内最近的尸兄</li>
 *   <li>{@code /zbrealm <目标>} —— 查看指定实体</li>
 * </ul>
 * 显示：境界名（人1 / 地2 / 天 / 神·前期…）、进化等级、血肉能量、突破失败次数。
 */
public final class ZbRealmCommand {

    private ZbRealmCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("zbrealm")
                .executes(context -> reportNearest(context.getSource()))
                .then(Commands.argument("target", EntityArgument.entity())
                        .executes(context -> report(context.getSource(),
                                EntityArgument.getEntity(context, "target")))));
    }

    private static int reportNearest(CommandSourceStack source) {
        Entity sourceEntity = source.getEntity();
        if (sourceEntity == null) {
            source.sendFailure(Component.translatable("command.corpseorigin.zbrealm.no_source"));
            return 0;
        }
        Mob nearest = source.getLevel()
                .getEntitiesOfClass(Mob.class, sourceEntity.getBoundingBox().inflate(32.0D),
                        NativeCorpseState::infected)
                .stream()
                .min(Comparator.comparingDouble(sourceEntity::distanceToSqr))
                .orElse(null);
        if (nearest == null) {
            source.sendFailure(Component.translatable("command.corpseorigin.zbrealm.none"));
            return 0;
        }
        return report(source, nearest);
    }

    private static int report(CommandSourceStack source, Entity target) {
        if (!(target instanceof Mob mob) || !NativeCorpseState.infected(mob)) {
            source.sendFailure(Component.translatable("command.corpseorigin.zbrealm.not_corpse"));
            return 0;
        }
        int level = NativeCorpseState.evolutionLevel(mob);
        int energy = NativeCorpseState.fleshEnergy(mob);
        int needed = level >= ZbEvolution.MAX_LEVEL ? 0 : ZbEvolution.thresholdForLevel(level);
        int failures = NativeCorpseState.breakthroughFailures(mob);
        Component realm = EvolutionTier.formatFullName(level).copy()
                .withStyle(style -> style.withColor(TextColor.fromRgb(EvolutionTier.colorOf(level))));
        source.sendSuccess(() -> Component.translatable("command.corpseorigin.zbrealm.info",
                mob.getName(), realm, level, energy, needed, failures), false);
        return 1;
    }
}
