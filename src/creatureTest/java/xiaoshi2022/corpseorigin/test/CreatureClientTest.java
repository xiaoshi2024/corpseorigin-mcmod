package xiaoshi2022.corpseorigin.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.CameraType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.GameType;
import net.minecraft.world.effect.MobEffects;
import xiaoshi2022.corpseorigin.character.*;
import xiaoshi2022.corpseorigin.skill.*;
import xiaoshi2022.corpseorigin.skill.chapter.*;
import xiaoshi2022.corpseorigin.entity.CorpseAntEntity;
import java.nio.file.*;

public final class CreatureClientTest implements FabricClientGameTest {
    private void picture(ClientGameTestContext c,String name){try{Path dir=Path.of("../../artifacts/creatures-2026-09-22");Files.createDirectories(dir);Files.copy(c.takeScreenshot(name),dir.resolve(name+".png"),StandardCopyOption.REPLACE_EXISTING);}catch(Exception e){throw new RuntimeException(e);}}
    private static void check(boolean ok,String reason){if(!ok)throw new AssertionError(reason);}
    @Override public void runTest(ClientGameTestContext c){
        try(var world=c.worldBuilder().create()){
            var server=world.getServer();c.waitFor(mc->mc.player!=null);
            server.runCommand("time set day");server.runCommand("fill -20 79 -20 20 79 20 minecraft:stone");server.runCommand("tp @a 0.5 80 0.5 0 8");
            server.runOnServer(s->s.getPlayerList().getPlayers().getFirst().setGameMode(GameType.CREATIVE));
            c.runOnClient(mc->{mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);mc.player.setXRot(8);});
            for(String role:new String[]{"qingwa_zb","hujie","chongmu","jingang_zb","xiongxing_zb","chongqun","xiaohui"}){
                server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();CharacterManager.getInstance().setPlayerCharacter(p,role);SkillManager.grantAllSkills(p);});
                c.waitFor(mc->role.equals(mc.player.getAttachedOrCreate(ChapterActorState.ROLE)));c.waitTicks(8);picture(c,role);
                if(role.equals("qingwa_zb")){
                    var target=server.computeOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();var l=(ServerLevel)p.level();var cow=new Cow(EntityTypes.COW,l);cow.setNoAi(true);cow.setPos(p.position().add(0,0,2));l.addFreshEntity(cow);check(SkillManager.activate(p,"killing_gas"),"Gas activation");return cow.getUUID();});
                    c.waitTicks(20);picture(c,"frog_gas");server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();var cow=(Cow)((ServerLevel)p.level()).getEntity(target);check(cow.hasEffect(MobEffects.NAUSEA),"Gas did not cause hallucination");check(p.hasEffect(MobEffects.STRENGTH),"Gas did not amplify owner");cow.discard();});
                }
                if(role.equals("hujie")){server.runCommand("corpse_scene threat @a");c.waitTicks(10);picture(c,"fox_threat");c.waitTicks(25);}
                if(role.equals("chongmu")){
                    var target=server.computeOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();var l=(ServerLevel)p.level();var cow=new Cow(EntityTypes.COW,l);cow.setNoAi(true);cow.setPos(p.position().add(0,0,4));l.addFreshEntity(cow);check(SkillManager.activate(p,"summon_swarm"),"Summon activation");return cow.getUUID();});
                    c.waitTicks(15);picture(c,"mother_summon");server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();var ants=p.level().getEntitiesOfClass(CorpseAntEntity.class,p.getBoundingBox().inflate(20));check(ants.size()==6,"Expected six ants");check(ants.stream().filter(CorpseAntEntity::isBullet).count()==2,"Expected two bullet ants");});
                    c.waitTicks(75);server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();var targetEntity=((ServerLevel)p.level()).getEntity(target);check(targetEntity==null || ((Cow)targetEntity).getHealth()<10,"Ants did not attack target");});
                    server.runCommand("kill @e[type=!minecraft:player]");
                    var captive=server.computeOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();var l=(ServerLevel)p.level();p.setYRot(0);p.setXRot(15);var cow=new Cow(EntityTypes.COW,l);cow.setNoAi(true);cow.setPos(p.position().add(0,0,3));l.addFreshEntity(cow);check(SkillManager.activate(p,"bag_capture"),"Bag capture activation");cow.setDeltaMovement(1,0,0);return cow.getUUID();});
                    c.waitTicks(20);picture(c,"bag_capture");server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();var cow=(Cow)((ServerLevel)p.level()).getEntity(captive);check(Math.abs(cow.getX()-p.getX())<.2,"Capture did not hold target");check(cow.hasEffect(MobEffects.POISON),"Capture poison missing");cow.discard();});
                }
                if(role.equals("jingang_zb")){
                    server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.SURVIVAL);p.setHealth(20);var cow=new Cow(EntityTypes.COW,p.level());p.hurtServer((ServerLevel)p.level(),p.damageSources().mobAttack(cow),3);check(p.getHealth()==20,"Iron protection failed");p.setGameMode(GameType.CREATIVE);});
                    server.runCommand("corpse_scene infant @a");c.waitTicks(35);picture(c,"jingang_infant");
                    server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();var shell=new xiaoshi2022.corpseorigin.shell.CharacterShellStateComponent(p);p.setAttached(CreatureAbilities.INFANT,false);shell.applyTo(p);check(p.getAttachedOrCreate(CreatureAbilities.INFANT),"Infant form lost across body snapshot");check(SkillManager.activate(p,"muscle_rage"),"Rage activation");check(!p.getAttachedOrCreate(CreatureAbilities.INFANT),"Rage did not evolve infant");});
                    c.waitTicks(10);picture(c,"jingang_rage");c.waitTicks(25);
                    server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.SURVIVAL);p.invulnerableTime=0;var cow=new Cow(EntityTypes.COW,p.level());float before=p.getHealth();p.hurtServer((ServerLevel)p.level(),p.damageSources().mobAttack(cow),3);check(p.getHealth()<before,"Rage weak point must disable iron protection");p.setGameMode(GameType.CREATIVE);check(SkillManager.activate(p,"pounce_combo"),"Pounce activation");});c.waitTicks(12);picture(c,"jingang_pounce");c.waitTicks(25);server.runCommand("tp @a 0.5 80 0.5 0 8");
                }
                if(role.equals("xiongxing_zb")){
                    server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();var level=(ServerLevel)p.level();for(int n=0;n<3;n++){var v=new Villager(EntityTypes.VILLAGER,level);v.setPos(p.position().add(3,0,0));level.addFreshEntity(v);v.hurtServer(level,p.damageSources().playerAttack(p),1000);}check(p.getAttachedOrCreate(CreatureAbilities.BEAR_ARMS)==3,"Villager kills did not grow arms");});
                    c.waitTicks(8);picture(c,"bear_three_arms");
                    server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();check(SkillManager.activate(p,"bear_charge"),"Bear charge activation");});c.waitTicks(8);picture(c,"bear_charge");c.waitTicks(15);server.runCommand("tp @a 0.5 80 0.5 0 8");
                }
                if(role.equals("xiaohui")){server.runCommand("corpse_scene black_general @a");server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();check(p.getAttachedOrCreate(ChapterScenes.CONDITION).isEmpty(),"Real XiaoHui must not receive black general state");check(CharacterManager.getInstance().getPlayerCharacter(p).getSkills().isEmpty(),"Real XiaoHui has combat skills");});}
            }
            server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();CharacterManager.getInstance().setPlayerCharacter(p,"bianselong_zb");SkillManager.grantAllSkills(p);SkillManager.activate(p,"chameleon_disguise");});c.waitTicks(5);
            for(String cue:new String[]{"bound","fear","black_general"}){server.runCommand("corpse_scene "+cue+" @a");c.waitTicks(8);picture(c,"disguise_"+cue);}
            server.runCommand("corpse_scene clear @a");
            server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();CharacterManager.getInstance().setPlayerCharacter(p,"mortal");});c.waitTicks(8);picture(c,"restored_player");
        }
    }
}
