package xiaoshi2022.corpseorigin.growth;
import net.fabricmc.fabric.api.client.gametest.v1.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.effect.MobEffects;
import xiaoshi2022.corpseorigin.item.HairDryerItem;
import xiaoshi2022.corpseorigin.registry.ModItems;
public final class HairDryerGameplayTest implements FabricClientGameTest {
 private static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
 public void runTest(ClientGameTestContext context){try(var world=context.worldBuilder().create()){
  context.waitTicks(40);world.getServer().runOnServer(server->{
   var p=server.getPlayerList().getPlayers().getFirst();p.setNoGravity(true);p.teleportTo(0,100,0);var l=p.level();
   var s=new ItemStack(ModItems.HAIR_DRYER);p.setItemInHand(InteractionHand.MAIN_HAND,s);var dryer=(HairDryerItem)ModItems.HAIR_DRYER;
   check(!HairDryerItem.powered(l,p.blockPosition()),"no free power");
   check(!dryer.finishCharge(s,l,p,100),"no unpowered luck");
   l.setBlock(new BlockPos(2,100,0),Blocks.REDSTONE_BLOCK.defaultBlockState(),3);
   check(HairDryerItem.powered(l,p.blockPosition()),"redstone powers dryer");
   check(!dryer.finishCharge(s,l,p,0),"instant release grants nothing");
   check(dryer.finishCharge(s,l,p,100),"full charge succeeds");
   check(p.hasEffect(MobEffects.LUCK)&&p.getEffect(MobEffects.LUCK).getAmplifier()==2,"full charge luck III");
   check(s.getDamageValue()==3,"full charge costs three durability");
   var cow=EntityTypes.COW.create(l,EntitySpawnReason.COMMAND);cow.setPos(0,100,3);cow.setNoAi(true);l.addFreshEntity(cow);
   l.setBlock(new BlockPos(0,100,3),Blocks.WATER.defaultBlockState(),3);
   var dropped=new ItemEntity(l,0,100,3,new ItemStack(ModItems.HAIR_DRYER));l.addFreshEntity(dropped);dropped.tickCount=10;
   HairDryerItem.tickDropped(dropped);
   check(cow.getHealth()<cow.getMaxHealth(),"powered water shocks victim");
   check(dropped.isRemoved()||HairDryerItem.waterDamaged(dropped.getItem()),"water breaks or marks dryer");
   float health=cow.getHealth();HairDryerItem.tickDropped(dropped);check(cow.getHealth()==health,"no repeated short circuit");
   var corpse=new xiaoshi2022.corpseorigin.entity.LowerLevelZbEntity(xiaoshi2022.corpseorigin.registry.ModEntities.LOWER_LEVEL_ZB,l);
   corpse.setEvolutionLevel(1);corpse.setNoAi(true);corpse.setNoGravity(true);corpse.setPos(0,100,-2.5);l.addFreshEntity(corpse);
   check(corpse.getHealth()==25,"ordinary corpse starts at full 25 HP");
   var broken=new ItemStack(ModItems.HAIR_DRYER);
   net.minecraft.world.item.component.CustomData.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA,broken,t->t.putBoolean("WaterDamaged",true));
   p.setItemInHand(InteractionHand.MAIN_HAND,broken);
   dryer.use(l,p,InteractionHand.MAIN_HAND);
   check(!corpse.isAlive(),"powered wet click kills full-health ordinary corpse at 2.5 blocks");
   check(broken.isEmpty(),"short circuit consumes dryer exactly once");
   check(dryer.use(l,p,InteractionHand.MAIN_HAND)==net.minecraft.world.InteractionResult.FAIL,"empty stack cannot explode again");
  });context.waitTicks(15);System.out.println("HAIR_DRYER_SCREENSHOT="+context.takeScreenshot("hair-dryer-port"));
  System.out.println("HairDryerGameplayTest passed: power, charge, durability, water damage and one-shot discharge.");
 }}
}
