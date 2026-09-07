package xiaoshi2022.corpseorigin.character;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.network.CorpseNetwork;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 角色管理器 - 注册角色、分配玩家角色
 */
public class CharacterManager {

    private static CharacterManager instance;

    private final Map<String, ICharacter> registeredCharacters = new HashMap<>();

    /** 客户端缓存的当前角色ID */
    private String clientCachedCharacterId = MortalCharacter.ID;

    private CharacterManager() {
    }

    public static CharacterManager getInstance() {
        if (instance == null) {
            instance = new CharacterManager();
        }
        return instance;
    }

    /** 在模组初始化时调用，注册所有内置角色 */
    public void registerDefaults() {
        registerCharacter(MortalCharacter.getInstance());
        registerCharacter(new BaiXiaoFei());
        registerCharacter(new LongYou());
        registerCharacter(new XiaoLu());
    }

    public void registerCharacter(ICharacter character) {
        registeredCharacters.put(character.getId(), character);
        CorpseOrigin.LOGGER.debug("Registered character: {}", character.getId());
    }

    public ICharacter getCharacter(String id) {
        if (id == null || id.isEmpty()) {
            return MortalCharacter.getInstance();
        }
        return registeredCharacters.getOrDefault(id, MortalCharacter.getInstance());
    }

    public List<ICharacter> getRegisteredCharacters() {
        return new ArrayList<>(registeredCharacters.values());
    }

    // ==================== 玩家角色分配 ====================

    /**
     * 设置玩家角色（服务端）
     */
    public boolean setPlayerCharacter(Player player, String characterId) {
        if (player.level().isClientSide()) {
            return false;
        }
        ICharacter character = getCharacter(characterId);
        if (character == null || !registeredCharacters.containsKey(character.getId())) {
            return false;
        }

        ServerPlayer serverPlayer = (ServerPlayer) player;
        PlayerCharacterData data = PlayerCharacterData.get(player);
        String oldId = data.getCharacterId(player.getUUID());

        if (oldId.equals(character.getId())) {
            return true;
        }

        // 清理旧角色
        ICharacter old = getCharacter(oldId);
        old.onLose(player);

        // 应用新角色
        data.setCharacterId(player.getUUID(), character.getId());
        character.onAcquire(player);

        CorpseOrigin.LOGGER.info("Player '{}' selected character: {}",
                player.getName().getString(), character.getId());
        syncToClient(serverPlayer);
        return true;
    }

    /** 回到凡人（清除角色） */
    public boolean clearPlayerCharacter(Player player) {
        return setPlayerCharacter(player, MortalCharacter.ID);
    }

    public ICharacter getPlayerCharacter(Player player) {
        if (player.level().isClientSide()) {
            return getCharacter(clientCachedCharacterId);
        }
        PlayerCharacterData data = PlayerCharacterData.get(player);
        return getCharacter(data.getCharacterId(player.getUUID()));
    }

    public String getPlayerCharacterId(Player player) {
        if (player.level().isClientSide()) {
            return clientCachedCharacterId;
        }
        return PlayerCharacterData.get(player).getCharacterId(player.getUUID());
    }

    public boolean isMortal(Player player) {
        return MortalCharacter.ID.equals(getPlayerCharacterId(player));
    }

    // ==================== 客户端缓存 ====================

    public void setClientCachedCharacter(String characterId) {
        this.clientCachedCharacterId = characterId != null ? characterId : MortalCharacter.ID;
    }

    public String getClientCachedCharacterId() {
        return clientCachedCharacterId;
    }

    public ICharacter getClientCachedCharacter() {
        return getCharacter(clientCachedCharacterId);
    }

    // ==================== 同步 ====================

    public void syncToClient(ServerPlayer player) {
        String characterId = getPlayerCharacterId(player);
        CorpseNetwork.sendCharacterSync(player, characterId);
        // TODO: 后续恢复技能和感染同步
    }
}