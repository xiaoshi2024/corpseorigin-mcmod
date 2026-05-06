package com.phagens.corpseorigin.client.gui.TechniqueSwapTable;

import com.phagens.corpseorigin.GongFU.MenuTypeRegister;
import com.phagens.corpseorigin.block.entity.TechniqueSwapTableEntity;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class TechniqueSwapTableMenu extends AbstractContainerMenu {
    private final TechniqueSwapTableEntity tableEntity;
    private static final int INPUT_SLOT_COUNT = 6;
    private static final int OUTPUT_SLOT_INDEX = 6;
    private static final int CONTAINER_SLOT_COUNT = 7;

    private final List<ItemStack> availableRecipes = new ArrayList<>();
    private int selectedRecipeIndex = -1;
    /**
     * @param containerId 容器ID（由Minecraft分配）
     * @param playerInventory 玩家背包
     * @param entity 功法兑换台方块实体
     */
    public TechniqueSwapTableMenu(int containerId, Inventory playerInventory, TechniqueSwapTableEntity entity) {
        super(MenuTypeRegister.TECHNIQUE_SWAP_TABLE_MENU.get(), containerId);
        this.tableEntity = entity;
        // 添加自定义槽位（输入槽和输出槽）
        addCustomSlots();
        // 添加玩家背包槽位
        addPlayerInventorySlots(playerInventory);
    }

    private void addCustomSlots() {
        // 输入槽 - 2列×3行布局
        // 第1列
        this.addSlot(new Slot(tableEntity, 0, 44, 17));   // 左上
        this.addSlot(new Slot(tableEntity, 1, 44, 35));   // 左中
        this.addSlot(new Slot(tableEntity, 2, 44, 53));   // 左下
        // 第2列
        this.addSlot(new Slot(tableEntity, 3, 62, 17));   // 右上
        this.addSlot(new Slot(tableEntity, 4, 62, 35));   // 右中
        this.addSlot(new Slot(tableEntity, 5, 62, 53));   // 右下
        // 输出槽 - 单独放置在右侧
        this.addSlot(new TechniqueSwapOutputSlot(tableEntity, OUTPUT_SLOT_INDEX, 116, 35));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int i) {
        ItemStack originalStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(i);
        if (slot != null && slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            originalStack = slotStack.copy();
            // 判断是容器槽位还是玩家背包槽位
            if (i < CONTAINER_SLOT_COUNT) {
                // 从容器移动到玩家背包
                if (!this.moveItemStackTo(slotStack, CONTAINER_SLOT_COUNT, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // 从玩家背包移动到容器
                // 优先尝试放入输出槽（如果可以）
                if (!this.moveItemStackTo(slotStack, 0, CONTAINER_SLOT_COUNT, false)) {
                    return ItemStack.EMPTY;
                }
            }
            // 如果物品全部移走，清空槽位
            if (slotStack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            // 如果移动的物品数量与原来相同，说明没有移动成功
            if (slotStack.getCount() == originalStack.getCount()) {
                return ItemStack.EMPTY;
            }
            // 通知槽位发生变化
            slot.onTake(player, slotStack);
        }
        return originalStack;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.tableEntity.stillValid(player);
    }
    /**
     * 槽位内容变化时的回调
     * 当输入槽物品变化时，触发配方匹配逻辑
     *
     * @param container 变化的容器
     */
    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);
        // TODO: 在这里触发配方匹配逻辑
        // 1. 获取所有输入槽的物品
        // 2. 调用配方管理器匹配可用配方
        // 3. 更新输出槽或发送配方列表到客户端
        updateRecipeMatches();
    }

    /**
     * 更新配方匹配
     * 根据当前输入槽的物品，计算可用的合成配方
     * TODO: 待实现完整的配方匹配逻辑
     */
    private void updateRecipeMatches() {
        this.availableRecipes.clear();
        this.selectedRecipeIndex = -1;
        // 临时实现：后续会接入配方系统
        // 这里应该：
        // 1. 收集输入槽的物品
        // 2. 查询配方管理器
        // 3. 更新可用配方列表
        // 4. 如果有选中的配方，检查是否可以合成
    }

    /**
     * 获取方块实体引用
     *
     * @return 功法兑换台方块实体
     */
    public TechniqueSwapTableEntity getTableEntity() {
        return this.tableEntity;
    }

    /**
     * 添加玩家背包槽位
     * 标准布局：27个主背包槽位 + 9个快捷栏槽位
     *
     * @param playerInventory 玩家背包
     */
    private void addPlayerInventorySlots(Inventory playerInventory) {
        // 主背包槽位（3行×9列）
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory,
                        col + row * 9 + 9,  // 槽位索引：9-35
                        8 + col * 18,        // X坐标
                        84 + row * 18));     // Y坐标
            }
        }

        // 快捷栏槽位（1行×9列）
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory,
                    col,                     // 槽位索引：0-8
                    8 + col * 18,            // X坐标
                    142));                   // Y坐标
        }
    }
    /**
     * 输出槽 - 特殊槽位类型
     * 限制：只能从输出槽取出物品，不能手动放入
     */
    private static class TechniqueSwapOutputSlot extends Slot {

        public TechniqueSwapOutputSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        /**
         * 检查物品是否可以放入此槽位
         * 输出槽不允许玩家手动放入物品
         *
         * @param stack 要放入的物品
         * @return 永远返回false
         */
        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        /**
         * 检查物品是否可以从此槽位取出
         * 允许玩家取出合成结果
         *
         * @return 永远返回true（只要有物品）
         */
        @Override
        public boolean mayPickup(Player player) {
            return true;
        }
    }

    /**
     * 获取可用的配方结果列表
     * @return 配方结果物品列表
     */
    public List<ItemStack> getAvailableRecipes() {
        return this.availableRecipes;
    }

    /**
     * 获取当前选中的配方索引
     * @return 选中的配方索引，-1 表示未选择
     */
    public int getSelectedRecipeIndex() {
        return this.selectedRecipeIndex;
    }

    /**
     * 设置选中的配方
     * @param index 配方索引
     */
    public void setSelectedRecipe(int index) {
        if (index >= 0 && index < this.availableRecipes.size()) {
            this.selectedRecipeIndex = index;
        }
    }

    /**
     * 执行合成操作
     * @return 合成是否成功
     */
    public boolean craftSelectedItem() {
        if (this.selectedRecipeIndex < 0 || this.selectedRecipeIndex >= this.availableRecipes.size()) {
            return false;
        }

        ItemStack result = this.availableRecipes.get(this.selectedRecipeIndex);
        if (result.isEmpty()) {
            return false;
        }

        // TODO: 验证材料是否足够
        // TODO: 消耗输入槽物品
        // TODO: 将结果放入输出槽

        return true;
    }


}
