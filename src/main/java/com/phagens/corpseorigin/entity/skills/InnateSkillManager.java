package com.phagens.corpseorigin.entity.skills;

import com.phagens.corpseorigin.CorpseOrigin;
import com.phagens.corpseorigin.GongFU.JSskill.JSSkillEngine;
import net.minecraft.world.entity.LivingEntity;

import java.util.*;

/**
 * 天生技能管理器 - 为任意生物添加JS技能
 */
public class InnateSkillManager {
    private static final Map<UUID, SkillData> entitySkills = new HashMap<>();

    public static class SkillData {
        List<String> skills;
        int cooldown;
        int currentIndex;

        public SkillData(List<String> skills) {
            this.skills = skills;
            this.cooldown = 0;
            this.currentIndex = 0;
        }
    }

    /**
     * 为生物设置天生技能
     */
    public static void setSkills(LivingEntity entity, String... skillNames) {
        UUID uuid = entity.getUUID();
        entitySkills.put(uuid, new SkillData(Arrays.asList(skillNames)));
        CorpseOrigin.LOGGER.info("实体 {} 设置天生技能: {}", entity.getId(), Arrays.toString(skillNames));
    }

    /**
     * 每tick调用此方法（在实体的tick中）
     */
    public static void tick(LivingEntity entity) {
        UUID uuid = entity.getUUID();
        SkillData data = entitySkills.get(uuid);

        if (data == null || data.skills.isEmpty()) {
            return;
        }

        if (data.cooldown > 0) {
            data.cooldown--;
            return;
        }

        if (entity.getRandom().nextFloat() > 0.3F) {
            return;
        }

        String skillName = data.skills.get(data.currentIndex);
        executeSkill(entity, skillName);

        data.currentIndex = (data.currentIndex + 1) % data.skills.size();
        data.cooldown = 100;
    }


    private static void executeSkill(LivingEntity entity, String skillName) {
        if (entity.level().isClientSide) {
            return;
        }

        try {
            JSSkillEngine engine = JSSkillEngine.getInstance();
            boolean success = engine.executeSkillForEntity(skillName, entity, null);

            if (success) {
                CorpseOrigin.LOGGER.debug("实体 {} 释放技能: {}", entity.getId(), skillName);
            }
        } catch (Exception e) {
            CorpseOrigin.LOGGER.error("实体 {} 技能执行失败: {}", entity.getId(), skillName, e);
        }
    }

    /**
     * 清理已死亡实体的数据
     */
    public static void onEntityDeath(LivingEntity entity) {
        entitySkills.remove(entity.getUUID());
    }
}
