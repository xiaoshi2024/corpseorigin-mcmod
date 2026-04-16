/**
 * 模组事件总线订阅器 - 处理实体属性注册
 *
 * 【功能说明】
 * 1. 注册自定义实体的属性（生命值、攻击力、移动速度等）
 * 2. 在实体创建事件时设置默认属性值
 *
 * 【注册的实体】
 * - ZBR_FISH: 尸兄鱼（水下敌对生物）
 * - LOWER_LEVEL_ZB: 低级尸兄（基础敌对生物）
 * - LONGYOU: 龙右（Boss级实体）
 * - KAIWEINAI: 开胃奶（NPC）
 *
 * 【属性定义】
 * 各实体的属性定义在对应实体类的createAttributes()方法中
 *
 * 【关联系统】
 * - EntityRegistry: 实体注册
 * - ZbrFishEntity/LowerLevelZbEntity/LongyouEntity/KaiWeiNaiEntity: 实体类
 *
 * @author Phagens
 * @version 1.0
 */
package com.phagens.corpseorigin.event;

import com.phagens.corpseorigin.CorpseOrigin;
import com.phagens.corpseorigin.GongFU.FaXiang.FaxiangEntity;
import com.phagens.corpseorigin.entity.Animals.CocoPenguinEntity;
import com.phagens.corpseorigin.entity.Animals.CocoZombieEntity;
import com.phagens.corpseorigin.entity.Animals.ZbWormEntity;
import com.phagens.corpseorigin.entity.GuigunEntity;
import com.phagens.corpseorigin.entity.LongyouEntity;
import com.phagens.corpseorigin.entity.LowerLevelZbEntity;
import com.phagens.corpseorigin.entity.ZbrFishEntity;
import com.phagens.corpseorigin.entity.npc.KaiWeiNaiEntity;
import com.phagens.corpseorigin.register.EntityRegistry;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;

@EventBusSubscriber(modid = CorpseOrigin.MODID)
public class ModEventBusSubscriber {

    /**
     * 注册实体属性
     * 在EntityAttributeCreationEvent事件时调用
     *
     * @param event 实体属性创建事件
     */
    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        // 注册尸兄鱼的属性
        event.put(EntityRegistry.ZBR_FISH.get(), ZbrFishEntity.createAttributes().build());
        // 注册低级尸兄的属性
        event.put(EntityRegistry.LOWER_LEVEL_ZB.get(), LowerLevelZbEntity.createAttributes().build());
        // 注册龙右的属性
        event.put(EntityRegistry.LONGYOU.get(), LongyouEntity.createAttributes().build());
        // 注册鬼棍的属性
        event.put(EntityRegistry.GUIGUN.get(), GuigunEntity.createAttributes().build());
        // 注册开胃奶的属性
        event.put(EntityRegistry.KAIWEINAI.get(), KaiWeiNaiEntity.createAttributes().build());
        //注册法相
        event.put(EntityRegistry.FAXIANG.get(), FaxiangEntity.createAttributes().build());
        // 注册CoCo企鹅的属性
        event.put(EntityRegistry.COCO_PENGUIN.get(), CocoPenguinEntity.createAttributes().build());
        // 注册CoCo尸兄化的属性
        event.put(EntityRegistry.COCO_ZOMBIE.get(), CocoZombieEntity.createAttributes().build());
        // 注册尸兄虫子的属性
        event.put(EntityRegistry.ZB_WORM.get(), ZbWormEntity.createAttributes().build());
    }
}
