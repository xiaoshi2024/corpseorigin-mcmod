package xiaoshi2022.corpseorigin.client;

import xiaoshi2022.corpseorigin.character.CharacterManager;

/**
 * 客户端桥接：更新客户端缓存角色
 */
final class CharacterManagerBridge {

    private CharacterManagerBridge() {
    }

    static void setCharacter(String characterId) {
        ClientState.characterId = characterId;
        CharacterManager.getInstance().setClientCachedCharacter(characterId);
        // 角色变化时清空冷却显示
        ClientState.cooldownEnds.clear();
    }
}
