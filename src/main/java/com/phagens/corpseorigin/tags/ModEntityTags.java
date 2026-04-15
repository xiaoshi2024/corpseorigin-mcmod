package com.phagens.corpseorigin.tags;

import com.phagens.corpseorigin.CorpseOrigin;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

public class ModEntityTags {
    // 企鹅标签 - 所有企鹅类实体都应该有这个标签
    public static final TagKey<EntityType<?>> PENGUINS = createTag("penguins");

    // CoCo专属标签（方便其他模组识别这是CoCo）
    public static final TagKey<EntityType<?>> COCO = createTag("coco");

    // 可被驯服的企鹅（后续扩展）
    public static final TagKey<EntityType<?>> TAMEABLE_PENGUINS = createTag("tameable_penguins");

    private static TagKey<EntityType<?>> createTag(String name) {
        return TagKey.create(Registries.ENTITY_TYPE,
                ResourceLocation.fromNamespaceAndPath(CorpseOrigin.MODID, name));
    }
}