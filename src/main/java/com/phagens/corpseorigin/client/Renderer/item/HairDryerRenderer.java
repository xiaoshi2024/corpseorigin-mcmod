package com.phagens.corpseorigin.client.Renderer.item;

import com.phagens.corpseorigin.Item.HairDryerItem;
import com.phagens.corpseorigin.client.Models.item.HairDryerModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class HairDryerRenderer extends GeoItemRenderer<HairDryerItem> {
    public HairDryerRenderer() {
        super(new HairDryerModel());
    }
}
