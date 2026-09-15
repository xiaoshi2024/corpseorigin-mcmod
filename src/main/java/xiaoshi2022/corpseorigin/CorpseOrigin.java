package xiaoshi2022.corpseorigin;

import com.mojang.datafixers.util.Either;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xiaoshi2022.corpseorigin.character.CharacterManager;
import xiaoshi2022.corpseorigin.command.CharacterCommands;
import xiaoshi2022.corpseorigin.command.SummonZbCommand;
import xiaoshi2022.corpseorigin.event.*;
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
		// ⚠️ 重要：先注册效果
		ModEffects.init();

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

		// ✅ 6. 实体
		ModEntities.init();

		// ✅ 7. 实体属性
		ModAttributes.register();

		// ✅ 8. 角色系统
		CharacterManager.getInstance().registerDefaults();

		// ✅ 注册 DataAttachment（必须在网络之前）
		ModDataAttachments.init();

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

		// ✅ 9. 网络
		CorpseNetwork.register();

		// ✅ 10. 事件
		ByWaterEventHandler.register();
		ServerEvents.register();
		ZombieKinEventHandler.register();
		HeiXiaoFeiEventHandler.register();
		EvolutionEventHandler.register();
		APSComboHandler.register();
		APSGreatSwordInterceptor.register();

		// ✅ 11. 命令
		net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT.register(
				(dispatcher, registryAccess, environment) -> {
					CharacterCommands.register(dispatcher);
					SummonZbCommand.register(dispatcher);
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

					Either<ShellState, String> result = shell.syncFromDeath(nearest);
					if (result.right().isPresent()) {
						return true;   // 夺舍失败，正常死亡
					}

					player.clearFire();
					player.removeAllEffects();
					player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
					CorpseNetwork.refreshShellStates(player);
					ServerPlayNetworking.send(player,
							SynchronizationResponsePacket.message(true, "意识已转移至最近的克隆体"));
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