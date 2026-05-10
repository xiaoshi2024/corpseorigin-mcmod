package com.phagens.corpseorigin.client.gui.TechniqueSwapTable;

import com.phagens.corpseorigin.GongFU.MenuTypeRegister;
import com.phagens.corpseorigin.Recipe.TechniqueSwapRecipe;
import com.phagens.corpseorigin.Recipe.TechniqueSwapRecipeManager;
import com.phagens.corpseorigin.block.entity.TechniqueSwapTableEntity;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class TechniqueSwapTableMenu extends AbstractContainerMenu {
    private final TechniqueSwapTableEntity tableEntity;
    private static final int INPUT_SLOT_COUNT = 6;//输入槽数量
    private static final int OUTPUT_SLOT_INDEX = 6;//输出索引
    private static final int CONTAINER_SLOT_COUNT = 7;//总容

    private final List<ItemStack> availableRecipes = new ArrayList<>();//可以配方结果列表
    private List<TechniqueSwapRecipe> matchedRecipes = new ArrayList<>();//匹配的配方对象列表
    private int selectedRecipeIndex = -1;//当前选中配方索引
    private int lastUpdateTick = 0;//上次更新tick

    /**
     * @param containerId 容器ID（由Minecraft分配）
     * @param playerInventory 玩家背包
     * @param entity 功法兑换台方块实体
     */
    public TechniqueSwapTableMenu(int containerId, Inventory playerInventory, TechniqueSwapTableEntity entity) {
        super(MenuTypeRegister.TECHNIQUE_SWAP_TABLE_MENU.get(), containerId);
        this.tableEntity = entity;

        addCustomSlots();
        addPlayerInventorySlots(playerInventory);
    }

    /**
     * 定期检查配方更新（由 Screen 每 tick 调用）
     */
    public void checkRecipeUpdate() {
        // 每 10 tick 检查一次，避免频繁计算
        if (this.tableEntity.getLevel() != null) {
            long currentTick = this.tableEntity.getLevel().getGameTime();
            if (currentTick - this.lastUpdateTick >= 10) {
                this.lastUpdateTick = (int) currentTick;
                updateRecipeMatches();
            }
        }
    }


    private void addCustomSlots() {
        // 输入槽
        this.addSlot(new Slot(tableEntity, 0, 19, 19));   // 左上
        this.addSlot(new Slot(tableEntity, 1, 37, 19));   // 左中
        this.addSlot(new Slot(tableEntity, 2, 55, 19));   // 左下
        this.addSlot(new Slot(tableEntity, 3, 19, 37));   // 右上
        this.addSlot(new Slot(tableEntity, 4, 37, 37));   // 右中
        this.addSlot(new Slot(tableEntity, 5, 55, 37));   // 右下
        // 输出槽
        this.addSlot(new TechniqueSwapOutputSlot(tableEntity, OUTPUT_SLOT_INDEX, 37, 64));
    }

    @Override//速度移动物品逻辑
    public ItemStack quickMoveStack(Player player, int i) {
        ItemStack originalStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(i);

        if (slot != null && slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            originalStack = slotStack.copy();
            // 判断是容器槽位还是玩家背包槽位
            // 判断是否是输出槽（索引6）
            if (i == OUTPUT_SLOT_INDEX) {
                // 输出槽特殊处理：模拟正常取出逻辑
                if (!this.moveItemStackTo(slotStack, CONTAINER_SLOT_COUNT, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }

                // 如果成功移动了物品，发送网络包消耗材料
                if (slotStack.getCount() < originalStack.getCount()) {
                    // 发送网络包到服务端消耗材料
                    if (this.selectedRecipeIndex >= 0 && this.selectedRecipeIndex < this.matchedRecipes.size()) {
                        com.phagens.corpseorigin.network.TechniqueSwapCraftPacket packet =
                                new com.phagens.corpseorigin.network.TechniqueSwapCraftPacket(
                                        this.tableEntity.getBlockPos(),
                                        this.selectedRecipeIndex
                                );
                        net.neoforged.neoforge.network.PacketDistributor.sendToServer(packet);
                    }

                    // 更新槽位
                    slot.setChanged();
                }
                return originalStack;
            }
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
    /**
     * 消耗材料并重置选中状态（用于Shift点击输出槽）
     */
    private void consumeMaterialsAndReset() {
        if (this.selectedRecipeIndex >= 0 && this.selectedRecipeIndex < this.matchedRecipes.size()) {
            TechniqueSwapRecipe recipe = this.matchedRecipes.get(this.selectedRecipeIndex);
            // 获取当前输入槽的物品
            ItemStack[] inputItems = new ItemStack[6];
            for (int i = 0; i < 6; i++) {
                inputItems[i] = this.tableEntity.getItem(i).copy();
            }

            // 调用 craft 消耗材料
            recipe.craft(inputItems);

            // 将消耗后的物品写回 BlockEntity
            for (int i = 0; i < 6; i++) {
                this.tableEntity.setItem(i, inputItems[i]);
            }

            // 清空输出槽
            this.tableEntity.setItem(OUTPUT_SLOT_INDEX, ItemStack.EMPTY);

            // 重置选中状态
            this.selectedRecipeIndex = -1;

            com.phagens.corpseorigin.CorpseOrigin.LOGGER.info("Shift合成完成，已消耗材料");
        }
    }
    @Override//检查是否可访问容器
    public boolean stillValid(Player player) {
        return this.tableEntity.stillValid(player);
    }
    /**
     * 槽位内容变化时的回调
     * 当输入槽物品变化时，触发配方匹配逻辑
     * @param container 变化的容器
     */
    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);
        // 1. 获取所有输入槽的物品
        // 2. 调用配方管理器匹配可用配方
        // 3. 更新输出槽或发送配方列表到客户端
        updateRecipeMatches();
    }


    /**
     * 更新配方匹配
     * 根据当前输入槽的物品，计算可用的合成配方
     */
    private void updateRecipeMatches() {
        // 1. 收集输入槽的物品
        ItemStack[] inputItems = new ItemStack[6];
        for (int i = 0; i < 6; i++) {
            inputItems[i] = tableEntity.getItem(i);
        }
//        // 调试日志：打印输入槽物品
//        StringBuilder inputDebug = new StringBuilder("输入槽物品: ");
//        for (int i = 0; i < 6; i++) {
//            if (!inputItems[i].isEmpty()) {
//                inputDebug.append("[").append(i).append(":").append(inputItems[i].getItem().toString()).append("]");
//            } else {
//                inputDebug.append("[").append(i).append(":空]");
//            }
//        }
//        com.phagens.corpseorigin.CorpseOrigin.LOGGER.info(inputDebug.toString());

        //调用配方管理器查找所有匹配的配方
        this.matchedRecipes = TechniqueSwapRecipeManager.getInstance().findMatchingRecipes(inputItems);

//        com.phagens.corpseorigin.CorpseOrigin.LOGGER.info("匹配到 {} 个配方", matchedRecipes.size());

        //提取配方的产出物品，用于GUI显示
        this.availableRecipes.clear();
        for (TechniqueSwapRecipe recipe : matchedRecipes) {
            this.availableRecipes.add(recipe.getOutput());
//            com.phagens.corpseorigin.CorpseOrigin.LOGGER.info("配方输出: {}", recipe.getOutput().toString());
        }
        //之前选中的配方已不存在，重置选中状态
        if (this.selectedRecipeIndex >= this.availableRecipes.size()) {
            this.selectedRecipeIndex = -1;
        }
        //更新输出槽显示的产物
        updateOutputSlot();
    }

    private void updateOutputSlot() {
        if (this.selectedRecipeIndex >= 0 && this.selectedRecipeIndex < this.matchedRecipes.size()) {
            TechniqueSwapRecipe selectedRecipe = this.matchedRecipes.get(this.selectedRecipeIndex);
            ItemStack[] inputItems = new ItemStack[6];
            for (int i = 0; i < 6; i++) {
                inputItems[i] = tableEntity.getItem(i);
            }


            if (selectedRecipe.matches(inputItems)) {
                ItemStack result = selectedRecipe.getOutput().copy();
                tableEntity.getItems().set(OUTPUT_SLOT_INDEX, result);
            }else {
                tableEntity.getItems().set(OUTPUT_SLOT_INDEX, ItemStack.EMPTY);
            }
        }else{
            tableEntity.getItems().set(OUTPUT_SLOT_INDEX, ItemStack.EMPTY);
        }
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
        /**
         * 玩家从输出槽取出物品时的回调
         * 核心功能：消耗输入材料、更新槽位、刷新配方列表
         *
         * @param player 取物品的玩家
         * @param stack 取出的物品
         */
        @Override
        public void onTake(Player player, ItemStack stack) {
            super.onTake(player, stack);

            TechniqueSwapTableMenu menu = (TechniqueSwapTableMenu) player.containerMenu;

            if (menu.selectedRecipeIndex >= 0 && menu.selectedRecipeIndex < menu.matchedRecipes.size()) {
                // 发送网络包到服务端执行合成
                com.phagens.corpseorigin.network.TechniqueSwapCraftPacket packet =
                        new com.phagens.corpseorigin.network.TechniqueSwapCraftPacket(
                                menu.tableEntity.getBlockPos(),
                                menu.selectedRecipeIndex
                        );
                net.neoforged.neoforge.network.PacketDistributor.sendToServer(packet);

                // 客户端立即清空输出槽（避免等待服务端同步时的视觉延迟）
                menu.tableEntity.setItem(OUTPUT_SLOT_INDEX, ItemStack.EMPTY);

                // 重置选中状态
//                menu.selectedRecipeIndex = -1;

                com.phagens.corpseorigin.CorpseOrigin.LOGGER.info("发送合成请求到服务端");
            }

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
            updateOutputSlot();
        }
    }






}
