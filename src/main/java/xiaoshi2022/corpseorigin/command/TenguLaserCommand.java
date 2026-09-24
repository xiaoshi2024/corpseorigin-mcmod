package xiaoshi2022.corpseorigin.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import xiaoshi2022.corpseorigin.character.CharacterManager;
import xiaoshi2022.corpseorigin.character.FengMoHuiTaiLang;
import xiaoshi2022.corpseorigin.entity.GreatTenguEntity;

/** Player command used by Feng Mo Hui Tailang to order his airship to fire. */
public final class TenguLaserCommand {
    private TenguLaserCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("tengu_laser")
                .requires(source -> source.getEntity() instanceof ServerPlayer)
                .then(Commands.argument("target", EntityArgument.entity())
                        .executes(context -> fire(context.getSource(),
                                EntityArgument.getEntity(context, "target")))));
    }

    private static int fire(CommandSourceStack source, net.minecraft.world.entity.Entity entity) {
        ServerPlayer player;
        try { player = source.getPlayerOrException(); }
        catch (com.mojang.brigadier.exceptions.CommandSyntaxException exception) { return 0; }

        if (!FengMoHuiTaiLang.ID.equals(CharacterManager.getInstance().getPlayerCharacterId(player))) {
            source.sendFailure(Component.translatable("command.corpseorigin.tengu_laser.wrong_role"));
            return 0;
        }
        if (!(entity instanceof LivingEntity target) || target == player || !target.isAlive()) {
            source.sendFailure(Component.translatable("command.corpseorigin.tengu_laser.invalid_target"));
            return 0;
        }
        GreatTenguEntity ship = player.level().getEntitiesOfClass(GreatTenguEntity.class,
                        player.getBoundingBox().inflate(96), candidate -> candidate.isOwnedBy(player))
                .stream().min(java.util.Comparator.comparingDouble(player::distanceToSqr)).orElse(null);
        if (ship == null) {
            source.sendFailure(Component.translatable("command.corpseorigin.tengu_laser.no_ship"));
            return 0;
        }
        if (player.distanceToSqr(target) > 4096 || !player.hasLineOfSight(target)) {
            source.sendFailure(Component.translatable("command.corpseorigin.tengu_laser.out_of_range"));
            return 0;
        }
        if (!ship.callLaser(target)) {
            source.sendFailure(Component.translatable("command.corpseorigin.tengu_laser.cooldown"));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("command.corpseorigin.tengu_laser.fired",
                target.getDisplayName()), false);
        return 1;
    }
}
