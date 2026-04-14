package com.phagens.corpseorigin.player;

import com.mojang.serialization.Codec;
import com.phagens.corpseorigin.CorpseOrigin;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public class CorpsePlayerAttachment {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = 
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, CorpseOrigin.MODID);

    // Boolean codec
    private static final Codec<Boolean> BOOL_CODEC = Codec.INT.xmap(
        i -> i == 1,
        b -> b ? 1 : 0
    );

    // Integer codec (直接使用Codec.INT)
    
    // CompoundTag codec
    private static final Codec<CompoundTag> COMPOUND_TAG_CODEC = Codec.PASSTHROUGH.xmap(
        dynamic -> (CompoundTag) dynamic.convert(NbtOps.INSTANCE).getValue(),
        compoundTag -> new com.mojang.serialization.Dynamic<>(NbtOps.INSTANCE, compoundTag)
    );

    public static final Supplier<AttachmentType<Boolean>> IS_CORPSE = ATTACHMENT_TYPES.register(
            "is_corpse",
            () -> AttachmentType.builder(() -> false)
                    .serialize(BOOL_CODEC)
                    .copyOnDeath()
                    .build()
    );

    public static final Supplier<AttachmentType<Integer>> CORPSE_TYPE = ATTACHMENT_TYPES.register(
            "corpse_type",
            () -> AttachmentType.builder(() -> 0)
                    .serialize(Codec.INT)
                    .copyOnDeath()
                    .build()
    );

    public static final Supplier<AttachmentType<CompoundTag>> CORPSE_DATA = ATTACHMENT_TYPES.register(
            "corpse_data",
            () -> AttachmentType.builder((Supplier<CompoundTag>) CompoundTag::new)
                    .serialize(COMPOUND_TAG_CODEC)
                    .copyOnDeath()
                    .build()
    );
}
