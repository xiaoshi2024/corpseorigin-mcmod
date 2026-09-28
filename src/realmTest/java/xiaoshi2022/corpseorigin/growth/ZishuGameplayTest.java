package xiaoshi2022.corpseorigin.growth;
import net.fabricmc.fabric.api.client.gametest.v1.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import xiaoshi2022.corpseorigin.entity.*;
import xiaoshi2022.corpseorigin.registry.*;
import xiaoshi2022.corpseorigin.skill.chapter.GroundShockwave;
public final class ZishuGameplayTest implements FabricClientGameTest {
 private ZishuRobotEntity robot;private LowerLevelZbEntity target;
 private static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
 public void runTest(ClientGameTestContext c){try(var w=c.worldBuilder().create()){
  c.waitTicks(40);var server=w.getServer();
  server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.CREATIVE);p.setNoGravity(true);p.teleportTo(0,100,0);p.setYRot(0);p.setXRot(0);var l=p.level();
   for(int x=-8;x<=8;x++)for(int z=-8;z<=12;z++){
    l.setBlock(new BlockPos(x,99,z),Blocks.GOLD_BLOCK.defaultBlockState(),2);
    l.setBlock(new BlockPos(x,98,z),Blocks.DEEPSLATE.defaultBlockState(),2);
    l.setBlock(new BlockPos(x,97,z),Blocks.IRON_BLOCK.defaultBlockState(),2);
    l.setBlock(new BlockPos(x,101,z),Blocks.DIAMOND_BLOCK.defaultBlockState(),2);
   }
   GroundShockwave.spawn(p,p.position(),3,1);
   var recipeInput=net.minecraft.world.item.crafting.CraftingInput.of(1,3,java.util.List.of(
           new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_BLOCK),
           new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_BLOCK),
           new net.minecraft.world.item.ItemStack(ModItems.ZBR_FLESH)));
   var recipe=s.getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING,recipeInput,l).orElseThrow();
   check(recipe.value().assemble(recipeInput).is(ModItems.MING_JUQUE),"iron blocks and corpse flesh craft first-stage Juque");
  });c.waitTicks(16);
  server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();var displays=p.level().getEntitiesOfClass(Display.BlockDisplay.class,new AABB(-8,90,-8,8,110,12));
   check(!displays.isEmpty(),"ground wave displays created");boolean gold=false,deep=false,center=false;
   for(var d:displays){check(!d.getBlockState().is(Blocks.DIAMOND_BLOCK),"never sample ceiling above feet");gold|=d.getBlockState().is(Blocks.GOLD_BLOCK);deep|=d.getBlockState().is(Blocks.DEEPSLATE);center|=Math.abs(d.getX()-.5)<.01&&Math.abs(d.getZ()-.5)<.01;}
   check(gold&&deep&&center,"wave uses foot center surface and real subsurface materials");
   for(var d:displays)check(d.getBrightnessOverride()!=null,"flying debris uses explicit exposed/animated light, not buried entity light");
   for(int x=-8;x<=8;x++)for(int z=-8;z<=12;z++)p.level().setBlock(new BlockPos(x,101,z),Blocks.AIR.defaultBlockState(),2);
   var data=ThermalSurveyData.get(p);data.visit(p);var before=ThermalSurvey.scan(p);
   target=new LowerLevelZbEntity(ModEntities.LOWER_LEVEL_ZB,p.level());target.setNoAi(true);target.setPos(2,100,7);p.level().addFreshEntity(target);
   check(ThermalSurvey.scan(p).corpses()==before.corpses()+1,"scanner counts current corpse in visited chunk");
   target.setPos(100,100,100);check(ThermalSurvey.scan(p).corpses()==before.corpses(),"unexplored chunk excluded");target.setPos(2,100,7);
   var encoded=ThermalSurveyData.CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE,data).getOrThrow();
   var decoded=ThermalSurveyData.CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE,encoded).getOrThrow();check(decoded.chunks(p).equals(data.chunks(p)),"exploration persists through codec");
   robot=new ZishuRobotEntity(ModEntities.ZISHU_ROBOT,p.level());robot.tame(p);robot.setPos(0,100,4);robot.setYRot(180);robot.setYBodyRot(180);robot.setOrderedToSit(true);p.level().addFreshEntity(robot);
   check(!robot.canEngage(p),"owner never targeted");check(robot.canEngage(target),"corpse identified for ion attack");
   p.getInventory().setItem(15,new net.minecraft.world.item.ItemStack(ModItems.ZISHU_THERMAL_SCANNER));
   check(ThermalSurvey.carriesScanner(p),"scanner works in ordinary inventory slot");
   p.teleportTo(0,104,-3);p.setXRot(40);
   GroundShockwave.spawn(p,new Vec3(0,100,3),3,2);
  });c.waitTicks(18);
  c.runOnClient(mc->mc.gui.hud.getChat().clearMessages(true));
  System.out.println("GROUND_LIGHT_SCREENSHOT="+c.takeScreenshot("ground-wave-light"));
  server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();
   var displays=p.level().getEntitiesOfClass(Display.BlockDisplay.class,new AABB(-8,90,-8,8,115,12));
   check(displays.stream().anyMatch(d->d.getBrightnessOverride()!=null&&d.getBrightnessOverride().sky()==15),"daylight debris receives exposed sky light");
   p.teleportTo(0,100,0);p.setXRot(0);
  });c.waitTicks(2);
  c.runOnClient(mc->{
   var hud=xiaoshi2022.corpseorigin.client.hud.ThermalHudOverlay.currentState();
   check(hud!=null&&hud.active()&&hud.result().corpses()>0,"inventory scanner syncs real server counts to HUD");
   mc.gui.hud.getChat().clearMessages(true);
  });c.waitTicks(2);System.out.println("ZISHU_SCREENSHOT="+c.takeScreenshot("zishu-thermal-robot"));
  server.runOnServer(s->s.getPlayerList().getPlayers().getFirst().getInventory().setItem(15,net.minecraft.world.item.ItemStack.EMPTY));
  c.waitTicks(10);
  c.runOnClient(mc->check(xiaoshi2022.corpseorigin.client.hud.ThermalHudOverlay.currentState()==null,"removing scanner clears HUD state"));
  server.runOnServer(s->{robot.setOrderedToSit(false);robot.performRangedAttack(target,1);});c.waitTicks(35);
  server.runOnServer(s->check(target.getHealth()<target.getMaxHealth()||!target.isAlive(),"ion projectile damages corpse"));
  System.out.println("ZishuGameplayTest passed: true underfoot materials, ceiling exclusion, explored-only counts, persistence, owner safety and real ion projectile.");
 }}
}
