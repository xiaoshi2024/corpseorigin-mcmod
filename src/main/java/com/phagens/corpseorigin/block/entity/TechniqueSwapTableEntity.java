package com.phagens.corpseorigin.block.entity;

import com.phagens.corpseorigin.register.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.openjdk.nashorn.internal.runtime.regexp.joni.ast.ConsAltNode;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

public class TechniqueSwapTableEntity extends BlockEntity implements Container, GeoAnimatable {

    private static final int INPUT_SLOT_COUNT = 6;

    private static final int OUTPUT_SLOT_INDEX = 6;

    private static final int TOTAL_SLOTS = 7;
    /** 物品列表：存储所有槽位的物品，使用NonNullList保证不会为null */
    private NonNullList<ItemStack> items = NonNullList.withSize(TOTAL_SLOTS, ItemStack.EMPTY);
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public TechniqueSwapTableEntity(BlockPos pos, BlockState blockState) {
        super(BlockEntityRegistry.TECHNIQUE_SWAP_TABLE.get(), pos, blockState);
    }


    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, this.items, registries);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, this.items, registries);
    }

    /**
     * 掉落容器内的物品
     * 当方块被破坏时调用，将所有物品（除输出槽外）掉落到世界中
     * 注意：只在服务端执行，避免客户端重复生成实体
     */
    public void dropItems() {
        if (this.level != null && !this.level.isClientSide) {
            for (int i = 0; i < this.items.size(); i++) {
                ItemStack stack = this.items.get(i);
                if (!stack.isEmpty() && i != OUTPUT_SLOT_INDEX) {
                    net.minecraft.world.entity.item.ItemEntity itemEntity =
                            new net.minecraft.world.entity.item.ItemEntity(
                                    this.level,
                                    this.worldPosition.getX() + 0.5,
                                    this.worldPosition.getY() + 0.5,
                                    this.worldPosition.getZ() + 0.5,
                                    stack.copy()
                            );
                    this.level.addFreshEntity(itemEntity);
                    this.items.set(i, ItemStack.EMPTY);
                }
            }
            this.setChanged();
        }
    }

    @Override//容器大小
    public int getContainerSize() {
        return TOTAL_SLOTS;
    }

    @Override//检查容器是否为空
    public boolean isEmpty() {
        for (ItemStack stack : this.items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int i) {
        return this.items.get(i);
    }

    @Override
    public ItemStack removeItem(int i, int i1) {
        ItemStack result = ContainerHelper.removeItem(this.items, i, i1);
        if (!result.isEmpty()) {
            this.setChanged();
        }
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int i) {
        ItemStack result = ContainerHelper.takeItem(this.items, i);
        if (!result.isEmpty()) {
            this.setChanged();
        }
        return result;
    }

    @Override
    public void setItem(int i, ItemStack itemStack) {
        this.items.set(i, itemStack);
        if (itemStack.getCount() > this.getMaxStackSize()) {
            itemStack.setCount(this.getMaxStackSize());
        }
        this.setChanged();

    }

    @Override
    public boolean stillValid(Player player) {
        if (this.level == null || this.level.getBlockEntity(this.worldPosition) != this) {
            return false;
        }
        return player.distanceToSqr(
                this.worldPosition.getX() + 0.5,
                this.worldPosition.getY() + 0.5,
                this.worldPosition.getZ() + 0.5
        ) <= 64.0;
    }

    @Override
    public void clearContent() {
        this.items.clear();
        this.setChanged();
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return 64;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot >= 0 && slot < INPUT_SLOT_COUNT;
    }

    @Override
    public boolean canTakeItem(Container target, int slot, ItemStack stack) {
        return true;
    }

    public NonNullList<ItemStack> getItems() {
        return this.items;
    }

    public Component getDisplayName() {
        return Component.translatable("container.corpseorigin.technique_swap_table");
    }


    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {

    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public double getTick(Object o) {
        return level != null ? level.getGameTime() : 0;
    }

}
