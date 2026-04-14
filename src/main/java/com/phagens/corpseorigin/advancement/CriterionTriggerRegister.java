package com.phagens.corpseorigin.advancement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.phagens.corpseorigin.CorpseOrigin;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Optional;

/**
 * 原版成就触发器注册
 * 
 * 【功能说明】
 * 1. 注册自定义成就触发器
 * 2. 提供触发方法供事件处理器调用
 * 
 * 【触发器列表】
 * - become_corpse: 成为尸兄
 * - meet_corpse_king: 遇见尸王
 * - weapon_shattered: 武器被尸王震坏
 * - cannibalism_discovery: 发现尸兄互相吞噬
 * 
 * @author Phagens
 * @version 1.0
 */
public class CriterionTriggerRegister {
    
    /** 触发器类型注册器 */
    public static final DeferredRegister<net.minecraft.advancements.CriterionTrigger<?>> TRIGGER_TYPES =
            DeferredRegister.create(Registries.TRIGGER_TYPE, CorpseOrigin.MODID);
    
    // ========== 触发器实例 ==========
    
    public static final DeferredHolder<net.minecraft.advancements.CriterionTrigger<?>, BecomeCorpseTrigger> BECOME_CORPSE =
            TRIGGER_TYPES.register("become_corpse", BecomeCorpseTrigger::new);
    
    public static final DeferredHolder<net.minecraft.advancements.CriterionTrigger<?>, MeetCorpseKingTrigger> MEET_CORPSE_KING =
            TRIGGER_TYPES.register("meet_corpse_king", MeetCorpseKingTrigger::new);
    
    public static final DeferredHolder<net.minecraft.advancements.CriterionTrigger<?>, WeaponShatteredTrigger> WEAPON_SHATTERED =
            TRIGGER_TYPES.register("weapon_shattered", WeaponShatteredTrigger::new);
    
    public static final DeferredHolder<net.minecraft.advancements.CriterionTrigger<?>, CannibalismDiscoveryTrigger> CANNIBALISM_DISCOVERY =
            TRIGGER_TYPES.register("cannibalism_discovery", CannibalismDiscoveryTrigger::new);
    
    // ========== 触发器实现 ==========
    
    /**
     * 成为尸兄触发器
     */
    public static class BecomeCorpseTrigger extends SimpleCriterionTrigger<BecomeCorpseTrigger.Instance> {
        
        @Override
        public Codec<Instance> codec() {
            return Instance.CODEC;
        }
        
        public void trigger(ServerPlayer player) {
            this.trigger(player, instance -> true);
        }
        
        public record Instance(Optional<ContextAwarePredicate> player) implements SimpleCriterionTrigger.SimpleInstance {
            public static final Codec<Instance> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                    ContextAwarePredicate.CODEC.optionalFieldOf("player").forGetter(Instance::player)
                ).apply(instance, Instance::new)
            );
        }
    }
    
    /**
     * 遇见尸王触发器
     */
    public static class MeetCorpseKingTrigger extends SimpleCriterionTrigger<MeetCorpseKingTrigger.Instance> {
        
        @Override
        public Codec<Instance> codec() {
            return Instance.CODEC;
        }
        
        public void trigger(ServerPlayer player) {
            this.trigger(player, instance -> true);
        }
        
        public record Instance(Optional<ContextAwarePredicate> player) implements SimpleCriterionTrigger.SimpleInstance {
            public static final Codec<Instance> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                    ContextAwarePredicate.CODEC.optionalFieldOf("player").forGetter(Instance::player)
                ).apply(instance, Instance::new)
            );
        }
    }
    
    /**
     * 武器被震坏触发器
     */
    public static class WeaponShatteredTrigger extends SimpleCriterionTrigger<WeaponShatteredTrigger.Instance> {
        
        @Override
        public Codec<Instance> codec() {
            return Instance.CODEC;
        }
        
        public void trigger(ServerPlayer player) {
            this.trigger(player, instance -> true);
        }
        
        public record Instance(Optional<ContextAwarePredicate> player) implements SimpleCriterionTrigger.SimpleInstance {
            public static final Codec<Instance> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                    ContextAwarePredicate.CODEC.optionalFieldOf("player").forGetter(Instance::player)
                ).apply(instance, Instance::new)
            );
        }
    }
    
    /**
     * 发现尸兄互相吞噬触发器
     */
    public static class CannibalismDiscoveryTrigger extends SimpleCriterionTrigger<CannibalismDiscoveryTrigger.Instance> {
        
        @Override
        public Codec<Instance> codec() {
            return Instance.CODEC;
        }
        
        public void trigger(ServerPlayer player) {
            this.trigger(player, instance -> true);
        }
        
        public record Instance(Optional<ContextAwarePredicate> player) implements SimpleCriterionTrigger.SimpleInstance {
            public static final Codec<Instance> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                    ContextAwarePredicate.CODEC.optionalFieldOf("player").forGetter(Instance::player)
                ).apply(instance, Instance::new)
            );
        }
    }
}
