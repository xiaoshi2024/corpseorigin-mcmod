package xiaoshi2022.corpseorigin.test;

import com.google.gson.Gson;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.CameraType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.level.GameType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import xiaoshi2022.corpseorigin.character.*;
import xiaoshi2022.corpseorigin.skill.*;
import xiaoshi2022.corpseorigin.registry.*;
import java.nio.file.*;
import java.util.*;

/** Isolated runtime sweep. Accepted activation is deliberately NOT an assertion of full functionality. */
public class SkillDemoTest implements FabricClientGameTest {
    private static final Path OUT=Path.of("../../artifacts/skill-demo-2026-09-21");
    private int frame;
    private void record(Map<String,Object> row) {
        try { Files.writeString(OUT.resolve("results.jsonl"),new Gson().toJson(row)+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND); }
        catch(Exception e) { throw new RuntimeException(e); }
    }
    private void capture(ClientGameTestContext context) {
        try {
            Path screenshot=context.takeScreenshot("skill-recording");
            Files.copy(screenshot,OUT.resolve("frames").resolve(String.format("%06d.png",frame++)),StandardCopyOption.REPLACE_EXISTING);
        } catch(Exception e) { throw new RuntimeException(e); }
    }
    @Override public void runTest(ClientGameTestContext context) {
        if(!Boolean.getBoolean("corpseorigin.skillDemo")) return;
        boolean retry=Boolean.getBoolean("corpseorigin.skillDemoRetry");
        Set<String> selected=new HashSet<>();
        try { Files.createDirectories(OUT.resolve("frames"));
            if(retry) {
                for(String line:Files.readAllLines(OUT.resolve("results.jsonl"))) {
                    var old=new Gson().fromJson(line,com.google.gson.JsonObject.class);
                    frame=Math.max(frame,old.get("endFrame").getAsInt());
                    String key=old.get("role").getAsString()+":"+old.get("skill").getAsString();
                    selected.remove(key);
                    if(old.get("status").getAsString().equals("PRECONDITION_BLOCKED") || (old.get("status").getAsString().equals("PASSIVE_REQUIRES_EVENT_TEST") && old.get("skill").getAsString().equals("black_gold_heart")))selected.add(key);
                }
            } else Files.writeString(OUT.resolve("results.jsonl"),"");
        }
        catch(Exception e) { throw new RuntimeException(e); }
        var roles=CharacterManager.getInstance().getRegisteredCharacters();
        for(var role:roles) {
            if(retry && role.getSkills().stream().noneMatch(sk->selected.contains(role.getId()+":"+sk.getId().getPath())))continue;
            try(var world=context.worldBuilder().create()) {
                var server=world.getServer();
                context.waitFor(c->c.player!=null);
                server.runCommand("time set day");
                server.runCommand("weather clear");
                server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.CREATIVE);CharacterManager.getInstance().setPlayerCharacter(p,role.getId());SkillManager.grantAllSkills(p);});
                context.runOnClient(c->c.options.setCameraType(CameraType.THIRD_PERSON_BACK));
                List<ISkill> skills=role.getSkills();
                if(skills.isEmpty()) {
                    server.runCommand("title @a actionbar {\"text\":\""+role.getId()+" : no registered skills\"}");
                    int start=frame;for(int i=0;i<5;i++){context.waitTicks(4);capture(context);}
                    record(Map.of("role",role.getId(),"skill","","status","NO_REGISTERED_SKILLS","startFrame",start,"endFrame",frame));
                }
                for(var skill:skills) {
                    if(retry && !selected.contains(role.getId()+":"+skill.getId().getPath()))continue;
                    var row=new LinkedHashMap<String,Object>();row.put("role",role.getId());row.put("skill",skill.getId().getPath());row.put("class",skill.getClass().getName());row.put("startFrame",frame);
                    ServerPlayer[] donor={null};
                    server.runCommand("kill @e[type=!minecraft:player]");
                    server.runCommand("fill -12 79 -12 12 79 12 minecraft:stone");
                    server.runCommand("fill -12 80 -12 12 90 12 minecraft:air");
                    server.runCommand("tp @a 0 80 0 0 12");
                    if(role.getId().equals("bianyi_guiyu"))server.runCommand("fill -8 80 -8 8 83 8 minecraft:water");
                    server.runOnServer(s->{
                        var p=s.getPlayerList().getPlayers().getFirst();var level=(ServerLevel)p.level();
                        p.removeAllEffects();p.setHealth(p.getMaxHealth());SkillManager.restoreRemaining(p,Map.of());InnerPowerManager.reset(p);
                        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(role.getId().equals("k")?ModItems.BLOOD_WING_BLADE:ModItems.RED_METEOR_SWORD));
                        if(role.getId().equals("xiaoyanzi")) {
                            var cage=new ItemStack(ModItems.DOG_CAGE);
                            var ham=new xiaoshi2022.corpseorigin.entity.HamEntity(ModEntities.HAM,level);
                            xiaoshi2022.corpseorigin.item.DogCageItem.capture(cage,p,ham);p.setItemInHand(InteractionHand.MAIN_HAND,cage);
                        }
                        for(int n=0;n<3;n++){var cow=new Cow(EntityTypes.COW,level);cow.setNoAi(true);cow.setNoGravity(true);cow.setPos(p.getX()+(n==0?0:n==1?2:-2),80,p.getZ()+3+n*2);level.addFreshEntity(cow);}
                        if(retry && skill.getId().getPath().equals("corpse_brother_rally")) {
                            for(int n=0;n<8;n++) {
                                var corpse=ModEntities.COCO_ZOMBIE.create(level,net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
                                corpse.setNoAi(true);corpse.setPos(p.getX()+(n%2)*.2,80,p.getZ()+8);level.addFreshEntity(corpse);
                            }
                        }
                        if(retry && skill.getId().getPath().equals("reverse_formation_fireball")) {
                            var profile=new com.mojang.authlib.GameProfile(UUID.randomUUID(),"FormationTest");
                            var helper=new ServerPlayer(s,level,profile,net.minecraft.server.level.ClientInformation.createDefault());
                            helper.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(s,new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),helper,net.minecraft.server.network.CommonListenerCookie.createInitial(profile,false));
                            helper.setPos(p.getX()+3,80,p.getZ());
                            CharacterManager.getInstance().setPlayerCharacter(helper,"muxi");InnerPowerManager.reset(helper);
                            var board=s.getScoreboard();var team=board.getPlayerTeam("demo_formation");if(team==null)team=board.addPlayerTeam("demo_formation");
                            board.addPlayerToTeam(p.getScoreboardName(),team);board.addPlayerToTeam(helper.getScoreboardName(),team);
                            xiaoshi2022.corpseorigin.skill.chapter.FiveElementsCombat.formation(helper);donor[0]=helper;
                            row.put("fixture","Synthetic server player supplies formation energy; not a connected multiplayer client");
                            row.put("donorEnergyBefore",InnerPowerManager.getInnerPower(helper));
                        }
                    });
                    context.waitTicks(8);
                    context.runOnClient(c->{c.player.setYRot(0);c.player.setXRot(15);});
                    context.waitTicks(2);
                    context.runOnClient(c->c.options.setCameraType(CameraType.THIRD_PERSON_BACK));
                    server.runCommand("title @a actionbar {\"text\":\""+role.getId()+" : "+skill.getId().getPath()+"\"}");
                    capture(context);
                    server.runOnServer(s->{
                        var p=s.getPlayerList().getPlayers().getFirst();
                        p.setYRot(0);p.setXRot(15);
                        try {
                            var owner=skill.getClass().getMethod("onActivate",ServerPlayer.class).getDeclaringClass();
                            if(!skill.isActivatable() && skill.getId().getPath().equals("black_gold_heart")) {
                                p.setGameMode(GameType.SURVIVAL);p.setHealth(10);
                                p.hurtServer((ServerLevel)p.level(),p.damageSources().generic(),1000);
                                boolean locked=p.isAlive() && p.getHealth()==1;
                                row.put("status",locked?"PASSIVE_LETHAL_HIT_VERIFIED":"PASSIVE_LETHAL_HIT_FAILED");
                                row.put("healthAfterLethalHit",p.getHealth());p.setGameMode(GameType.CREATIVE);
                            }
                            else if(!skill.isActivatable())row.put("status","PASSIVE_REQUIRES_EVENT_TEST");
                            else if(owner==AbstractSkill.class || owner==ISkill.class)row.put("status","UNIMPLEMENTED_EMPTY_HANDLER");
                            else {
                                var blocked=skill.checkUsable(p);
                                if(blocked!=null){row.put("status","PRECONDITION_BLOCKED");row.put("reason",blocked.getString());}
                                else row.put("status",SkillManager.activate(p,skill.getId().getPath())?"ACTIVATED_REQUIRES_EFFECT_REVIEW":"ACTIVATION_REJECTED");
                            }
                        } catch(Exception e){row.put("status","EXCEPTION");row.put("reason",e.toString());}
                    });
                    for(int i=0;i<15;i++){context.waitTicks(4);capture(context);}
                    server.runOnServer(s->{
                        var p=s.getPlayerList().getPlayers().getFirst();var entities=p.level().getEntities(p,p.getBoundingBox().inflate(48));
                        row.put("nearbyEntities",entities.stream().map(e->e.getType().toString()).toList());
                        row.put("targetHealth",entities.stream().filter(e->e instanceof Cow).map(e->((LivingEntity)e).getHealth()).toList());
                        row.put("playerHealth",p.getHealth());row.put("playerPosition",p.position().toString());row.put("cooldowns",SkillManager.snapshotRemaining(p));
                        if(donor[0]!=null)row.put("donorEnergyAfter",InnerPowerManager.getInnerPower(donor[0]));
                    });
                    row.put("endFrame",frame);record(row);
                }
            }
        }
    }
}
