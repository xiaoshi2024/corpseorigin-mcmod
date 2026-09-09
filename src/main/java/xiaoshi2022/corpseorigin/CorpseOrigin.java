package xiaoshi2022.corpseorigin;

import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xiaoshi2022.corpseorigin.character.CharacterManager;
import xiaoshi2022.corpseorigin.command.CharacterCommands;
import xiaoshi2022.corpseorigin.command.SummonZbCommand;
import xiaoshi2022.corpseorigin.event.ByWaterEventHandler;
import xiaoshi2022.corpseorigin.event.ServerEvents;
import xiaoshi2022.corpseorigin.network.CorpseNetwork;
import xiaoshi2022.corpseorigin.registry.ModAttributes;
import xiaoshi2022.corpseorigin.registry.ModBlocks;
import xiaoshi2022.corpseorigin.registry.ModEffects;
import xiaoshi2022.corpseorigin.registry.ModEntities;
import xiaoshi2022.corpseorigin.registry.ModFluids;
import xiaoshi2022.corpseorigin.registry.ModItems;

public class CorpseOrigin implements ModInitializer {
	public static final String MOD_ID = "corpseorigin";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// ⚠️ 重要：先注册效果
		ModEffects.init();

		// ✅ 1. 注册流体
		ModFluids.init();

		// ✅ 2. 注册方块
		ModBlocks.init();

		// ✅ 3. 注册物品
		ModItems.init();

		// ✅ 4. 注册创造物品栏
		Registry.register(
				BuiltInRegistries.CREATIVE_MODE_TAB,
				id("main"),
				ModItems.CORPSE_ORIGIN_TAB
		);

		// ✅ 5. 实体
		ModEntities.init();

		// ✅ 6. 实体属性
		ModAttributes.register();

		// ✅ 7. 角色系统
		CharacterManager.getInstance().registerDefaults();

		// ✅ 8. 网络
		CorpseNetwork.register();

		// ✅ 9. 事件
		ByWaterEventHandler.register();

		ServerEvents.register();

		// ✅ 10. 命令
		net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT.register(
				(dispatcher, registryAccess, environment) -> {
					CharacterCommands.register(dispatcher);
					SummonZbCommand.register(dispatcher);
				}
		);

		LOGGER.info("CorpseOrigin (Fabric 26.2) initialized");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}