package xiaoshi2022.corpseorigin.event;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import xiaoshi2022.corpseorigin.entity.CorpseFishEggEntity;
import java.util.Comparator;
public final class FishEggInteraction {
    private FishEggInteraction() {}
    public static void register() {
        UseEntityCallback.EVENT.register((player,world,hand,target,hit) ->
                player.isShiftKeyDown() ? removeOne(player,target) : InteractionResult.PASS);
        UseItemCallback.EVENT.register((player,world,hand) ->
                player.isShiftKeyDown() ? removeOne(player,player) : InteractionResult.PASS);
    }
    private static InteractionResult removeOne(Player player,Entity host) {
        if(player.isSpectator() || player.distanceToSqr(host)>25) return InteractionResult.PASS;
        var eggs=host.level().getEntitiesOfClass(CorpseFishEggEntity.class,host.getBoundingBox().inflate(1),e -> e.attachedTo(host));
        var egg=eggs.stream().min(Comparator.comparingDouble(e -> e.distanceToSqr(player.getEyePosition()))).orElse(null);
        if(egg==null) return InteractionResult.PASS;
        if(!host.level().isClientSide()) egg.discard();
        return InteractionResult.SUCCESS;
    }
}
