package xiaoshi2022.corpseorigin.growth;

import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import xiaoshi2022.corpseorigin.entity.*;
import xiaoshi2022.corpseorigin.entity.ai.CorpseGrappleGoal;
import xiaoshi2022.corpseorigin.entity.animation.ZbLayerAnimationCache;
import xiaoshi2022.corpseorigin.registry.ModEntities;

/** Regression for the actual GeckoLib timeline, attachment isolation and death lifecycle. */
public final class CorpseHorrorGameplayTest implements FabricClientGameTest {
    private LowerLevelZbEntity corpse;
    private Villager prey;
    private CorpseGrappleGoal grapple;
    private int entityId;
    private double firstTime;
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    @Override public void runTest(ClientGameTestContext context){
        var config=CorpseHorror.config();boolean enabled=config.enabled;int worms=config.maggotsPerCorpse;
        double ribsChance=config.livingRibsChance,maggotChance=config.deathMaggotChance;
        config.livingRibsChance=1;config.deathMaggotChance=1;
        config.enabled=true;config.maggotsPerCorpse=3;
        try(var world=context.worldBuilder().create()){
            var server=world.getServer();context.waitTicks(50);
            server.runOnServer(s->{
                var p=s.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.CREATIVE);p.teleportTo(0,100,0);p.setYRot(0);p.setXRot(8);
                for(int x=-10;x<=10;x++)for(int z=-5;z<=18;z++)p.level().setBlock(new BlockPos(x,99,z),Blocks.STONE.defaultBlockState(),2);
                corpse=new LowerLevelZbEntity(ModEntities.LOWER_LEVEL_ZB,p.level());corpse.setPos(0,100,4);corpse.setYRot(180);corpse.setYBodyRot(180);corpse.setYHeadRot(180);corpse.setNoAi(true);
                for(boolean eyes:new boolean[]{true,false}){
                    corpse.setCorpseEye(eyes);
                    corpse.setCracked(eyes);
                    var output=net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING,p.level().registryAccess());
                    corpse.saveWithoutId(output);
                    var copy=new LowerLevelZbEntity(ModEntities.LOWER_LEVEL_ZB,p.level());
                    copy.load(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,p.level().registryAccess(),output.buildResult()));
                    check(copy.hasCorpseEye()==eyes,"both eye variants survive save/load");
                    check(copy.isCracked()==eyes,"cracked appearance survives save/load");
                }
                corpse.setCorpseEye(false);
                corpse.setCracked(true);
                var component=xiaoshi2022.corpseorigin.component.PlayerCorpseComponent.get(p);
                xiaoshi2022.corpseorigin.character.CharacterManager.getInstance().setPlayerCharacter(p,"mortal");
                xiaoshi2022.corpseorigin.component.PlayerCorpseComponent.setPlayerAsCorpse(p,0,0);
                var progression=xiaoshi2022.corpseorigin.character.PlayerCharacterData.get(p);
                for(int rank:new int[]{4,5,15,4}){
                    progression.setPoints(p.getUUID(),xiaoshi2022.corpseorigin.skill.EvolutionManager.getThreshold(rank),0);
                    xiaoshi2022.corpseorigin.component.PlayerCorpseComponent.syncEvolvedEye(p);
                    var appearance=new xiaoshi2022.corpseorigin.client.CorpseOriginClient.ClientCorpseData(true,0,component.getDataPublic());
                    check(appearance.showsCorpseEye()==(rank<5),"player eye threshold and downgrade at "+rank);
                }
                p.level().addFreshEntity(corpse);entityId=corpse.getId();
            });
            context.waitTicks(25);
            context.runOnClient(mc->{
                var z=(LowerLevelZbEntity)mc.level.getEntity(entityId);check(z!=null,"corpse reached client");
                check(!z.hasCorpseEye(),"eye-free variant reaches tracking client");
                check(z.isCracked(),"cracked model variant reaches tracking client");
                var movement=z.getAnimatableInstanceCache().getManagerForId(z.getId()).getAnimationControllers().get("movement");
                check(movement.getCurrentRawAnimation().equals(RawAnimation.begin().thenLoop("idle")),"stationary corpse plays idle");
                firstTime=movement.getCurrentTimelineTime();
                var organs=z.getAnimatableInstanceCache().getManagerForId(ZbLayerAnimationCache.HORROR_ID).getAnimationControllers();
                check(organs.get("movement").getPlayState()==PlayState.STOP,"ribs do not play body movement clips");
                check(organs.get("custom_organs").isAnimatingBones(),"ribs have an independent active animation");
            });
            context.waitTicks(15);
            context.runOnClient(mc->{
                var z=(LowerLevelZbEntity)mc.level.getEntity(entityId);
                var movement=z.getAnimatableInstanceCache().getManagerForId(z.getId()).getAnimationControllers().get("movement");
                // GeckoLib 5 stores timeline time in seconds, not Minecraft ticks.
                double elapsed=movement.getCurrentTimelineTime()-firstTime;
                check(elapsed>.35 && elapsed<1.3,"idle advances with real time (seconds): "+elapsed);
            });
            context.runOnClient(mc->mc.gui.hud.getChat().clearMessages(true));
            context.waitTicks(2);
            System.out.println("CORPSE_IDLE_SCREENSHOT="+context.takeScreenshot("corpse-idle-ribs"));
            context.waitTicks(65);
            context.runOnClient(mc->{
                var z=(LowerLevelZbEntity)mc.level.getEntity(entityId);
                var movement=z.getAnimatableInstanceCache().getManagerForId(z.getId()).getAnimationControllers().get("movement");
                check(movement.isAnimatingBones() && !movement.hasAnimationFinished(),"idle remains active across multiple loops");
            });
            server.runOnServer(s->{
                var level=corpse.level();prey=EntityTypes.VILLAGER.create(level,EntitySpawnReason.COMMAND);prey.setNoAi(true);prey.setPos(.65,100,4);
                prey.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);prey.setHealth(100);level.addFreshEntity(prey);corpse.setTarget(prey);
                var ally=new LowerLevelZbEntity(ModEntities.LOWER_LEVEL_ZB,level);ally.setPos(4,100,4);ally.setNoAi(true);level.addFreshEntity(ally);
                var passive=new AotumanZbEntity(ModEntities.AOTUMAN_ZB,level);passive.setPos(5,100,4);passive.setNoAi(true);level.addFreshEntity(passive);
                int oldTick=corpse.tickCount;corpse.tickCount=40;CorpseHorror.tick(corpse);corpse.tickCount=oldTick;
                check(ally.getTarget()==prey,"nearby corpse joins the hunt");check(passive.getTarget()==null,"passive variant excluded");
                grapple=new CorpseGrappleGoal(corpse);check(grapple.canUse(),"close prey can be grappled");grapple.start();
                check(corpse.getGrappleTarget()==prey.getId(),"grapple state synced");
                corpse.tickCount+=8;grapple.tick();corpse.tickCount-=8;
                check(prey.getHealth()<100,"gnaw inflicts actual damage");
            });
            context.waitTicks(10);
            System.out.println("CORPSE_GNAW_SCREENSHOT="+context.takeScreenshot("corpse-gnaw"));
            server.runOnServer(s->{
                corpse.hurtTime=5;check(!grapple.canContinueToUse(),"hitting the corpse breaks grapple");grapple.stop();corpse.hurtTime=0;
                check(corpse.getGrappleTarget()==-1 && !grapple.canUse(),"release starts cooldown");
                prey.discard();corpse.setTarget(null);
                corpse.hurtServer((net.minecraft.server.level.ServerLevel)corpse.level(),corpse.damageSources().genericKill(),10000);
            });
            context.waitTicks(25);
            server.runOnServer(s->{
                check(!corpse.isRemoved() && corpse.deathTime>=20,"custom death is not removed by vanilla at tick twenty");
                check(corpse.horrorWormsReleased,"death release marked once");
                long count=corpse.level().getEntitiesOfClass(CorpseMaggotEntity.class,corpse.getBoundingBox().inflate(32)).size();
                check(count==3,"death releases exactly three maggots, got "+count);
            });
            System.out.println("CORPSE_DEATH_SCREENSHOT="+context.takeScreenshot("corpse-death-maggots"));
            context.waitTicks(35);
            server.runOnServer(s->{
                check(corpse.isRemoved(),"custom corpse removed after animation");
                check(corpse.level().getEntitiesOfClass(CorpseMaggotEntity.class,corpse.getBoundingBox().inflate(48)).size()==3,"no duplicate maggot burst");
                prey=EntityTypes.VILLAGER.create(corpse.level(),EntitySpawnReason.COMMAND);prey.setNoAi(true);prey.setPos(0,100,4);
                prey.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);prey.setHealth(100);corpse.level().addFreshEntity(prey);
                var maggot=corpse.level().getEntitiesOfClass(CorpseMaggotEntity.class,corpse.getBoundingBox().inflate(48)).getFirst();
                maggot.setPos(0,100,4);check(maggot.tryAttach(prey),"maggot attaches to human host");entityId=maggot.getId();
            });
            context.waitTicks(40);
            System.out.println("PARASITE_SCREENSHOT="+context.takeScreenshot("maggot-attached-feeding"));
            server.runOnServer(s->check(prey.getHealth()<100,"attached maggot gnaws human host"));
            context.waitTicks(170);
            server.runOnServer(s->{
                check(prey.hasEffect(xiaoshi2022.corpseorigin.registry.ModEffects.QIANS),"sustained parasitism applies real infection");
                prey.removeEffect(xiaoshi2022.corpseorigin.registry.ModEffects.QIANS);
            });
            context.waitTicks(2);
            server.runOnServer(s->{var m=(CorpseMaggotEntity)corpse.level().getEntity(entityId);check(m!=null && m.getHostId()<0,"curing infection dislodges parasite");});
            System.out.println("CorpseHorrorGameplayTest passed: advancing idle, isolated accessory animation, pack targeting, breakable gnaw, delayed death and one maggot burst.");
        }finally{config.enabled=enabled;config.maggotsPerCorpse=worms;config.livingRibsChance=ribsChance;config.deathMaggotChance=maggotChance;}
    }
}
