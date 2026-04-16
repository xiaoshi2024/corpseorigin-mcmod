package com.phagens.corpseorigin.client.Renderer.item;

import com.phagens.corpseorigin.Item.zbritem.ZbWormitem;
import com.phagens.corpseorigin.client.Models.item.ZbWormitemModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class ZbWormitemRenderer extends GeoItemRenderer<ZbWormitem> {
    public ZbWormitemRenderer() {
        super(new ZbWormitemModel());
    }
}