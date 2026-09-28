package xiaoshi2022.corpseorigin.entity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
public class DamoEntity extends TianDoctorEntity {
 public DamoEntity(EntityType<? extends PathfinderMob> type, Level level){super(type,level);}
 @Override public InteractionResult mobInteract(Player player, InteractionHand hand){ ItemStack s=player.getItemInHand(hand); if(!level().isClientSide() && (s.isEdible() || s.is(Items.COOKED_CHICKEN))){ if(!player.getAbilities().instabuild)s.shrink(1); player.sendSystemMessage(Component.translatable("message.corpseorigin.damo.qi")); discard(); return InteractionResult.SUCCESS;} return InteractionResult.SUCCESS; }
}
