
package com.phagens.corpseorigin.GongFU.Sceen;

import com.phagens.corpseorigin.CorpseOrigin;

import com.phagens.corpseorigin.GongFU.GongFaZL.GongFaCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;


@OnlyIn(Dist.CLIENT)
public class GongFuSceen extends AbstractContainerScreen<GongFuMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(CorpseOrigin.MODID, "textures/gui/kong_fu_cd.png");



    private static final ResourceLocation SLOT_BACKGROUND_GF = ResourceLocation.fromNamespaceAndPath(CorpseOrigin.MODID, "textures/gui/slot_gf.png");
    private static final ResourceLocation SLOT_BACKGROUND_XM = ResourceLocation.fromNamespaceAndPath(CorpseOrigin.MODID, "textures/gui/slot_xm.png");
    private static final ResourceLocation SLOT_BACKGROUND_YN = ResourceLocation.fromNamespaceAndPath(CorpseOrigin.MODID, "textures/gui/slot_yn.png");
    private static final ResourceLocation SLOT_BACKGROUND_FB = ResourceLocation.fromNamespaceAndPath(CorpseOrigin.MODID, "textures/gui/slot_fb.png");
    private static final ResourceLocation SLOT_BACKGROUND_ST = ResourceLocation.fromNamespaceAndPath(CorpseOrigin.MODID, "textures/gui/slot_st.png");
    private static final ResourceLocation SLOT_BACKGROUND_SG = ResourceLocation.fromNamespaceAndPath(CorpseOrigin.MODID, "textures/gui/slot_sg.png");
    private static final ResourceLocation SLOT_BACKGROUND_SZ = ResourceLocation.fromNamespaceAndPath(CorpseOrigin.MODID, "textures/gui/slot_sz.png");
    private static final ResourceLocation SLOT_BACKGROUND_TFST = ResourceLocation.fromNamespaceAndPath(CorpseOrigin.MODID, "textures/gui/slot_tfst.png");
    private static final ResourceLocation SLOT_BACKGROUND_QY = ResourceLocation.fromNamespaceAndPath(CorpseOrigin.MODID, "textures/gui/slot_qy.png");
    private static final ResourceLocation SLOT_BACKGROUND_UNIVERSAL = ResourceLocation.fromNamespaceAndPath(CorpseOrigin.MODID, "textures/gui/slot_universal.png");

    private static final int SCROLLER_WIDTH = 12;
    private static final int SCROLLER_HEIGHT = 15;
    private static final int SCROLLER_X = 157;
    private static final int SCROLLER_Y_START = 18;
    private static final int SCROLLER_Y_END = 128;

    private float scrollAmount;
    private boolean scrolling;

    public GongFuSceen(GongFuMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 160;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float v, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

//        renderScroller(guiGraphics, x, y);

        renderSlotBackgrounds(guiGraphics, x, y);
        renderSlotTooltip(guiGraphics, mouseX, mouseY, x, y);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {

    }




    private void renderSlotBackgrounds(GuiGraphics guiGraphics, int left, int top) {
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 9; col++) {
                int slotIndex = row * 9 + col + this.menu.getScrollOffset();

                if (slotIndex >= this.menu.getUnlockedSlotCount()) {
                    continue;
                }

                var slot = this.menu.slots.stream()
                        .filter(s -> s.index == slotIndex)
                        .findFirst()
                        .orElse(null);

                if (slot instanceof GongFuMenu.TypeRestrictedSlot typeSlot) {
                    int slotX = left + 9 + col * 18;
                    int slotY = top + 8 + row * 18;

                    GongFaCategory category = typeSlot.getAllowedCategory();
                    ResourceLocation backgroundTexture = getSlotBackgroundTexture(category);

                    guiGraphics.blit(backgroundTexture, slotX, slotY, 0, 0, 16, 16, 16, 16);
                }
            }
        }
    }
    private ResourceLocation getSlotBackgroundTexture(GongFaCategory category) {
        return switch (category) {
            case GF -> SLOT_BACKGROUND_GF;
            case XM -> SLOT_BACKGROUND_XM;
            case YN -> SLOT_BACKGROUND_YN;
            case FB -> SLOT_BACKGROUND_FB;
            case ST -> SLOT_BACKGROUND_ST;
            case SG -> SLOT_BACKGROUND_SG;
            case SZ -> SLOT_BACKGROUND_SZ;
            case TFST -> SLOT_BACKGROUND_TFST;
            case QY -> SLOT_BACKGROUND_QY;
            default -> SLOT_BACKGROUND_UNIVERSAL;
        };
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        renderSlotTooltip(guiGraphics, mouseX, mouseY, x, y);

    }

    private void renderSlotTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY, int left, int top) {
        Slot hoveredSlot = null;

        for (Slot slot : this.menu.slots) {
            if (slot instanceof GongFuMenu.TypeRestrictedSlot &&
                    mouseX >= left + slot.x && mouseX < left + slot.x + 18 &&
                    mouseY >= top + slot.y && mouseY < top + slot.y + 18) {
                hoveredSlot = slot;
                break;
            }
        }

        if (hoveredSlot instanceof GongFuMenu.TypeRestrictedSlot typeSlot) {
            GongFaCategory category = typeSlot.getAllowedCategory();
            Component tooltipText = Component.literal("槽位类型: ")
                    .append(Component.translatable("gongfa.category." + category.getName())
                            .withStyle(style -> style.withColor(category.getColor())));

            guiGraphics.renderComponentTooltip(this.font, java.util.List.of(tooltipText), mouseX, mouseY);
        }
    }



    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        this.scrolling = false;
        int left = (this.width - this.imageWidth) / 2;
        int top = (this.height - this.imageHeight) / 2;

        if (this.menu.canScroll() &&
                mouseX >= left + SCROLLER_X &&
                mouseX < left + SCROLLER_X + SCROLLER_WIDTH &&
                mouseY >= top + SCROLLER_Y_START &&
                mouseY < top + SCROLLER_Y_END) {
            this.scrolling = true;
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.scrolling = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.scrolling && this.menu.canScroll()) {
            int top = (this.height - this.imageHeight) / 2;
            int scrollerTrackHeight = SCROLLER_Y_END - SCROLLER_Y_START - SCROLLER_HEIGHT;

            this.scrollAmount = ((float)mouseY - top - SCROLLER_Y_START - (float)(SCROLLER_HEIGHT / 2)) / (float)scrollerTrackHeight;
            this.scrollAmount = net.minecraft.util.Mth.clamp(this.scrollAmount, 0.0F, 1.0F);

            int maxScroll = this.menu.getMaxScroll();
            int newOffset = (int) (this.scrollAmount * maxScroll + 0.5D);

            if (newOffset != this.menu.getScrollOffset()) {
                this.menu.updateScrollOffset(newOffset);
            }

            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.menu.canScroll()) {
            int maxScroll = this.menu.getMaxScroll();
            int currentOffset = this.menu.getScrollOffset();

            int newOffset = currentOffset - (int) Math.signum(scrollY);
            newOffset = net.minecraft.util.Mth.clamp(newOffset, 0, maxScroll);

            if (newOffset != currentOffset) {
                this.menu.updateScrollOffset(newOffset);
                this.scrollAmount = maxScroll > 0 ? (float)newOffset / maxScroll : 0.0F;
            }

            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }
}