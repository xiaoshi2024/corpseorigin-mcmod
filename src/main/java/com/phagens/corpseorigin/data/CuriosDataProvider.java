package com.phagens.corpseorigin.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import top.theillusivec4.curios.api.type.capability.ICurio;

import java.util.concurrent.CompletableFuture;

public class CuriosDataProvider extends top.theillusivec4.curios.api.CuriosDataProvider {
    public CuriosDataProvider(String modId, PackOutput output, ExistingFileHelper fileHelper, CompletableFuture<HolderLookup.Provider> registries) {
        super(modId, output, fileHelper, registries);
    }

    @Override
    public void generate(HolderLookup.Provider registries, ExistingFileHelper fileHelper) {
        // Generate belt slot
        this.createSlot("back")
            .size(1)
            .dropRule(ICurio.DropRule.ALWAYS_KEEP)
            .addCosmetic(false);

        // Generate entities file for player
        this.createEntities("player")
            .addPlayer()
            .addSlots("back");
    }

    public static void gatherData(GatherDataEvent event) {
        event.getGenerator().addProvider(
            event.includeServer(),
            new CuriosDataProvider(
                "corpseorigin",
                event.getGenerator().getPackOutput(),
                event.getExistingFileHelper(),
                event.getLookupProvider()
            )
        );
    }
}