package xiaoshi2022.corpseorigin.registry;

import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import xiaoshi2022.corpseorigin.CorpseOrigin;

public final class ModDataComponents {

    public static final DataComponentType<CompoundTag> PLAYER_CORPSE = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            Identifier.fromNamespaceAndPath(CorpseOrigin.MOD_ID, "player_corpse"),
            DataComponentType.<CompoundTag>builder()
                    .persistent(CompoundTag.CODEC)
                    .networkSynchronized(ByteBufCodecs.COMPOUND_TAG)  // ✅ 使用 ByteBufCodecs.COMPOUND_TAG
                    .build()
    );

    // ✅ 血莲宝灯储存的血气
    public static final DataComponentType<Integer> STORED_BLOOD_QI =
            Registry.register(
                    BuiltInRegistries.DATA_COMPONENT_TYPE,
                    Identifier.fromNamespaceAndPath(CorpseOrigin.MOD_ID, "stored_blood_qi"),
                    DataComponentType.<Integer>builder()
                            .persistent(Codec.INT)
                            .networkSynchronized(ByteBufCodecs.INT)
                            .build()
            );

    // ✅ 角色选择书记录的角色 ID
    public static final DataComponentType<String> CHARACTER_ID =
            Registry.register(
                    BuiltInRegistries.DATA_COMPONENT_TYPE,
                    Identifier.fromNamespaceAndPath(CorpseOrigin.MOD_ID, "character_id"),
                    DataComponentType.<String>builder()
                            .persistent(Codec.STRING)
                            .networkSynchronized(ByteBufCodecs.STRING_UTF8)
                            .build()
            );

    public static void init() {
        CorpseOrigin.LOGGER.info("CorpseOrigin data components registered");
    }
}