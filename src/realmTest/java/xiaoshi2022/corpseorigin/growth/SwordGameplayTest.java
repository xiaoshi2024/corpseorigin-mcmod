package xiaoshi2022.corpseorigin.growth;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.ai.attributes.Attributes;
import xiaoshi2022.corpseorigin.character.*;
import xiaoshi2022.corpseorigin.entity.JuQueBeamEntity;
import xiaoshi2022.corpseorigin.skill.EvolutionManager;
import xiaoshi2022.corpseorigin.skill.chapter.*;

/** Actual projectile, client rendering and a protected test mountain in an isolated world. */
public final class SwordGameplayTest implements FabricClientGameTest {
    private static void check(boolean c,String s){if(!c)throw new AssertionError(s);}
    @Override public void runTest(ClientGameTestContext context){
        var cfg=RealmProgression.config();
        int budget=cfg.swordRiftBlocksPerTick,scans=cfg.swordRiftScansPerTick,length=cfg.swordRiftMaxLength;
        boolean enabled=cfg.swordTerrainDestruction;
        try(var world=context.worldBuilder().create()){
            var server=world.getServer();context.waitTicks(60);
            server.runOnServer(s->{
                var p=s.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.CREATIVE);p.setNoGravity(true);
                p.teleportTo(0,104,0);p.setYRot(0);p.setXRot(0);
                CharacterManager.getInstance().setPlayerCharacter(p,"mortal");
                PlayerCharacterData.get(p).setPoints(p.getUUID(),EvolutionManager.getThreshold(12),5000);
                cfg.swordRiftBlocksPerTick=64;cfg.swordRiftScansPerTick=4096;cfg.swordRiftMaxLength=24;cfg.swordTerrainDestruction=true;
                for(int x=-8;x<=8;x++)for(int y=80;y<=128;y++)for(int z=20;z<44;z++)p.level().setBlock(new BlockPos(x,y,z),Blocks.STONE.defaultBlockState(),2|16);
                p.level().setBlock(new BlockPos(0,100,21),Blocks.BEDROCK.defaultBlockState(),2|16);
                p.level().setBlock(new BlockPos(0,100,23),Blocks.CHEST.defaultBlockState(),2|16);
                var protectedWorld=p.level();
                PlayerBlockBreakEvents.BEFORE.register((l,player,pos,state,entity)->l!=protectedWorld || !pos.equals(new BlockPos(0,100,22)));
                check(!SwordRift.start(p,new Vec3(0,104,20),new Vec3(0,0,1),9),"below god tier cannot excavate");
                cfg.swordTerrainDestruction=false;
                check(!SwordRift.start(p,new Vec3(0,104,20),new Vec3(0,0,1),12),"destruction off respected");
                cfg.swordTerrainDestruction=true;
                check(SwordRift.start(p,new Vec3(0,104,20),new Vec3(0,0,1),12),"mountain cutting queued");
                check(p.level().getBlockState(new BlockPos(0,104,20)).is(Blocks.STONE),"start does not instantly erase terrain");
                check(!SwordRift.start(p,new Vec3(0,104,20),new Vec3(0,0,1),12),"same player cannot queue unlimited rifts");
            });
            context.waitTicks(1);
            server.runOnServer(s->check(SwordRift.removedBlocks()<=128,"per-tick removal budget respected"));
            context.waitTicks(260);
            server.runOnServer(s->{
                var p=s.getPlayerList().getPlayers().getFirst();
                check(SwordRift.removedBlocks()>1000,"real mountain blocks removed progressively");
                check(p.level().getBlockState(new BlockPos(0,104,20)).isAir(),"front cut opens");
                check(p.level().getBlockState(new BlockPos(0,104,42)).isAir(),"cut crosses the test mountain");
                check(p.level().getBlockState(new BlockPos(0,100,21)).is(Blocks.BEDROCK),"bedrock retained");
                check(p.level().getBlockState(new BlockPos(0,100,23)).is(Blocks.CHEST),"container retained");
                check(p.level().getBlockState(new BlockPos(0,100,22)).is(Blocks.STONE),"protection veto retained");
                check(p.level().getBlockState(new BlockPos(8,104,30)).is(Blocks.STONE),"outside canyon walls retained");
                p.teleportTo(0,109,7);p.setXRot(15);p.setYRot(0);
            });
            context.waitTicks(10);
            System.out.println("SWORD_CANYON_SCREENSHOT="+context.takeScreenshot("sword-real-canyon"));
            server.runOnServer(s->{
                var p=s.getPlayerList().getPlayers().getFirst();p.teleportTo(0,108,3);p.setXRot(0);p.setYRot(0);
                var cow=EntityTypes.COW.create(p.level(),EntitySpawnReason.COMMAND);check(cow!=null,"impact target exists");
                cow.setNoAi(true);cow.setNoGravity(true);cow.setPos(0,108,14);
                cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1e8);cow.setHealth(1e8f);p.level().addFreshEntity(cow);
                var beam=new JuQueBeamEntity(p.level(),p);beam.setPos(0,109,5);beam.setDamage(100);beam.setDeltaMovement(0,0,2);
                check(beam.getRealmTier()==12 && beam.getBladeHeight()==224,"realm snapshots mountain sized sword");
                p.level().addFreshEntity(beam);
            });
            context.waitTicks(3);
            System.out.println("SWORD_BEAM_SCREENSHOT="+context.takeScreenshot("sword-mountain-beam"));
            context.waitTicks(5);
            context.runOnClient(mc->check(xiaoshi2022.corpseorigin.client.render.SwordImpactRenderer.effectCount()>0,"real hit produces client flower effect"));
            System.out.println("SWORD_IMPACT_SCREENSHOT="+context.takeScreenshot("sword-hit-flower"));
            context.waitTicks(35);
            context.runOnClient(mc->check(Math.abs(xiaoshi2022.corpseorigin.client.render.SwordImpactRenderer.shake(0))<.001,"camera shake expires"));
            System.out.println("SwordGameplayTest passed: progressive canyon, budgets, protections, tier size, real hit packet and camera recovery.");
        }finally{cfg.swordTerrainDestruction=enabled;cfg.swordRiftBlocksPerTick=budget;cfg.swordRiftScansPerTick=scans;cfg.swordRiftMaxLength=length;}
    }
}
