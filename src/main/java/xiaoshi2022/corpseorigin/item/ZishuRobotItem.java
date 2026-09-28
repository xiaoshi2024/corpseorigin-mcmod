package xiaoshi2022.corpseorigin.item;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.server.level.ServerLevel;
import xiaoshi2022.corpseorigin.registry.ModEntities;
import xiaoshi2022.corpseorigin.entity.ZishuRobotEntity;
public final class ZishuRobotItem extends Item {
    public ZishuRobotItem(Properties p){super(p);}
    @Override public void appendHoverText(ItemStack s,TooltipContext c,net.minecraft.world.item.component.TooltipDisplay d,java.util.function.Consumer<net.minecraft.network.chat.Component> out,TooltipFlag f){out.accept(net.minecraft.network.chat.Component.translatable("thermal.corpseorigin.robot_hint"));}
    @Override public InteractionResult useOn(UseOnContext c){
        if(c.getPlayer()==null)return InteractionResult.FAIL;
        if(!(c.getLevel() instanceof ServerLevel l))return InteractionResult.SUCCESS;
        var pos=c.getClickedPos().relative(c.getClickedFace());
        if(!l.mayInteract(c.getPlayer(),pos))return InteractionResult.FAIL;
        var robot=new ZishuRobotEntity(ModEntities.ZISHU_ROBOT,l);robot.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5);robot.tame(c.getPlayer());
        if(!l.noCollision(robot)||!l.addFreshEntity(robot))return InteractionResult.FAIL;
        c.getItemInHand().consume(1,c.getPlayer());return InteractionResult.SUCCESS;
    }
}
