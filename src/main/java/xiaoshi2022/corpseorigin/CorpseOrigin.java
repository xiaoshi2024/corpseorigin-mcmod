package xiaoshi2022.corpseorigin;

import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xiaoshi2022.corpseorigin.character.CharacterManager;
import xiaoshi2022.corpseorigin.command.CharacterCommands;
import xiaoshi2022.corpseorigin.event.ServerEvents;
import xiaoshi2022.corpseorigin.network.CorpseNetwork;
import xiaoshi2022.corpseorigin.registry.ModEntities;

public class CorpseOrigin implements ModInitializer {
	public static final String MOD_ID = "corpseorigin";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// 1. 物品
//		ModItems.init();

//		// 2. 注册创造物品栏
//		Registry.register(
//				BuiltInRegistries.CREATIVE_MODE_TAB,
//				id("main"),
//				ModItems.CORPSE_ORIGIN_TAB
//		);

		// 3. 角色系统
		CharacterManager.getInstance().registerDefaults();

		// 4. 实体（空实现）
		ModEntities.init();

		// 5. 网络
		CorpseNetwork.register();

		// 6. 事件
		ServerEvents.register();

		// 7. 命令
		net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT.register(
				(dispatcher, registryAccess, environment) -> CharacterCommands.register(dispatcher));

		LOGGER.info("CorpseOrigin (Fabric 26.2) initialized - 角色选择系统");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}