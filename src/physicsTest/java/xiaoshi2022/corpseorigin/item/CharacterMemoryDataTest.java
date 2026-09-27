package xiaoshi2022.corpseorigin.item;

import net.minecraft.nbt.CompoundTag;
import xiaoshi2022.corpseorigin.character.PlayerCharacterData;
import xiaoshi2022.corpseorigin.shell.CharacterShellStateComponent;

import java.util.Set;
import java.util.UUID;

public final class CharacterMemoryDataTest {
    public static void main(String[] args) {
        UUID owner = UUID.randomUUID();
        PlayerCharacterData beforeDeath = new PlayerCharacterData();
        beforeDeath.setCharacterId(owner, "longyou");
        beforeDeath.learnSkill(owner, "blood_reserve");
        beforeDeath.learnSkill(owner, "golden_cicada_shell");
        beforeDeath.setPoints(owner, 240, 73);
        CompoundTag snapshot = new CompoundTag();
        snapshot.putString("Uuid", owner.toString());
        snapshot.put("Data", beforeDeath.writeNbt(owner));

        // Exercise the actual old-body serializer used by dropMemoryBook.
        CharacterShellStateComponent body = new CharacterShellStateComponent();
        body.readNbt(snapshot);
        CompoundTag book = new CompoundTag();
        body.writeNbt(book);
        CompoundTag restored = CharacterMemoryData.characterData(book);
        require(restored != null, "Valid shell memory rejected");
        PlayerCharacterData afterDeath = new PlayerCharacterData();
        afterDeath.readNbt(owner, restored);
        require(afterDeath.getCharacterId(owner).equals("longyou"), "Character lost");
        require(afterDeath.getLearnedSkills(owner).equals(Set.of("blood_reserve", "golden_cicada_shell")), "Skills lost");
        require(afterDeath.getEarnedPoints(owner) == 240, "Earned points lost");
        require(afterDeath.getAvailablePoints(owner) == 73, "Available points lost");
        require(book.getStringOr("Uuid", "").equals(owner.toString()), "Owner lost");

        restored.putString("CharacterId", "mortal");
        require(CharacterMemoryData.characterData(book).getStringOr("CharacterId", "").equals("longyou"), "Book mutated");
        require(CharacterMemoryData.characterData(beforeDeath.writeNbt(owner)) != null, "Flat legacy book rejected");
        require(CharacterMemoryData.characterData(null) == null, "Null accepted");
        require(CharacterMemoryData.characterData(new CompoundTag()) == null, "Empty book accepted");
        snapshot.put("Data", new CompoundTag());
        require(CharacterMemoryData.characterData(snapshot) == null, "Empty nested book accepted");
        snapshot.putString("Data", "broken");
        require(CharacterMemoryData.characterData(snapshot) == null, "Invalid nested type accepted");
        System.out.println("Character memory: snapshot, legacy, ownership, skills, points and invalid-data checks passed.");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
