package com.phagens.corpseorigin.wthit;

import com.phagens.corpseorigin.GongFU.ModUtlis.GongFUDataUtlis;
import mcp.mobius.waila.api.*;
import mcp.mobius.waila.api.component.ItemComponent;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

/**
 * 尸兄玩家信息提供者 - 为WTHIT高亮模组提供尸兄玩家的详细信息
 * 
 * 【重要说明】
 * 在服务器环境中，WTHIT需要从服务器同步数据。
 * 这个类使用从服务器同步的NBT数据来显示信息，
 * 而不是直接访问玩家对象的数据。
 */
public enum CorpsePlayerProvider implements IEntityComponentProvider {

    INSTANCE;

    private static final ItemStack CORPSE_ICON = new ItemStack(Items.ROTTEN_FLESH);

    @Nullable
    @Override
    public ITooltipComponent getIcon(IEntityAccessor accessor, IPluginConfig config) {
        // 从同步的数据中检查是否是尸兄
        CompoundTag data = accessor.getData().raw();
        if (data.getBoolean("isCorpse")) {
            return new ItemComponent(CORPSE_ICON);
        }
        
        // 如果是客户端且可以直接访问玩家数据，也检查一次
        if (accessor.getEntity() instanceof Player player) {
            if (com.phagens.corpseorigin.player.PlayerCorpseData.isCorpse(player)) {
                return new ItemComponent(CORPSE_ICON);
            }
        }
        return null;
    }

    @Override
    public void appendHead(ITooltip tooltip, IEntityAccessor accessor, IPluginConfig config) {
        // 从同步的数据中检查是否是尸兄
        CompoundTag data = accessor.getData().raw();
        boolean isCorpse = data.getBoolean("isCorpse");
        
        // 如果是客户端且可以直接访问玩家数据，也检查一次
        if (!isCorpse && accessor.getEntity() instanceof Player player) {
            isCorpse = com.phagens.corpseorigin.player.PlayerCorpseData.isCorpse(player);
        }
        
        if (!isCorpse) {
            return;
        }

        tooltip.addLine(Component.literal("⚰️ 尸兄").withStyle(ChatFormatting.DARK_PURPLE));
    }

    @Override
    public void appendBody(ITooltip tooltip, IEntityAccessor accessor, IPluginConfig config) {
        // 优先使用从服务器同步的数据
        CompoundTag serverData = accessor.getData().raw();
        boolean isCorpse = serverData.getBoolean("isCorpse");
        
        // 如果是客户端且可以直接访问玩家数据，也检查一次
        Player player = null;
        if (accessor.getEntity() instanceof Player p) {
            player = p;
            if (!isCorpse) {
                isCorpse = com.phagens.corpseorigin.player.PlayerCorpseData.isCorpse(player);
            }
        }
        
        if (!isCorpse) {
            return;
        }

        // 优先使用服务器同步的数据，如果没有则使用本地数据
        int evolutionLevel = serverData.contains("evolutionLevel") ? serverData.getInt("evolutionLevel") : 
                            (player != null ? com.phagens.corpseorigin.player.PlayerCorpseData.getEvolutionLevel(player) : 0);
        int kills = serverData.contains("kills") ? serverData.getInt("kills") : 
                   (player != null ? com.phagens.corpseorigin.player.PlayerCorpseData.getKills(player) : 0);
        int hunger = serverData.contains("hunger") ? serverData.getInt("hunger") : 
                    (player != null ? com.phagens.corpseorigin.player.PlayerCorpseData.getHunger(player) : 0);
        boolean hasConsciousness = serverData.getBoolean("hasConsciousness") || 
                                  (player != null && com.phagens.corpseorigin.player.PlayerCorpseData.hasConsciousness(player));
        boolean consciousnessRestored = serverData.getBoolean("consciousnessRestored") || 
                                       (player != null && com.phagens.corpseorigin.player.PlayerCorpseData.isConsciousnessRestored(player));
        boolean hasWing = serverData.getBoolean("hasWing") || 
                         (player != null && com.phagens.corpseorigin.player.PlayerCorpseData.hasWing(player));
        boolean hasTail = serverData.getBoolean("hasTail") || 
                         (player != null && com.phagens.corpseorigin.player.PlayerCorpseData.hasTail(player));
        int extraEyeCount = serverData.contains("extraEyeCount") ? serverData.getInt("extraEyeCount") : 
                           (player != null ? com.phagens.corpseorigin.player.PlayerCorpseData.getExtraEyeCount(player) : 0);

        tooltip.addLine(Component.literal("进化等级: " + evolutionLevel).withStyle(ChatFormatting.GREEN));
        tooltip.addLine(Component.literal("击杀数: " + kills).withStyle(ChatFormatting.RED));
        tooltip.addLine(Component.literal("饥饿度: " + hunger + "/100").withStyle(ChatFormatting.GOLD));

        if (hasConsciousness || consciousnessRestored) {
            tooltip.addLine(Component.literal("意识: " + (consciousnessRestored ? "已恢复" : "保留")).withStyle(ChatFormatting.AQUA));
        } else {
            tooltip.addLine(Component.literal("意识: 失去").withStyle(ChatFormatting.GRAY));
        }

        if (hasWing) {
            tooltip.addLine(Component.literal("✓ 羽翼").withStyle(ChatFormatting.LIGHT_PURPLE));
        }

        if (hasTail) {
            tooltip.addLine(Component.literal("✓ 鱼尾").withStyle(ChatFormatting.BLUE));
        }

        if (extraEyeCount > 0) {
            tooltip.addLine(Component.literal("✓ 多眼 (" + extraEyeCount + ")").withStyle(ChatFormatting.YELLOW));
        }

        // 技能数据 - 优先使用服务器同步的数据
        int learnedSkills = serverData.contains("learnedSkills") ? serverData.getInt("learnedSkills") : 0;
        int evolutionPoints = serverData.contains("evolutionPoints") ? serverData.getInt("evolutionPoints") : 0;
        
        // 如果服务器没有同步，尝试从本地获取
        if (learnedSkills == 0 && player != null) {
            var skillHandler = com.phagens.corpseorigin.skill.SkillAttachment.getSkillHandler(player);
            if (skillHandler != null) {
                learnedSkills = skillHandler.getLearnedSkills().size();
                evolutionPoints = skillHandler.getEvolutionPoints();
            }
        }

        if (learnedSkills > 0) {
            tooltip.addLine(Component.literal("已学技能: " + learnedSkills + " 个").withStyle(ChatFormatting.DARK_AQUA));
        }

        if (evolutionPoints > 0) {
            tooltip.addLine(Component.literal("进化点: " + evolutionPoints).withStyle(ChatFormatting.DARK_GREEN));
        }

        // 功法数据 - 只在客户端可用时获取
        if (player != null) {
            var gongFuItems = GongFUDataUtlis.getGongFuItems(player);
            int gongFuCount = 0;
            for (var stack : gongFuItems) {
                if (!stack.isEmpty()) {
                    gongFuCount++;
                }
            }

            if (gongFuCount > 0) {
                tooltip.addLine(Component.literal("功法: " + gongFuCount + " 个").withStyle(ChatFormatting.DARK_RED));
            }
        }
    }
}
