package com.phagens.corpseorigin.GongFU.Sceen;

import com.phagens.corpseorigin.CorpseOrigin;
import com.phagens.corpseorigin.GongFU.GongFaZL.*;
import com.phagens.corpseorigin.GongFU.MenuTypeRegister;
import com.phagens.corpseorigin.GongFU.ModUtlis.GongFUDataUtlis;
import com.phagens.corpseorigin.skill.ISkill;
import com.phagens.corpseorigin.skill.ISkillHandler;
import com.phagens.corpseorigin.skill.SkillAttachment;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Map;


public class GongFuMenu extends AbstractContainerMenu {

    private static final int VISIBLE_ROWS = 4;
    private static final int SLOTS_PER_ROW = 9;
    private static final int VISIBLE_SLOTS = VISIBLE_ROWS * SLOTS_PER_ROW;
    // 使用持久化数据存储
    private NonNullList<ItemStack> containerItems;
    private final Player player;
    private int unlockedSlotCount;
    private int scrollOffset = 0;


    public GongFuMenu(int containerId, Inventory playerInventory) {
        super(MenuTypeRegister.GONG_FU_MENU.get(), containerId);
        this.player = playerInventory.player;
        this.unlockedSlotCount = SlotConfigManager.getUnlockedSlotCount(player);

        this.containerItems = loadOrCreateContainerItems(player);

        addCustomSlots();
        addPlayerInventorySlots(player);

        CorpseOrigin.LOGGER.info("【功法容器】创建容器 - 槽位数: {}, 总槽位数(含背包): {}",
                unlockedSlotCount, this.slots.size());
    }



    private NonNullList<ItemStack> loadOrCreateContainerItems(Player player) {
        CompoundTag playerData = player.getPersistentData();
        CompoundTag containerData = playerData.getCompound("GongFuContainer");

        int currentUnlockedSlots = SlotConfigManager.getUnlockedSlotCount(player);
        NonNullList<ItemStack> items = NonNullList.withSize(currentUnlockedSlots, ItemStack.EMPTY);

        if (!containerData.isEmpty()) {
            ContainerHelper.loadAllItems(containerData, items, player.registryAccess());

            // 如果加载的物品数量与当前槽位数不匹配，调整大小
            if (items.size() != currentUnlockedSlots) {
                CorpseOrigin.LOGGER.warn("功法容器大小不匹配：NBT中有{}个槽位，但当前解锁{}个槽位，调整为{}",
                        items.size(), currentUnlockedSlots, currentUnlockedSlots);

                net.minecraft.core.NonNullList<ItemStack> newItems = net.minecraft.core.NonNullList.withSize(currentUnlockedSlots, ItemStack.EMPTY);
                for (int i = 0; i < Math.min(items.size(), currentUnlockedSlots); i++) {
                    newItems.set(i, items.get(i));
                }
                items = newItems;
            }
        }

        CorpseOrigin.LOGGER.debug("创建功法容器，槽位数: {}", currentUnlockedSlots);
        return items;
    }

    private void saveContainerData() {
        CompoundTag playerData = player.getPersistentData();
        CompoundTag containerData = new CompoundTag();
        ContainerHelper.saveAllItems(containerData, containerItems, player.registryAccess());
        playerData.put("GongFuContainer", containerData);
    }

    private void onContainerChanged() {
        saveContainerData();
        GongFUDataUtlis.applyGongFaAttributes(player);
    }

    private boolean hasGongFaType(String typeId) {
        for (ItemStack stack : containerItems) {
            if (!stack.isEmpty() && stack.getItem() instanceof BaseGongFaItem gongFaItem) {
                GongFaData data = gongFaItem.getDataFromItem(stack);
                if (data != null && data.getTypeId().equals(typeId)) {
                    return true;
                }
            }
        }
        return false;
    }


    private void addPlayerInventorySlots(Player player) {
        // 添加玩家背包槽位 (标准布局)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(player.getInventory(),
                        col + row * 9 + 9,
                        8 + col * 18,
                        84 + row * 18));
            }
        }

        // 添加快捷栏槽位
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(player.getInventory(),
                    col,
                    8 + col * 18,
                    142));
        }
    }



    private void addCustomSlots() {
        for (int i = 0; i < Math.min(unlockedSlotCount, VISIBLE_SLOTS); i++) {
            int row = i / SLOTS_PER_ROW;
            int col = i % SLOTS_PER_ROW;

            int x = 9 + col * 18;
            int y = 8 + row * 18;

            GongFaCategory category = SlotConfigManager.getSlotCategory(player, i);

            this.addSlot(new TypeRestrictedSlot(i, x, y, category));
        }
    }

    public void refreshSlots() {

    }

    public void updateScrollOffset(int offset) {

    }

    public int getScrollOffset() {
        return 0;
    }

    public int getMaxScroll() {
        return 0;
    }

    public boolean canScroll() {
        return false;
    }


    public int getUnlockedSlotCount() {
        return unlockedSlotCount;
    }




    @Override//快速移动
    public ItemStack quickMoveStack(Player player, int i) {
        Slot slot = this.slots.get(i);
        if (slot.hasItem()) {
            ItemStack itemstack = slot.getItem();
            ItemStack itemstack1 = itemstack.copy();

            int customSlotCount = Math.min(unlockedSlotCount, VISIBLE_SLOTS);
            // 定义移动规则：从容器槽位到玩家背包，或反之
            if (i < customSlotCount) {
                if (!this.moveItemStackTo(itemstack1, customSlotCount, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (!this.moveItemStackTo(itemstack1, 0, customSlotCount, false)) {
                    return ItemStack.EMPTY;
                }
            }
            if (itemstack1.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            return itemstack;
        }
        return ItemStack.EMPTY;
    }

    // 添加getter方法
    public NonNullList<ItemStack> getContainer() {
        return this.containerItems;
    }

    @Override
    public boolean stillValid(Player player) {
        return !player.isDeadOrDying();
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        GongFUDataUtlis.applyGongFaAttributes(player);

        // 学习/遗忘功法技能
        if (!player.level().isClientSide) {
            updateGongFuSkills(player);
        }
    }

    /**
     * 更新玩家的功法技能学习状态
     */
    private void updateGongFuSkills(Player player) {
        ISkillHandler handler = SkillAttachment.getSkillHandler(player);
        if (handler == null) return;

        java.util.Set<ResourceLocation> equippedGongFuSkills = new java.util.HashSet<>();
        for (ItemStack stack : containerItems) {
            if (!stack.isEmpty() && stack.getItem() instanceof BaseGongFaItem gongFaItem) {
                GongFaData data = gongFaItem.getDataFromItem(stack);
                if (data != null) {
                    ResourceLocation skillId = GongFaSkillManager.getInstance()
                            .getGongFuSkillId(data.getTypeId(), data.getRarity(), data.getCeng());
                    if (skillId != null) {
                        equippedGongFuSkills.add(skillId);
                        if (!handler.hasLearned(skillId)) {
                            if (handler instanceof com.phagens.corpseorigin.skill.SkillHandler skillHandler) {
                                skillHandler.learnGongFuSkill(skillId);
                            }
                        }
                    }
                }
            }
        }

        for (ISkill skill : handler.getLearnedSkills()) {
            if (skill.getId().getPath().startsWith("gongfu_")) {
                if (!equippedGongFuSkills.contains(skill.getId())) {
                    handler.forgetSkill(skill);
                }
            }
        }
    }

    public class TypeRestrictedSlot extends Slot {
        private final GongFaCategory allowedCategory;

        public TypeRestrictedSlot(int index, int x, int y, GongFaCategory category) {
            super(new Container() {
                @Override
                public void clearContent() {
                    containerItems.clear();
                }

                @Override
                public int getContainerSize() {
                    return containerItems.size();
                }

                @Override
                public boolean isEmpty() {
                    return containerItems.stream().allMatch(ItemStack::isEmpty);
                }

                @Override
                public ItemStack getItem(int slot) {
                    if (slot >= 0 && slot < containerItems.size()) {
                        return containerItems.get(slot);
                    }
                    return ItemStack.EMPTY;
                }

                @Override
                public ItemStack removeItem(int slot, int amount) {
                    if (slot >= 0 && slot < containerItems.size()) {
                        ItemStack stack = containerItems.get(slot);
                        if (!stack.isEmpty()) {
                            ItemStack result = stack.split(amount);
                            onContainerChanged();
                            return result;
                        }
                    }
                    return ItemStack.EMPTY;
                }

                @Override
                public ItemStack removeItemNoUpdate(int slot) {
                    if (slot >= 0 && slot < containerItems.size()) {
                        ItemStack stack = containerItems.get(slot);
                        containerItems.set(slot, ItemStack.EMPTY);
                        return stack;
                    }
                    return ItemStack.EMPTY;
                }

                @Override
                public void setItem(int slot, ItemStack stack) {
                    if (slot >= 0 && slot < containerItems.size()) {
                        containerItems.set(slot, stack);
                        onContainerChanged();
                    }
                }

                @Override
                public void setChanged() {
                    onContainerChanged();
                }

                @Override
                public boolean stillValid(Player player) {
                    return true;
                }

                @Override
                public void startOpen(Player player) {}

                @Override
                public void stopOpen(Player player) {}

                @Override
                public int getMaxStackSize() {
                    return 64;
                }
            }, index, x, y);
            this.allowedCategory = category;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            if (!(stack.getItem() instanceof BaseGongFaItem gongFaItem)) {
                return false;
            }

            GongFaData newData = gongFaItem.getDataFromItem(stack);
            if (newData == null) {
                return false;
            }

            if (hasGongFaType(newData.getTypeId())) {
                return false;
            }

            if (allowedCategory == GongFaCategory.UNIVERSAL) {
                return true;
            }

            GongFaCategory itemCategory = gongFaItem.getItemCategory(stack);
            return allowedCategory == itemCategory;
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public int getMaxStackSize(ItemStack stack) {
            return 1;
        }

        public GongFaCategory getAllowedCategory() {
            return allowedCategory;
        }

        public boolean isEmptyWithCategory() {
            return getItem().isEmpty();
        }
    }


}
