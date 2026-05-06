package com.phagens.corpseorigin.client.gui.TechniqueSwapTable;

import com.phagens.corpseorigin.CorpseOrigin;
import com.phagens.corpseorigin.GongFU.Sceen.GongFuMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public class TechniqueSwapTableScreen extends AbstractContainerScreen<TechniqueSwapTableMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(CorpseOrigin.MODID, "textures/gui/technique_swap_table.png");

    private static final int RECIPE_AREA_X = 134;
    private static final int RECIPE_AREA_Y = 17;
    private static final int RECIPE_SLOT_SIZE = 18;
    private static final int RECIPES_PER_ROW = 3;
    private static final int RECIPE_AREA_WIDTH = RECIPES_PER_ROW * RECIPE_SLOT_SIZE;
    private static final int RECIPE_AREA_HEIGHT = 54;

    public TechniqueSwapTableScreen(TechniqueSwapTableMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;

        // 添加合成按钮
        Button craftButton = Button.builder(
                Component.literal("合成"),
                button -> this.menu.craftSelectedItem()
        ).bounds(this.leftPos + 134, this.topPos + 75, 40, 20).build();
        this.addRenderableWidget(craftButton);
    }



    @Override
    protected void renderBg(GuiGraphics guiGraphics, float v, int i, int i1) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        // 渲染配方选择区域背景
        guiGraphics.fill(
                this.leftPos + RECIPE_AREA_X - 2,
                this.topPos + RECIPE_AREA_Y - 2,
                this.leftPos + RECIPE_AREA_X + RECIPE_AREA_WIDTH + 2,
                this.topPos + RECIPE_AREA_Y + RECIPE_AREA_HEIGHT + 2,
                0x66000000
        );    }
    /**
     * 渲染文本标签
     * 作用：绘制GUI中的文字（标题、标签等）
     * 触发时机：每一帧渲染时调用（在renderBg()之后）
     * 参数：guiGraphics=渲染工具，mouseX/mouseY=鼠标坐标
     * 逻辑：绘制主标题→绘制背包标签→绘制"可选配方:"提示文字
     */
    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);

        // 绘制配方列表标签
        guiGraphics.drawString(this.font, "可选配方:", 134, 7, 4210752, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // 渲染配方结果物品
        renderRecipeSlots(guiGraphics, mouseX, mouseY);

        this.renderTooltip(guiGraphics, mouseX, mouseY);}
    /**
     * 渲染配方槽位（自定义方法）
     * 作用：绘制所有可用配方的物品图标和选中状态
     * 触发时机：render()方法内部调用
     * 逻辑：遍历配方列表→计算每个槽位坐标→绘制背景→绘制物品→鼠标悬停显示提示
     * 样式：选中=蓝色高亮+白色边框，未选中=灰色背景+灰色边框
     */
    private void renderRecipeSlots(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        List<ItemStack> recipes = this.menu.getAvailableRecipes();
        int selectedIndex = this.menu.getSelectedRecipeIndex();

        for (int i = 0; i < recipes.size(); i++) {
            int row = i / RECIPES_PER_ROW;
            int col = i % RECIPES_PER_ROW;
            int x = this.leftPos + RECIPE_AREA_X + col * RECIPE_SLOT_SIZE;
            int y = this.topPos + RECIPE_AREA_Y + row * RECIPE_SLOT_SIZE;

            ItemStack stack = recipes.get(i);

            // 绘制槽位背景
            int bgColor = (i == selectedIndex) ? 0x8844AAFF : 0x66555555;
            guiGraphics.fill(x, y, x + 16, y + 16, bgColor);
            guiGraphics.renderOutline(x, y, 16, 16, (i == selectedIndex) ? 0xFFFFFFFF : 0x88888888);

            // 渲染物品
            guiGraphics.renderItem(stack, x, y);
            guiGraphics.renderItemDecorations(this.font, stack, x, y);

            // 检查鼠标是否在槽位上
            if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) {
                guiGraphics.renderTooltip(this.font, stack, mouseX, mouseY);
            }
        }
    }
    /**
     * 鼠标点击事件
     * 作用：检测玩家点击操作，处理配方选择逻辑
     * 触发时机：玩家点击鼠标时调用
     * 参数：mouseX/mouseY=点击坐标，button=按键（0=左键，1=右键，2=中键）
     * 逻辑：遍历配方槽位→检测点击坐标→更新Menu选中状态→返回true阻止默认行为
     */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            List<ItemStack> recipes = this.menu.getAvailableRecipes();

            for (int i = 0; i < recipes.size(); i++) {
                int row = i / RECIPES_PER_ROW;
                int col = i % RECIPES_PER_ROW;
                int x = this.leftPos + RECIPE_AREA_X + col * RECIPE_SLOT_SIZE;
                int y = this.topPos + RECIPE_AREA_Y + row * RECIPE_SLOT_SIZE;

                if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) {
                    this.menu.setSelectedRecipe(i);
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }
}
