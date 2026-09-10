package xiaoshi2022.corpseorigin.registry;

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

    public static void init() {
        CorpseOrigin.LOGGER.info("CorpseOrigin data components registered");
    }
}