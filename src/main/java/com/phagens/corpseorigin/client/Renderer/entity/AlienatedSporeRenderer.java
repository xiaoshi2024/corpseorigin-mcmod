package com.phagens.corpseorigin.client.Renderer.entity;

import com.phagens.corpseorigin.entity.AlienatedSporeEntity;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class AlienatedSporeRenderer extends EntityRenderer<AlienatedSporeEntity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("corpseorigin", "textures/entity/alienated_spore.png");

    public AlienatedSporeRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(AlienatedSporeEntity entity) {
        return TEXTURE;
    }
}
