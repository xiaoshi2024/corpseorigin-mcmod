package xiaoshi2022.corpseorigin.skill.heixiaofei;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import xiaoshi2022.corpseorigin.skill.*;
import xiaoshi2022.corpseorigin.registry.ModItems;
public class TigerClawBeeWheelSkill extends AbstractSkill {
 public static final String PATH="tiger_claw_bee_wheel";
 public TigerClawBeeWheelSkill(){super(PATH,SkillType.COMBAT,100);}
 @Override public Component checkUsable(ServerPlayer p){return p.getMainHandItem().is(ModItems.BEE_WHEEL)?null:Component.literal("需要主手持虎爪—飞蜂轮。");}
 @Override public void onActivate(ServerPlayer p){xiaoshi2022.corpseorigin.item.BeeWheelItem.throwWheel(p);}
}
