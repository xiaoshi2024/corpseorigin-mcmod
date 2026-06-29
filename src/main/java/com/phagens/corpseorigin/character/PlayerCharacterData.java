package com.phagens.corpseorigin.character;

import com.phagens.corpseorigin.CorpseOrigin;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerCharacterData extends SavedData {

    private static final String DATA_NAME = CorpseOrigin.MODID + "_character_data";

    private final Map<UUID, String> playerCharacters = new HashMap<>();

    private PlayerCharacterData() {}

    public static PlayerCharacterData getInstance(ServerLevel level) {
        ServerLevel overworld = level.getServer().overworld();
        DimensionDataStorage storage = overworld.getDataStorage();

        return storage.computeIfAbsent(
                new SavedData.Factory<>(
                        PlayerCharacterData::new,
                        PlayerCharacterData::load,
                        null
                ),
                DATA_NAME
        );
    }

    public static PlayerCharacterData getInstance(Level level) {
        if (level.isClientSide()) {
            throw new IllegalStateException("Cannot get PlayerCharacterData on client side");
        }
        return getInstance((ServerLevel) level);
    }

    public static PlayerCharacterData load(CompoundTag tag, HolderLookup.Provider registries) {
        PlayerCharacterData data = new PlayerCharacterData();
        CompoundTag playersTag = tag.getCompound("player_characters");

        for (String key : playersTag.getAllKeys()) {
            String characterId = playersTag.getString(key);
            if (characterId.isEmpty()) continue;

            try {
                UUID uuid = UUID.fromString(key);
                if (!MortalCharacter.ID.equals(characterId)) {
                    data.playerCharacters.put(uuid, characterId);
                }
            } catch (IllegalArgumentException e) {
                CorpseOrigin.LOGGER.warn("Skipping legacy player data (name: {}, character: {})", key, characterId);
            }
        }

        CorpseOrigin.LOGGER.debug("PlayerCharacterData loaded {} records", data.playerCharacters.size());
        return data;
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider registries) {
        CompoundTag playersTag = new CompoundTag();

        for (Map.Entry<UUID, String> entry : playerCharacters.entrySet()) {
            playersTag.putString(entry.getKey().toString(), entry.getValue());
        }

        tag.put("player_characters", playersTag);

        if (!playerCharacters.isEmpty()) {
            CorpseOrigin.LOGGER.debug("PlayerCharacterData saved {} records", playerCharacters.size());
        }

        return tag;
    }

    public void setPlayerCharacter(Player player, String characterId) {
        setPlayerCharacter(player.getUUID(), characterId);
    }

    public void setPlayerCharacter(UUID playerUuid, String characterId) {
        String oldValue = playerCharacters.get(playerUuid);
        boolean changed = false;

        if (characterId == null || MortalCharacter.ID.equals(characterId)) {
            if (oldValue != null) {
                playerCharacters.remove(playerUuid);
                changed = true;
                CorpseOrigin.LOGGER.debug("Removed character for player UUID '{}'", playerUuid);
            }
        } else {
            if (!characterId.equals(oldValue)) {
                playerCharacters.put(playerUuid, characterId);
                changed = true;
                CorpseOrigin.LOGGER.debug("Set character '{}' for player UUID '{}'", characterId, playerUuid);
            }
        }

        if (changed) {
            this.setDirty();
        }
    }

    public String getPlayerCharacterId(Player player) {
        return getPlayerCharacterId(player.getUUID());
    }

    public String getPlayerCharacterId(UUID playerUuid) {
        return playerCharacters.getOrDefault(playerUuid, MortalCharacter.ID);
    }

    public boolean hasCharacter(Player player) {
        return hasCharacter(player.getUUID());
    }

    public boolean hasCharacter(UUID playerUuid) {
        String id = playerCharacters.get(playerUuid);
        return id != null && !MortalCharacter.ID.equals(id);
    }

    public void clearPlayerCharacter(Player player) {
        clearPlayerCharacter(player.getUUID());
    }

    public void clearPlayerCharacter(UUID playerUuid) {
        if (playerCharacters.remove(playerUuid) != null) {
            this.setDirty();
            CorpseOrigin.LOGGER.debug("Cleared character for player UUID '{}'", playerUuid);
        }
    }
}