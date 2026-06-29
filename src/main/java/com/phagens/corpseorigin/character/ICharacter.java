package com.phagens.corpseorigin.character;

import com.phagens.corpseorigin.skill.ISkill;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public interface ICharacter {

    String getId();

    Component getName();

    Component getDescription();

    ResourceLocation getIcon();

    List<ISkill> getSkills();

    boolean isPassive();

    List<Component> getTraits();
}