package xiaoshi2022.corpseorigin.growth;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.fish.AbstractFish;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import xiaoshi2022.corpseorigin.entity.ZbrFishEntity;
import xiaoshi2022.corpseorigin.registry.*;
import java.util.ArrayList;
import java.util.List;

public final class CorpseFishGameplayTest implements FabricClientGameTest {
    private final List<AbstractFish> fish = new ArrayList<>();
    private AbstractFish cleanFish;
    private Villager prey;
    private static void check(boolean b,String message){if(!b)throw new AssertionError(message);}
    @Override public void runTest(ClientGameTestContext c) {
        var cfg=CorpseHorror.config();double chance=cfg.fishWaterInfectionChance, village=cfg.villagerInfectionChance;int duration=cfg.fishInfectionTicks;
        cfg.fishWaterInfectionChance=1;cfg.fishInfectionTicks=100;cfg.villagerInfectionChance=0;
        try(var world=c.worldBuilder().create()) {
            c.waitTicks(40);
            world.getServer().runOnServer(s->{
                var p=s.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.CREATIVE);p.teleportTo(12,106,12);
                var l=p.level();
                List<EntityType<? extends AbstractFish>> types=List.of(EntityTypes.COD,EntityTypes.SALMON,EntityTypes.PUFFERFISH,EntityTypes.TROPICAL_FISH,EntityTypes.COD);
                for(int i=0;i<types.size();i++) {
                    int ox=i*6;
                    for(int x=ox;x<=ox+4;x++)for(int z=0;z<=4;z++)for(int y=99;y<=104;y++) {
                        boolean wall=x==ox||x==ox+4||z==0||z==4||y==99;
                        l.setBlock(new BlockPos(x,y,z),wall?Blocks.STONE.defaultBlockState():i==4?Blocks.WATER.defaultBlockState():ModFluids.INFECTED_WATER.defaultFluidState().createLegacyBlock(),2);
                    }
                    var f=types.get(i).create(l,EntitySpawnReason.COMMAND);f.setNoAi(true);f.setPos(ox+2,101,2);
                    f.setCustomName(Component.literal("fish-"+i));f.setPersistenceRequired();l.addFreshEntity(f);
                    if(i==4)cleanFish=f;else fish.add(f);
                }
            });
            c.waitTicks(25);
            world.getServer().runOnServer(s->{
                for(var f:fish)check(f.hasEffect(ModEffects.QIANS),"fish species becomes infected in actual corpse water: "+f.getType());
                check(!cleanFish.hasEffect(ModEffects.QIANS),"ordinary water never infects fish");
                int remaining=fish.getFirst().getEffect(ModEffects.QIANS).getDuration();
                CorpseInfection.touchInfectedWater(fish.getFirst());
                check(remaining==fish.getFirst().getEffect(ModEffects.QIANS).getDuration(),"water contact does not reset incubation");
            });
            c.waitTicks(110);
            world.getServer().runOnServer(s->{
                var l=s.getPlayerList().getPlayers().getFirst().level();
                var infected=l.getEntitiesOfClass(ZbrFishEntity.class,new net.minecraft.world.phys.AABB(0,98,0,30,108,5));
                check(infected.size()==4,"four fish become four corpse fish without duplication");
                for(var f:fish)check(f.isRemoved(),"original fish replaced");
                for(var f:infected){check(f.hasCustomName()&&f.isPersistenceRequired(),"name/persistence preserved");check(!f.hasEffect(ModEffects.QIANS),"corpse fish cannot be reinfected");}
                check(cleanFish.isAlive()&&!cleanFish.isRemoved(),"clean-water fish stays unchanged");
                var hunter=infected.stream().filter(f->f.getX()<5).findFirst().orElseThrow();hunter.setNoAi(false);
                prey=EntityTypes.VILLAGER.create(l,EntitySpawnReason.COMMAND);prey.setNoAi(true);prey.setPos(2,101,2.8);l.addFreshEntity(prey);
            });
            c.waitTicks(60);
            world.getServer().runOnServer(s->check(prey.getHealth()<prey.getMaxHealth(),"corpse fish autonomously attacks submerged villager"));
            System.out.println("CorpseFishGameplayTest passed: four species infect/convert in corpse water, clean water safe, no duplicate conversion, aquatic hunting.");
        } finally {cfg.fishWaterInfectionChance=chance;cfg.fishInfectionTicks=duration;cfg.villagerInfectionChance=village;}
    }
}
