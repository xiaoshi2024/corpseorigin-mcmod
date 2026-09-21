package xiaoshi2022.corpseorigin.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.CameraType;
import net.minecraft.world.item.ItemStack;
import xiaoshi2022.corpseorigin.character.CharacterManager;
import xiaoshi2022.corpseorigin.registry.ModItems;
import xiaoshi2022.corpseorigin.skill.chapter.*;

public class ChapterClientTest implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        try(var world=context.worldBuilder().create()) {
            var server=world.getServer();
            context.waitFor(client->client.player!=null);
            context.runOnClient(client->{client.options.setCameraType(CameraType.THIRD_PERSON_FRONT);client.player.setXRot(10);});
            for(String role:new String[]{"bianyi_guiyu","k","bianselong_zb","muxi","tushu","siyangyuan_zb","kuaidiyuan_zb","zhaoritian","fengmohuitailang","laura","jack","yanyan"}) {
                server.runOnServer(s->{
                    var p=s.getPlayerList().getPlayers().getFirst();
                    if(!CharacterManager.getInstance().setPlayerCharacter(p,role))throw new AssertionError("Missing role "+role);
                    p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(role.equals("k")?ModItems.BLOOD_WING_BLADE:ModItems.RED_METEOR_SWORD));
                });
                context.waitFor(client->role.equals(client.player.getAttachedOrCreate(ChapterActorState.ROLE)));
                if(role.equals("muxi") || role.equals("tushu"))server.runCommand("corpse_scene injured @a");
                if(role.equals("k"))server.runOnServer(s->new xiaoshi2022.corpseorigin.skill.k.BatCloakSkill().onActivate(s.getPlayerList().getPlayers().getFirst()));
                if(role.equals("fengmohuitailang"))server.runOnServer(s->new xiaoshi2022.corpseorigin.skill.fengmohuitailang.TenguDivineArraySkill().onActivate(s.getPlayerList().getPlayers().getFirst()));
                if(role.equals("laura"))server.runOnServer(s->new SwordFlowerSkill().onActivate(s.getPlayerList().getPlayers().getFirst()));
                if(role.equals("jack"))server.runOnServer(s->new BlackFridaySkill().onActivate(s.getPlayerList().getPlayers().getFirst()));
                if(role.equals("yanyan"))server.runOnServer(s->ChapterScenes.action(s.getPlayerList().getPlayers().getFirst(),"charge",40));
                if(role.equals("bianyi_guiyu"))server.runOnServer(s->{
                    var p=s.getPlayerList().getPlayers().getFirst();
                    p.setAttached(ChapterActorState.BITE_UNTIL,p.level().getGameTime()+12);
                    var egg=xiaoshi2022.corpseorigin.entity.CorpseFishEggEntity.create(p,0);
                    p.level().addFreshEntity(egg);
                });
                context.waitTicks(8);
                context.takeScreenshot("chapter-"+role);
            }
            server.runCommand("corpse_scene clear @a");
            context.waitTicks(35);
            context.runOnClient(client->{
                if(client.options.getCameraType()!=CameraType.THIRD_PERSON_FRONT)throw new AssertionError("Scene camera was not restored");
                client.options.setCameraType(CameraType.FIRST_PERSON);
            });
            server.runOnServer(s->{
                var p=s.getPlayerList().getPlayers().getFirst();
                CharacterManager.getInstance().setPlayerCharacter(p,"tushu");
                ChapterScenes.action(p,"drain",20);
            });
            context.waitTicks(5);
            context.runOnClient(client->{if(client.options.getCameraType()!=CameraType.THIRD_PERSON_FRONT)throw new AssertionError("Scene close-up not activated");});
            context.waitTicks(25);
            context.runOnClient(client->{if(client.options.getCameraType()!=CameraType.FIRST_PERSON)throw new AssertionError("Scene failed to restore first-person camera");});
            server.runOnServer(s->{
                var p=s.getPlayerList().getPlayers().getFirst();
                CharacterManager.getInstance().setPlayerCharacter(p,"mortal");
            });
            context.waitFor(client->"mortal".equals(client.player.getAttachedOrCreate(ChapterActorState.ROLE)));
            context.waitTicks(5);
            context.takeScreenshot("chapter-restored");
            var victimId=server.computeOnServer(s->{
                var p=s.getPlayerList().getPlayers().getFirst();
                var level=(net.minecraft.server.level.ServerLevel)p.level();
                var cow=new net.minecraft.world.entity.animal.cow.Cow(net.minecraft.world.entity.EntityTypes.COW,level);
                cow.setNoAi(true);cow.setNoGravity(true);cow.setPos(p.position().add(0,0,3));level.addFreshEntity(cow);
                CharacterManager.getInstance().setPlayerCharacter(p,"k");
                p.setHealth(10);p.setYRot(0);p.setXRot(15);
                p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(ModItems.BLOOD_WING_BLADE));
                var skill=new xiaoshi2022.corpseorigin.skill.k.BloodWingBladeSkill();
                if(skill.checkUsable(p)!=null)throw new AssertionError("Blade target/weapon check failed");
                skill.onActivate(p);
                if(p.getHealth()<=10 || p.getHealth()>14)throw new AssertionError("Lifesteal must follow actual damage and cap at 4");
                cow.discard();
                var host=new net.minecraft.world.entity.animal.cow.Cow(net.minecraft.world.entity.EntityTypes.COW,level);
                host.setNoAi(true);host.setNoGravity(true);host.setPos(p.position().add(3,0,0));level.addFreshEntity(host);
                CharacterManager.getInstance().setPlayerCharacter(p,"bianyi_guiyu");
                var egg=xiaoshi2022.corpseorigin.entity.CorpseFishEggEntity.create(p,0);
                egg.setPos(host.position().add(0,.5,0));egg.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);level.addFreshEntity(egg);
                return host.getUUID();
            });
            context.waitTicks(8);
            server.runOnServer(s->{
                var p=s.getPlayerList().getPlayers().getFirst();var level=(net.minecraft.server.level.ServerLevel)p.level();
                var host=level.getEntity(victimId);
                if(host==null || level.getEntitiesOfClass(xiaoshi2022.corpseorigin.entity.CorpseFishEggEntity.class,host.getBoundingBox().inflate(2),e->e.attachedTo(host)).size()!=1)
                    throw new AssertionError("Egg did not attach");
            });
            context.waitTicks(65);
            server.runOnServer(s->{
                var p=s.getPlayerList().getPlayers().getFirst();var level=(net.minecraft.server.level.ServerLevel)p.level();
                var host=(net.minecraft.world.entity.LivingEntity)level.getEntity(victimId);
                if(host==null || host.getHealth()>=host.getMaxHealth())throw new AssertionError("Egg did not drain");
                if(!level.getEntitiesOfClass(xiaoshi2022.corpseorigin.entity.CorpseFishEggEntity.class,host.getBoundingBox().inflate(2),e->e.attachedTo(host)).isEmpty())
                    throw new AssertionError("Mob egg survived beyond 3 seconds");
                CharacterManager.getInstance().setPlayerCharacter(p,"muxi");
                FiveElementsCombat.bind(p,host);
                host.setDeltaMovement(1,.2,0);
            });
            context.waitTicks(5);
            server.runOnServer(s->{
                var p=s.getPlayerList().getPlayers().getFirst();var level=(net.minecraft.server.level.ServerLevel)p.level();
                var host=level.getEntity(victimId);
                if(host==null || Math.abs(host.getX()-(p.getX()+3))>.11)throw new AssertionError("Vines failed to hold horizontal anchor");
            });
        }
    }
}
