package com.phagens.corpseorigin.Datagen;

import com.phagens.corpseorigin.CorpseOrigin;
import com.phagens.corpseorigin.register.Moditems;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class ModItemModelProvider extends ItemModelProvider {

    public ModItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, CorpseOrigin.MODID, existingFileHelper);
    }

    @Override
    protected void registerModels() {

        basicItem(Moditems.BASE_GONG_FA.get());
        basicItem(Moditems.GF_CY_REN.get());
        basicItem(Moditems.GF_CY_DI.get());
        basicItem(Moditems.GF_CY_TIAN.get());
        basicItem(Moditems.GF_CY_SHEN.get());
        basicItem(Moditems.GF_CY_CHAOSHENG.get());

        basicItem(Moditems.YNS_A.get());
        basicItem(Moditems.YNS_B.get());
        basicItem(Moditems.YNS_C.get());
        basicItem(Moditems.YNS_S.get());
        basicItem(Moditems.YNS_SS.get());
        basicItem(Moditems.YNS_SSS.get());
    }
}
