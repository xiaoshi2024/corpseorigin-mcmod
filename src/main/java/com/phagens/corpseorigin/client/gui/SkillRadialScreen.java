package com.phagens.corpseorigin.client.gui;

import com.phagens.corpseorigin.CorpseOrigin;
import com.phagens.corpseorigin.client.gui.radialmenu.GuiRadialMenu;
import com.phagens.corpseorigin.client.gui.radialmenu.IRadialMenuSlot;
import com.phagens.corpseorigin.client.gui.radialmenu.RadialMenu;
import com.phagens.corpseorigin.client.gui.radialmenu.RadialMenuSlot;
import com.phagens.corpseorigin.network.ActivateSkillPacket;
import com.phagens.corpseorigin.player.PlayerCorpseData;
import com.phagens.corpseorigin.skill.ISkill;
import com.phagens.corpseorigin.skill.ISkillHandler;
import com.phagens.corpseorigin.skill.SkillAttachment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 技能轮盘界面 - 快速选择和激活已学习的技能
 *
 * 【功能说明】
 * 1. 以轮盘形式展示所有已学习的主动技能
 * 2. 支持鼠标移动选择技能
 * * 3. 显示技能冷却时间（灰色遮罩+倒计时数字）
 * 4. 点击或按键激活选中的技能
 * 5. 根据技能类型显示不同颜色的扇形区域
 *
 * 【界面布局】
 * - 中央：轮盘菜单，分为多个扇形区域
 * - 每个扇形：显示技能图标
 * - 底部：选中技能的名称、描述、冷却信息
 *
 * 【交互操作】
 * - 鼠标移动：高亮对应的扇形区域
 * - 点击/按键：激活选中的技能
 * - 技能冷却时：显示灰色遮罩和剩余时间
 *
 * 【技能分类显示】
 * - 基础进化：灰色
 * - 力量型变异：红色
 * - 敏捷型变异：绿色
 * - 特殊型变异：蓝色
 * - 神级能力：紫色
 * - 超神级能力：金色
 *
 * 【冷却显示】
 * - 扇形区域变暗
 * - 中央显示剩余秒数
 * - 底部显示详细冷却信息
 *
 * 【关联系统】
 * - GuiRadialMenu: 轮盘菜单基类
 * - RadialMenu: 轮盘数据模型
 * - ISkillHandler: 技能状态查询
 * - ActivateSkillPacket: 技能激活网络包
 *
 * @author Phagens
 * @version 1.0
 */
public class SkillRadialScreen extends GuiRadialMenu<ISkill> {

    private final Player player;
    private final ISkillHandler skillHandler;

    // 当前选中的技能（占位）- 静态字段，跨界面保存
    public static ISkill selectedSkill = null;
    // 静态引用，方便按键调用
    private static SkillRadialScreen instance = null;

    public SkillRadialScreen(Player player) {
        super(createRadialMenu(player));
        this.player = player;
        this.skillHandler = SkillAttachment.getSkillHandler(player);
        instance = this;
    }


    public static SkillRadialScreen getInstance() {
        return instance;
    }


    /**
     * 获取选中的技能
     */
    public static ISkill getSelectedSkill() {
        return selectedSkill;
    }

    //释放选中
    public void releaseSkill() {
        if (selectedSkill == null) {
            return;
        }

        if (skillHandler.isOnCooldown(selectedSkill)) {
            int remaining = skillHandler.getCooldownRemaining(selectedSkill);
            player.sendSystemMessage(Component.translatable(
                    "time", remaining / 20));
            return;
        }

        // 发送激活技能包到服务器
        net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                new ActivateSkillPacket(selectedSkill.getId()));
    }


    // 修改 createRadialMenu 方法，添加调试日志

    private static RadialMenu<ISkill> createRadialMenu(Player player) {
        ISkillHandler handler = SkillAttachment.getSkillHandler(player);
        List<IRadialMenuSlot<ISkill>> slots = new ArrayList<>();

        CorpseOrigin.LOGGER.info("创建技能轮盘 - 玩家: {}, 处理器: {}",
                player.getName().getString(), handler != null ? "存在" : "null");

        if (handler != null) {
            // 使用有序列表存储可激活技能，确保顺序一致
            List<ISkill> activatableSkills = getSortedActivatableSkills(handler);
            CorpseOrigin.LOGGER.info("玩家有 {} 个可激活技能", activatableSkills.size());

            for (ISkill skill : activatableSkills) {
                CorpseOrigin.LOGGER.info("添加技能到轮盘: {} - ID: {}",
                        skill.getName().getString(), skill.getId());
                slots.add(new RadialMenuSlot<>(skill.getName(), skill));
            }
        }

        if (slots.isEmpty()) {
            CorpseOrigin.LOGGER.warn("没有可激活的技能，显示空槽位");
            slots.add(new RadialMenuSlot<>(
                    Component.translatable("skill.corpseorigin.no_activatable"), null));
        }

        return new RadialMenu<>(
                (index) -> onSkillSelected(player, index),
                slots,
                null,
                -1
        );
    }

    /**
     * 获取排序后的可激活技能列表
     */
    private static List<ISkill> getSortedActivatableSkills(ISkillHandler handler) {
        List<ISkill> skills = new ArrayList<>();

        boolean isCorpse = PlayerCorpseData.isCorpse(handler.getPlayer());

        for (ISkill skill : handler.getLearnedSkills()) {
            if (skill.isActivatable()) {
                // 功法技能始终显示，其他技能只有僵尸状态显示
                if (skill.getId().getPath().startsWith("gongfu_")) {
                    skills.add(skill);
                } else if (isCorpse) {
                    skills.add(skill);
                }
            }
        }
        // 按技能ID排序，确保顺序一致
        skills.sort(Comparator.comparing(ISkill::getId));
        return skills;
    }

    private static void onSkillSelected(Player player, int index) {
//        ISkillHandler handler = SkillAttachment.getSkillHandler(player);
//        List<ISkill> activatableSkills = getSortedActivatableSkills(handler);
//
//        if (index >= 0 && index < activatableSkills.size()) {
//            ISkill selectedSkill = activatableSkills.get(index);
//
//            CorpseOrigin.LOGGER.info("选中技能: index={}, skill={}", index, selectedSkill.getId());
//
//            if (handler.isOnooldown(selectedSkill)) {
//                int remaining = handler.getCooldownRemaining(selectedSkill);
//                player.sendSystemMessage(Component.translatable(
//                        "skill.corpseorigin.cooldown", remaining / 20));
//                return;
//            }
//
//            net.neoforged.neoforge.network.PacketDistributor.sendToServer(
//                    new ActivateSkillPacket(selectedSkill.getId()));
//        }
        CorpseOrigin.LOGGER.info("轮盘选中技能：index={}", index);

    }

    @Override
    protected void drawIcon(GuiGraphics graphics, ISkill skill, int x, int y) {
        if (skill == null) {
            graphics.renderItem(new ItemStack(Items.BARRIER), x, y);
            return;
        }

        boolean onCooldown = skillHandler.isOnCooldown(skill);

        ResourceLocation iconPath = skill.getIcon();
        if (iconPath != null) {
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 10);

            graphics.blit(iconPath, x, y, 0, 0, 16, 16, 16, 16);

            graphics.pose().popPose();
        } else {
            graphics.renderItem(getSkillItemIcon(skill), x, y);
        }

        if (onCooldown) {
            int remaining = skillHandler.getCooldownRemaining(skill);

            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 20);

            graphics.fill(x, y, x + 16, y + 16, 0xAA000000);

            String cooldownText = String.valueOf(remaining / 20 + 1);
            int textWidth = minecraft.font.width(cooldownText);
            graphics.drawString(minecraft.font, cooldownText,
                    x + 8 - textWidth / 2, y + 4, 0xFFAAAA, true);

            graphics.pose().popPose();
        }
    }

    private ItemStack getSkillItemIcon(ISkill skill) {
        return switch (skill.getSkillType()) {
            case BASIC_EVOLUTION -> new ItemStack(Items.IRON_CHESTPLATE);
            case POWER_MUTATION -> new ItemStack(Items.DIAMOND_SWORD);
            case AGILITY_MUTATION -> new ItemStack(Items.FEATHER);
            case SPECIAL_MUTATION -> new ItemStack(Items.POTION);
            case DIVINE_ABILITY -> new ItemStack(Items.NETHER_STAR);
            case SUPREME_ABILITY -> new ItemStack(Items.DRAGON_EGG);
        };
    }

    @Override
    protected void drawSecondaryIcon(GuiGraphics graphics, ISkill icon, int x, int y) {
        // 技能没有次要图标
    }


    @Override
    protected int getSliceColor(IRadialMenuSlot<ISkill> slot, boolean highlighted, int index) {
        ISkill skill = slot.primarySlotIcon();

        if (skill == null) {
            return COLOR_DISABLED;
        }

        if (skillHandler.isOnCooldown(skill)) {
            return COLOR_COOLDOWN;
        }

        if (highlighted) {
            return COLOR_HIGHLIGHT;
        }

        return getColorBySkillType(skill.getSkillType());
    }

    private int getColorBySkillType(ISkill.SkillType type) {
        return switch (type) {
            case BASIC_EVOLUTION -> 0xCC666666;
            case POWER_MUTATION -> 0xCCAA4444;
            case AGILITY_MUTATION -> 0xCC44AA44;
            case SPECIAL_MUTATION -> 0xCC4444AA;
            case DIVINE_ABILITY -> 0xCCAA44AA;
            case SUPREME_ABILITY -> 0xCCFFAA00;
        };
    }

    @Override
    protected void renderSelectedInfo(GuiGraphics graphics) {
        if (selectedItem >= 0 && selectedItem < radialMenuSlots.size()) {
            IRadialMenuSlot<ISkill> slot = radialMenuSlots.get(selectedItem);
            ISkill skill = slot.primarySlotIcon();

            if (skill == null) return;

            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, Z_TEXT);

            Component name = skill.getName();
            int textWidth = minecraft.font.width(name);
            int x = (this.width - textWidth) / 2;
            int y = this.height - 65;

            graphics.fill(x - 5, y - 5, x + textWidth + 5, y + 50, 0xCC000000);
            graphics.renderOutline(x - 5, y - 5, textWidth + 10, 55, 0xFFFFFFFF);

            graphics.drawString(minecraft.font, name, x, y, 0xFFFFFF, true);

            if (skill.getDescription() != null) {
                Component desc = skill.getDescription();
                int descWidth = minecraft.font.width(desc);
                int descX = (this.width - descWidth) / 2;
                graphics.drawString(minecraft.font, desc, descX, y + 15, 0xAAAAAA, true);
            }

            if (skillHandler.isOnCooldown(skill)) {
                int remaining = skillHandler.getCooldownRemaining(skill);
                Component cooldownText = Component.translatable(
                        "skill.corpseorigin.cooldown_remaining", remaining / 20);
                int cdWidth = minecraft.font.width(cooldownText);
                int cdX = (this.width - cdWidth) / 2;
                graphics.drawString(minecraft.font, cooldownText, cdX, y + 30, 0xFF4444, true);
            }

            graphics.pose().popPose();
        }
    }

    public static void show() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            minecraft.setScreen(new SkillRadialScreen(minecraft.player));
        }
    }

    @Override
    public void onClose() {
        super.onClose();

        // 关闭时如果有选中的技能，设置为占位（持久保存）
        if (selectedItem >= 0 && selectedItem < radialMenuSlots.size()) {
            IRadialMenuSlot<ISkill> slot = radialMenuSlots.get(selectedItem);
            ISkill skill = slot.primarySlotIcon();

            if (skill != null && !skillHandler.isOnCooldown(skill)) {
                selectedSkill = skill;

                CorpseOrigin.LOGGER.info("设置占位技能：{}", skill.getName().getString());

//                if (player != null) {
//                    player.sendSystemMessage(Component.translatable(
//                            "message.corpseorigin.skill_selected", skill.getName()));
//                }
            }
        }

        // 注意：instance 设为 null，但 selectedSkill 保持不变为持久保存
        instance = null;
    }

    public static void validateSelectedSkill(Player player) {
        if (selectedSkill == null) {
            return;
        }

        ISkillHandler handler = SkillAttachment.getSkillHandler(player);

        // 如果技能不再存在或不在已学技能列表中，清除占位
        boolean stillValid = handler.getLearnedSkills().contains(selectedSkill);

        if (!stillValid) {
            CorpseOrigin.LOGGER.warn("占位技能 {} 已无效，清除占位", selectedSkill.getName().getString());
            selectedSkill = null;
        }
    }
}