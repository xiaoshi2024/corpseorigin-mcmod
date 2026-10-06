package xiaoshi2022.corpseorigin.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * 快递包裹：黑色火线投递演出道具——强化药剂等物品先塞进包裹再空投。
 * <ul>
 *   <li>装包：包裹在任一手 + 另一手拿着要寄的物品 → 右键（整组塞入，一件包裹一件货）</li>
 *   <li>拆包：潜行 + 右键 → 内容物回到背包（满了掉地上），包裹恢复空包裹可重复用</li>
 * </ul>
 * 副手拿包裹、主手拿"哑物品"（原版材料等不拦截右键的）时，副手包裹的 useOn 同样会被调用；
 * 主手物品自带右键功能（如药剂）时请空手拆/或主手持包操作。
 */
public class CourierPackageItem extends Item {
    private static final String TAG_ITEM = "StoredItem";
    private static final String TAG_COUNT = "StoredCount";

    public CourierPackageItem(Properties properties) { super(properties); }

    /** @return 包裹里装的东西（空 = 未装） */
    public static ItemStack peek(ItemStack pkg) {
        CustomData data = pkg.get(DataComponents.CUSTOM_DATA);
        if (data == null) return ItemStack.EMPTY;
        CompoundTag tag = data.copyTag();
        String id = tag.getStringOr(TAG_ITEM, "");
        if (id.isEmpty()) return ItemStack.EMPTY;
        Identifier key = Identifier.tryParse(id);
        if (key == null) return ItemStack.EMPTY;
        var item = BuiltInRegistries.ITEM.getValue(key);
        if (item == null || item == net.minecraft.world.item.Items.AIR) return ItemStack.EMPTY;
        return new ItemStack(item, Math.max(1, tag.getIntOr(TAG_COUNT, 1)));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return handle(context.getPlayer(), context.getHand());
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        return handle(player, hand);
    }

    private InteractionResult handle(Player player, InteractionHand hand) {
        if (player == null) return InteractionResult.PASS;
        ItemStack pkg = player.getItemInHand(hand);
        if (!(pkg.getItem() instanceof CourierPackageItem)) return InteractionResult.PASS;
        if (player.isShiftKeyDown()) return unpack(player, pkg);
        return pack(player, pkg, hand);
    }

    /** 右键：把另一只手的整组物品塞进包裹。 */
    private InteractionResult pack(Player player, ItemStack pkg, InteractionHand hand) {
        ItemStack other = player.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        if (other.isEmpty() || other.getItem() instanceof CourierPackageItem) {
            return InteractionResult.PASS;
        }
        if (!peek(pkg).isEmpty()) {
            if (!player.level().isClientSide())
                player.sendSystemMessage(Component.translatable("message.corpseorigin.courier_package.full"));
            return InteractionResult.FAIL;
        }
        if (!player.level().isClientSide()) {
            Component name = other.getHoverName();
            int count = other.getCount();
            CompoundTag tag = new CompoundTag();
            tag.putString(TAG_ITEM, BuiltInRegistries.ITEM.getKey(other.getItem()).toString());
            tag.putInt(TAG_COUNT, count);
            pkg.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            other.setCount(0);
            player.sendSystemMessage(Component.translatable("message.corpseorigin.courier_package.packed",
                    count, name));
            player.level().playSound(null, player.blockPosition(),
                    SoundEvents.ARMOR_EQUIP_LEATHER.value(), SoundSource.PLAYERS, 0.8F, 1.1F);
        }
        return InteractionResult.SUCCESS;
    }

    /** 潜行 + 右键：拆包，内容物回背包（满了掉地上）；每次拆包损耗 1 点耐久，耗尽包裹报废。 */
    private InteractionResult unpack(Player player, ItemStack pkg) {
        ItemStack stored = peek(pkg);
        if (stored.isEmpty()) {
            if (!player.level().isClientSide())
                player.sendSystemMessage(Component.translatable("message.corpseorigin.courier_package.empty"));
            return InteractionResult.FAIL;
        }
        if (!player.level().isClientSide()) {
            pkg.remove(DataComponents.CUSTOM_DATA);
            if (!player.getInventory().add(stored)) player.drop(stored, false);
            player.sendSystemMessage(Component.translatable("message.corpseorigin.courier_package.unpacked",
                    stored.getHoverName()));
            player.level().playSound(null, player.blockPosition(),
                    SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.8F, 0.9F);
            // 拆包损耗：纸壳包裹经不起反复拆，拆 3 次就烂（空包裹报废，货已到手）
            if (pkg.getMaxDamage() > 0) {
                int damage = pkg.getDamageValue() + 1;
                if (damage >= pkg.getMaxDamage()) {
                    pkg.setCount(0);
                    player.sendSystemMessage(Component.translatable("message.corpseorigin.courier_package.broken"));
                } else {
                    pkg.setDamageValue(damage);
                }
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                java.util.function.Consumer<Component> tooltip, TooltipFlag flag) {
        ItemStack stored = peek(stack);
        if (stored.isEmpty()) {
            tooltip.accept(Component.translatable("tooltip.corpseorigin.courier_package.empty")
                    .withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.accept(Component.translatable("tooltip.corpseorigin.courier_package.contents",
                    stored.getCount(), stored.getHoverName()).withStyle(ChatFormatting.GOLD));
        }
        tooltip.accept(Component.translatable("tooltip.corpseorigin.courier_package.howto")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
