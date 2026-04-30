package com.phagens.corpseorigin.entity;

import com.phagens.corpseorigin.CorpseOrigin;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;

import java.util.*;

public class CorpseSwarmIntelligence extends SavedData {
    
    private static final String DATA_NAME = "corpse_swarm_intelligence";
    
    private static net.minecraft.resources.ResourceLocation id(String path) {
        return net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(CorpseOrigin.MODID, path);
    }
    
    // 信息素类型
    public enum PheromoneType {
        ENEMY_LOCATION,    // 敌人位置标记
        GATHER_POINT,      // 集合点标记
        DANGER_ZONE,       // 危险区域标记
        FOOD_SOURCE        // 食物源标记
    }
    
    // 尸兄角色分工
    public enum SwarmRole {
        ATTACKER,    // 前锋：近战攻击
        THROWER,     // 投掷手：远程攻击
        HEALER,      // 治疗者：恢复友方
        SCOUT,       // 侦察兵：探索
        DEFENDER     // 防御者：保护首领
    }
    
    // 信息素数据类
    public static class Pheromone {
        public final PheromoneType type;
        public final BlockPos pos;
        public final UUID sourceId;
        public long expirationTime;
        public final float strength;
        public LivingEntity target; // 用于敌人位置标记
        
        public Pheromone(PheromoneType type, BlockPos pos, UUID sourceId, long duration, float strength) {
            this.type = type;
            this.pos = pos;
            this.sourceId = sourceId;
            this.expirationTime = System.currentTimeMillis() + duration;
            this.strength = strength;
            this.target = null;
        }
        
        public Pheromone(PheromoneType type, BlockPos pos, UUID sourceId, long duration, float strength, LivingEntity target) {
            this.type = type;
            this.pos = pos;
            this.sourceId = sourceId;
            this.expirationTime = System.currentTimeMillis() + duration;
            this.strength = strength;
            this.target = target;
        }
        
        public boolean isExpired() {
            return System.currentTimeMillis() > expirationTime;
        }
    }
    
    // 统帅光环效果
    public static class CommanderAura {
        public final UUID commanderId;
        public final double range;
        public final int level;
        public long activationTime;
        public final double damageBonus;
        public final double speedBonus;
        public final double defenseBonus;
        
        public CommanderAura(UUID commanderId, double range, int level) {
            this.commanderId = commanderId;
            this.range = range;
            this.level = level;
            this.activationTime = System.currentTimeMillis();
            this.damageBonus = 0.1 + (level * 0.05);
            this.speedBonus = 0.05 + (level * 0.025);
            this.defenseBonus = 0.1 + (level * 0.05);
        }
        
        public boolean isActive() {
            return System.currentTimeMillis() - activationTime < 30000; // 30秒持续
        }
        
        public void refresh() {
            this.activationTime = System.currentTimeMillis();
        }
    }
    
    private final List<Pheromone> pheromones = new java.util.concurrent.CopyOnWriteArrayList<>();
    private final Map<UUID, SwarmRole> swarmRoles = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<UUID, CommanderAura> activeAuras = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<UUID, UUID> leaderAssignments = new java.util.concurrent.ConcurrentHashMap<>(); // 尸兄 -> 首领
    
    public CorpseSwarmIntelligence() {
        super();
    }
    
    public CorpseSwarmIntelligence(CompoundTag nbt) {
        super();
        load(nbt);
    }
    
    public static CorpseSwarmIntelligence get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
            new SavedData.Factory<>(CorpseSwarmIntelligence::new, CorpseSwarmIntelligence::load),
            DATA_NAME
        );
    }
    
    public static CorpseSwarmIntelligence load(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        CorpseSwarmIntelligence data = new CorpseSwarmIntelligence();
        // 可以在这里添加加载逻辑
        return data;
    }
    
    // ===== 信息素通讯系统 =====
    
    /**
     * 放置信息素标记
     */
    public void placePheromone(ServerLevel level, PheromoneType type, BlockPos pos, UUID sourceId, long duration) {
        placePheromone(level, type, pos, sourceId, duration, 1.0f);
    }
    
    public void placePheromone(ServerLevel level, PheromoneType type, BlockPos pos, UUID sourceId, long duration, float strength) {
        pheromones.add(new Pheromone(type, pos, sourceId, duration, strength));
        setDirty();
        
        // 生成粒子效果
        if (type == PheromoneType.ENEMY_LOCATION) {
            level.sendParticles(
                net.minecraft.core.particles.ParticleTypes.ANGRY_VILLAGER,
                pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5,
                5, 0.3, 0.5, 0.3, 0.1
            );
        }
    }
    
    /**
     * 放置敌人位置信息素（带目标实体）
     */
    public void placeEnemyPheromone(ServerLevel level, BlockPos pos, UUID sourceId, LivingEntity target) {
        pheromones.add(new Pheromone(PheromoneType.ENEMY_LOCATION, pos, sourceId, 10000, 1.0f, target));
        setDirty();
        
        level.sendParticles(
            net.minecraft.core.particles.ParticleTypes.DAMAGE_INDICATOR,
            pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5,
            10, 0.5, 0.5, 0.5, 0.2
        );
    }
    
    /**
     * 获取附近的信息素
     */
    public List<Pheromone> getNearbyPheromones(BlockPos pos, double range) {
        List<Pheromone> result = new ArrayList<>();
        for (Pheromone p : pheromones) {
            if (!p.isExpired() && p.pos.distSqr(pos) <= range * range) {
                result.add(p);
            }
        }
        return result;
    }
    
    /**
     * 获取特定类型的信息素
     */
    public List<Pheromone> getPheromonesByType(PheromoneType type) {
        List<Pheromone> result = new ArrayList<>();
        for (Pheromone p : pheromones) {
            if (!p.isExpired() && p.type == type) {
                result.add(p);
            }
        }
        return result;
    }
    
    /**
     * 清除过期信息素
     */
    public void cleanExpiredPheromones() {
        pheromones.removeIf(Pheromone::isExpired);
    }
    
    // ===== 分工协作系统 =====
    
    /**
     * 为尸兄分配角色
     */
    public SwarmRole assignRole(UUID corpseId) {
        // 根据进化等级和随机因素分配角色
        Random random = new Random(corpseId.getMostSignificantBits());
        int roll = random.nextInt(100);
        
        SwarmRole role;
        if (roll < 40) {
            role = SwarmRole.ATTACKER;      // 40% 前锋
        } else if (roll < 60) {
            role = SwarmRole.THROWER;       // 20% 投掷手
        } else if (roll < 75) {
            role = SwarmRole.SCOUT;         // 15% 侦察兵
        } else if (roll < 85) {
            role = SwarmRole.DEFENDER;      // 10% 防御者
        } else {
            role = SwarmRole.HEALER;        // 5% 治疗者
        }
        
        swarmRoles.put(corpseId, role);
        setDirty();
        return role;
    }
    
    /**
     * 获取尸兄的角色
     */
    public SwarmRole getRole(UUID corpseId) {
        SwarmRole role = swarmRoles.get(corpseId);
        if (role == null) {
            role = assignRole(corpseId);
        }
        return role;
    }
    
    /**
     * 根据角色获取行为优先级调整
     */
    public int getRolePriorityBonus(SwarmRole role) {
        return switch (role) {
            case ATTACKER -> 2;    // 攻击优先级+2
            case THROWER -> 1;     // 远程攻击优先级+1
            case HEALER -> -1;     // 攻击优先级-1（优先治疗）
            case SCOUT -> 0;       // 正常优先级
            case DEFENDER -> -2;   // 攻击优先级-2（优先防御）
        };
    }
    
    // ===== 统帅链接系统 =====
    
    /**
     * 激活统帅光环
     */
    public void activateCommanderAura(UUID commanderId, double range, int level) {
        CommanderAura aura = activeAuras.computeIfAbsent(commanderId, 
            id -> new CommanderAura(id, range, level));
        aura.refresh();
        setDirty();
    }
    
    /**
     * 获取有效的统帅光环
     */
    public CommanderAura getActiveCommanderAura(UUID commanderId) {
        CommanderAura aura = activeAuras.get(commanderId);
        if (aura != null && !aura.isActive()) {
            activeAuras.remove(commanderId);
            return null;
        }
        return aura;
    }
    
    /**
     * 检查尸兄是否在统帅光环范围内
     */
    public boolean isInCommanderRange(ServerLevel level, UUID corpseId, UUID commanderId) {
        CommanderAura aura = getActiveCommanderAura(commanderId);
        if (aura == null) return false;
        
        net.minecraft.world.entity.Entity commanderEntity = level.getEntity(commanderId);
        net.minecraft.world.entity.Entity corpseEntity = level.getEntity(corpseId);
        
        if (!(commanderEntity instanceof Mob commander) || !(corpseEntity instanceof Mob corpse)) return false;
        
        return commander.distanceTo(corpse) <= aura.range;
    }
    
    /**
     * 获取尸兄附近的统帅光环
     */
    public CommanderAura getNearbyCommanderAura(ServerLevel level, Mob corpse) {
        for (CommanderAura aura : activeAuras.values()) {
            if (!aura.isActive()) continue;
            
            net.minecraft.world.entity.Entity commanderEntity = level.getEntity(aura.commanderId);
            if (!(commanderEntity instanceof Mob commander)) continue;
            
            if (commander.isAlive() && commander.distanceTo(corpse) <= aura.range) {
                return aura;
            }
        }
        return null;
    }
    
    /**
     * 应用统帅加成
     */
    public void applyCommanderBonus(Mob corpse, CommanderAura aura) {
        if (aura == null) return;
        
        // 攻击伤害加成
        AttributeModifier damageMod = new AttributeModifier(
            id("commander_damage_bonus"),
            aura.damageBonus,
            AttributeModifier.Operation.ADD_MULTIPLIED_BASE
        );
        corpse.getAttribute(Attributes.ATTACK_DAMAGE).addTransientModifier(damageMod);
        
        // 移动速度加成
        AttributeModifier speedMod = new AttributeModifier(
            id("commander_speed_bonus"),
            aura.speedBonus,
            AttributeModifier.Operation.ADD_MULTIPLIED_BASE
        );
        corpse.getAttribute(Attributes.MOVEMENT_SPEED).addTransientModifier(speedMod);
        
        // 护甲加成
        AttributeModifier armorMod = new AttributeModifier(
            id("commander_armor_bonus"),
            aura.defenseBonus,
            AttributeModifier.Operation.ADD_MULTIPLIED_BASE
        );
        corpse.getAttribute(Attributes.ARMOR).addTransientModifier(armorMod);
    }
    
    /**
     * 分配首领给尸兄
     */
    public void assignLeader(UUID corpseId, UUID leaderId) {
        leaderAssignments.put(corpseId, leaderId);
        setDirty();
    }
    
    /**
     * 获取尸兄的首领
     */
    public UUID getLeader(UUID corpseId) {
        return leaderAssignments.get(corpseId);
    }
    
    /**
     * 获取首领周围的所有尸兄
     */
    public List<Mob> getCorpsesUnderLeader(ServerLevel level, UUID leaderId) {
        List<Mob> result = new ArrayList<>();
        net.minecraft.world.entity.Entity leaderEntity = level.getEntity(leaderId);
        if (!(leaderEntity instanceof Mob leader)) {
            // 清理无效的首领分配
            leaderAssignments.entrySet().removeIf(entry -> entry.getValue().equals(leaderId));
            return result;
        }
        
        if (!leader.isAlive()) {
            // 清理无效的首领分配
            leaderAssignments.entrySet().removeIf(entry -> entry.getValue().equals(leaderId));
            return result;
        }
        
        AABB searchBox = leader.getBoundingBox().inflate(50.0D);
        List<Mob> nearbyMobs = level.getEntitiesOfClass(Mob.class, searchBox,
            mob -> mob instanceof ICorpseBrother && mob.isAlive() && mob != leader
        );
        
        for (Mob mob : nearbyMobs) {
            UUID corpseId = mob.getUUID();
            // 如果没有分配首领，自动分配
            if (!leaderAssignments.containsKey(corpseId)) {
                assignLeader(corpseId, leaderId);
            }
            if (leaderAssignments.get(corpseId).equals(leaderId)) {
                result.add(mob);
            }
        }
        
        return result;
    }
    
    // ===== 群体战术系统 =====
    
    /**
     * 根据群体规模调整战术
     */
    public void adjustTactics(ServerLevel level, LongyouEntity leader) {
        List<Mob> corpsUnderLeader = getCorpsesUnderLeader(level, leader.getUUID());
        int swarmSize = corpsUnderLeader.size();
        
        // 根据群体规模激活不同等级的统帅光环
        int auraLevel = Math.min(3, swarmSize / 5 + 1); // 每5个尸兄提升一级
        double auraRange = 20.0 + (auraLevel * 10);     // 基础20格，每级+10
        
        activateCommanderAura(leader.getUUID(), auraRange, auraLevel);
        
        // 生成战术粒子效果
        if (auraLevel >= 2) {
            level.sendParticles(
                net.minecraft.core.particles.ParticleTypes.GLOW,
                leader.getX(), leader.getY() + 1, leader.getZ(),
                20, 3, 2, 3, 0.1
            );
        }
    }
    
    /**
     * 更新所有附近尸兄的状态
     */
    public void updateSwarm(ServerLevel level, LongyouEntity leader) {
        List<Mob> corpsUnderLeader = getCorpsesUnderLeader(level, leader.getUUID());
        
        for (Mob mob : corpsUnderLeader) {
            ICorpseBrother brother = (ICorpseBrother) mob;
            CommanderAura aura = getNearbyCommanderAura(level, mob);
            
            // 应用统帅加成
            if (aura != null) {
                applyCommanderBonus(mob, aura);
            }
            
            // 发送统帅指令
            if (leader.getTarget() != null && brother.getHiveMindTarget() == null) {
                brother.setHiveMindTarget(leader.getTarget());
            }
        }
    }
    
    // ===== 保存/加载 =====
    
    @Override
    public CompoundTag save(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries) {
        // 保存信息素
        CompoundTag pheromonesTag = new CompoundTag();
        for (int i = 0; i < pheromones.size(); i++) {
            Pheromone p = pheromones.get(i);
            CompoundTag pTag = new CompoundTag();
            pTag.putString("type", p.type.name());
            pTag.putInt("posX", p.pos.getX());
            pTag.putInt("posY", p.pos.getY());
            pTag.putInt("posZ", p.pos.getZ());
            pTag.putUUID("sourceId", p.sourceId);
            pTag.putLong("expirationTime", p.expirationTime);
            pTag.putFloat("strength", p.strength);
            pheromonesTag.put("pheromone_" + i, pTag);
        }
        nbt.put("pheromones", pheromonesTag);
        
        // 保存角色分配
        CompoundTag rolesTag = new CompoundTag();
        for (Map.Entry<UUID, SwarmRole> entry : swarmRoles.entrySet()) {
            rolesTag.putString(entry.getKey().toString(), entry.getValue().name());
        }
        nbt.put("swarmRoles", rolesTag);
        
        return nbt;
    }
    
    public void load(CompoundTag nbt) {
        // 加载信息素
        if (nbt.contains("pheromones")) {
            CompoundTag pheromonesTag = nbt.getCompound("pheromones");
            for (String key : pheromonesTag.getAllKeys()) {
                CompoundTag pTag = pheromonesTag.getCompound(key);
                try {
                    PheromoneType type = PheromoneType.valueOf(pTag.getString("type"));
                    BlockPos pos = new BlockPos(
                        pTag.getInt("posX"),
                        pTag.getInt("posY"),
                        pTag.getInt("posZ")
                    );
                    UUID sourceId = pTag.getUUID("sourceId");
                    long expirationTime = pTag.getLong("expirationTime");
                    float strength = pTag.getFloat("strength");
                    
                    Pheromone p = new Pheromone(type, pos, sourceId, 0, strength);
                    p.expirationTime = expirationTime;
                    if (!p.isExpired()) {
                        pheromones.add(p);
                    }
                } catch (Exception e) {
                    CorpseOrigin.LOGGER.warn("Failed to load pheromone: {}", e.getMessage());
                }
            }
        }
        
        // 加载角色分配
        if (nbt.contains("swarmRoles")) {
            CompoundTag rolesTag = nbt.getCompound("swarmRoles");
            for (String key : rolesTag.getAllKeys()) {
                try {
                    UUID corpseId = UUID.fromString(key);
                    SwarmRole role = SwarmRole.valueOf(rolesTag.getString(key));
                    swarmRoles.put(corpseId, role);
                } catch (Exception e) {
                    CorpseOrigin.LOGGER.warn("Failed to load swarm role: {}", e.getMessage());
                }
            }
        }
    }
    
    // ===== 定时清理 =====
    
    public void tick() {
        cleanExpiredPheromones();
        
        // 清理过期的统帅光环
        activeAuras.entrySet().removeIf(entry -> !entry.getValue().isActive());
    }
}