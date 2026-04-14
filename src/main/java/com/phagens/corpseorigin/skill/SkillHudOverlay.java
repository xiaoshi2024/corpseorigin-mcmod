package com.phagens.corpseorigin.skill;

import com.phagens.corpseorigin.CorpseOrigin;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

import java.util.Set;

import static com.phagens.corpseorigin.client.gui.SkillRadialScreen.selectedSkill;


/**
 * HUD 渲染器 - 在屏幕右侧显示所有已学技能
 */
@EventBusSubscriber(modid = CorpseOrigin.MODID, value = Dist.CLIENT)
public class SkillHudOverlay {

    /**
     * 渲染游戏 HUD 时显示所有技能图标
     */
    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();

        // 确保在正常游戏世界中（不是菜单或聊天）
        if (minecraft.player == null || minecraft.screen != null) {
            return;
        }

        GuiGraphics graphics = event.getGuiGraphics();
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();

        // 获取技能处理器
        var handler = SkillAttachment.getSkillHandler(minecraft.player);
        if (handler == null) {
            return;
        }

        // 获取所有已学技能
        Set<ISkill> learnedSkills = handler.getLearnedSkills();
        if (learnedSkills.isEmpty()) {
            return;
        }

        // 渲染位置：屏幕最右边
        int x = width - 20;  // 距离右边 4 像素（留白）
        int startY = 20;     // 距离顶部 20 像素

        // 竖着排列渲染所有技能图标（16x16）
        int index = 0;
        for (ISkill skill : learnedSkills) {
            // 跳过被动技能，只渲染主动技能
            if (!skill.isActivatable()) {
                continue;
            }

            int y = startY + (index * 20);  // 每个图标间隔 20 像素（16+4 间距）

            // 检查是否是占位技能（高亮显示）
            boolean isSelected = selectedSkill != null && selectedSkill.equals(skill);

            // 检查是否在冷却中
            boolean onCooldown = handler.isOnCooldown(skill);

            // 绘制背景框（16x16）
            // 占位技能且不在 CD 中：蓝色背景；其他：黑色背景
            int bgColor = (isSelected && !onCooldown) ? 0xAA4444FF : 0xAA000000;
            graphics.fill(x - 2, y - 2, x + 18, y + 18, bgColor);

            // 绘制技能图标（16x16）
            graphics.pose().pushPose();
            graphics.pose().translate(x, y, 100);

            ResourceLocation iconPath = skill.getIcon();

            // 设置颜色：冷却中变暗（40% 亮度），正常亮度 100%
            float colorMultiplier = onCooldown ? 0.4f : 1.0f;
            graphics.setColor(colorMultiplier, colorMultiplier, colorMultiplier, 1.0f);

            if (iconPath != null) {
                graphics.blit(iconPath, 0, 0, 0, 0, 16, 16, 16, 16);
            } else {
                graphics.renderItem(getSkillItemIcon(skill), 0, 0);
            }

            // 恢复颜色
            graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
            graphics.pose().popPose();

            // 如果在冷却中，绘制半透明遮罩
            if (onCooldown) {
                graphics.pose().pushPose();
                graphics.pose().translate(x, y, 150);
                graphics.fill(0, 0, 16, 16, 0x66000000);  // 淡黑色遮罩
                graphics.pose().popPose();
            }

            // 绘制边框
            // 占位技能且不在 CD 中：白色边框；CD 中：深灰色；其他：浅灰色
            int borderColor;
            if (isSelected && !onCooldown) {
                borderColor = 0xFFFFFFFF;      // 白色边框（占位，可用）
            } else if (onCooldown) {
                borderColor = 0xFF666666;      // 深灰色边框（CD 中）
            } else {
                borderColor = 0xFFAAAAAA;      // 浅灰色边框（普通）
            }
            graphics.pose().pushPose();
            graphics.pose().translate(x - 2, y - 2, 250);
            graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
            graphics.renderOutline(0, 0, 20, 20, borderColor);
            graphics.pose().popPose();

            index++;
        }
    }

    /**
     * 获取技能的物品图标
     */
    private static net.minecraft.world.item.ItemStack getSkillItemIcon(ISkill skill) {
        return switch (skill.getSkillType()) {
            case BASIC_EVOLUTION -> new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_CHESTPLATE);
            case POWER_MUTATION -> new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND_SWORD);
            case AGILITY_MUTATION -> new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.FEATHER);
            case SPECIAL_MUTATION -> new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.POTION);
            case DIVINE_ABILITY -> new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.NETHER_STAR);
            case SUPREME_ABILITY -> new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DRAGON_EGG);
        };
    }
}
