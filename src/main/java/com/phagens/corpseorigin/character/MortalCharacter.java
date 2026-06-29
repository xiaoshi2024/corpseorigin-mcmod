package com.phagens.corpseorigin.character;

import com.phagens.corpseorigin.CorpseOrigin;
import com.phagens.corpseorigin.skill.ISkill;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public class MortalCharacter implements ICharacter {

    public static final String ID = "mortal";
    private static MortalCharacter instance;

    private MortalCharacter() {}

    public static MortalCharacter getInstance() {
        if (instance == null) {
            instance = new MortalCharacter();
        }
        return instance;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public Component getName() {
        return Component.translatable("character.corpseorigin.mortal");
    }

    @Override
    public Component getDescription() {
        return Component.translatable("character.corpseorigin.mortal.desc");
    }

    @Override
    public ResourceLocation getIcon() {
        return ResourceLocation.fromNamespaceAndPath(CorpseOrigin.MODID, "textures/character/mortal.png");
    }

    @Override
    public List<ISkill> getSkills() {
        return new ArrayList<>();
    }

    @Override
    public boolean isPassive() {
        return true;
    }

    @Override
    public List<Component> getTraits() {
        List<Component> traits = new ArrayList<>();
        traits.add(Component.translatable("character.corpseorigin.mortal.trait"));
        return traits;
    }
}