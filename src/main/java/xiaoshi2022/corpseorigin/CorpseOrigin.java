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
import xiaoshi2022.corpseorigin.event.ServerEvents;
import xiaoshi2022.corpseorigin.network.CorpseNetwork;
import xiaoshi2022.corpseorigin.registry.ModAttributes;
import xiaoshi2022.corpseorigin.registry.ModEntities;

public class CorpseOrigin implements ModInitializer {
	public static final String MOD_ID = "corpseorigin";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
//		// 1. 物品
//		ModItems.init();
//
//		// 2. 注册创造物品栏
//		Registry.register(
//				BuiltInRegistries.CREATIVE_MODE_TAB,
//				id("main"),
//				ModItems.CORPSE_ORIGIN_TAB
//		);

		// 3. 实体
		ModEntities.init();

		// 4. 实体属性
		ModAttributes.register();

		// 5. 角色系统
		CharacterManager.getInstance().registerDefaults();

		// 6. 网络
		CorpseNetwork.register();

		// 7. 事件
		ServerEvents.register();

		// 8. 命令
		net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT.register(
				(dispatcher, registryAccess, environment) -> {
					CharacterCommands.register(dispatcher);
					SummonZbCommand.register(dispatcher);  // ✅ 添加召唤尸兄命令
				}
		);

		LOGGER.info("CorpseOrigin (Fabric 26.2) initialized");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}