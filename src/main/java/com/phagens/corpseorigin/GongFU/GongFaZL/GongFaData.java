package com.phagens.corpseorigin.GongFU.GongFaZL;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

//功法数据
public class GongFaData {
    /// nbt:TypeId 类型 ，
    /// nbt:Rarity 稀有度 1-9
    /// nbt:Ceng 层数
    /// nbt:Attributes 属性
    /// nbt:Skills 技能
    private final String typeId;  // 类型标识
    private final String Type;
    private final String name;  // 名字
    private final Map<String, Double> attributes;  // 属性加成

    private final List<String> skills;  // 技能列表
    private final int rarity;  // 稀有度 1-9
    private final String Ceng;//层级
    private final int cooldown;   //冷却时间
    private final String iconPath;   //图片路径

    public GongFaData(String typeId, String type, String name, Map<String, Double> attributes, List<String> skills, int rarity, String ceng, int cooldown, String iconPath) {
        this.typeId = typeId;
        this.Type = type;
        this.name = name;
        this.attributes = new HashMap<>(attributes);
        this.skills = new ArrayList<>(skills);
        this.rarity = rarity;
        this.Ceng=ceng ;
        this.cooldown = cooldown > 0 ? cooldown : 300;
        this.iconPath = iconPath;
    }

    public String getTypeId() { return typeId; }
    public Map<String, Double> getAttributes() { return attributes; }
    public List<String> getSkills() { return skills; }
    public List<ResourceLocation> getSkillIds() {return skills.stream().map(ResourceLocation::tryParse).filter(java.util.Objects::nonNull).collect(Collectors.toList());}
    public int getRarity() { return rarity; }
    public String getCeng() { return Ceng; }
    public String getName() { return name; }
    public int getCooldown() {return cooldown;}
    public String getIconPath() {return iconPath;}
    public String getType() { return Type; }
    //序列化到nbt
    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("TypeId", typeId);
        tag.putString("Type", Type);
        tag.putString("Name", name);
        tag.putInt("Rarity", rarity);
        tag.putString("Ceng", Ceng);
        tag.putInt("Cooldown", cooldown);
        tag.putString("icon", iconPath != null ? iconPath : "");
        // 保存属性
        CompoundTag attrTag = new CompoundTag();
        attributes.forEach(attrTag::putDouble);
        tag.put("Attributes", attrTag);
        // 保存技能
        ListTag skillsTag = new ListTag();
        skills.forEach(skill -> skillsTag.add(StringTag.valueOf(skill)));
        tag.put("Skills", skillsTag);
        return tag;
    }

    // 从NBT反序列化
    public static GongFaData fromNBT(CompoundTag tag) {
        String typeId = tag.getString("TypeId");
        String type = tag.getString("Type");
        String name = tag.getString("Name");
        int rarity = tag.getInt("Rarity");
        String ceng =tag.getString("Ceng");
        int cooldown = tag.contains("Cooldown") ? tag.getInt("Cooldown") : 300;
        String icon = tag.getString("icon");



        Map<String, Double> attributes = new HashMap<>();
        if (tag.contains("Attributes")) {
            CompoundTag attrTag = tag.getCompound("Attributes");
            for (String key : attrTag.getAllKeys()) {
                attributes.put(key, attrTag.getDouble(key));
            }
        }

        List<String> skills = new ArrayList<>();
        if (tag.contains("Skills")) {
            ListTag skillsTag = tag.getList("Skills", Tag.TAG_STRING);
            for (int i = 0; i < skillsTag.size(); i++) {
                skills.add(skillsTag.getString(i));
            }
        }

        return new GongFaData(typeId, type,name,attributes, skills, rarity,ceng, cooldown,icon);
    }


}
