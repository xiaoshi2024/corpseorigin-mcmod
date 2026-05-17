
package com.phagens.corpseorigin.Item.GF;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * 功法基础物品类 - 支持基于枚举的功法品级文本描述
 */
public class GongFaBaseItem extends Item {

    private final GongFaRarity gongFaRarity;

    public GongFaBaseItem(Properties properties, GongFaRarity gongFaRarity) {
        super(properties);
        this.gongFaRarity = gongFaRarity;
    }

    /**
     * 获取物品的功法品级枚举
     */
    public GongFaRarity getGongFaRarity() {
        return gongFaRarity;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);

        // 添加功法品级描述
        if (gongFaRarity != null) {
            tooltipComponents.add(Component.literal("可辅助领悟功法品阶: ")
                    .append(Component.literal(gongFaRarity.getDisplayName())
                            .withStyle(style -> style.withColor(gongFaRarity.getTextColor()))));

            // 根据枚举动态生成描述文本
            tooltipComponents.add(Component.literal(gongFaRarity.generateDescription())
                    .withStyle(style -> style.withColor(0x808080)));
        }
    }

    /**
     * 功法品阶枚举 - 从人阶到超神阶
     */
    public enum GongFaRarity {
        MORTAL(1, "人阶", 0xFFFFFF),
        EARTH(2, "地阶", 0x55FF55),
        HEAVEN(3, "天阶", 0x55FFFF),
        DIVINE(4, "神阶", 0xFF55FF),
        TRANSCENDENT(5, "超神阶", 0xFFAA00);

        private final int level;
        private final String displayName;
        private final int textColor;

        GongFaRarity(int level, String displayName, int textColor) {
            this.level = level;
            this.displayName = displayName;
            this.textColor = textColor;
        }

        /**
         * 获取功法品阶等级数值
         */
        public int getLevel() {
            return level;
        }

        /**
         * 获取显示名称（如：人阶、天阶、超神阶）
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
                case MORTAL -> "参悟人阶功法的重要媒介之一，可帮助修行者奠定武道根基";
                case EARTH -> "参悟地阶功法的重要媒介之一，蕴含大地之力，能稳固修行境界";
                case HEAVEN -> "参悟天阶功法的重要媒介之一，承载天道眷顾，可突破凡俗极限";
                case DIVINE -> "参悟神阶功法的重要媒介之一，稀有度极高，能引发功力质变";
                case TRANSCENDENT -> "参悟超神阶功法的重要媒介之一，绝世珍宝，拥有登临绝顶的机缘";
            };
        }

        /**
         * 根据等级数值获取对应的功法品阶枚举
         * @param level 等级数值 (1-5)
         * @return 对应的功法品阶枚举
         */
        public static GongFaRarity fromLevel(int level) {
            for (GongFaRarity rarity : values()) {
                if (rarity.level == level) {
                    return rarity;
                }
            }
            return MORTAL; // 默认返回人阶
        }

        /**
         * 获取Minecraft格式化颜色代码（用于聊天消息等）
         */
        public String getFormatCode() {
            return switch (level) {
                case 1 -> "§f";  // 白色 - 人阶
                case 2 -> "§a";  // 绿色 - 地阶
                case 3 -> "§b";  // 青色 - 天阶
                case 4 -> "§d";  // 粉色 - 神阶
                case 5 -> "§6";  // 金色 - 超神阶
                default -> "§7"; // 灰色 - 未知
            };
        }
    }
}