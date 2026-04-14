package com.phagens.corpseorigin.client.gui;

import com.phagens.corpseorigin.CorpseOrigin;
import com.phagens.corpseorigin.client.gui.widget.ConnectionRenderer;
import com.phagens.corpseorigin.client.gui.widget.SkillDetailsPanel;
import com.phagens.corpseorigin.client.gui.widget.SkillNodeWidget;
import com.phagens.corpseorigin.network.SkillUnlockPacket;
import com.phagens.corpseorigin.skill.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 技能树界面 - 尸兄进化技能树的可视化展示和交互
 *
 * 【功能说明】
 * 1. 可视化展示尸兄进化技能树，包含所有技能节点
 * 2. 支持鼠标拖拽平移和滚轮缩放
 * 3. 显示技能节点之间的连接关系
 * 4. 点击节点查看技能详情
 * 5. 解锁技能（消耗进化点）
 * 6. 经验转化为进化点
 *
 * 【界面布局】
 * - 中央区域：技能树可视化（可拖拽、缩放）
 * - 左侧面板：技能详情面板
 * - 底部按钮：解锁按钮、经验转化按钮
 * - 右上角：进化点数和尸兄等级显示
 *
 * 【交互操作】
 * - 左键拖拽：平移技能树
 * - 滚轮：缩放技能树（0.5x - 2.0x）
 * - 点击节点：选中技能，显示详情
 * - 点击解锁：消耗进化点学习技能
 * - 点击转化：将经验等级转化为进化点
 *
 * 【节点状态】
 * - LOCKED（灰色）：未解锁，前置条件不满足
 * - AVAILABLE（黄色）：可解锁，满足条件且进化点充足
 * - UNLOCKED（绿色）：已解锁
 *
 * 【连接线颜色】
 * - 绿色：目标节点已解锁
 * - 黄色：目标节点可解锁
 * - 灰色：目标节点未解锁
 *
 * 【关联系统】
 * - ISkillTree/SkillTree: 技能树数据
 * - ISkillHandler: 技能状态管理
 * - SkillNodeWidget: 节点UI组件
 * - ConnectionRenderer: 连接线渲染
 * - SkillDetailsPanel: 详情面板
 * - SkillUnlockPacket: 解锁请求网络包
 *
 * @author Phagens
 * @version 1.0
 */
public class SkillTreeScreen extends Screen {

    private static final ResourceLocation BACKGROUND_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(CorpseOrigin.MODID, "textures/gui/skill_tree_bg.png");

    private static final ResourceLocation DETAILS_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(CorpseOrigin.MODID, "textures/gui/details_panel.png");

    private static final int NODE_SIZE = 32;
    private static final int NODE_SPACING_X = 80;
    private static final int NODE_SPACING_Y = 70;

    // 滚动偏移
    private double scrollX = 0;
    private double scrollY = 0;
    private boolean isDragging = false;
    private double lastMouseX;
    private double lastMouseY;

    // 缩放
    private double scale = 1.0;
    private static final double MIN_SCALE = 0.5;
    private static final double MAX_SCALE = 2.0;
    private static final double SCALE_STEP = 0.1;

    // 选中的技能
    private ISkill selectedSkill = null;

    // 技能树数据
    private ISkillTree skillTree;
    private ISkillHandler skillHandler;

    // UI组件
    private Button unlockButton;
    private SkillDetailsPanel detailsPanel;
    private final Map<ISkillNode, SkillNodeWidget> nodeWidgets = new HashMap<>();

    public SkillTreeScreen() {
        super(Component.translatable("skilltree.corpseorigin.corpse_evolution"));
    }

    @Override
    protected void init() {
        super.init();

        // 获取技能树和处理器
        skillTree = SkillManager.getInstance().getCorpseEvolutionTree();
        if (minecraft != null && minecraft.player != null) {
            skillHandler = SkillManager.getInstance().getSkillHandler(minecraft.player);
        }

        // 创建解锁按钮
        unlockButton = Button.builder(
                        Component.translatable("gui.corpseorigin.unlock"),
                        this::onUnlockButtonClicked)
                .pos(this.width / 2 - 110, this.height - 40)
                .size(100, 20)
                .build();
        this.addRenderableWidget(unlockButton);
        unlockButton.active = false;
        
        // 创建经验转化按钮
        Button convertButton = Button.builder(
                        Component.translatable("gui.corpseorigin.convert_experience"),
                        this::onConvertButtonClicked)
                .pos(this.width / 2 + 10, this.height - 40)
                .size(100, 20)
                .build();
        this.addRenderableWidget(convertButton);

        // 创建详情面板
        detailsPanel = new SkillDetailsPanel(10, 30, 150, 120, DETAILS_TEXTURE);
        this.addRenderableWidget(detailsPanel);

        // 创建节点控件
        createNodeWidgets();

        // 初始化滚动位置
        scrollX = this.width / 2.0;
        scrollY = 100;
    }

    private void createNodeWidgets() {
        if (skillTree == null) return;
        nodeWidgets.clear();

        for (ISkillNode node : skillTree.getAllNodes()) {
            if (node.getSkills().isEmpty()) continue;

            ISkill skill = node.getSkills().get(0);
            int x = getNodeScreenX(node);
            int y = getNodeScreenY(node);

            SkillNodeWidget.NodeState state = getNodeState(node);

            SkillNodeWidget widget = new SkillNodeWidget(
                    x, y,
                    skill.getName(),
                    skill,
                    state,
                    node.getTier()
            ) {
                @Override
                public void onClick(double mouseX, double mouseY) {
                    onNodeClicked(skill);
                }
            };
            widget.setScale((float) scale);

            nodeWidgets.put(node, widget);
            this.addRenderableWidget(widget);
        }
    }

    private int getNodeScreenX(ISkillNode node) {
        return (int) (scrollX + node.getX() * NODE_SPACING_X * scale) - (int) (NODE_SIZE * scale) / 2;
    }

    private int getNodeScreenY(ISkillNode node) {
        return (int) (scrollY + node.getY() * NODE_SPACING_Y * scale) - (int) (NODE_SIZE * scale) / 2;
    }

    private void updateNodePositions() {
        if (skillTree == null) return;

        for (ISkillNode node : skillTree.getAllNodes()) {
            SkillNodeWidget widget = nodeWidgets.get(node);
            if (widget != null) {
                int x = getNodeScreenX(node);
                int y = getNodeScreenY(node);
                widget.setPosition(x, y);
                widget.setScale((float) scale);

                // 更新节点状态
                SkillNodeWidget.NodeState newState = getNodeState(node);
                widget.setState(newState);
            }
        }
    }

    private SkillNodeWidget.NodeState getNodeState(ISkillNode node) {
        if (isNodeUnlocked(node)) {
            return SkillNodeWidget.NodeState.UNLOCKED;
        } else if (canUnlockNode(node)) {
            return SkillNodeWidget.NodeState.AVAILABLE;
        } else {
            return SkillNodeWidget.NodeState.LOCKED;
        }
    }

    private void onNodeClicked(ISkill skill) {
        this.selectedSkill = skill;
        if (detailsPanel != null) {
            boolean learned = isSkillLearned(skill);
            boolean canUnlock = canUnlockSkill(skill);
            int playerLevel = (minecraft != null && minecraft.player != null)
                    ? com.phagens.corpseorigin.player.PlayerCorpseData.getEvolutionLevel(minecraft.player)
                    : 1;
            String reason = canUnlock ? null : getCannotUnlockReason(skill);
            detailsPanel.setSkill(skill, learned, canUnlock, reason, playerLevel);
        }
        unlockButton.active = canUnlockSkill(skill);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // 渲染平铺背景
        renderTiledBackground(graphics);

        // 渲染连接线
        renderConnections(graphics);

        // 渲染其他UI组件
        super.render(graphics, mouseX, mouseY, partialTick);

        // 渲染进化点数
        renderEvolutionPoints(graphics);

        // 渲染标题
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 10, 0xFFD700);
    }

    private void renderTiledBackground(GuiGraphics graphics) {
//        int tileSize = 256;
//        for (int x = 0; x < this.width; x += tileSize) {
//            for (int y = 0; y < this.height; y += tileSize) {
//                graphics.blit(BACKGROUND_TEXTURE, x, y, 0, 0,
//                        Math.min(tileSize, this.width - x),
//                        Math.min(tileSize, this.height - y),
//                        tileSize, tileSize);
//            }
//        }
        graphics.fill(0, 0, this.width, this.height, 0xFF1A1A2E);
    }

    private void renderConnections(GuiGraphics graphics) {
        if (!(skillTree instanceof SkillTree tree)) return;

        for (ISkillNode node : skillTree.getAllNodes()) {
            List<ISkillNode> parents = tree.getParentNodes(node);

            for (ISkillNode parent : parents) {
                int x1 = (int) (scrollX + parent.getX() * NODE_SPACING_X * scale);
                int y1 = (int) (scrollY + parent.getY() * NODE_SPACING_Y * scale);
                int x2 = (int) (scrollX + node.getX() * NODE_SPACING_X * scale);
                int y2 = (int) (scrollY + node.getY() * NODE_SPACING_Y * scale);

                int color;
                if (isNodeUnlocked(node)) {
                    color = FastColor.ARGB32.color(255, 100, 200, 100); // 绿色 - 已解锁
                } else if (canUnlockNode(node)) {
                    color = FastColor.ARGB32.color(255, 200, 200, 100); // 黄色 - 可解锁
                } else {
                    color = FastColor.ARGB32.color(255, 100, 100, 100); // 灰色 - 未解锁
                }

                ConnectionRenderer.renderConnection(graphics, x1, y1, x2, y2, color, null);
            }
        }
    }

    private void renderEvolutionPoints(GuiGraphics graphics) {
        if (skillHandler == null || minecraft == null || minecraft.player == null) return;

        int points = skillHandler.getEvolutionPoints();
        Component text = Component.translatable("gui.corpseorigin.evolution_points", points);

        int x = this.width - 120;
        int y = 10;

        // 使用精灵绘制背景框
        graphics.blit(DETAILS_TEXTURE, x, y, 0, 120, 110, 25, 256, 256);

        // 绘制文字
        graphics.drawString(this.font, text, x + 5, y + 6, 0xFFD700, true);
        
        // 显示尸兄等级
        int evolutionLevel = com.phagens.corpseorigin.player.PlayerCorpseData.getEvolutionLevel(minecraft.player);
        Component levelText = Component.translatable("gui.corpseorigin.evolution_level", evolutionLevel);
        
        int levelX = this.width - 120;
        int levelY = 35;
        
        // 使用精灵绘制背景框
        graphics.blit(DETAILS_TEXTURE, levelX, levelY, 0, 120, 110, 25, 256, 256);
        
        // 绘制文字
        graphics.drawString(this.font, levelText, levelX + 5, levelY + 6, 0xFFD700, true);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && skillTree != null) {
            isDragging = true;
            lastMouseX = mouseX;
            lastMouseY = mouseY;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        isDragging = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (isDragging) {
            scrollX += mouseX - lastMouseX;
            scrollY += mouseY - lastMouseY;
            lastMouseX = mouseX;
            lastMouseY = mouseY;
            updateNodePositions();
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY != 0) {
            double oldScale = scale;
            double scaleDelta = scrollY > 0 ? SCALE_STEP : -SCALE_STEP;
            scale = Math.clamp(scale + scaleDelta, MIN_SCALE, MAX_SCALE);

            if (scale != oldScale) {
                // 以鼠标位置为中心缩放
                double scaleRatio = scale / oldScale;
                this.scrollX = mouseX - (mouseX - this.scrollX) * scaleRatio;
                this.scrollY = mouseY - (mouseY - this.scrollY) * scaleRatio;
                updateNodePositions();
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    // 修改 onUnlockButtonClicked 方法
    private void onUnlockButtonClicked(Button button) {
        if (selectedSkill == null || minecraft == null || minecraft.player == null) return;

        if (canUnlockSkill(selectedSkill)) {
            net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                    new SkillUnlockPacket(selectedSkill.getId()));

            // 本地预测更新
            skillHandler.learnSkill(selectedSkill);
            unlockButton.active = false;

            // 只更新节点状态，不重新创建
            updateNodeStates();  // <-- 新增方法

            // 更新选中技能状态
            if (detailsPanel != null) {
                detailsPanel.setSkill(selectedSkill, true, false);
            }
        }
    }

    // 新增方法：更新所有节点的状态
    private void updateNodeStates() {
        if (skillTree == null) return;

        for (ISkillNode node : skillTree.getAllNodes()) {
            SkillNodeWidget widget = nodeWidgets.get(node);
            if (widget != null) {
                SkillNodeWidget.NodeState newState = getNodeState(node);
                widget.setState(newState);
            }
        }
    }
    
    // 经验转化按钮点击事件
    private void onConvertButtonClicked(Button button) {
        if (minecraft == null || minecraft.player == null || skillHandler == null) return;
        
        // 检查玩家是否是尸族
        if (!com.phagens.corpseorigin.player.PlayerCorpseData.isCorpse(minecraft.player)) {
            minecraft.player.sendSystemMessage(Component.literal("§c只有尸族玩家才能转化经验！"));
            return;
        }
        
        // 检查玩家是否有足够的经验等级
        if (minecraft.player.experienceLevel < 5) {
            minecraft.player.sendSystemMessage(Component.literal("§c需要至少5级经验才能转化！"));
            return;
        }
        
        // 发送经验转化请求到服务器
        net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                new com.phagens.corpseorigin.network.ExperienceConvertPacket()
        );
    }

    private boolean isNodeUnlocked(ISkillNode node) {
        if (skillHandler == null) return false;
        return node.getSkills().stream()
                .anyMatch(skill -> skillHandler.hasLearned(skill.getId()));
    }

    private boolean canUnlockNode(ISkillNode node) {
        if (skillHandler == null) return false;
        return node.getSkills().stream()
                .anyMatch(this::canUnlockSkill);
    }

    private boolean isSkillLearned(ISkill skill) {
        return skillHandler != null && skillHandler.hasLearned(skill.getId());
    }

    private boolean canUnlockSkill(ISkill skill) {
        if (skillHandler == null || isSkillLearned(skill)) return false;
        if (skillHandler.getEvolutionPoints() < skill.getCost()) return false;

        // 检查尸兄等级要求
        if (minecraft != null && minecraft.player != null) {
            int playerEvolutionLevel = com.phagens.corpseorigin.player.PlayerCorpseData.getEvolutionLevel(minecraft.player);
            if (playerEvolutionLevel < skill.getRequiredLevel()) {
                return false;
            }
        }

        for (ResourceLocation prereq : skill.getPrerequisites()) {
            if (!skillHandler.hasLearned(prereq)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 获取技能无法解锁的原因（用于UI显示）
     */
    private String getCannotUnlockReason(ISkill skill) {
        if (skillHandler == null) return "gui.corpseorigin.skill_error";
        if (isSkillLearned(skill)) return "gui.corpseorigin.skill_already_learned";
        if (skillHandler.getEvolutionPoints() < skill.getCost()) {
            return "gui.corpseorigin.skill_need_points";
        }

        // 检查尸兄等级
        if (minecraft != null && minecraft.player != null) {
            int playerEvolutionLevel = com.phagens.corpseorigin.player.PlayerCorpseData.getEvolutionLevel(minecraft.player);
            if (playerEvolutionLevel < skill.getRequiredLevel()) {
                return "gui.corpseorigin.skill_need_level";
            }
        }

        // 检查前置技能
        for (ResourceLocation prereq : skill.getPrerequisites()) {
            if (!skillHandler.hasLearned(prereq)) {
                return "gui.corpseorigin.skill_need_prereq";
            }
        }
        return "gui.corpseorigin.skill_unknown";
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public static void show() {
        Minecraft.getInstance().setScreen(new SkillTreeScreen());
    }
}