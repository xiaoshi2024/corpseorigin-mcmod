package xiaoshi2022.corpseorigin.entity;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import xiaoshi2022.corpseorigin.character.CharacterManager;
import xiaoshi2022.corpseorigin.growth.*;
import xiaoshi2022.corpseorigin.registry.*;
import xiaoshi2022.corpseorigin.shell.*;
import xiaoshi2022.corpseorigin.skill.chapter.*;
import java.util.*;

/** Real client/server regression: equipment, projectiles, role AI, flight, persistence and GEO extraction. */
public final class CloneGameplayTest implements FabricClientGameTest {
    private UUID flyer, fighter, gourd, niunai, flyingTarget, beamTarget, lowQi, highQi;
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }

    private static CloneAvatarEntity clone(ServerPlayer player, String role, double x, double z, CompoundTag organs) {
        return clone(player,role,x,z,organs,0);
    }
    private static CloneAvatarEntity clone(ServerPlayer player, String role, double x, double z, CompoundTag organs, int earned) {
        ShellState body = ShellState.of(player, player.blockPosition());
        var component = body.getComponent().as(CharacterShellStateComponent.class);
        CompoundTag data = new CompoundTag();
        data.putString("CharacterId", role);
        data.putInt("Earned",earned);
        data.put("EvolutionParts", organs.copy());
        CompoundTag serialized = new CompoundTag(); serialized.put("Data", data); component.readNbt(serialized);
        var clone = ModEntities.CLONE_AVATAR.create(player.level(), EntitySpawnReason.COMMAND);
        require(clone != null, "Cannot create clone");
        clone.setPos(x, 201, z); clone.setOwnerUuid(player.getUUID()); clone.setNoAi(true);
        player.level().addFreshEntity(clone);
        clone.setBodyState(body);
        return clone;
    }
    private static LivingEntity target(ServerPlayer p,double x,double y,double z) {
        var target=EntityTypes.COW.create(p.level(),EntitySpawnReason.COMMAND);
        require(target!=null,"Cannot create target");
        target.setNoAi(true); target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(2000); target.setHealth(2000);
        target.setPos(x,y,z);p.level().addFreshEntity(target);return target;
    }
    private static CompoundTag wings() {
        var body=new CompoundTag();
        body.putBoolean("wings",true);body.putBoolean("gills",true);body.putBoolean("vampire",true);
        var wing=OrganLibrary.builtin().getFirst();
        body.putString(OrganLibrary.BODY_KEY,OrganLibrary.JSON.toJson(List.of(
                new OrganSlot(wing.id(),"body",0,0,0,0,0,0,1,false),
                new OrganSlot("aquatic_tail","body",0,0,0,0,0,0,1,false))));
        return body;
    }
    @Override public void runTest(ClientGameTestContext context) {
        try(var world=context.worldBuilder().create()) {
            var server=world.getServer();
            server.runOnServer(s->{
                ServerPlayer p=s.getPlayerList().getPlayers().getFirst();
                p.teleportTo(0,201,0);
                for(int x=-25;x<=25;x++)for(int z=-25;z<=25;z++)p.level().setBlockAndUpdate(new BlockPos(x,200,z),Blocks.STONE.defaultBlockState());
                checkCoverage();
                // Every mod weapon must be accepted/equipped, including weapons without vanilla sword attributes.
                var armed=clone(p,"mortal",-10,-10,new CompoundTag());
                Item[] weapons={ModItems.JUQUE_TW,ModItems.BLOOD_WING_BLADE,ModItems.GUIGUN_WEAP,ModItems.GUIGUN_CLUB,
                        ModItems.RED_METEOR_SWORD,ModItems.TIAN_GANG_KEY,ModItems.BLOOD_LOTUS_LAMP,ModItems.BEE_WHEEL,
                        ModItems.PARCEL_BOMB,ModItems.BILLIARD_EIGHT,ModItems.DOG_CAGE};
                for(Item weapon:weapons){
                    armed.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);
                    ItemStack stack=new ItemStack(weapon);
                    require(armed.wantsToPickUp(p.level(),stack),"Weapon rejected: "+weapon);
                    var drop=new net.minecraft.world.entity.item.ItemEntity(p.level(),armed.getX(),armed.getY(),armed.getZ(),stack);
                    p.level().addFreshEntity(drop);
                    armed.setNoAi(false);
                    armed.aiStep();
                    armed.setNoAi(true);
                    require(armed.getMainHandItem().is(weapon),"Weapon not equipped: "+weapon);
                    require(drop.isRemoved(),"Picked up weapon left a duplicate drop: "+weapon);
                }
                armed.setTarget(target(p,-10,201,-7));
                for(Item weapon:weapons){
                    if(weapon==ModItems.DOG_CAGE)continue; // A real captured Ham is mandatory; an empty cage must not fabricate one.
                    armed.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(weapon));
                    int result=CloneWeaponArts.fire(armed,armed.getMainHandItem());
                    require(result>0,"No weapon technique: "+weapon);
                }
                armed.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(ModItems.DOG_CAGE));
                require(CloneWeaponArts.fire(armed,armed.getMainHandItem())==0,"Empty dog cage spawned a projectile");
                var ham=ModEntities.HAM.create(p.level(),EntitySpawnReason.COMMAND);
                require(ham!=null,"Cannot create captured Ham");
                p.level().addFreshEntity(ham);
                xiaoshi2022.corpseorigin.item.DogCageItem.capture(armed.getMainHandItem(),p,ham);
                require(CloneWeaponArts.fire(armed,armed.getMainHandItem())>0,"Loaded dog cage did not fire");
                require(CloneWeaponArts.fire(armed,armed.getMainHandItem())==0,"Weapon cooldown bypassed");
                require(xiaoshi2022.corpseorigin.item.DogCageItem.isLoaded(armed.getMainHandItem()),"Firing lost captured Ham");
                require(!ChapterCombat.canHit(armed,p),"Clone can damage owner with skills");
                armed.discard();

                int testedRoles=0;
                for(var role:CharacterManager.getInstance().getRegisteredCharacters()) {
                    if(FreeGrowth.isFree(role.getId()))continue; // Free-growth bodies only know explicitly inherited skills.
                    var combat=role.getSkills().stream().filter(skl->skl.isActivatable()&&CloneRoleSkills.supported(skl.getId().getPath())).toList();
                    if(combat.isEmpty())continue;
                    var actor=clone(p,role.getId(),-18,0,new CompoundTag());
                    actor.setTarget(target(p,-18,201,2));
                    Item required=combat.stream().map(skl->CloneRoleSkills.weaponFor(skl.getId().getPath())).filter(Objects::nonNull).findFirst().orElse(null);
                    if(required!=null)actor.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(required,16));
                    if(required==ModItems.DOG_CAGE) {
                        var captured=ModEntities.HAM.create(p.level(),EntitySpawnReason.COMMAND);
                        require(captured!=null,"Cannot prepare role dog cage");
                        p.level().addFreshEntity(captured);
                        xiaoshi2022.corpseorigin.item.DogCageItem.capture(actor.getMainHandItem(),p,captured);
                    }
                    require(CloneCaster.cast(actor),"Role could not use any combat skill: "+role.getId());
                    actor.getTarget().discard();actor.discard();testedRoles++;
                }
                require(testedRoles>20,"Too few combat roles exercised");
                System.out.println("CLONE_COMBAT_ROLES_TESTED="+testedRoles);
                var shooter=clone(p,"mortal",18,-15,new CompoundTag());
                var victim=target(p,18,201,-8);beamTarget=victim.getUUID();
                shooter.setTarget(victim);shooter.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(ModItems.JUQUE_TW));
                require(CloneWeaponArts.fire(shooter,shooter.getMainHandItem())>0,"Beam was not launched");
                require(CloneWeaponArts.fire(shooter,shooter.getMainHandItem())==0,"Repeated beam ignored cooldown");
                var weak=clone(p,"mortal",-22,-20,new CompoundTag(),0);
                var strong=clone(p,"mortal",-20,-20,new CompoundTag(),2300);
                weak.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(4);
                strong.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(4);
                var shortBeam=new JuQueBeamEntity(p.level(),weak).setLevel(2);
                var longBeam=new JuQueBeamEntity(p.level(),strong).setLevel(2);
                require(shortBeam.getPower()==0&&longBeam.getPower()==1,"Qi did not use the clone body's own evolution level");
                require(shortBeam.getMaxRange()==21&&longBeam.getMaxRange()==63,"Incorrect progression range endpoints");
                longBeam.setLevel(2);
                require(longBeam.getMaxRange()==63,"Repeated level assignment stacked range");
                var qiSaved=net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING,p.level().registryAccess());
                longBeam.saveWithoutId(qiSaved);
                var qiRestored=new JuQueBeamEntity(ModEntities.JUQUE_BEAM,p.level());
                qiRestored.load(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,p.level().registryAccess(),qiSaved.buildResult()));
                require(qiRestored.getPower()==1&&qiRestored.getMaxRange()==63,"Reload lost qi strength/range");
                shortBeam.setPos(-22,230,-15);longBeam.setPos(-20,230,-15);
                shortBeam.setDeltaMovement(0,0,1.5);longBeam.setDeltaMovement(0,0,1.5);
                lowQi=shortBeam.getUUID();highQi=longBeam.getUUID();
                p.level().addFreshEntity(shortBeam);p.level().addFreshEntity(longBeam);

                var a=clone(p,"xiaojingang",2,0,wings()); gourd=a.getUUID();
                a.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(ModItems.JUQUE_TW));
                require(a.isBoss(),"Role clone not promoted to boss");
                a.setActive(true);
                var b=clone(p,"kaiweinai",5,0,new CompoundTag());niunai=b.getUUID();b.setActive(true);
                require(CloneOrganEffects.hasTrait(a,"wings")&&a.canBreatheUnderwater(),"Inherited organ traits absent");
                float health=a.getHealth();
                require(!a.causeFallDamage(30,1,a.damageSources().fall())&&a.getHealth()==health,"Wings did not prevent falling damage");
                var body=a.getAttachedOrCreate(SurvivalGrowth.BODY).copy();body.putInt("organ_water:test:water",13);
                a.setAttached(SurvivalGrowth.BODY,body);
                require(a.getBodyState().getComponent().as(CharacterShellStateComponent.class).getEvolutionParts()
                        .getIntOr("organ_water:test:water",0)==13,"Runtime organ state not written to transferable body");

                var flying=clone(p,"mortal",-6,10,wings());flyer=flying.getUUID();
                flying.promoteToBoss();flying.setActive(true);flying.setNoAi(false);
                var elevated=target(p,-6,209,18);elevated.setNoGravity(true);flyingTarget=elevated.getUUID();flying.setTarget(elevated);
                var fighting=clone(p,"baixiaofei",8,12,new CompoundTag());fighter=fighting.getUUID();
                fighting.setActive(true);fighting.setNoAi(false);fighting.setTarget(target(p,8,201,16));
            });
            context.waitTicks(20);
            server.runOnServer(s->{
                var level=s.getPlayerList().getPlayers().getFirst().level();
                require(level.getEntity(lowQi)==null,"Low-strength qi exceeded its maximum range");
                require(level.getEntity(highQi) instanceof JuQueBeamEntity beam && beam.getZ()>6,"High-strength qi did not travel beyond the base range");
            });
            context.runOnClient(client->{
                JuQueBeamEntity synced=null;
                for(var entity:client.level.entitiesForRendering())if(entity.getUUID().equals(highQi)&&entity instanceof JuQueBeamEntity beam)synced=beam;
                require(synced!=null&&synced.getPower()==1,"Client did not receive qi opening strength");
            });
            context.waitTicks(100);
            server.runOnServer(s->{
                var level=s.getPlayerList().getPlayers().getFirst().level();
                require(level.getEntity(highQi)==null,"High-strength qi exceeded its bounded range");
                require(level.getEntity(beamTarget) instanceof LivingEntity victim && victim.getHealth()<2000,"Clone sword beam did not damage target");
                var flying=(CloneAvatarEntity)level.getEntity(flyer);
                require(flying!=null&&flying.getY()>203,"Winged clone never took off toward elevated target");
                var fighting=(CloneAvatarEntity)level.getEntity(fighter);
                require(fighting!=null&&!fighting.getAttachedOrCreate(ChapterScenes.ACTION).isEmpty(),"Melee starved role skill AI");
                var a=(CloneAvatarEntity)level.getEntity(gourd);
                require(a.getAttachedOrCreate(SurvivalGrowth.BODY).getIntOr("organ_water:test:water",0)==13,"Periodic sync reset organ resources");
                a.setNoGravity(true);
                var saved=net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING,level.registryAccess());
                a.saveWithoutId(saved);
                a.setNoGravity(false);
                var restored=ModEntities.CLONE_AVATAR.create(level,EntitySpawnReason.LOAD);
                require(restored!=null,"Cannot restore clone");
                restored.load(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,level.registryAccess(),saved.buildResult()));
                require(restored.isBoss()&&restored.getBodyRole().equals("xiaojingang"),"Reload lost role/boss state");
                require(!restored.isNoGravity(),"Reload retained stale flight gravity");
                require(restored.getMainHandItem().is(ModItems.JUQUE_TW),"Reload lost picked up weapon");
                require(restored.getAttachedOrCreate(SurvivalGrowth.BODY).getIntOr("organ_water:test:water",0)==13,"Reload lost organ resources");
            });
            context.waitFor(client -> client.level != null && client.level.entitiesForRendering().iterator().hasNext());
            context.runOnClient(client->{
                var list=new ArrayList<CloneAvatarEntity>();
                for(var entity:client.level.entitiesForRendering())if(entity instanceof CloneAvatarEntity c)list.add(c);
                for(UUID id:List.of(gourd,niunai)){
                    var c=list.stream().filter(e->e.getUUID().equals(id)).findFirst().orElseThrow(()->new AssertionError("Clone not tracked on client"));
                    var renderer=(xiaoshi2022.corpseorigin.client.renderer.entity.CloneAvatarRenderer)client.getEntityRenderDispatcher().getRenderer(c);
                    var state=renderer.createRenderState();renderer.extractRenderState(c,state,0);
                    require(client.getEntityRenderDispatcher().getRenderer(state)==renderer,"Render-state dispatch bypassed clone organ layers");
                    if(id.equals(gourd)){
                        require(state.getGeckolibData(xiaoshi2022.corpseorigin.client.render.layer.CloneRoleGeoLayer.SNAPSHOT_GOURD)!=null,"Gourd snapshot absent");
                        var frames=state.getGeckolibData(xiaoshi2022.corpseorigin.client.render.layer.CloneRoleGeoLayer.SNAPSHOT_ORGAN);
                        require(frames!=null&&frames.size()==2,"Custom organ snapshots absent");
                        require(state.getMainHandItemStack().is(ModItems.JUQUE_TW),"Equipped weapon not extracted for rendering");
                    }else require(Boolean.TRUE.equals(state.getGeckolibData(xiaoshi2022.corpseorigin.client.renderer.player.NiunaiXRenderData.ACTIVE)),"Niunai back mount absent");
                    var chamberId=UUID.randomUUID();
                    xiaoshi2022.corpseorigin.client.CorpseOriginClient.cloneBodyDataCache.put(chamberId,
                            new xiaoshi2022.corpseorigin.client.CorpseOriginClient.ClientCloneBody(
                                    c.getAttachedOrCreate(ChapterActorState.ROLE),c.getAttachedOrCreate(SurvivalGrowth.BODY).copy(),false,0));
                    var chamber=new net.minecraft.client.renderer.entity.state.AvatarRenderState();
                    xiaoshi2022.corpseorigin.client.render.layer.CloneRoleGeoLayer.extractForChamber(chamberId,chamber,0);
                    if(id.equals(gourd)) {
                        require(chamber.getGeckolibData(xiaoshi2022.corpseorigin.client.render.layer.CloneRoleGeoLayer.SNAPSHOT_GOURD)!=null,"Chamber gourd missing");
                        require(chamber.getGeckolibData(xiaoshi2022.corpseorigin.client.render.layer.CloneRoleGeoLayer.SNAPSHOT_ORGAN).size()==2,"Chamber custom organs missing");
                    } else require(Boolean.TRUE.equals(chamber.getGeckolibData(xiaoshi2022.corpseorigin.client.renderer.player.NiunaiXRenderData.ACTIVE)),"Chamber Niunai mount missing");
                    xiaoshi2022.corpseorigin.client.CorpseOriginClient.cloneBodyDataCache.remove(chamberId);
                }
            });
            server.runOnServer(s->{
                var p=s.getPlayerList().getPlayers().getFirst();
                for(var old:p.level().getEntitiesOfClass(CloneAvatarEntity.class,p.getBoundingBox().inflate(90)))old.discard();
                var direct=clone(p,"xiaojingang",-2.5,6,new CompoundTag());
                var inheritedParts=new CompoundTag();inheritedParts.putString(CharacterShellStateComponent.ORGAN_ROLE_KEY,"xiaojingang");
                var inherited=clone(p,"mortal",0,6,inheritedParts);
                var custom=clone(p,"kaiweinai",2.5,6,wings());
                int label=0;
                for(var display:List.of(direct,inherited,custom)) {
                    display.setActive(true);display.setYRot(0);display.yBodyRot=0;display.setYHeadRot(0);
                    display.setCustomName(net.minecraft.network.chat.Component.literal("BODY "+(++label)));
                    display.setCustomNameVisible(true);
                }
            });
            context.getInput().lookAt(0,0);
            context.waitTicks(10);
            System.out.println("ORGAN_PREVIEW="+context.takeScreenshot("clone-organs-labels"));
            context.getInput().pressKey(options -> options.keyToggleGui);
            context.waitTicks(2);
            System.out.println("ORGAN_GEOMETRY_PREVIEW="+context.takeScreenshot("clone-organs-geometry"));
            context.getInput().pressKey(options -> options.keyToggleGui);
            // Freeze real client projectile instances for visual inspection of both shared renderers.
            context.runOnClient(client->{
                client.player.setYRot(0);client.player.setXRot(0);
                var gold=new JuQueBeamEntity(ModEntities.JUQUE_BEAM,client.level);
                var blood=new BloodWingBeamEntity(ModEntities.BLOOD_WING_BEAM,client.level);
                gold.setId(-20001);blood.setId(-20002);
                gold.setLevel(1000);blood.setLevel(1000);
                gold.setPos(-1.4,202.6,5);blood.setPos(1.4,202.6,5);
                gold.setYRot(180);blood.setYRot(180);
                client.level.addEntity(gold);client.level.addEntity(blood);
            });
            context.getInput().pressKey(options -> options.keyToggleGui);
            context.waitTicks(3);
            System.out.println("CRESCENT_PREVIEW="+context.takeScreenshot("crescent-sword-qi"));
            context.getInput().pressKey(options -> options.keyToggleGui);
            // Exercise the actual player weapon entry point, including eligibility and owner power.
            context.runOnClient(client->{
                for(int id:new int[]{-20001,-20002}){
                    var preview=client.level.getEntity(id);if(preview!=null)preview.discard();
                }
            });
            server.runOnServer(s->{
                var p=s.getPlayerList().getPlayers().getFirst();
                for(var e:p.level().getEntitiesOfClass(CloneAvatarEntity.class,p.getBoundingBox().inflate(30)))e.discard();
                var sword=new ItemStack(ModItems.JUQUE_TW);
                p.setItemSlot(EquipmentSlot.MAINHAND,sword);
                xiaoshi2022.corpseorigin.character.PlayerCharacterData.get(p).setPoints(p.getUUID(),2300,2300);
                p.setYRot(0);p.setXRot(0);
                xiaoshi2022.corpseorigin.item.sword.JuQue.releaseBeamStatic(p,sword);
                var beams=p.level().getEntitiesOfClass(JuQueBeamEntity.class,p.getBoundingBox().inflate(4));
                require(beams.stream().anyMatch(b->b.getOwner()==p&&b.getPower()==1F&&b.getMaxRange()==63),
                        "Player sword did not emit strength-scaled qi through its real weapon entry point");
                // Slow this real server projectile only for the screenshot, preserving its synchronized state.
                for(var b:beams)if(b.getOwner()==p){b.setPos(0,202.6,5);b.setDeltaMovement(0,0,.02);}
            });
            context.waitTicks(4);
            context.getInput().pressKey(options -> options.keyToggleGui);
            System.out.println("PLAYER_QI_PREVIEW="+context.takeScreenshot("player-shaped-qi"));
            context.getInput().pressKey(options -> options.keyToggleGui);
            System.out.println("CLONE_GAMEPLAY_REGRESSION_PASS");
        }
    }
    private static void checkCoverage(){
        Set<String> existing=Set.of("guigun_sweep","guigun_guard","guigun_resonance","guigun_crush","wuchou_blade","wuchou_step","wusheng_twin","wusheng_cross");
        var missing=new ArrayList<String>();
        for(var role:CharacterManager.getInstance().getRegisteredCharacters())for(var skill:role.getSkills()){
            String path=skill.getId().getPath();
            if(skill.isActivatable()&&!CloneRoleSkills.supported(path)&&!existing.contains(path)&&!CloneRoleSkills.PLAYER_CONTROLS.contains(path))missing.add(role.getId()+":"+path);
        }
        require(missing.isEmpty(),"Missing role skill adapters: "+missing);
    }
}
