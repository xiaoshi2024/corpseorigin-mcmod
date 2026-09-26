package xiaoshi2022.corpseorigin.growth;

import com.mojang.brigadier.arguments.BoolArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import xiaoshi2022.corpseorigin.character.CharacterManager;
import xiaoshi2022.corpseorigin.config.CorpseConfig;

/** K's sunlight weakness and its persistent, server-wide administrator switch. */
public final class VampirePhysiology {
    private VampirePhysiology() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registry, environment) ->
                dispatcher.register(Commands.literal("vampire_sunlight")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(context -> {
                            boolean enabled = CorpseConfig.get().growth.vampireSunlightDamage;
                            context.getSource().sendSuccess(() -> status(enabled), false);
                            return enabled ? 1 : 0;
                        })
                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                                .executes(context -> {
                                    boolean enabled = BoolArgumentType.getBool(context, "enabled");
                                    CorpseConfig.get().growth.vampireSunlightDamage = enabled;
                                    CorpseConfig.save();
                                    context.getSource().sendSuccess(() -> status(enabled), true);
                                    return 1;
                                }))));
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (!CorpseConfig.get().growth.vampireSunlightDamage) return;
            for (var player : server.getPlayerList().getPlayers()) {
                if (player.tickCount % 40 != 0) continue;
                ServerLevel level = (ServerLevel) player.level();
                BlockPos eyes = BlockPos.containing(player.getEyePosition());
                if (VampireRules.sunlightHurts(
                        CharacterManager.getInstance().getPlayerCharacterId(player), true,
                        player.isAlive() && !player.isCreative() && !player.isSpectator(),
                        level.dimensionType().hasSkyLight(), level.isBrightOutside(),
                        level.canSeeSky(eyes), level.isRainingAt(eyes), player.isUnderWater())) {
                    player.hurtServer(level, player.damageSources().magic(), 1.0f);
                }
            }
        });
    }

    private static Component status(boolean enabled) {
        return Component.translatable("command.corpseorigin.vampire_sunlight."
                + (enabled ? "enabled" : "disabled"));
    }
}
