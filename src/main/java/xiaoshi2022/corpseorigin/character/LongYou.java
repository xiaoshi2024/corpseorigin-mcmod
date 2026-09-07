package xiaoshi2022.corpseorigin.character;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.List;

/**
 * 龙右 - 尸王
 */
public class LongYou implements ICharacter {

    public static final String ID = "longyou";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public Component getName() {
        return Component.translatable("character.corpseorigin.longyou");
    }

    @Override
    public Component getDescription() {
        return Component.translatable("character.corpseorigin.longyou.desc");
    }

    @Override
    public Identifier getIcon() {
        return ICharacter.iconId("longyou");
    }

    @Override
    public boolean isPassive() {
        return false;
    }

    @Override
    public List<Component> getTraits() {
        return List.of(
                Component.translatable("character.corpseorigin.longyou.trait1"),
                Component.translatable("character.corpseorigin.longyou.trait2"),
                Component.translatable("character.corpseorigin.longyou.trait3")
        );
    }

    @Override
    public float getInfectionMultiplier() {
        return 0.0f;
    }
}