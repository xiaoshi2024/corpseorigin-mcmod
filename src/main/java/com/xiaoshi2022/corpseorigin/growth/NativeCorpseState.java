package com.xiaoshi2022.corpseorigin.growth;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import com.xiaoshi2022.corpseorigin.entity.HybridZombie;
import com.xiaoshi2022.corpseorigin.registry.ModEffects;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * 原生生物感染状态中心（1.20.1 Forge 版本）。
 * <p>
 * 数据持久化策略（替代 26.2 Fabric 的 Attachment API）：
 * <ul>
 *   <li>{@code generation / incubating} → entityTags（已有 HybridZombie tag 机制）</li>
 *   <li>{@code appearance / organLoadout / incubation tick} → 本类静态 WeakHashMap 缓存，
 *       并通过 {@link NativeCorpseDataMixin} 注入 Entity#addAdditionalSaveData / readAdditionalSaveData
 *       写入 CompoundTag，保证重启不丢失</li>
 * </ul>
 * <p>
 * 感染后原版 Mob 实体保留（不替换），外观叠加骨骼/器官，AI 转为尸兄猎手。
 */
public final class NativeCorpseState {
    public static final int EYE = 1, RIBS = 2;

    // 用 WeakHashMap 缓存 per-entity 的扩展状态；Entity 回收时自动清理
    private static final Map<Entity, Integer> APPEARANCE_CACHE = new WeakHashMap<>();
    private static final Map<Entity, String> ORGANS_CACHE = new WeakHashMap<>();
    private static final Map<Entity, Integer> INCUBATION_CACHE = new WeakHashMap<>();

    // 客户端：服务端通过 CorpseOriginNetwork 同步过来的「实体 UUID -> 外观掩码」
    private static final Map<java.util.UUID, Integer> CLIENT_INFECTED = new java.util.concurrent.ConcurrentHashMap<>();
    private static final Map<java.util.UUID, String> CLIENT_ORGANS = new java.util.concurrent.ConcurrentHashMap<>();

    public static void acceptClientOrgans(java.util.UUID id, String json) {
        CLIENT_ORGANS.put(id, OrganLibrary.toJson(OrganLibrary.parseSlots(json)));
    }

    private NativeCorpseState() {}

    // ========== 读取 ==========

    /**
     * 读取「已完成变异」的代数。
     * <p>
     * 只认最终标记 {@code corpseorigin:infection_generation:<n>}；
     * 潜伏期用的是 {@code _pending:<n>}，不能算作已感染，
     * 否则 transform() 会被 infected() 提前挡掉，变异永远不会完成。
     */
    public static int generation(Entity entity) {
        if (entity == null) return 0;
        String prefix = HybridZombie.INFECTION_GENERATION + ":";
        for (String tag : entity.getTags()) {
            if (tag.startsWith(prefix)) {
                try { return Integer.parseInt(tag.substring(prefix.length())); }
                catch (NumberFormatException ignored) { return 1; }
            }
        }
        return 0;
    }

    public static boolean infected(Entity entity) {
        return entity instanceof Mob && generation(entity) > 0;
    }

    /**
     * 客户端可用的「已转化」判定。
     * <p>
     * Entity tags 不会同步到客户端，所以转化完成后会在实体上挂一个
     * 无限时长、无粒子、无图标的 QIANS 效果作为同步标记；
     * 客户端通过 {@link MobEffectInstance#isInfiniteDuration()} 识别。
     */
    public static boolean transformed(Entity entity) {
        if (!(entity instanceof net.minecraft.world.entity.LivingEntity living)) return false;
        // 客户端优先读网络同步状态（最可靠，独立于 effect/tag 同步）
        if (living.level().isClientSide()) {
            Integer synced = CLIENT_INFECTED.get(entity.getUUID());
            if (synced != null) return true;
        }
        if (generation(entity) > 0) return true;
        net.minecraft.world.effect.MobEffectInstance instance = markerEffect(living);
        return instance != null && instance.isInfiniteDuration();
    }

    /** 客户端收到服务端同步后写入。 */
    public static void acceptClientState(java.util.UUID entityId, int mask) {
        CLIENT_INFECTED.put(entityId, mask);
    }

    /** 切换世界/退出时清空，避免 UUID 复用导致误渲染。 */
    public static void clearClientState() {
        CLIENT_INFECTED.clear();
        CLIENT_ORGANS.clear();
    }

    /** 读取尸变同步标记：优先按对象匹配，失败则按注册名匹配（兼容客户端注册表实例差异）。 */
    private static net.minecraft.world.effect.MobEffectInstance markerEffect(
            net.minecraft.world.entity.LivingEntity living) {
        net.minecraft.world.effect.MobEffect marker = ModEffects.QIANS.get();
        net.minecraft.world.effect.MobEffectInstance direct = living.getEffect(marker);
        if (direct != null) return direct;
        String key = String.valueOf(net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.getKey(marker));
        for (net.minecraft.world.effect.MobEffectInstance instance : living.getActiveEffects()) {
            String other = String.valueOf(net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT
                    .getKey(instance.getEffect()));
            if (key.equals(other)) return instance;
        }
        return null;
    }

    public static boolean incubating(Entity entity) {
        Integer remaining = INCUBATION_CACHE.get(entity);
        return remaining != null && remaining > 0;
    }

    public static int appearance(Entity entity) {
        Integer mask = APPEARANCE_CACHE.get(entity);
        return mask == null ? 0 : mask;
    }

    /** 渲染用外观掩码：优先读网络同步状态，其次读 effect amplifier，最后读本地缓存。 */
    public static int renderAppearance(Entity entity) {
        Integer synced = CLIENT_INFECTED.get(entity.getUUID());
        if (synced != null) return synced & (EYE | RIBS);
        if (entity instanceof net.minecraft.world.entity.LivingEntity living) {
            net.minecraft.world.effect.MobEffectInstance instance = markerEffect(living);
            if (instance != null && instance.isInfiniteDuration()) {
                return instance.getAmplifier() & (EYE | RIBS);
            }
        }
        return appearance(entity);
    }

    public static String organLoadout(Entity entity) {
        if (entity.level().isClientSide()) return CLIENT_ORGANS.getOrDefault(entity.getUUID(), "[]");
        String saved = ORGANS_CACHE.get(entity);
        return saved == null ? "[]" : saved;
    }

    public static List<OrganSlot> organSlots(Entity entity) {
        return parseSlots(organLoadout(entity));
    }

    private static List<OrganSlot> parseSlots(String json) {
        try { return OrganLibrary.parseSlots(json); }
        catch (IllegalArgumentException e) { return List.of(); }
    }

    // ========== 写入 ==========

    public static void setAppearance(Entity entity, int mask) {
        APPEARANCE_CACHE.put(entity, mask);
    }

    public static void setOrganLoadout(Entity entity, String json) {
        ORGANS_CACHE.put(entity, OrganLibrary.toJson(OrganLibrary.parseSlots(json)));
    }

    public static void setIncubation(Entity entity, int ticks) {
        INCUBATION_CACHE.put(entity, Math.max(0, ticks));
    }

    // 感染源器官快照缓存（per-target，消费一次后清除）
    private static final Map<Entity, String> SOURCE_ORGANS_CACHE = new WeakHashMap<>();

    /** 快照感染源的器官配置（宿主未加载时无法访问源实体）。 */
    public static void captureSource(Entity target, Entity source) {
        if (!(target instanceof Mob)) return;
        String json = "[]";
        if (source != null && (infected(source) || source instanceof net.minecraft.world.entity.player.Player)) {
            json = organLoadout(source);
        }
        SOURCE_ORGANS_CACHE.put(target, json);
    }

    private static void initializeAppearance(Mob mob) {
        if (APPEARANCE_CACHE.containsKey(mob)) return;
        var rng = mob.getRandom();
        int flags = (rng.nextDouble() < 0.5 ? EYE : 0)
                | (rng.nextDouble() < 0.5 ? RIBS : 0);
        // 保证感染体至少有一处可见的骨骼/器官，避免「什么都没长」的观感
        if (flags == 0) flags = RIBS;
        APPEARANCE_CACHE.put(mob, flags);
        List<OrganSlot> inherited = new java.util.ArrayList<>();
        var source = parseSlots(SOURCE_ORGANS_CACHE.getOrDefault(mob, "[]")).stream().filter(s -> !s.replacesBody()).toList();
        for (var slot : source) {
            if (OrganLibrary.find(slot.organ()) != null && rng.nextFloat() < .5F) inherited.add(slot);
        }
        if (source.isEmpty() && rng.nextFloat() < .5F) {
            var pool = OrganLibrary.definitions().stream().filter(d -> OrganLibrary.isCustomOrgan(d.id())).toList();
            if (!pool.isEmpty()) inherited.add(new OrganSlot(pool.get(rng.nextInt(pool.size())).id(), "body", 0, 0, 0, 0, 0, 0, 1, false));
        }
        ORGANS_CACHE.putIfAbsent(mob, OrganLibrary.toJson(inherited));
        SOURCE_ORGANS_CACHE.remove(mob);
    }

    /**  incubation 倒计时结束后调用：在原实体上标记「已感染」。 */
    public static void transform(Mob mob, ServerLevel level) {
        if (!mob.isAlive() || infected(mob)) return;
        String prefix = HybridZombie.INFECTION_GENERATION + "_pending:";
        int generation = 1;
        for (String tag : List.copyOf(mob.getTags())) {
            if (!tag.startsWith(prefix)) continue;
            try { generation = Math.max(1, Integer.parseInt(tag.substring(prefix.length()))); }
            catch (NumberFormatException ignored) { }
            mob.removeTag(tag);
        }
        HybridZombie.tagAsHybrid(mob, EntityType.getKey(mob.getType()).toString(), HybridZombie.GRADE_HALF, generation);
        initializeAppearance(mob);
        INCUBATION_CACHE.remove(mob);
    }

    /** 保证已转化实体身上挂着用于客户端识别的无限 QIANS 标记。 */
    private static void ensureSyncMarker(Mob mob) {
        int mask = appearance(mob);
        net.minecraft.world.effect.MobEffectInstance current = mob.getEffect(ModEffects.QIANS.get());
        if (current != null && current.isInfiniteDuration() && current.getAmplifier() == mask) return;
        if (current != null) {
            mob.removeEffect(ModEffects.QIANS.get());
            if (mob.level() instanceof ServerLevel level) {
                level.getChunkSource().broadcast(mob, new net.minecraft.network.protocol.game
                        .ClientboundRemoveMobEffectPacket(mob.getId(), ModEffects.QIANS.get()));
            }
        }
        net.minecraft.world.effect.MobEffectInstance marker = new net.minecraft.world.effect.MobEffectInstance(
                ModEffects.QIANS.get(),
                net.minecraft.world.effect.MobEffectInstance.INFINITE_DURATION,
                mask, false, false, false);
        boolean added = mob.addEffect(marker);
        // 1.20.1 对「已在客户端追踪中的实体」新增效果不会自动同步，
        // 只有玩家开始追踪时才会补发（ServerPlayer#trackStart），所以这里手动广播。
        if (added && mob.level() instanceof ServerLevel level) {
            level.getChunkSource().broadcast(mob,
                    new net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket(mob.getId(), marker));
        }
        // 自定义同步：UUID + 外观掩码，渲染层不再依赖 effect/tag 是否同步
        com.xiaoshi2022.corpseorigin.network.CorpseOriginNetwork.sync(mob);
        com.xiaoshi2022.corpseorigin.network.OrganNetwork.sync(mob);
    }

    /** LivingEntity.tick() TAIL 处注入调用。 */
    public static void tick(Mob mob) {
        if (!(mob.level() instanceof ServerLevel level) || !mob.isAlive()) return;
        if (infected(mob)) {
            initializeAppearance(mob);
            ensureSyncMarker(mob);
        }
        Integer remaining = INCUBATION_CACHE.get(mob);
        if (remaining == null) return;
        if (remaining <= 1) {
            transform(mob, level);
            INCUBATION_CACHE.remove(mob);
        } else {
            INCUBATION_CACHE.put(mob, remaining - 1);
        }
    }

    // ========== Mixin 持久化钩子 ==========

    /** 在 Entity#addAdditionalSaveData 中调用：把缓存写进 CompoundTag。 */
    public static void writeNbt(Entity entity, CompoundTag tag) {
        Integer app = APPEARANCE_CACHE.get(entity);
        if (app != null) tag.putInt("co_nc_appearance", app);
        String organs = ORGANS_CACHE.get(entity);
        if (organs != null && !"[]".equals(organs)) tag.putString("co_nc_organs", organs);
        Integer inc = INCUBATION_CACHE.get(entity);
        if (inc != null) tag.putInt("co_nc_incubation", inc);
        String source = SOURCE_ORGANS_CACHE.get(entity);
        if (source != null) tag.putString("co_nc_source_organs", source);
    }

    /** 在 Entity#readAdditionalSaveData 中调用：从 CompoundTag 恢复缓存。 */
    public static void readNbt(Entity entity, CompoundTag tag) {
        if (tag.contains("co_nc_appearance")) APPEARANCE_CACHE.put(entity, tag.getInt("co_nc_appearance"));
        if (tag.contains("co_nc_organs")) ORGANS_CACHE.put(entity, tag.getString("co_nc_organs"));
        if (tag.contains("co_nc_incubation")) INCUBATION_CACHE.put(entity, tag.getInt("co_nc_incubation"));
        if (tag.contains("co_nc_source_organs")) SOURCE_ORGANS_CACHE.put(entity, tag.getString("co_nc_source_organs"));
    }
}
