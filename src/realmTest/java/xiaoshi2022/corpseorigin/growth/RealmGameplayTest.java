package xiaoshi2022.corpseorigin.growth;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameType;
import xiaoshi2022.corpseorigin.character.*;
import xiaoshi2022.corpseorigin.skill.*;
import xiaoshi2022.corpseorigin.network.CorpseNetwork;
import xiaoshi2022.corpseorigin.event.EvolutionEventHandler;

public final class RealmGameplayTest implements FabricClientGameTest {
    private static void check(boolean condition,String label){if(!condition)throw new AssertionError(label);}
    @Override public void runTest(ClientGameTestContext context) {
        try(var world=context.worldBuilder().create()) {
            var server=world.getServer();
            context.waitTicks(80); // Vanilla spawn invulnerability must expire before testing damage.
            server.runOnServer(s->{
                ServerPlayer p=s.getPlayerList().getPlayers().getFirst();
                p.setGameMode(GameType.SURVIVAL);
                CharacterManager.getInstance().setPlayerCharacter(p,"mortal");
                var data=PlayerCharacterData.get(p); var id=p.getUUID();
                var legacy=new CompoundTag();legacy.putString("CharacterId","mortal");legacy.putInt("Earned",950);legacy.putInt("Available",900);
                data.readNbt(id,legacy);
                check(RealmProgression.level(p)==15,"legacy EX preserved");
                check(data.getAvailablePoints(id)==900,"migration does not mint spendable points");
                int earned=data.getEarnedPoints(id);
                data.readNbt(id,data.writeNbt(id));
                check(data.getEarnedPoints(id)==earned,"migration idempotent through shell restore");
                EvolutionStats.reconcile(p);
                check(p.getMaxHealth()>9e7,"health range is actually extended");
                check(p.getAttributeValue(Attributes.ATTACK_DAMAGE)>=1e6,"actual attack attribute");
                check(InnerPowerManager.getMaxInnerPower(p)>=1000000,"qi capacity");
                float maxHealth=p.getMaxHealth();EvolutionStats.reconcile(p);EvolutionStats.reconcile(p);
                check(p.getMaxHealth()==maxHealth,"reconcile does not stack modifiers");
                p.setHealth(maxHealth);p.invulnerableTime=0;
                p.hurtServer(p.level(),p.damageSources().magic(),10000000);
                check(p.getHealth()<maxHealth && p.getHealth()>maxHealth*.98,"realm protection really reduces incoming damage: before="+maxHealth+", after="+p.getHealth());
                check(RealmProgression.purchase(p,"power",10)==1,"batch purchase");
                check(data.getAvailablePoints(id)==710,"arithmetic-series price 190");
                check(data.getEarnedPoints(id)==earned,"purchase preserves tier progress");
                check(RealmProgression.rank(p,"power")==10,"training rank applied");
                check(RealmProgression.purchase(p,"power",1000)==0,"insufficient balance rejected");
                check(!data.spendPoints(id,-1) && data.getAvailablePoints(id)==710,"negative purchase cannot mint points");
                var snapshot=data.writeNbt(id);
                data.setCultivation(id,new CompoundTag());data.readNbt(id,snapshot);
                check(RealmProgression.rank(p,"power")==10,"training persisted with shell ledger");
                var cow=EntityTypes.COW.create(p.level(),EntitySpawnReason.COMMAND);
                check(cow!=null,"test target");cow.setNoAi(true);cow.setPos(p.position().add(2,0,0));
                cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1e7);cow.setHealth(1e7f);p.level().addFreshEntity(cow);
                cow.hurtServer(p.level(),p.damageSources().playerAttack(p),20);
                check(cow.getHealth()<9.1e6f,"fixed damage passes through realm mixin");
                check(data.cultivation(id).getLongOr("xp_power",0)>0,"combat really awards practice");
                cow.discard();
                long xp=data.cultivation(id).getLongOr("xp_vitality",0);
                RealmProgression.onFlesh(p);
                check(data.cultivation(id).getLongOr("xp_vitality",0)==xp+RealmProgression.config().fleshPracticeXp,"food practice entry");
                p.setHealth(p.getMaxHealth()); InnerPowerManager.reset(p);
                if(xiaoshi2022.corpseorigin.skill.longyou.BloodReserve.isEligible(p))xiaoshi2022.corpseorigin.skill.longyou.BloodReserve.add(p,600);
                int balance=data.getAvailablePoints(id);
                check(RealmProgression.recharge(p)==0 && data.getAvailablePoints(id)==balance,"full-state recharge is free no-op");
                p.setHealth(p.getMaxHealth()*.5f);
                check(RealmProgression.recharge(p)==1 && data.getAvailablePoints(id)==balance-RealmProgression.config().rechargePointCost,"repeatable point sink");
                data.setPoints(id,Integer.MAX_VALUE,10);
                check(EvolutionEventHandler.awardPoints(p,5)==5 && data.getAvailablePoints(id)==15,"can earn after progress saturation");
                data.setPoints(id,earned,5000);EvolutionStats.reconcile(p);
                CorpseNetwork.sendEvolutionSync(p);
                RealmNetworking.send(p,false);
            });
            context.waitTicks(20);
            context.runOnClient(client->{
                check(xiaoshi2022.corpseorigin.client.ClientState.evolutionLevel==15,"server-authoritative tier sync");
                check(client.player.getMaxHealth()>9e7,"large health attribute reaches client");
                client.gui.setScreen(null);
                client.gui.hud.getChat().clearMessages(true);
            });
            context.waitTicks(5);
            System.out.println("REALM_HEALTH_SCREENSHOT="+context.takeScreenshot("realm-ex-health"));
            context.runOnClient(client->xiaoshi2022.corpseorigin.client.RealmGrowthScreen.open());
            context.waitTicks(10);
            context.runOnClient(client->check(client.gui.screen() instanceof xiaoshi2022.corpseorigin.client.RealmGrowthScreen,"growth menu opens via packet"));
            System.out.println("REALM_MENU_SCREENSHOT="+context.takeScreenshot("realm-growth-menu"));
            context.runOnClient(client->client.gui.setScreen(null));
            server.runOnServer(s->{
                ServerPlayer p=s.getPlayerList().getPlayers().getFirst();
                InnerPowerManager.reset(p);
                var data=PlayerCharacterData.get(p); int before=data.getEarnedPoints(p.getUUID());
                p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.AMETHYST_SHARD,2));
                check(RealmProgression.refine(p,"qi")==1,"material refinement succeeds");
                check(p.getMainHandItem().getCount()==1,"material consumed exactly once");
                check(data.cultivation(p.getUUID()).getIntOr("job_qi",0)==5,"separate profession xp");
                check(data.getEarnedPoints(p.getUUID())==before,"profession does not inflate realm");
                check(RealmProgression.refine(p,"qi")==0 && p.getMainHandItem().getCount()==1,"refinement cooldown prevents double consumption");
                check(RealmProgression.burst(p)==1,"EX signature ability");
                int qi=InnerPowerManager.getInnerPower(p);
                check(RealmProgression.burst(p)==0 && InnerPowerManager.getInnerPower(p)==qi,"burst cooldown does not charge twice");
                p.setShiftKeyDown(true);p.setOnGround(true);p.getFoodData().setFoodLevel(20);
                RealmProgression.config().meditationTicks=40;
                check(RealmProgression.meditate(p)==1,"meditation starts");
            });
            context.waitTicks(85);
            server.runOnServer(s->{
                var p=s.getPlayerList().getPlayers().getFirst();
                check(PlayerCharacterData.get(p).cultivation(p.getUUID()).getLongOr("xp_recovery",0)>0,"meditation awards independent recovery practice");
                RealmProgression.config().meditationTicks=200;
                p.setShiftKeyDown(false);
            });
            server.runOnServer(s->{
                var p=s.getPlayerList().getPlayers().getFirst();
                var data=PlayerCharacterData.get(p);
                data.learnSkill(p.getUUID(),"flame_strike");
                data.learnSkill(p.getUUID(),"ancient_poetry_sword");
                InnerPowerManager.reset(p);
                SkillManager.restoreRemaining(p,java.util.Map.of());
                check(SkillManager.activate(p,"flame_strike"),"ordinary cast succeeds");
                int remaining=SkillManager.snapshotRemaining(p).getOrDefault("flame_strike",0);
                check(remaining>0 && remaining<=44,"EX ordinary duration actually shortened");
                int qi=InnerPowerManager.getInnerPower(p);
                check(!SkillManager.activate(p,"flame_strike") && InnerPowerManager.getInnerPower(p)==qi,"cooldown rejects before payment");
                SkillManager.restoreRemaining(p,java.util.Map.of("flame_strike",100));
                check(SkillManager.snapshotRemaining(p).get("flame_strike")>90,"restoring cooldown never applies reduction twice");
                var poetry=new xiaoshi2022.corpseorigin.skill.baixiaofei.AncientPoetrySwordSkill();
                var target=EntityTypes.COW.create(p.level(),EntitySpawnReason.COMMAND);
                check(target!=null,"qi test target exists");
                target.setNoAi(true);target.setPos(p.position().add(2,0,0));
                target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1e9);target.setHealth(1e9f);p.level().addFreshEntity(target);
                var normal=p.damageSources().playerAttack(p);
                var qiSource=QiSkillDamageSource.wrap(normal);
                check(qiSource.getEntity()==p && qiSource.getDirectEntity()==p && qiSource.typeHolder().equals(normal.typeHolder()),"qi marker preserves attribution and type");
                check(QiSkillDamageSource.wrap(qiSource)==qiSource,"marker cannot stack");
                var saved=data.cultivation(p.getUUID());
                var trained=saved.copy();trained.putInt("rank_qi",400);data.setCultivation(p.getUUID(),trained);
                double factor=RealmRules.qiSkillMultiplier(InnerPowerManager.getMaxInnerPower(p),RealmProgression.level(p),RealmProgression.config());
                check(factor>2,"qi training improves offensive output");
                float normalDamage=RealmProgression.adjustDamage(target,normal,20);
                float boostedDamage=RealmProgression.adjustDamage(target,qiSource,20);
                check(Math.abs(boostedDamage/normalDamage-factor)<.0001,"qi marked hits multiply once");
                InnerPowerManager.set(p,1);
                check(RealmProgression.adjustDamage(target,qiSource,20)==boostedDamage,"spent qi does not reduce skill potency");
                target.hurtServer(p.level(),qiSource,20);
                check(Math.abs((1e9f-target.getHealth())-boostedDamage)<128,"actual damage pipeline receives qi bonus");
                data.setCultivation(p.getUUID(),saved);
                check(RealmProgression.adjustDamage(target,normal,20)==normalDamage,"qi investment leaves ordinary melee unchanged");
                target.discard();
                var ultimate=new xiaoshi2022.corpseorigin.skill.fengmohuitailang.TenguDivineArraySkill();
                check(poetry.hasFixedCooldown() && ultimate.hasFixedCooldown(),"poetry and ultimates excluded");
                check(SkillBalance.cooldown(p,ultimate)==ultimate.getCooldownTicks(),"ultimate base cooldown retained");
                check(SkillBalance.cost(p,ultimate).inner()>=InnerPowerManager.getMaxInnerPower(p)*.05,"ultimate percentage cost");
                InnerPowerManager.set(p,1);
                check(!SkillManager.activate(p,"ancient_poetry_sword") && !poetry.isRunning(p),"insufficient qi cannot start poetry");
                check(!SkillManager.snapshotRemaining(p).containsKey("ancient_poetry_sword"),"failed start has no cooldown");
                InnerPowerManager.reset(p);
                int start=InnerPowerManager.getInnerPower(p), price=SkillBalance.cost(p,poetry).inner();
                check(SkillManager.activate(p,"ancient_poetry_sword"),"poetry starts");
                check(InnerPowerManager.getInnerPower(p)==start-price,"poetry charges exact percentage once");
                check(SkillManager.snapshotRemaining(p).get("ancient_poetry_sword")>1190,"poetry keeps sixty second cooldown at EX");
                check(SkillManager.activate(p,"ancient_poetry_sword") && !poetry.isRunning(p),"free cancellation works during cooldown");
                check(InnerPowerManager.getInnerPower(p)==start-price,"cancellation free");
                check(!SkillManager.activate(p,"ancient_poetry_sword"),"cancel does not reset cooldown");
                for(int stage=0;stage<=5;stage++) {
                    int priceNow=SkillBalance.poetryCost(p,stage).inner();
                    check(priceNow>=10000,"all poetry actions scale with large capacity");
                    InnerPowerManager.reset(p);
                    int old=InnerPowerManager.getInnerPower(p);
                    check(SkillResources.pay(p,SkillBalance.poetryCost(p,stage)) && InnerPowerManager.getInnerPower(p)==old-priceNow,"stage payment exact");
                }
            });
            context.waitTicks(5);
            context.runOnClient(client->{
                var durations=xiaoshi2022.corpseorigin.client.ClientState.cooldownDurations;
                check(durations.getOrDefault("flame_strike",0)==44,"client receives actual ordinary duration");
                check(durations.getOrDefault("ancient_poetry_sword",0)==1200,"client receives full poetry duration");
            });
            System.out.println("RealmGameplayTest passed: migration, actual attributes/damage, spending, persistence, practice, currency saturation, sync and HUD.");
        }
    }
}
