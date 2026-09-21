package xiaoshi2022.corpseorigin.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.CameraType;
import xiaoshi2022.corpseorigin.character.CharacterManager;
import xiaoshi2022.corpseorigin.client.ChameleonDisguiseScreen;
import xiaoshi2022.corpseorigin.network.ChameleonDisguisePayload;
import xiaoshi2022.corpseorigin.skill.SkillManager;
import xiaoshi2022.corpseorigin.skill.chapter.ChapterActorState;

public class ChameleonClientTest implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context){
        try(var world=context.worldBuilder().create()){
            var server=world.getServer();
            server.runOnServer(s->{
                var p=s.getPlayerList().getPlayers().getFirst();
                CharacterManager.getInstance().setPlayerCharacter(p,"bianselong_zb");SkillManager.grantAllSkills(p);
            });
            context.waitFor(c->c.player!=null && "bianselong_zb".equals(c.player.getAttachedOrCreate(ChapterActorState.ROLE)));
            context.runOnClient(c->{c.options.setCameraType(CameraType.THIRD_PERSON_FRONT);ChameleonDisguiseScreen.activate("chameleon_disguise");});
            context.waitTicks(3);context.takeScreenshot("chameleon-choice-default");
            context.clickScreenButton("gui.corpseorigin.disguise.confirm");
            context.waitFor(c->c.gui.screen()==null && ChapterActorState.XIAOHUI.equals(c.player.getAttachedOrCreate(ChapterActorState.DISGUISE)));
            context.runOnClient(c->{
                var path=xiaoshi2022.corpseorigin.client.skin.ChameleonSkins.XIAOHUI.body().texturePath();
                if(c.getResourceManager().getResource(path).isEmpty())throw new AssertionError("Xiao Hui texture path does not exist: "+path);
            });
            context.waitTicks(3);context.takeScreenshot("chameleon-xiaohui");
            server.runOnServer(s->{
                var p=s.getPlayerList().getPlayers().getFirst();
                if(!SkillManager.snapshotRemaining(p).containsKey("chameleon_disguise"))throw new AssertionError("Successful disguise has no cooldown");
                SkillManager.restoreRemaining(p,java.util.Map.of());p.setAttached(ChapterActorState.DISGUISE,"");
            });
            context.waitTicks(25);
            context.runOnClient(c->ClientPlayNetworking.send(new ChameleonDisguisePayload.Select("bad/id")));
            context.waitTicks(5);
            server.runOnServer(s->{
                var p=s.getPlayerList().getPlayers().getFirst();
                if(!p.getAttachedOrCreate(ChapterActorState.DISGUISE).isEmpty() || SkillManager.snapshotRemaining(p).containsKey("chameleon_disguise"))
                    throw new AssertionError("Invalid ID changed disguise or cooldown");
            });
            context.waitTicks(25);
            context.runOnClient(c->{
                ChameleonDisguiseScreen.activate("chameleon_disguise");
                var screen=c.gui.screen();
                for(var widget:screen.children())if(widget instanceof net.minecraft.client.gui.components.CycleButton<?> cycle){
                    @SuppressWarnings("unchecked") var mode=(net.minecraft.client.gui.components.CycleButton<Boolean>)cycle;
                    mode.onPress(new net.minecraft.client.input.KeyEvent(org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER,0,0));
                }
                for(var widget:screen.children())if(widget instanceof net.minecraft.client.gui.components.EditBox edit)edit.setValue(c.player.getGameProfile().name());
            });
            context.waitTicks(3);context.takeScreenshot("chameleon-choice-player");
            context.clickScreenButton("gui.corpseorigin.disguise.confirm");
            context.waitFor(c->c.gui.screen()==null && c.player.getUUID().toString().equals(c.player.getAttachedOrCreate(ChapterActorState.DISGUISE)));
            server.runOnServer(s->{
                var p=s.getPlayerList().getPlayers().getFirst();
                if(p.getAttached(ChapterActorState.DISGUISE_PROFILE)==null)throw new AssertionError("Player profile was not synced");
                CharacterManager.getInstance().setPlayerCharacter(p,"mortal");
            });
            context.waitFor(c->c.player.getAttachedOrCreate(ChapterActorState.DISGUISE).isEmpty());
        }
    }
}
