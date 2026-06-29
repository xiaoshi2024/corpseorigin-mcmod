package com.phagens.corpseorigin.client.model;

import com.phagens.corpseorigin.CorpseOrigin;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import org.jetbrains.annotations.Nullable;

public class GongFuItemBakedModel extends BakedModelWrapper<BakedModel> {

    private final ItemOverrides itemOverrides;

    public GongFuItemBakedModel(BakedModel original) {
        super(original);
        this.itemOverrides = new ItemOverrides() {
            @Override
            public BakedModel resolve(BakedModel model, ItemStack stack,
                                      @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
                CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
                if (tag.contains("GongFaData")) {
                    CompoundTag gongFaTag = tag.getCompound("GongFaData");
                    String itemTexture = gongFaTag.getString("ItemTexture");
                    if (!itemTexture.isEmpty()) {
                        ResourceLocation textureRL = ResourceLocation.tryParse(itemTexture);
                        if (textureRL != null) {
                            String path = textureRL.getPath();
                            if (path.startsWith("item/")) {
                                path = path.substring(5);
                            }
                            ResourceLocation strippedRL = ResourceLocation.fromNamespaceAndPath(
                                    textureRL.getNamespace(), path);
                            ModelResourceLocation modelLoc = ModelResourceLocation.inventory(strippedRL);
                            BakedModel customModel = Minecraft.getInstance()
                                    .getModelManager().getModel(modelLoc);
                            if (customModel != Minecraft.getInstance().getModelManager().getMissingModel()) {
                                return customModel;
                            }
                        }
                    }
                }
                return original;
            }
        };
    }

    @Override
    public ItemOverrides getOverrides() {
        return this.itemOverrides;
    }
}
