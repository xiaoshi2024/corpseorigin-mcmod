package xiaoshi2022.corpseorigin.growth;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import xiaoshi2022.corpseorigin.entity.LowerLevelZbEntity;
import xiaoshi2022.corpseorigin.entity.UncleEntity;
import xiaoshi2022.corpseorigin.entity.ai.CorpseGrappleGoal;
import xiaoshi2022.corpseorigin.registry.ModEffects;
import xiaoshi2022.corpseorigin.registry.ModEntities;

public final class VillagerHuntGameplayTest implements FabricClientGameTest {
    private LowerLevelZbEntity hunter; private Villager prey;
    private UncleEntity uncle;
    private static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
    public void runTest(ClientGameTestContext c){
        var cfg=CorpseHorror.config();double ribs=cfg.livingRibsChance,worms=cfg.deathMaggotChance,escape=cfg.grappleEscapeChance, infection=cfg.villagerInfectionChance;
        cfg.livingRibsChance=0;cfg.deathMaggotChance=0;cfg.grappleEscapeChance=0;
        cfg.villagerInfectionChance=0;
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
            w.getServer().runOnServer(s->{
                var l=hunter.level();prey.discard();hunter.setTarget(null);hunter.setPos(0,100,0);
                uncle=new UncleEntity(ModEntities.UNCLE,l);uncle.setNoAi(true);uncle.setPos(0,100,4);l.addFreshEntity(uncle);
                check(CorpseHorror.validPrey(hunter,uncle),"uncle is valid grapple prey");
            });
            c.waitTicks(100);
            w.getServer().runOnServer(s->{
                check(uncle.getHealth()<uncle.getMaxHealth(),"corpse autonomously hunts uncle");
                var l=s.getPlayerList().getPlayers().getFirst().level();hunter.discard();uncle.discard();
                hunter=new LowerLevelZbEntity(ModEntities.LOWER_LEVEL_ZB,l);hunter.setNoAi(true);hunter.setPos(0,100,0);l.addFreshEntity(hunter);
                hunter.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).setBaseValue(2);
                uncle=new UncleEntity(ModEntities.UNCLE,l);uncle.setNoAi(true);uncle.setPos(.8,100,0);l.addFreshEntity(uncle);
                hunter.setTarget(uncle);var hold=new CorpseGrappleGoal(hunter);
                check(hold.canUse(),"uncle can be grabbed");hold.start();hunter.tickCount+=8;hold.tick();
                check(uncle.getHealth()<uncle.getMaxHealth(),"grab actually bites uncle");hold.stop();
                cfg.grappleEscapeChance=1;hold=new CorpseGrappleGoal(hunter);
                check(hold.canUse(),"fresh grapple starts");hold.start();hunter.tickCount+=5;hold.tick();
                check(hunter.getGrappleTarget()==-1&&!hold.canContinueToUse(),"certain escape releases target");
                float health=uncle.getHealth();hunter.tickCount+=3;hold.tick();
                check(uncle.getHealth()==health,"escaped target takes no further bite damage");
                uncle.discard();cfg.grappleEscapeChance=0;
                prey=EntityTypes.VILLAGER.create(l,EntitySpawnReason.COMMAND);prey.setNoAi(true);prey.setPos(.8,100,0);l.addFreshEntity(prey);
                hunter.doHurtTarget(l,prey);check(!prey.hasEffect(ModEffects.QIANS),"zero infection chance prevents transmission");
                prey.invulnerableTime=0;cfg.villagerInfectionChance=1;
                hunter.setTarget(prey);hold=new CorpseGrappleGoal(hunter);check(hold.canUse(),"villager can be grabbed");
                hold.start();hunter.tickCount+=8;hold.tick();hold.stop();
                check(prey.hasEffect(ModEffects.QIANS),"successful grapple bite infects villager through damage event");
                hunter.discard();
            });
            c.waitTicks(320);
            w.getServer().runOnServer(s->{
                var l=s.getPlayerList().getPlayers().getFirst().level();
                check(prey.isRemoved(),"infected villager transforms after incubation");
                check(!l.getEntitiesOfClass(LowerLevelZbEntity.class,new net.minecraft.world.phys.AABB(-3,99,-3,3,104,3)).isEmpty(),"conversion creates living corpse brother");
            });
            System.out.println("VillagerHuntGameplayTest passed: villager/uncle hunt, grapple bite and escape, infection conversion, concealed ribs and probabilistic death.");
        }finally{cfg.livingRibsChance=ribs;cfg.deathMaggotChance=worms;cfg.grappleEscapeChance=escape;cfg.villagerInfectionChance=infection;}
    }
}
