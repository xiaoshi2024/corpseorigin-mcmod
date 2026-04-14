package com.phagens.corpseorigin.client.Models.item;

import com.phagens.corpseorigin.Item.HairDryerItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedItemGeoModel;

public class HairDryerModel extends DefaultedItemGeoModel<HairDryerItem> {
    public HairDryerModel() {
        super(ResourceLocation.fromNamespaceAndPath("corpseorigin", "hair_dryer"));
    }
}
