package com.phagens.corpseorigin.GongFU.GongFaZL;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import com.phagens.corpseorigin.GongFU.JsonLoader.GongFaJsonLoader;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;

import java.util.List;

import static com.phagens.corpseorigin.GongFU.ModUtlis.GongFUDataUtlis.getCengMultiplier;


//模板
public class BaseGongFaItem extends Item {
    final String gongFaType;
    public BaseGongFaItem(Properties properties, String type) {
        super(properties);
        this.gongFaType = type;
    }
    
    /**
     * 创建带有默认数据的功法物品
     */
    public static ItemStack createGongFaItem(String typeId, int rarity, String ceng) {
        // 从 JSON 加载器获取功法数据
        GongFaData data = GongFaJsonLoader.getGongFaData(typeId, rarity, ceng);
        if (data == null) {
            throw new IllegalArgumentException("找不到功法数据: " + typeId + "_" + rarity + "_" + ceng);
        }
        
        // 创建物品堆栈
        ItemStack stack = new ItemStack(getGongFaItemByType(typeId));
        
        // 设置功法数据到物品
        if (stack.getItem() instanceof BaseGongFaItem gongFaItem) {
            gongFaItem.setDataToItem(stack, data);
        }
        
        return stack;
    }
    
    /**
     * 根据类型ID获取对应的功法物品实例
     */
    private static Item getGongFaItemByType(String typeId) {
        // 这里需要根据类型ID返回对应的功法物品实例
        // 暂时返回一个默认实例，实际使用时需要根据类型ID创建对应的物品
        return new BaseGongFaItem(new Properties().stacksTo(1), typeId);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        GongFaData data = getDataFromItem(stack);
        if (data != null){
            String type = data.getType();
            String cengPrefix = getCengPrefix(type);
            String attrPrefix = getAttributePrefix(type);

            tooltipComponents.add(Component.literal(getRarityColor(data.getRarity()) +
                    "品级：" + getRarityName(data.getRarity())));
            // 显示层级（根据 type 使用不同前缀）
            tooltipComponents.add(Component.literal("§e" + cengPrefix + getCengName(data.getCeng())));
            double multiplier = getCengMultiplier(data.getCeng());
            tooltipComponents.add(Component.literal("§7等级加成：§ax" + String.format("%.1f", multiplier)));
            // 显示属性（根据 type 使用不同前缀）
            tooltipComponents.add(Component.literal(attrPrefix));
            data.getAttributes().forEach((attr, value) ->
                    tooltipComponents.add(Component.literal("§7" + getAttributeName(attr) + ": §a+" + String.format("%.1f", value))));
            if (!data.getSkills().isEmpty()) {
                tooltipComponents.add(Component.literal(getSkillPrefix(type)));
                data.getSkills().forEach(skill ->
                        tooltipComponents.add(Component.literal("§7• " + skill)));
            }
        }
    }
    // 根据 type 返回体系名称
    private String getTypeName(String type) {
        return switch (type) {
            case "gf" -> "功法";
            case "yn" -> "异能";
            case "xm" -> "血脉";
            default -> "未知体系";
        };
    }

    // 根据 type 返回颜色代码
    private String getTypeColor(String type) {
        return switch (type) {
            case "gf" -> "§9";      // 蓝色 - 功法
            case "yn" -> "§6";      // 金色 - 异能
            case "xm" -> "§c";      // 红色 - 血脉
            default -> "§7";
        };
    }
    private int getTypeColortwo(String type) {
        return switch (type) {
            case "gf" -> 0x4169E1;   // 皇家蓝 - 功法
            case "yn" -> 0xFFD700;   // 金色 - 异能
            case "xm" -> 0xDC143C;   // 深红色 - 血脉
            default -> 0x808080;     // 灰色 - 默认
        };
    }

    // 根据 type 返回层级前缀
    private String getCengPrefix(String type) {
        return switch (type) {
            case "gf" -> "功法";case "yn" -> "异能";case "xm" -> "血脉";
            default -> "";
        };
    }

    // 根据 type 返回属性前缀标题
    private String getAttributePrefix(String type) {
        return switch (type) {
            case "gf" -> "§6武学加持:";case "yn" -> "§6异能增幅:";case "xm" -> "§6血脉之力:";
            default -> "§6属性加成:";
        };
    }

    // 根据 type 返回技能前缀标题
    private String getSkillPrefix(String type) {
        return switch (type) {
            case "gf" -> "§d武学技艺:";case "yn" -> "§d异能权柄:";case "xm" -> "§d血脉神通:";
            default -> "§d技能:";
        };
    }

    public static GongFaData getDataFromItem(ItemStack stack) {
        //getOrDEfault 从物品获取数据组件 不存在返回默认
        // 值 1 新的数据组件系统存储物品自定义的NBT数据  2 空的自定义数据    //copytype复制一份tag对象 避免重复
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (tag.contains("GongFaData")) {
            return GongFaData.fromNBT(tag.getCompound("GongFaData"));  //提取GongFaData子标签  fromNBT数据序列化为对象
        }
        return null;
    }

    // 设置功法数据到物品
    public void setDataToItem(ItemStack stack, GongFaData data) {
        CompoundTag rootTag = new CompoundTag();
        rootTag.put("GongFaData", data.toNBT());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(rootTag));
    }

    //属性名
    private String getAttributeName(String attrKey) {
        return switch (attrKey) {
            case "attack_damage" -> "攻击力";
            case "movement_speed" -> "移速";
            case "max_health" -> "生命值";
            case "armor" -> "护甲";
            case "knockback_resistance"->"抗性";
            default -> attrKey;
        };
    }

    private String getCengName(String attrKey) {
        return switch (attrKey) {
            case "copy_1" -> "一级";case "copy_2" -> "二级";case "copy_3" -> "三级";
            case "copy_4" -> "四级";case "copy_5" -> "五级";case "copy_6" -> "六级";
            case "copy_7" -> "七级";case "copy_8" -> "八级";case "copy_9" -> "九级";
            default -> attrKey;
        };
    }


    private String getRarityName(int rarity) {
        return switch (rarity) {
            case 1 -> "人"; case 2 -> "地"; case 3 -> "天";
            case 4 -> "神"; case 5 -> "超神";case 6 -> "地阴";
            case 7 -> "天阳";case 8 -> "黑洞级";case 9 -> "白洞级";
            case 10 -> "星海";case 11 -> "寰宇";default -> "？";
        };
    }

    private String getRarityColor(int rarity) {
        return switch (rarity) {
            case 1 -> "§f"; case 2 -> "§a"; case 3 -> "§b";
            case 4 -> "§d"; case 5 -> "§6";case 6 -> "§9";
            case 7 -> "§c";case 8 -> "§0";case 9 -> "§f";
            case 10 -> "§5";case 11 -> "§6"; default -> "§7";
        };
    }

    @Override
    public Component getName(ItemStack stack) {
        GongFaData data = getDataFromItem(stack);
        if (data != null && data.getName() != null) {
            return Component.literal(data.getName())
                    .withStyle(style -> style.withBold(true).withColor(getTypeColortwo(data.getType())));
        }
        return super.getName(stack);
    }
}
