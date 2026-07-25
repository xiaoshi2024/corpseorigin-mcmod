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
        basicItem(Moditems.BALL_BAT.get());
        basicItem(Moditems.BLOOD_SWROD.get());
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

        withExistingParent(Moditems.LOWER_LEVEL_ZB_SPAWN_EGG.getId().getPath(), mcLoc("item/template_spawn_egg"));
        withExistingParent(Moditems.LONGYOU_SPAWN_EGG.getId().getPath(), mcLoc("item/template_spawn_egg"));
        withExistingParent(Moditems.ZBR_FISH_SPAWN_EGG.getId().getPath(), mcLoc("item/template_spawn_egg"));
        withExistingParent(Moditems.KAIWEINAI_SPAWN_EGG.getId().getPath(), mcLoc("item/template_spawn_egg"));
        withExistingParent(Moditems.COCO_PENGUIN_SPAWN_EGG.getId().getPath(), mcLoc("item/template_spawn_egg"));
        withExistingParent(Moditems.COCO_ZOMBIE_SPAWN_EGG.getId().getPath(), mcLoc("item/template_spawn_egg"));
        withExistingParent(Moditems.ZB_WORM_SPAWN_EGG.getId().getPath(), mcLoc("item/template_spawn_egg"));
        withExistingParent(Moditems.UNCLE_SPAWN_EGG.getId().getPath(), mcLoc("item/template_spawn_egg"));
        withExistingParent(Moditems.COCO_ZOMBIE_X_SPAWN_EGG.getId().getPath(), mcLoc("item/template_spawn_egg"));
        withExistingParent(Moditems.GUIGUN_SPAWN_EGG.getId().getPath(), mcLoc("item/template_spawn_egg"));
        withExistingParent(Moditems.CENTIPEDE_SPAWN_EGG.getId().getPath(), mcLoc("item/template_spawn_egg"));
        withExistingParent(Moditems.CHOUNIU_SPAWN_EGG.getId().getPath(), mcLoc("item/template_spawn_egg"));
        withExistingParent(Moditems.ZISHU_SPAWN_EGG.getId().getPath(), mcLoc("item/template_spawn_egg"));
        withExistingParent(Moditems.MAOTU_SPAWN_EGG.getId().getPath(), mcLoc("item/template_spawn_egg"));
        withExistingParent(Moditems.CALABASH_BOY_COS_SPAWN_EGG.getId().getPath(), mcLoc("item/template_spawn_egg"));
    }
}
