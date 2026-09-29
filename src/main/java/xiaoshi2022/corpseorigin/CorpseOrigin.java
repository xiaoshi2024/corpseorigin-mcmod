package xiaoshi2022.corpseorigin;

import com.mojang.datafixers.util.Either;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xiaoshi2022.corpseorigin.character.CharacterManager;
import xiaoshi2022.corpseorigin.command.CharacterCommands;
import xiaoshi2022.corpseorigin.command.LimbCommand;
import xiaoshi2022.corpseorigin.command.SkillCommand;
import xiaoshi2022.corpseorigin.command.SummonZbCommand;
import xiaoshi2022.corpseorigin.event.*;
import xiaoshi2022.corpseorigin.limb.LimbEvents;
import xiaoshi2022.corpseorigin.network.CorpseNetwork;
import xiaoshi2022.corpseorigin.network.SynchronizationResponsePacket;
import xiaoshi2022.corpseorigin.registry.*;
import xiaoshi2022.corpseorigin.shell.*;
import xiaoshi2022.corpseorigin.skill.baixiaofei.APSComboHandler;

public class CorpseOrigin implements ModInitializer {
	public static final String MOD_ID = "corpseorigin";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// ✅ 0. 配置文件（config/corpseorigin.json）—— 不存在就生成一份默认的，
		//    后面刷怪权重、名字名单、皮肤染色强度都从它读，所以必须最先加载
		xiaoshi2022.corpseorigin.config.CorpseConfig.get();
        xiaoshi2022.corpseorigin.growth.RealmProgression.initialize();
        xiaoshi2022.corpseorigin.growth.ThermalSurvey.initialize();
        xiaoshi2022.corpseorigin.growth.RuinLoot.initialize();

		// ⚠️ 重要：先注册效果
		ModEffects.init();

		// ✅ 自定义音效（尸兄"吃~~"等）
		ModSounds.init();

		// ✅ 成就触发器：必须在数据包加载前注册，否则进度 JSON 会因为"未知的触发器"加载失败
		xiaoshi2022.corpseorigin.advancement.CorpseAdvancements.init();

		// ✅ 1. 注册数据组件（必须在物品之前）
		ModDataComponents.init();

		// ✅ 2. 注册流体
		ModFluids.init();

		// ✅ 3. 注册方块
		ModBlocks.init();

		// ✅ 3.5 注册方块实体（必须在方块之后、物品之前）
		ModBlockEntities.init();

		// ✅ 4. 注册物品
		ModItems.init();

		// ✅ 5. 注册创造物品栏
		Registry.register(
				BuiltInRegistries.CREATIVE_MODE_TAB,
				id("main"),
				ModItems.CORPSE_ORIGIN_TAB
		);

		// ✅ 5.1 角色选择书页签（带搜索框）
		Registry.register(
				BuiltInRegistries.CREATIVE_MODE_TAB,
				id("character_books"),
				ModItems.CHARACTER_BOOK_TAB
		);
		Registry.register(
				BuiltInRegistries.CREATIVE_MODE_TAB,
				id("skill_books"),
				ModItems.SKILL_BOOK_TAB
		);

		// ✅ 6. 实体
		ModEntities.init();

		// ✅ 7. 实体属性
		ModAttributes.register();

		// ✅ 7.5 自然生成（生成规则 + 进生物群系生成表）
		ModSpawns.register();
        xiaoshi2022.corpseorigin.event.CorpseWormSpawns.register();

		// ✅ 7.6 地形生成（尸水泉）
		ModWorldGen.register();

		// ✅ 8. 角色系统
		CharacterManager.getInstance().registerDefaults();
		xiaoshi2022.corpseorigin.item.SkillBookItem.registerAll();

		// ✅ 注册 DataAttachment（必须在网络之前）
		ModDataAttachments.init();
		xiaoshi2022.corpseorigin.event.ConsciousnessInteractions.register();
		xiaoshi2022.corpseorigin.skill.longyou.BloodReserve.init();
		xiaoshi2022.corpseorigin.skill.longyou.JingangInfantLink.register();
		xiaoshi2022.corpseorigin.skill.longyou.BodyPossession.init();

		ShellStateComponentRegistry.getInstance().register(
				CorpseShellStateComponent::new,
				CorpseShellStateComponent::new
		);
		ShellStateComponentRegistry.getInstance().register(
				CharacterShellStateComponent::new,
				CharacterShellStateComponent::new
		);
		ShellStateComponentRegistry.getInstance().register(
				ApsShellStateComponent::new,
				ApsShellStateComponent::new
		);
		ShellStateComponentRegistry.getInstance().register(
				SkillCooldownShellStateComponent::new,
				SkillCooldownShellStateComponent::new
		);
		ShellStateComponentRegistry.getInstance().register(
				InnerPowerShellStateComponent::new,
				InnerPowerShellStateComponent::new
		);

		// ✅ 9. 网络
		CorpseNetwork.register();
        xiaoshi2022.corpseorigin.growth.OrganNetwork.register();
        xiaoshi2022.corpseorigin.skill.heixiaofei.HeartImplant.register();
        xiaoshi2022.corpseorigin.skill.zhaoritian.CrimsonBloodSpearSkill.register();
        xiaoshi2022.corpseorigin.skill.zhaoritian.TianGangKeySkill.register();
        xiaoshi2022.corpseorigin.skill.longyou.CorpseNestLighting.register();
        xiaoshi2022.corpseorigin.item.SagentItem.register();

		// ✅ 10. 事件
		ByWaterEventHandler.register();
		ServerEvents.register();
        xiaoshi2022.corpseorigin.skill.chapter.FiveElementsCombat.register();
        xiaoshi2022.corpseorigin.skill.chapter.SkillRework.register();
        xiaoshi2022.corpseorigin.skill.chapter.GuigunCombat.register();
        xiaoshi2022.corpseorigin.skill.chapter.FlameSea.register();
        xiaoshi2022.corpseorigin.skill.chapter.WuchangCombat.register();
        xiaoshi2022.corpseorigin.skill.chapter.CloneCaster.register();
        xiaoshi2022.corpseorigin.skill.chapter.ImpactTerrain.register();
        xiaoshi2022.corpseorigin.skill.chapter.SwordRift.register();
        xiaoshi2022.corpseorigin.skill.chapter.BodySkillState.register();
        xiaoshi2022.corpseorigin.skill.longyou.TianGangCombat.register();
        FishEggInteraction.register();
        xiaoshi2022.corpseorigin.skill.chapter.ChapterScenes.register();
        xiaoshi2022.corpseorigin.skill.chapter.ChapterActorState.register();
        xiaoshi2022.corpseorigin.skill.chapter.CreatureAbilities.register();
		// ⚠️ 天线宝宝必须排在其他 ALLOW_DAMAGE 监听器之前：
		//   Fabric 的 ALLOW_DAMAGE 一旦有人 return false 就会中断后续监听器，
		//   而"穿戴套装的生物攻击时发动吸食"挂在这个事件上 ——
		//   排在尸族规则后面的话，尸兄互伤被拦掉时吸食连着动画一起没了。
		//   它自己永远 return true，不取消任何人，排最前面没有副作用。
		TianXianBaoBaoEventHandler.register();
		ZombieKinEventHandler.register();
		HeiXiaoFeiEventHandler.register();
		LongYouEventHandler.register();
		EvolutionEventHandler.register();
        xiaoshi2022.corpseorigin.growth.SurvivalGrowth.register();
        xiaoshi2022.corpseorigin.character.CharacterBookPolicy.init();
        xiaoshi2022.corpseorigin.growth.GourdOrganState.register();
        xiaoshi2022.corpseorigin.skill.chapter.GourdCapture.register();
        xiaoshi2022.corpseorigin.skill.chapter.GourdInheritance.register();
        xiaoshi2022.corpseorigin.growth.WeaponEligibility.register();
		xiaoshi2022.corpseorigin.event.GuardianPetDeathHandler.register();
		APSComboHandler.register();
		APSGreatSwordInterceptor.register();
		LimbEvents.register();
		// ★ 开胃奶菊花盾（正面格挡 + 反弹箭矢）排在这一组最后：
		//   它会 return false 直接取消伤害，而 ALLOW_DAMAGE 一旦被取消就中断后续监听器 ——
		//   排在前面的话，被盾挡下的那一下会让后面的规则（断肢、尸族互伤之类）整个看不到。
		//   盾本来就是要"整下挡掉"，所以让它在最后收尾。
		KaiWeiNaiEventHandler.register();
		SkillUnlockEvents.register();

		// ✅ 11. 命令
		net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT.register(
				(dispatcher, registryAccess, environment) -> {
					CharacterCommands.register(dispatcher);
				SummonZbCommand.register(dispatcher);
				LimbCommand.register(dispatcher);
				SkillCommand.register(dispatcher);
				xiaoshi2022.corpseorigin.command.EvolutionPointsCommand.register(dispatcher);
				xiaoshi2022.corpseorigin.command.TenguLaserCommand.register(dispatcher);
				xiaoshi2022.corpseorigin.command.ChessZbCommand.register(dispatcher);
				}
		);

		net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.ALLOW_DEATH.register(
				(entity, source, amount) -> {
					if (!(entity instanceof ServerPlayer player)) {
						return true;
					}

					// 找备用身体
					ServerShell shell = ServerShell.of(player);
					TransferredBody nearest = shell.findNearestBody();
					if (nearest == null) {
						return true;   // 没有备用身体，正常死亡
					}

					// ★ 先取一次快照：下面的夺舍会把目标身体消耗掉（分身被移除、快照清空），
					//   而过场动画要用它来算出"从哪里飞到哪里"
					ShellState deathTarget = nearest.snapshot();

					Either<ShellState, Component> result = shell.syncFromDeath(nearest);
					if (result.right().isPresent()) {
						return true;   // 夺舍失败，正常死亡
					}

					player.clearFire();
					player.removeAllEffects();
					player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
					CorpseNetwork.refreshShellStates(player);

					// ★ 灵魂出窍过场：这里服务端已经换完身体了，动画纯粹是给玩家看的
					if (deathTarget != null && deathTarget.getPos() != null) {
						Identifier toWorld = deathTarget.getWorld() != null
								? deathTarget.getWorld()
								: player.level().dimension().identifier();
						ServerPlayNetworking.send(player, new SynchronizationResponsePacket(
								true, true, SynchronizationResponsePacket.CameraStyle.STAIRWAY,
								Component.translatable("message.corpseorigin.transfer.complete"),
								deathTarget.getUuid(),
								player.level().dimension().identifier(), player.blockPosition(),
								player.getDirection(),
								toWorld, deathTarget.getPos(),
								net.minecraft.core.Direction.NORTH));
					} else {
						ServerPlayNetworking.send(player,
								SynchronizationResponsePacket.message(true, Component.translatable("message.corpseorigin.transfer.complete")));
					}
					return false;   // 拦截死亡
				});

		net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerLevel level : server.getAllLevels()) {
				xiaoshi2022.corpseorigin.skill.baixiaofei.aps.APSTerrainManager.tick(level);
			}
		});

		net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			xiaoshi2022.corpseorigin.block.entity.CloneChamberBlockEntity.REGISTRY.clear();
		});

		net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayer player = handler.getPlayer();
			xiaoshi2022.corpseorigin.skill.baixiaofei.aps.APSTerrainManager.onPlayerLogin(player, player.level());
		});

		net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			for (ServerLevel level : server.getAllLevels()) {
				xiaoshi2022.corpseorigin.skill.baixiaofei.aps.APSTerrainManager.saveAllSnapshots(level);
			}
			xiaoshi2022.corpseorigin.skill.baixiaofei.aps.APSTerrainManager.waitForAllSaves();
		});

		LOGGER.debug("CorpseOrigin (Fabric 26.2) initialized");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
