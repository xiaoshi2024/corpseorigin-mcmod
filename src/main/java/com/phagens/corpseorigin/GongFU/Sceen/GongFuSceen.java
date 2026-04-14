package com.phagens.corpseorigin.GongFU.Sceen;

import com.phagens.corpseorigin.CorpseOrigin;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;


@OnlyIn(Dist.CLIENT)
public class GongFuSceen extends AbstractContainerScreen<GongFuMenu> {
    // 背景纹理
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(CorpseOrigin.MODID, "textures/gui/kong_fu_cd.png");

    public GongFuSceen(GongFuMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        // 设置GUI尺寸
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float v, int i, int i1) {
        // 渲染背景
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
