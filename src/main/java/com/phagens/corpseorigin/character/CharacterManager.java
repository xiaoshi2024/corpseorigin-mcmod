package com.phagens.corpseorigin.character;

import com.phagens.corpseorigin.CorpseOrigin;
import com.phagens.corpseorigin.network.CharacterSyncPacket;
import com.phagens.corpseorigin.skill.ISkill;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CharacterManager {

    private static CharacterManager instance;

    private final Map<String, ICharacter> registeredCharacters = new HashMap<>();

    private String clientCachedCharacterId = MortalCharacter.ID;

    private CharacterManager() {
        registerCharacter(MortalCharacter.getInstance());
    }

    public static CharacterManager getInstance() {
        if (instance == null) {
            instance = new CharacterManager();
        }
        return instance;
    }

    public void registerCharacter(ICharacter character) {
        registeredCharacters.put(character.getId(), character);
        CorpseOrigin.LOGGER.debug("Registered character: {}", character.getId());
    }

    public ICharacter getCharacter(String id) {
        return registeredCharacters.get(id);
    }

    public List<ICharacter> getRegisteredCharacters() {
        return new ArrayList<>(registeredCharacters.values());
    }

    public void setPlayerCharacter(Player player, ICharacter character) {
        if (player.level().isClientSide()) {
            return;
        }

        String characterId = character != null ? character.getId() : MortalCharacter.ID;
        ServerPlayer serverPlayer = (ServerPlayer) player;

        PlayerCharacterData dataManager = getDataManager(serverPlayer);
        dataManager.setPlayerCharacter(player.getUUID(), characterId);

        syncToClient(serverPlayer, characterId);

        CorpseOrigin.LOGGER.debug("Set player '{}' character to {}", player.getName().getString(), characterId);
    }

    public void clearPlayerCharacter(Player player) {
        if (player.level().isClientSide()) {
            return;
        }

        ServerPlayer serverPlayer = (ServerPlayer) player;
        PlayerCharacterData dataManager = getDataManager(serverPlayer);
        dataManager.clearPlayerCharacter(player.getUUID());

        syncToClient(serverPlayer, MortalCharacter.ID);

        CorpseOrigin.LOGGER.debug("Cleared character for player {}", player.getUUID());
    }

    public ICharacter getPlayerCharacter(Player player) {
        if (player.level().isClientSide()) {
            ICharacter character = registeredCharacters.getOrDefault(
                    clientCachedCharacterId, MortalCharacter.getInstance()
            );
            return character;
        }

        PlayerCharacterData dataManager = getDataManager((ServerPlayer) player);
        String characterId = dataManager.getPlayerCharacterId(player.getUUID());
        return registeredCharacters.getOrDefault(characterId, MortalCharacter.getInstance());
    }

    public String getPlayerCharacterId(Player player) {
        if (player.level().isClientSide()) {
            return clientCachedCharacterId;
        }
        PlayerCharacterData dataManager = getDataManager((ServerPlayer) player);
        return dataManager.getPlayerCharacterId(player.getUUID());
    }

    public boolean hasCharacter(Player player) {
        if (player.level().isClientSide()) {
            return !MortalCharacter.ID.equals(clientCachedCharacterId);
        }
        PlayerCharacterData dataManager = getDataManager((ServerPlayer) player);
        return dataManager.hasCharacter(player.getUUID());
    }

    public boolean isMortal(Player player) {
        return MortalCharacter.ID.equals(getPlayerCharacterId(player));
    }

    public List<ISkill> getPlayerSkills(Player player) {
        ICharacter character = getPlayerCharacter(player);
        if (character == null || character.isPassive()) {
            return new ArrayList<>();
        }
        return character.getSkills();
    }

    public void setClientCachedCharacter(String characterId) {
        this.clientCachedCharacterId = characterId != null ? characterId : MortalCharacter.ID;
        CorpseOrigin.LOGGER.debug("Client cached character updated: {}", clientCachedCharacterId);
    }

    public String getClientCachedCharacterId() {
        return clientCachedCharacterId;
    }

    private PlayerCharacterData getDataManager(ServerPlayer player) {
        return PlayerCharacterData.getInstance(player.server.overworld());
    }

    private void syncToClient(ServerPlayer player, String characterId) {
        PacketDistributor.sendToPlayer(player, new CharacterSyncPacket(player.getId(), characterId));
    }
}