package com.phagens.corpseorigin.GongFU.GongFaZL;

import com.phagens.corpseorigin.CorpseOrigin;
import com.phagens.corpseorigin.GongFU.JSskill.JSSkillEngine;
import com.phagens.corpseorigin.GongFU.ModUtlis.GongFUDataUtlis;
import com.phagens.corpseorigin.skill.BaseSkill;
import com.phagens.corpseorigin.skill.ISkill;
import com.phagens.corpseorigin.skill.SkillManager;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * 功法技能管理器 - 管理从 JSON 动态加载的功法技能
 */
public class GongFaSkillManager {
    private static GongFaSkillManager INSTANCE;

    // 存储功法技能映射：功法类型_稀有度_层数 -> 技能 ID
    private final Map<String, ResourceLocation> gongFuSkills = new HashMap<>();

    private GongFaSkillManager() {}

    public static GongFaSkillManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new GongFaSkillManager();
        }
        return INSTANCE;
    }
    /**
     * 为功法动态创建并注册技能
     */
    public void registerGongFuSkill(GongFaData data) {
        String key = data.getTypeId() + "_" + data.getRarity() + "_" + data.getCeng();

        // 检查是否已注册
        if (gongFuSkills.containsKey(key)) {
            return;
        }

        // 创建技能 ID
        ResourceLocation skillId = ResourceLocation.fromNamespaceAndPath(
                "corpseorigin",
                "gongfu_" + data.getTypeId().toLowerCase() + "_" + data.getRarity() + "_" + data.getCeng()
        );

        // 创建技能对象
        ISkill gongFuSkill = createGongFuSkill(skillId, data);

        // 注册到技能管理器
        SkillManager.getInstance().registerSkill(gongFuSkill);

        // 保存映射关系
        gongFuSkills.put(key, skillId);

        CorpseOrigin.LOGGER.info("注册功法技能：{} -> {}", key, skillId);
    }
    /**
     * 创建功法技能对象
     */
    private ISkill createGongFuSkill(ResourceLocation skillId, GongFaData data) {
        return new GongFuSkill(skillId, data);
    }


    /**
     * 激活功法技能的实现 - 使用 JS 引擎
     */
    private static void activateGongFuSkills(Player player, GongFaData data) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;  // 只在服务器端执行
        }

        for (String skillName : data.getSkills()) {
            CorpseOrigin.LOGGER.debug("尝试激活功法技艺：{}", skillName);

            // 调用 JS 引擎执行脚本
            boolean success = JSSkillEngine.getInstance()
                    .executeSkill(skillName, serverPlayer, data);

            if (success) {
                CorpseOrigin.LOGGER.info("功法技艺执行成功：{}", skillName);
            } else {
                CorpseOrigin.LOGGER.warn("功法技艺执行失败：{}", skillName);
            }
        }
    }

    /**
     * 激活功法技能的实现 - 支持任意实体
     * @param entity 任意生物实体（玩家、怪物、NPC等）
     * @param data 功法数据
     */
    public static void activateGongFuSkillsForEntity(LivingEntity entity, GongFaData data) {
        if (entity.level().isClientSide) {
            return;  // 只在服务器端执行
        }

        for (String skillName : data.getSkills()) {
            CorpseOrigin.LOGGER.debug("尝试激活功法技艺（实体）：{}", skillName);

            // 调用 JS 引擎执行脚本（支持任意实体）
            boolean success = JSSkillEngine.getInstance()
                    .executeSkillForEntity(skillName, entity, data);

            if (success) {
                CorpseOrigin.LOGGER.info("功法技艺执行成功（实体）：{}", skillName);
            } else {
                CorpseOrigin.LOGGER.warn("功法技艺执行失败（实体）：{}", skillName);
            }
        }
    }
    /**
     * 检查玩家是否装备了指定功法
     */
    private static boolean hasGongFaEquipped(Player player, GongFaData data) {
        //从修行容器中获取物品
        NonNullList<ItemStack> gongFuItems = GongFUDataUtlis.getGongFuItems(player);

        for (ItemStack stack : gongFuItems) {
            if (!stack.isEmpty() && stack.getItem() instanceof BaseGongFaItem gongFaItem) {
                GongFaData containerData = gongFaItem.getDataFromItem(stack);
                if (containerData != null &&
                        containerData.getTypeId().equals(data.getTypeId()) &&
                        containerData.getRarity() == data.getRarity() &&
                        containerData.getCeng().equals(data.getCeng())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 获取功法对应的技能 ID
     */
    public ResourceLocation getGongFuSkillId(String typeId, int rarity, String ceng) {
        String key = typeId + "_" + rarity + "_" + ceng;
        return gongFuSkills.get(key);
    }
    /**
     * 清除所有注册的功法技能（用于重载）
     */
    public void clear() {
        // 清除 JS 脚本缓存
        JSSkillEngine.getInstance().clearCache();

        // 移除所有注册的功法技能
        for (ResourceLocation skillId : gongFuSkills.values()) {
            SkillManager.getInstance().unregisterSkill(skillId);
        }
        gongFuSkills.clear();
        CorpseOrigin.LOGGER.info("已清除所有功法技能注册");
    }

    /**
     * 功法技能实现类 - 继承 BaseSkill
     */
    private static class GongFuSkill extends BaseSkill {
        private final GongFaData gongFaData;

        public GongFuSkill(ResourceLocation skillId, GongFaData data) {
            super(new Builder(skillId)
                    .name(Component.translatable(data.getName()))
                    .description(Component.literal("功法技艺：" + String.join(", ", data.getSkills())))
                    .cost(0)
                    .skillType(ISkill.SkillType.SPECIAL_MUTATION)
                    .requiredLevel(1)
                    .passive(false)
                    .cooldown(data.getCooldown())
                    .icon(data.getIconPath()!= null?ResourceLocation.parse(data.getIconPath())
                            : ResourceLocation.fromNamespaceAndPath(
                            "corpseorigin",
                            "textures/skills/corpse_king_power.png")));
            this.gongFaData = data;
        }

        @Override
        public void onActivate(Player player) {
            super.onActivate(player);

            CorpseOrigin.LOGGER.info("【功法技能激活】玩家：{}, 功法：{}",
                    player.getName().getString(), gongFaData.getName());

            if (!player.level().isClientSide && player instanceof ServerPlayer serverPlayer) {
                activateGongFuSkills(serverPlayer, gongFaData);
            }
        }

        @Override
        public boolean canLearn(Player player) {
            return hasGongFaEquipped(player, gongFaData);
        }
    }
}
