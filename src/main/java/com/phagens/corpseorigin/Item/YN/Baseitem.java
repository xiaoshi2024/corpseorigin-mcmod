package com.phagens.corpseorigin.Item.YN;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class Baseitem extends Item {

    private final AbilityLevel abilityLevel;

    public Baseitem(Properties properties, AbilityLevel abilityLevel) {
        super(properties);
        this.abilityLevel = abilityLevel;
    }

    /**
     * 获取物品的异能等级枚举
     */
    public AbilityLevel getAbilityLevel() {
        return abilityLevel;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);

        // 添加异能等级描述
        if (abilityLevel != null) {
            tooltipComponents.add(Component.literal("可觉醒异能等级: ")
                    .append(Component.literal(abilityLevel.getDisplayName())
                            .withStyle(style -> style.withColor(abilityLevel.getTextColor()))));

            // 根据枚举动态生成描述文本
            tooltipComponents.add(Component.literal(abilityLevel.generateDescription())
                    .withStyle(style -> style.withColor(0x808080)));
        }
    }

    /**
     * 异能等级枚举 - C级到SSS级
     */
    public enum AbilityLevel {
        C_RANK(1, "C级", 0x55FFFF),
        B_RANK(2, "B级", 0xFF55FF),
        A_RANK(3, "A级", 0xFFAA00),
        S_RANK(4, "S级", 0xFF5555),
        SS_RANK(5, "SS级", 0xAA00AA),
        SSS_RANK(6, "SSS级", 0xFF0000);

        private final int level;
        private final String displayName;
        private final int textColor;

        AbilityLevel(int level, String displayName, int textColor) {
            this.level = level;
            this.displayName = displayName;
            this.textColor = textColor;
        }

        /**
         * 获取异能等级数值
         */
        public int getLevel() {
            return level;
        }

        /**
         * 获取显示名称（如：C级、S级、SSS级）
         */
        public String getDisplayName() {
            return displayName;
        }

        /**
         * 获取文本颜色（RGB格式）
         */
        public int getTextColor() {
            return textColor;
        }

        /**
         * 根据枚举动态生成描述文本
         * @return 完整的描述文本
         */
        public String generateDescription() {
            return switch (this) {
                case C_RANK -> "觉醒C级异能的重要素材之一，可帮助普通人觉醒";
                case B_RANK -> "觉醒B级异能的重要素材之一，能够显著提升异能威力和操控精度";
                case A_RANK -> "觉醒A级异能的重要素材之一，蕴含强大的能量，可突破异能瓶颈";
                case S_RANK -> "觉醒S级异能的重要素材之一，稀有度极高，能引发异能质变";
                case SS_RANK -> "觉醒SS级异能的重要素材之一，传说级材料，可重塑异能本质";
                case SSS_RANK -> "觉醒SSS级异能的重要素材之一，绝世珍宝，拥有改写规则的力量";
            };
        }

        /**
         * 根据等级数值获取对应的异能等级枚举
         * @param level 等级数值 (1-6)
         * @return 对应的异能等级枚举
         */
        public static AbilityLevel fromLevel(int level) {
            for (AbilityLevel ability : values()) {
                if (ability.level == level) {
                    return ability;
                }
            }
            return C_RANK; // 默认返回C级
        }

        /**
         * 获取Minecraft格式化颜色代码（用于聊天消息等）
         */
        public String getFormatCode() {
            return switch (level) {
                case 1 -> "§b";  // 青色 - C级
                case 2 -> "§d";  // 粉色 - B级
                case 3 -> "§6";  // 金色 - A级
                case 4 -> "§c";  // 红色 - S级
                case 5 -> "§5";  // 紫色 - SS级
                case 6 -> "§4";  // 深红 - SSS级
                default -> "§7"; // 灰色 - 未知
            };
        }
    }
}