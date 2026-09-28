package xiaoshi2022.corpseorigin.growth;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import xiaoshi2022.corpseorigin.entity.LowerLevelZbEntity;
import xiaoshi2022.corpseorigin.registry.ModEntities;

public final class VillagerHuntGameplayTest implements FabricClientGameTest {
    private LowerLevelZbEntity hunter; private Villager prey;
    private static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
    public void runTest(ClientGameTestContext c){
        var cfg=CorpseHorror.config();double ribs=cfg.livingRibsChance,worms=cfg.deathMaggotChance,escape=cfg.grappleEscapeChance;
        cfg.livingRibsChance=0;cfg.deathMaggotChance=0;cfg.grappleEscapeChance=0;
        try(var w=c.worldBuilder().create()){
            c.waitTicks(40);
            w.getServer().runOnServer(s->{
                var p=s.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.CREATIVE);p.teleportTo(0,100,-4);
                var l=p.level();for(int x=-8;x<=8;x++)for(int z=-8;z<=12;z++)l.setBlock(new BlockPos(x,99,z),Blocks.STONE.defaultBlockState(),2);
                hunter=new LowerLevelZbEntity(ModEntities.LOWER_LEVEL_ZB,l);hunter.setPos(0,100,0);hunter.setHunger(100);l.addFreshEntity(hunter);
                prey=EntityTypes.VILLAGER.create(l,EntitySpawnReason.COMMAND);prey.setNoAi(true);prey.setPos(0,100,5);l.addFreshEntity(prey);
                check(!CorpseHorror.ribsVisible(hunter),"normal living corpse has no exposed ribs");
                var dead=new LowerLevelZbEntity(ModEntities.LOWER_LEVEL_ZB,l);dead.setPos(6,100,6);dead.setHealth(0);dead.deathTime=7;
                CorpseHorror.deathTick(dead);check(CorpseHorror.ribsVisible(dead),"ribs appear during death");
                for(int i=0;i<15;i++)CorpseHorror.deathTick(dead);
                check(dead.horrorWormsReleased,"death decision recorded once even without worms");
                check(l.getEntitiesOfClass(xiaoshi2022.corpseorigin.entity.CorpseMaggotEntity.class,dead.getBoundingBox().inflate(32)).isEmpty(),"zero probability never bursts worms");
            });
            c.waitTicks(100);
            w.getServer().runOnServer(s->{check(prey.getHealth()<prey.getMaxHealth(),"fed corpse autonomously finds, approaches and attacks villager");check(!hunter.hasVisibleRibs(),"server keeps living ribs hidden");});
            System.out.println("VillagerHuntGameplayTest passed: autonomous villager hunt, concealed living ribs, death reveal and no-worm death.");
        }finally{cfg.livingRibsChance=ribs;cfg.deathMaggotChance=worms;cfg.grappleEscapeChance=escape;}
    }
}
