package xiaoshi2022.corpseorigin.mixin;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.growth.NativeCorpseState;

/** Add the actual corpse loot table after vanilla loot, with the same killer and looting context. */
@Mixin(LivingEntity.class)
public abstract class NativeCorpseLootMixin {
    @Unique private static final ResourceKey<LootTable> corpseorigin$corpseLoot =
            ResourceKey.create(Registries.LOOT_TABLE, CorpseOrigin.id("entities/lower_level_zb"));

    @Inject(method = "dropFromLootTable(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;Z)V",
            at = @At("TAIL"))
    private void corpseorigin$additionalDrops(ServerLevel level, DamageSource source, boolean playerKill, CallbackInfo ci) {
        LivingEntity self = (LivingEntity)(Object)this;
        if (NativeCorpseState.infected(self)) self.dropFromLootTable(level, source, playerKill, corpseorigin$corpseLoot);
    }
}
