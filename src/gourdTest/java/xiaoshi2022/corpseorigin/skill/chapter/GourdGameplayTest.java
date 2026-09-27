package xiaoshi2022.corpseorigin.skill.chapter;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import xiaoshi2022.corpseorigin.character.*;
import xiaoshi2022.corpseorigin.component.PlayerCorpseComponent;
import xiaoshi2022.corpseorigin.config.CorpseConfig;
import xiaoshi2022.corpseorigin.growth.GourdOrganState;
import xiaoshi2022.corpseorigin.skill.SkillManager;

public final class GourdGameplayTest implements FabricClientGameTest {
    private static void require(boolean value, String reason) { if (!value) throw new AssertionError(reason); }
    private static LivingEntity donor(ServerPlayer p, String id) {
        LivingEntity entity = (LivingEntity) BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.withDefaultNamespace(id))
                .create(p.level(), EntitySpawnReason.COMMAND);
        if (entity == null) throw new AssertionError("Cannot create " + id);
        entity.setPos(p.position().add(2, 0, 0));
        if (entity instanceof Mob mob) mob.setNoAi(true);
        p.level().addFreshEntity(entity);
        return entity;
    }
    @Override public void runTest(ClientGameTestContext context) {
        double previousChance = CorpseConfig.get().gourdInheritance.passiveChance;
        try (var world = context.worldBuilder().create()) {
            var server = world.getServer();
            server.runOnServer(s -> {
                ServerPlayer p = s.getPlayerList().getPlayers().getFirst();
                CharacterManager.getInstance().setPlayerCharacter(p, "xiaojingang");
                require(GourdInheritance.reason(p, 2) != null, "Disguise must require a villager roll");
                CorpseConfig.get().gourdInheritance.passiveChance = 1;
                LivingEntity villager = donor(p, "villager");
                require(GourdCapture.begin(p, villager), "Villager capture rejected");
                require(!GourdMemory.knows(GourdInheritance.memory(p), GourdTrait.VILLAGER), "Granted before devour completed");
            });
            context.waitTicks(80);
            server.runOnServer(s -> {
                ServerPlayer p = s.getPlayerList().getPlayers().getFirst();
                require(GourdMemory.knows(GourdInheritance.memory(p), GourdTrait.VILLAGER), "Completed swallow did not unlock disguise");
                GourdOrganState.toggle(p);
                require(GourdOrganState.detached(p), "Test organ did not detach");
                require(SkillManager.activate(p, "gourd_mortal_disguise", false), "Disguise activation failed");
                require(GourdInheritance.disguised(p), "Disguise flag not set");
                require(!GourdOrganState.detached(p), "Detached gourd was not recalled");
                require(CharacterManager.getInstance().getPlayerCharacterId(p).equals("xiaojingang"), "Disguise changes role");
                require(PlayerCorpseComponent.get(p).isCorpse(), "Disguise changes real corpse identity");
            });
            context.waitFor(client -> client.player != null && GourdInheritance.disguised(client.player));
            context.runOnClient(client -> {
                AvatarRenderer renderer = client.getEntityRenderDispatcher().getPlayerRenderer(client.player);
                AvatarRenderState state = new AvatarRenderState();
                renderer.extractRenderState(client.player, state, 0);
                require(!xiaoshi2022.corpseorigin.client.render.layer.CustomOrganLayer.hasCustomFrames(state), "Disguised gourd remains visible");
                require(state.skin.equals(client.player.getSkin()), "Disguise replaced original skin");
                require(state.getGeckolibData(xiaoshi2022.corpseorigin.client.limb.LimbRenderData.LIMB_MASK) == null, "Disguise leaks corpse limbs");
            });
            server.runOnServer(s -> {
                ServerPlayer p = s.getPlayerList().getPlayers().getFirst();
                require(SkillManager.activate(p, "gourd_eyes", false), "Other gourd skill rejected");
                require(!GourdInheritance.disguised(p), "Casting failed to reveal player");
                CorpseConfig.get().gourdInheritance.passiveChance = 0;
                LivingEntity sheep = donor(p, "sheep"); sheep.setHealth(sheep.getMaxHealth() * .25F);
                require(GourdCapture.begin(p, sheep), "Sheep capture rejected");
                GourdCapture.cancel(p);
                require(sheep.isAlive() && sheep.getAttachedOrCreate(GourdCapture.ANCHOR) == -1, "Cancelled prey not released");
                require(!GourdMemory.knows(GourdInheritance.memory(p), GourdTrait.SHEEP), "Cancelled capture granted a trait");
                require(GourdCapture.begin(p, sheep), "Second sheep capture rejected");
            });
            context.waitTicks(80);
            server.runOnServer(s -> {
                ServerPlayer p = s.getPlayerList().getPlayers().getFirst();
                require(!GourdMemory.knows(GourdInheritance.memory(p), GourdTrait.SHEEP), "Zero chance granted a trait");
                var saved = PlayerCharacterData.get(p).writeNbt(p.getUUID());
                PlayerCharacterData.get(p).readNbt(p.getUUID(), saved);
                require(GourdMemory.knows(GourdInheritance.memory(p), GourdTrait.VILLAGER), "Snapshot lost disguise unlock");
                CharacterManager.getInstance().setPlayerCharacter(p, "mortal");
            });
            context.waitTicks(3);
            server.runOnServer(s -> {
                ServerPlayer p = s.getPlayerList().getPlayers().getFirst();
                require(!GourdInheritance.disguised(p) && GourdInheritance.selected(p) == null, "Role change leaks adaptation");
                CharacterManager.getInstance().setPlayerCharacter(p, "xiaojingang");
                require(GourdMemory.knows(GourdInheritance.memory(p), GourdTrait.VILLAGER), "Role switching erased learned adaptation");
            });
            System.out.println("GOURD_GAMEPLAY_REGRESSION_PASS");
        } finally {
            CorpseConfig.get().gourdInheritance.passiveChance = previousChance;
        }
    }
}
