package xiaoshi2022.corpseorigin.skill.chapter;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import xiaoshi2022.corpseorigin.character.PlayerCharacterData;
import xiaoshi2022.corpseorigin.item.CharacterMemoryData;
import java.util.HashSet;
import java.util.UUID;

public final class GourdInheritanceTest {
    private static int checks;
    public static void main(String[] args) {
        HashSet<String> ids = new HashSet<>();
        CompoundTag memory = new CompoundTag();
        for (GourdTrait t : GourdTrait.values()) {
            require(ids.add(t.id), "Duplicate donor key");
            require(GourdTrait.donor("minecraft:" + t.id) == t, "Missing source " + t.id);
            require(t.blood >= 0 && t.cooldown > 0, "Unbounded resource/cooldown");
            memory = GourdMemory.learn(memory, t);
            require(GourdMemory.knows(memory, t), "Learning failed");
        }
        require(ids.size() == 32, "Donor catalogue incomplete");
        require(GourdTrait.donor("minecraft:ocelot") == GourdTrait.CAT, "Ocelot mapping");
        require(GourdTrait.donor("minecraft:wandering_trader") == GourdTrait.VILLAGER, "Trader disguise mapping");
        require(GourdTrait.donor("othermod:villager") == null, "Unregistered namespace accepted");
        require(GourdTrait.donor("minecraft:player") == null, "Player donor accepted");
        require(GourdTrait.donor(null) == null, "Null donor accepted");
        require(!GourdTrait.succeeds(0, 0, false), "Zero chance succeeds");
        require(GourdTrait.succeeds(.999999, 1, false), "Certain roll fails");
        require(GourdTrait.succeeds(.3999, .4, false) && !GourdTrait.succeeds(.4, .4, false), "Probability boundary");
        require(!GourdTrait.succeeds(0, 1, true), "Duplicate adaptation rolls");
        require(!GourdTrait.succeeds(Double.NaN, 1, false) && !GourdTrait.succeeds(0, Double.NaN, false), "NaN accepted");
        require(!GourdTrait.bossEdible(40, 200) && GourdTrait.bossEdible(39, 200), "Boss threshold");
        require(!GourdTrait.bossEdible(0, 200) && !GourdTrait.bossEdible(Float.NaN, 200), "Dead/invalid boss accepted");
        require(GourdMemory.known(GourdMemory.learn(memory, GourdTrait.VILLAGER)).size() == 32, "Duplicate stacks");
        require(GourdMemory.selected(GourdMemory.cycle(memory, true)) == GourdTrait.WITHER, "Reverse cycle wrap");
        require(GourdMemory.selected(GourdMemory.cycle(memory, false)) == GourdTrait.COW, "Forward cycle");
        require(GourdMemory.selected(new CompoundTag()) == null, "Empty selection");
        CompoundTag corrupt = new CompoundTag(); corrupt.putString("Selected", "villager");
        require(GourdMemory.selected(corrupt) == null, "Unlearned selection accepted");

        UUID owner = UUID.randomUUID();
        PlayerCharacterData original = new PlayerCharacterData();
        original.setCharacterId(owner, "xiaojingang");
        original.setGourdMemory(owner, memory);
        var encoded = PlayerCharacterData.TYPE.codec().encodeStart(NbtOps.INSTANCE, original).getOrThrow();
        var decoded = PlayerCharacterData.TYPE.codec().parse(NbtOps.INSTANCE, encoded).getOrThrow();
        require(GourdMemory.known(decoded.getGourdMemory(owner)).size() == 32, "World save codec lost adaptations");
        memory.putString("Selected", "not_real");
        require(GourdMemory.selected(original.getGourdMemory(owner)) == GourdTrait.SHEEP, "Stored data aliased");
        CompoundTag book = new CompoundTag(); book.putString("Uuid", owner.toString()); book.put("Data", original.writeNbt(owner));
        PlayerCharacterData restored = new PlayerCharacterData();
        restored.readNbt(owner, CharacterMemoryData.characterData(book));
        require(GourdMemory.known(restored.getGourdMemory(owner)).size() == 32, "Memory-book data lost");
        require(GourdMemory.selected(restored.getGourdMemory(owner)) == GourdTrait.SHEEP, "Memory-book selection lost");
        restored.readNbt(owner, new CompoundTag());
        require(GourdMemory.known(restored.getGourdMemory(owner)).isEmpty(), "Old snapshot fails migration");
        System.out.println("Gourd inheritance: " + checks + " checks passed.");
    }
    private static void require(boolean value, String message) { checks++; if (!value) throw new AssertionError(message); }
}
