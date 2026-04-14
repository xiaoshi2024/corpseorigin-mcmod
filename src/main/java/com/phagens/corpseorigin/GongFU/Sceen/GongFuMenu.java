package com.phagens.corpseorigin.GongFU.Sceen;

import com.phagens.corpseorigin.GongFU.GongFaZL.BaseGongFaItem;
import com.phagens.corpseorigin.GongFU.GongFaZL.GongFaData;
import com.phagens.corpseorigin.GongFU.GongFaZL.GongFaSkillManager;
import com.phagens.corpseorigin.GongFU.MenuTypeRegister;
import com.phagens.corpseorigin.GongFU.ModUtlis.GongFUDataUtlis;
import com.phagens.corpseorigin.skill.ISkill;
import com.phagens.corpseorigin.skill.ISkillHandler;
import com.phagens.corpseorigin.skill.SkillAttachment;
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



public class GongFuMenu extends AbstractContainerMenu {

    private static final int CONTAINER_SIZE = 10; // 或其他你需要的数量
    // 使用持久化数据存储
    private final SimpleContainer container;


    public GongFuMenu(int containerId, Inventory playerInventory) {
        super(MenuTypeRegister.GONG_FU_MENU.get(), containerId);
        this.container =loadOrCreateContainer(playerInventory.player);
        Player player = playerInventory.player;
        // 添加自定义槽位
        addCustomSlots();
        // 添加玩家背包槽位
        addPlayerInventorySlots(player);
    }



    private SimpleContainer loadOrCreateContainer(Player player) {
        // 从玩家NBT数据中加载容器数据
        CompoundTag playerData = player.getPersistentData();
        CompoundTag containerData = playerData.getCompound("GongFuContainer");
        SimpleContainer container = new SimpleContainer(CONTAINER_SIZE) {
            @Override
            public void setChanged() {
                super.setChanged();
                // 保存到玩家数据
                saveContainerData(player, this);
                GongFuMenu.this.slotsChanged(this);
                GongFUDataUtlis.applyGongFaAttributes(player);
            }

        };
        // 从NBT恢复物品
        if (!containerData.isEmpty()) {   // 如果存在保存的数据
                                        //目标NBT标签 用于存储序列化物品数据  //要保存的物品堆栈   //注册表访问提供者
            ContainerHelper.loadAllItems(containerData,container.getItems(),player.registryAccess());   // 从NBT恢复物品
        }
        return  container;
    }
    private void saveContainerData(Player player, SimpleContainer container) {
        CompoundTag playerData = player.getPersistentData();   // 获取玩家的持久化数据
        CompoundTag containerData = new CompoundTag();    // 获取修行容器的NBT数据

        // 正确的参数顺序：tag, items, registryProvider
        ContainerHelper.saveAllItems(containerData, container.getItems(), player.registryAccess()); // 序列化所有物品到NBT

        playerData.put("GongFuContainer", containerData);  // 将容器数据保存到玩家数据中
    }

    private boolean hasGongFaType(String typeId) {
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
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
        //1列
        this.addSlot(new GongFuSlot(container, 0, 16, 20));
        this.addSlot(new GongFuSlot(container, 1, 16, 45));
        //2列
        this.addSlot(new GongFuSlot(container, 2, 48, 20));
        this.addSlot(new GongFuSlot(container, 3, 48, 45));
        //3列
        this.addSlot(new GongFuSlot(container, 4, 80, 20));
        this.addSlot(new GongFuSlot(container, 5, 80, 45));

        this.addSlot(new GongFuSlot(container, 6, 110, 20));
        this.addSlot(new GongFuSlot(container, 7, 110, 45));

        this.addSlot(new GongFuSlot(container, 8, 140, 20));
        this.addSlot(new GongFuSlot(container, 9, 140, 45));

    }

    @Override//快速移动
    public ItemStack quickMoveStack(Player player, int i) {
        Slot slot = this.slots.get(i);
        if (slot.hasItem()) {
            ItemStack itemstack = slot.getItem();
            ItemStack itemstack1 = itemstack.copy();
            // 定义移动规则：从容器槽位到玩家背包，或反之
            if (i < CONTAINER_SIZE) {
                // 从自定义容器移动到玩家背包
                if (!this.moveItemStackTo(itemstack1, CONTAINER_SIZE, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // 从玩家背包移动到自定义容器
                if (!this.moveItemStackTo(itemstack1, 0, CONTAINER_SIZE, false)) {
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
    public SimpleContainer getContainer() {
        return this.container;
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

        // 获取当前容器中的所有功法
        java.util.Set<ResourceLocation> equippedGongFuSkills = new java.util.HashSet<>();
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (!stack.isEmpty() && stack.getItem() instanceof BaseGongFaItem gongFaItem) {
                GongFaData data = gongFaItem.getDataFromItem(stack);
                if (data != null) {
                    ResourceLocation skillId = GongFaSkillManager.getInstance()
                            .getGongFuSkillId(data.getTypeId(), data.getRarity(), data.getCeng());
                    if (skillId != null) {
                        equippedGongFuSkills.add(skillId);
                        // 学习技能 - 使用 learnGongFuSkill 绕过尸兄检查
                        if (!handler.hasLearned(skillId)) {
                            if (handler instanceof com.phagens.corpseorigin.skill.SkillHandler skillHandler) {
                                skillHandler.learnGongFuSkill(skillId);
                            }
                        }
                    }
                }
            }
        }

        // 遗忘不再装备的功法技能
        for (ISkill skill : handler.getLearnedSkills()) {
            if (skill.getId().getPath().startsWith("gongfu_")) {
                if (!equippedGongFuSkills.contains(skill.getId())) {
                    handler.forgetSkill(skill);
                }
            }
        }
    }

    public class GongFuSlot extends Slot {
        public GongFuSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            if (stack.getItem() instanceof BaseGongFaItem gongFaItem) {
                GongFaData newData = gongFaItem.getDataFromItem(stack);
                if (newData != null) {
                    if (hasGongFaType(newData.getTypeId())) {
                        return false;
                    }
                }
            }
            return true;
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public int getMaxStackSize(ItemStack stack) {
            return 1;
        }
    }


}
