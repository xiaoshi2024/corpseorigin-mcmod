package xiaoshi2022.corpseorigin.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import xiaoshi2022.corpseorigin.block.entity.CloneChamberBlockEntity;
import xiaoshi2022.corpseorigin.character.LongYou;
import xiaoshi2022.corpseorigin.network.CorpseNetwork;
import xiaoshi2022.corpseorigin.shell.CharacterShellStateComponent;
import xiaoshi2022.corpseorigin.shell.ShellBodyIndex;
import xiaoshi2022.corpseorigin.shell.ShellState;
import xiaoshi2022.corpseorigin.shell.TransferredBody;
import xiaoshi2022.corpseorigin.skill.longyou.InfrasoundFieldHandler;

import java.util.UUID;

/**
 * 移动备用克隆身体。
 * <p>
 * 由克隆仓培育完成后激活生成，持有玩家的一份完整状态快照 {@link #bodyState}。
 * 玩家可以：
 * <ul>
 *   <li>部分意识转移：分身按自己的 AI 行动（未来扩展）；</li>
 *   <li>完全意识转移：玩家死亡或主动使用存储仓时，把 bodyState apply 到 ServerPlayer。</li>
 * </ul>
 */
public class CloneAvatarEntity extends PathfinderMob implements TransferredBody {

    /** 同步到客户端的 owner UUID */
    private static final EntityDataAccessor<String> DATA_OWNER_UUID =
            SynchedEntityData.defineId(CloneAvatarEntity.class, EntityDataSerializers.STRING);

    /** 同步：是否已激活（激活后 BER 不再渲染假人） */
    private static final EntityDataAccessor<Boolean> DATA_ACTIVE =
            SynchedEntityData.defineId(CloneAvatarEntity.class, EntityDataSerializers.BOOLEAN);

    /** 同步：培育进度，用于渲染 */
    private static final EntityDataAccessor<Float> DATA_PROGRESS =
            SynchedEntityData.defineId(CloneAvatarEntity.class, EntityDataSerializers.FLOAT);

    /** 同步：这具身体是不是尸兄克隆体（尸水培育出来的），决定 AI 是否像低阶尸兄 */
    private static final EntityDataAccessor<Boolean> DATA_CORPSE_CLONE =
            SynchedEntityData.defineId(CloneAvatarEntity.class, EntityDataSerializers.BOOLEAN);

    /** 同步：这具身体穿的盔甲（头/胸/腿/脚），客户端渲染要用 */
    private static final EntityDataAccessor<ItemStack> DATA_HEAD_EQUIPMENT =
            SynchedEntityData.defineId(CloneAvatarEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<ItemStack> DATA_CHEST_EQUIPMENT =
            SynchedEntityData.defineId(CloneAvatarEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<ItemStack> DATA_LEGS_EQUIPMENT =
            SynchedEntityData.defineId(CloneAvatarEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<ItemStack> DATA_FEET_EQUIPMENT =
            SynchedEntityData.defineId(CloneAvatarEntity.class, EntityDataSerializers.ITEM_STACK);

    /** 服务端持有的完整身体快照，不参与实体同步（走自定义包或 BE 数据） */
    @Nullable
    private ShellState bodyState;

    /** 派生出这具分身的克隆仓（位置）：夺舍后旧身体要还回那座仓，才能来回换 */
    @Nullable
    private BlockPos sourceChamberPos;

    public CloneAvatarEntity(EntityType<? extends CloneAvatarEntity> type, Level level) {
        super(type, level);

        // 身上的盔甲必定掉落：原版生物默认掉率只有 8.5%，这具身体被击杀时盔甲基本等于蒸发
        for (EquipmentSlot slot : ShellState.EQUIPMENT_SLOTS) {
            this.setDropChance(slot, 2.0F);
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_OWNER_UUID, "");
        builder.define(DATA_ACTIVE, false);
        builder.define(DATA_PROGRESS, 0.0F);
        builder.define(DATA_CORPSE_CLONE, false);
        builder.define(DATA_HEAD_EQUIPMENT, net.minecraft.world.item.ItemStack.EMPTY);
        builder.define(DATA_CHEST_EQUIPMENT, net.minecraft.world.item.ItemStack.EMPTY);
        builder.define(DATA_LEGS_EQUIPMENT, net.minecraft.world.item.ItemStack.EMPTY);
        builder.define(DATA_FEET_EQUIPMENT, net.minecraft.world.item.ItemStack.EMPTY);
    }

    // ==================== 同步数据访问 ====================

    public void setOwnerUuid(@Nullable UUID uuid) {
        this.entityData.set(DATA_OWNER_UUID, uuid == null ? "" : uuid.toString());
    }

    /**
     * 设置本体玩家的名字，并常显在头顶名字牌上。
     * <p>
     * 直接走原版的 {@link #setCustomName} —— 它本身就是被同步、被存档的实体字段，
     * 所以不需要再额外加一个同步字段；客户端也无需自己拼名字。
     * <p>
     * 名字来自培育时记录的本体名（{@code CloneState#getOwnerName}），
     * 所以本体不在线时分身照样有名字。
     */
    public void setOwnerName(@Nullable String name) {
        if (name == null || name.isEmpty()) {
            this.setCustomName(null);
            this.setCustomNameVisible(false);
            return;
        }
        this.setCustomName(Component.literal(name));
        this.setCustomNameVisible(true);
    }

    @Nullable
    public UUID getOwnerUuid() {
        String s = this.entityData.get(DATA_OWNER_UUID);
        if (s.isEmpty()) return null;
        try {
            return UUID.fromString(s);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public void setActive(boolean active) {
        this.entityData.set(DATA_ACTIVE, active);
    }

    public boolean isActive() {
        return this.entityData.get(DATA_ACTIVE);
    }

    public void setProgress(float progress) {
        this.entityData.set(DATA_PROGRESS, progress);
    }

    public float getProgress() {
        return this.entityData.get(DATA_PROGRESS);
    }

    // ==================== 身体快照 ====================

    @Nullable
    public ShellState getBodyState() {
        return this.bodyState;
    }

    public void setBodyState(@Nullable ShellState bodyState) {
        this.bodyState = bodyState;
        this.syncEquipment(bodyState);
        this.syncBodyCorpseData(true);
    }

    /**
     * 把这具身体自己的尸兄状态发出去。
     * <p>
     * 客户端按分身 uuid 缓存，于是分身的外骨骼/多眼按它自己那份状态渲染，
     * 而不是沿用账号当前那份。
     * <p>
     * ★ 广播给同维度所有玩家：分身别人也看得见，只发给 owner 别人会看不到外骨骼。
     */
    private void syncBodyCorpseData(boolean healWhenInherited) {
        if (this.level().isClientSide() || this.bodyState == null) {
            return;
        }
        net.minecraft.nbt.CompoundTag tag =
                xiaoshi2022.corpseorigin.shell.ShellState.corpseTagOf(this.bodyState.getComponent());
        // 尸水培育出来的克隆体是尸兄：行为也按低阶尸兄走
        boolean corpseClone = tag != null && tag.getBoolean("is_corpse").orElse(false);
        // 只在"从干净人形变成尸兄克隆体"这一次做属性迁移，
        // 否则每次重发身体状况都会把血回满
        if (corpseClone && !this.isCorpseClone()) {
            this.inheritLowerLevelZbAttributes(healWhenInherited);
        }
        this.entityData.set(DATA_CORPSE_CLONE, corpseClone);
        // 这具身体是不是"龙右身体"：决定它会不会像尸王那样自己放次声波
        this.corpseKingBody = isCorpseKingBody(this.bodyState);
        // 身体的大小（尸王原体是缩小版）
        AttributeInstance scale = this.getAttribute(Attributes.SCALE);
        if (scale != null) {
            scale.setBaseValue(this.bodyState == null ? 1.0F : this.bodyState.getScale());
        }
        // 原体体术：跳得高、摔不伤（"原体"形态的分身同样适用）
        LongYou.applyOriginalBodyAgility(this, this.bodyState != null && this.bodyState.getScale() < 1.0F);
        // 龙右身体要套上尸王的基础数值（血量 50 / 护甲 10 / 击退抗性 50% ……）
        LongYou.applyIfLongYou(this);
        if (healWhenInherited && this.corpseKingBody) {
            this.setHealth(this.getMaxHealth());
        }

        if (this.level() instanceof ServerLevel level) {
            CorpseNetwork.broadcastBodyCorpseSync(level, this.ownerPlayer(), this.getUUID(), tag);
        }
    }

    /** 把身体自带的盔甲同步给客户端（渲染克隆人时用） */
    private void syncEquipment(@Nullable ShellState bodyState) {
        java.util.List<net.minecraft.world.item.ItemStack> equipment =
                bodyState == null ? java.util.List.of() : bodyState.getEquipment();
        this.entityData.set(DATA_HEAD_EQUIPMENT, equipmentAt(equipment, 0));
        this.entityData.set(DATA_CHEST_EQUIPMENT, equipmentAt(equipment, 1));
        this.entityData.set(DATA_LEGS_EQUIPMENT, equipmentAt(equipment, 2));
        this.entityData.set(DATA_FEET_EQUIPMENT, equipmentAt(equipment, 3));
    }

    private static net.minecraft.world.item.ItemStack equipmentAt(
            java.util.List<net.minecraft.world.item.ItemStack> equipment, int index) {
        return index < equipment.size() ? equipment.get(index) : net.minecraft.world.item.ItemStack.EMPTY;
    }

    /** 客户端：取出这具身体穿的盔甲（顺序：头/胸/腿/脚） */
    public java.util.List<net.minecraft.world.item.ItemStack> clientEquipment() {
        return java.util.List.of(
                this.entityData.get(DATA_HEAD_EQUIPMENT),
                this.entityData.get(DATA_CHEST_EQUIPMENT),
                this.entityData.get(DATA_LEGS_EQUIPMENT),
                this.entityData.get(DATA_FEET_EQUIPMENT));
    }

    /**
     * 把同步过来的盔甲真正落到实体槽位上（客户端/服务端都会走）。
     * <p>
     * <b>为什么非落不可</b>：GeckoLib 的盔甲渲染是从 {@code LivingEntity.getItemBySlot} 读装备的
     * （见 {@code GeoArmorRenderer.getRelevantSlotsForRendering}）。只把盔甲写进同步字段、真实槽位空着的话，
     * 它根本找不到可渲染的盔甲，就放行给原版盔甲层 —— 于是天线宝宝盔甲被按 {@code ArmorMaterial}
     * 画成了钻石甲。落到真实槽位后两边口径一致，geo 通道才会接管。
     */
    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);

        if (DATA_HEAD_EQUIPMENT.equals(key)) {
            mirrorArmor(DATA_HEAD_EQUIPMENT, EquipmentSlot.HEAD);
        } else if (DATA_CHEST_EQUIPMENT.equals(key)) {
            mirrorArmor(DATA_CHEST_EQUIPMENT, EquipmentSlot.CHEST);
        } else if (DATA_LEGS_EQUIPMENT.equals(key)) {
            mirrorArmor(DATA_LEGS_EQUIPMENT, EquipmentSlot.LEGS);
        } else if (DATA_FEET_EQUIPMENT.equals(key)) {
            mirrorArmor(DATA_FEET_EQUIPMENT, EquipmentSlot.FEET);
        }
    }

    /** 同步字段 → 真实槽位（要 copy：同步数据返回的是同一个 ItemStack 实例，不能直接共享） */
    private void mirrorArmor(EntityDataAccessor<ItemStack> key, EquipmentSlot slot) {
        ItemStack stack = this.entityData.get(key);
        this.setItemSlot(slot, stack.isEmpty() ? ItemStack.EMPTY : stack.copy());
    }

    public void setSourceChamber(@Nullable BlockPos pos) {
        this.sourceChamberPos = pos;
    }

    /** 取出生它的克隆仓（在分身自己所在维度里按需加载；仓没了返回 null） */
    @Nullable
    public CloneChamberBlockEntity sourceChamber() {
        if (this.sourceChamberPos == null || !(this.level() instanceof ServerLevel level)) {
            return null;
        }
        level.getChunkAt(this.sourceChamberPos);
        return level.getBlockEntity(this.sourceChamberPos) instanceof CloneChamberBlockEntity chamber
                ? chamber : null;
    }

    /** 从玩家创建一份快照，作为这具分身的"可转移身体" */
    public void captureFrom(ServerPlayer player) {
        this.setOwnerUuid(player.getUUID());
        this.setOwnerName(player.getName().getString());
        this.bodyState = ShellState.of(player, this.blockPosition());
        this.setActive(true);
        this.setProgress(1.0F);
    }

    // ==================== 基础行为 ====================

    /**
     * 基础 AI：会浮水、随机漫步（避开水）、看向附近的玩家、原地张望。
     * <p>
     * 攻击与索敌目标在这里一并注册，但是用 {@link #isCorpseClone()} 门控：
     * 清水培育出的干净人形保持被动，尸水培育出的尸兄克隆体才会像低阶尸兄那样主动攻击。
     */
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new CorpseMeleeGoal());
        this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));

        // 索敌同样只在尸兄克隆体上启用
        this.targetSelector.addGoal(0, new CorpseRetaliateGoal());
        this.targetSelector.addGoal(1, new CorpseTargetGoal());
    }

    /** 这具身体是不是尸兄克隆体（尸水培育出来的） */
    public boolean isCorpseClone() {
        return this.entityData.get(DATA_CORPSE_CLONE);
    }

    /**
     * 这具身体是不是"龙右身体"（尸王）。
     * <p>
     * 由 {@link #syncBodyCorpseData} 在身体状况同步时算好缓存下来 ——
     * 每 tick 都去翻一遍 NBT 里的角色 id 没必要。
     */
    private static boolean isCorpseKingBody(@Nullable ShellState state) {
        if (state == null) {
            return false;
        }
        CharacterShellStateComponent character = state.getComponent().as(CharacterShellStateComponent.class);
        return character != null && LongYou.ID.equals(character.getCharacterId());
    }

    /** 这具身体是不是龙右身体（缓存值，见 {@link #syncBodyCorpseData}） */
    public boolean isCorpseKingBody() {
        return this.corpseKingBody;
    }

    // ==================== 尸王分身：自动次声波 ====================

    /** 自己放次声波的间隔（12 秒，和玩家那招的冷却对齐） */
    private static final int INFRASOUND_INTERVAL = 240;
    /** 身边没目标时的重试间隔（1 秒）：别每 tick 都扫一遍实体 */
    private static final int INFRASOUND_RETRY = 20;

    /** 这具身体是不是龙右身体（由身体状况同步刷新） */
    private boolean corpseKingBody;
    /** 距下一次放次声波还有多少 tick：刚培育出来的身体先等满一个间隔 */
    private int infrasoundCooldown = INFRASOUND_INTERVAL;

    /**
     * 龙右身体的克隆分身会像尸王一样自己放次声波：震散飞来的箭矢、压制周围的人，
     * 顺便把附近的尸兄收编成听命于自己的打手（目标就是分身自己正在打的那个）。
     * <p>
     * 身边没有值得放的东西（没人、没箭）就不放 —— 免得白放一发还刷一堆粒子。
     */
    private void tickInfrasound() {
        if (!this.corpseKingBody || InfrasoundFieldHandler.isActive(this.getUUID())) {
            return;
        }
        if (this.infrasoundCooldown > 0) {
            this.infrasoundCooldown--;
            return;
        }
        if (!this.hasInfrasoundTarget()) {
            this.infrasoundCooldown = INFRASOUND_RETRY;
            return;
        }
        if (InfrasoundFieldHandler.start(this)) {
            this.infrasoundCooldown = INFRASOUND_INTERVAL;
        }
    }

    /** 身边有没有"值得放次声波"的东西：活着的生物（本体不算），或者飞行中的投掷物 */
    private boolean hasInfrasoundTarget() {
        AABB area = this.getBoundingBox().inflate(InfrasoundFieldHandler.FIELD_RADIUS);
        if (!this.level().getEntitiesOfClass(AbstractArrow.class, area).isEmpty()) {
            return true;
        }
        UUID owner = this.getOwnerUuid();
        return !this.level().getEntitiesOfClass(LivingEntity.class, area,
                e -> e != this && e.isAlive() && (owner == null || !owner.equals(e.getUUID()))).isEmpty();
    }

    // ---- 下面三个目标只在尸兄克隆体上生效，干净人形克隆体保持被动 ----

    private final class CorpseMeleeGoal extends MeleeAttackGoal {
        CorpseMeleeGoal() { super(CloneAvatarEntity.this, 1.2D, true); }

        @Override
        public boolean canUse() { return isCorpseClone() && super.canUse(); }

        @Override
        public boolean canContinueToUse() { return isCorpseClone() && super.canContinueToUse(); }
    }

    private final class CorpseRetaliateGoal extends HurtByTargetGoal {
        CorpseRetaliateGoal() { super(CloneAvatarEntity.this); }

        @Override
        public boolean canUse() {
            // 尸王打了也不还手：尸族对龙右只有敬畏（尸兄克隆体也一样）
            return isCorpseClone()
                    && !ZombieKin.isZombieKing(getLastHurtByMob())
                    && super.canUse();
        }
    }

    /**
     * 尸兄克隆体的索敌：抓"活人"。
     * <p>
     * ⚠️ 原来是 {@code NearestAttackableTargetGoal<Player>} —— 只认玩家，
     * 所以村民从它面前走过完全不会触发，看着就不像尸兄。
     * 现在放宽到 {@link LivingEntity}，再用 {@link #isPrey} 收窄成"人形猎物"。
     */
    private final class CorpseTargetGoal extends NearestAttackableTargetGoal<LivingEntity> {
        CorpseTargetGoal() {
            super(CloneAvatarEntity.this, LivingEntity.class, 10, true, false,
                    (target, level) -> isPrey(target));
        }

        @Override
        public boolean canUse() { return isCorpseClone() && super.canUse(); }

        @Override
        public boolean canContinueToUse() { return isCorpseClone() && super.canContinueToUse(); }
    }

    /**
     * 尸兄要吃的东西：玩家、村民（含流浪商人），以及护村的铁傀儡。
     * <p>
     * 刻意不放进牛羊猪这类动物 —— 原版僵尸也不猎食它们，尸兄吃的是"人"。
     * 尸族（{@link ZombieKin}）也排除掉，免得克隆体去啃同类。
     */
    private static boolean isPrey(LivingEntity target) {
        if (target instanceof ZombieKin) {
            return false;
        }
        // 尸王：尸族不敢对他不敬，尸兄克隆体也一样
        if (ZombieKin.isZombieKing(target)) {
            return false;
        }
        return target instanceof Player
                || target instanceof Villager
                || target instanceof AbstractVillager
                || target instanceof IronGolem;
    }

    /**
     * 备用身体不按距离自然消失。
     * <p>
     * 默认的 Mob 会在附近没有玩家时被清理掉，那样玩家走远一次就丢了这具身体。
     */
    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    // ==================== 属性 ====================

    /**
     * 移动速度。
     * <p>
     * 原版玩家的 {@code MOVEMENT_SPEED} 是 0.1，但同一个数值直接给生物会显得偏慢 ——
     * 属性值只是"目标速度"，AI 走路时会不断重新选路、绕障碍、进出水、在目标之间停顿，
     * 实际平均速度远达不到按属性值换算出来的水平（原版僵尸干脆用了 0.23 来抵消这一点）。
     * <p>
     * 这里取 0.13（= 玩家疾跑速度 <code>0.1 × 1.3</code>），让分身跟得上正常行走或小跑的玩家。
     * 还嫌跟不上就往上调，0.2 ~ 0.23 是原版僵尸/骷髅的水平。
     */
    public static final double MOVEMENT_SPEED = 0.13;

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, MOVEMENT_SPEED)
                .add(Attributes.ATTACK_DAMAGE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 16.0);
    }

    /**
     * 尸兄克隆体直接继承低等尸兄的属性。
     * <p>
     * 上面 {@link #createAttributes()} 给的是"干净人形"的底子（速度 0.13、伤害 1.0、无护甲），
     * 尸水培育出来的克隆体不该是这个水平 —— 它跟 {@link LowerLevelZbEntity} 是同一种东西，
     * 血量 / 攻击 / 速度 / 护甲 / 索敌范围都该照搬。
     * <p>
     * 数值不写死，而是现取 {@link LowerLevelZbEntity#createAttributes()} 的基准值，
     * 以后调整低等尸兄的属性时这里会自动跟着变，不会对不上。
     * <p>
     * ⚠️ 必须用 {@code setBaseValue} 而不是替换 AttributeSupplier：实体属性表在构造时就固定了，
     * 这里只改基础数值，既保留了装备/药水等修饰符的槽位，也不需要重建实体。
     *
     * @param heal true = 回满血（刚苏醒的身体是满状态的）；
     *             false = 只把超出新上限的血量收回去（重载时用，免得变成"重进世界就回血"）
     */
    private void inheritLowerLevelZbAttributes(boolean heal) {
        AttributeSupplier source = LowerLevelZbEntity.createAttributes().build();
        copyBaseValue(source, Attributes.MAX_HEALTH);
        copyBaseValue(source, Attributes.ATTACK_DAMAGE);
        copyBaseValue(source, Attributes.MOVEMENT_SPEED);
        copyBaseValue(source, Attributes.ARMOR);
        copyBaseValue(source, Attributes.FOLLOW_RANGE);
        if (heal || this.getHealth() > this.getMaxHealth()) {
            this.setHealth(this.getMaxHealth());
        }
    }

    /** 把 source 里某个属性的基础值搬到自己身上（自己没有该属性时跳过） */
    private void copyBaseValue(AttributeSupplier source, Holder<Attribute> attribute) {
        AttributeInstance instance = this.getAttribute(attribute);
        if (instance != null) {
            instance.setBaseValue(source.getBaseValue(attribute));
        }
    }

    // ==================== 持久化 ====================

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        UUID owner = this.getOwnerUuid();
        if (owner != null) {
            out.putString("Owner", owner.toString());
        }
        out.putBoolean("Active", this.isActive());
        out.putFloat("Progress", this.getProgress());
        if (this.sourceChamberPos != null) {
            out.putLong("SourceChamber", this.sourceChamberPos.asLong());
        }
        if (this.bodyState != null) {
            this.bodyState.writeTo(out.child("BodyState"));
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        in.getString("Owner").ifPresent(s -> {
            try {
                this.setOwnerUuid(UUID.fromString(s));
            } catch (IllegalArgumentException ignored) {
            }
        });
        this.setActive(in.getBooleanOr("Active", false));
        this.setProgress(in.getFloatOr("Progress", 0.0F));
        this.sourceChamberPos = in.getLong("SourceChamber").isPresent()
                ? BlockPos.of(in.getLongOr("SourceChamber", 0L)) : null;
        // ★ 走 setBodyState 的派生逻辑而不是直接赋值：它会把"是不是尸兄克隆体"
        //   （连带低等尸兄属性）和身上这套盔甲重新推出来。直接写字段的话，重进世界后
        //   尸兄克隆体会退回干净人形的属性、盔甲也不会回到槽位上。
        //   这里传 false：重载只恢复属性，不顺手回血。
        this.bodyState = in.child("BodyState").map(ShellState::read).orElse(null);
        this.syncEquipment(this.bodyState);
        this.syncBodyCorpseData(false);
    }

    // ==================== 消失处理 ====================

    /**
     * 分身被击杀时，把这具身体自带的背包也吐出来。
     * <p>
     * 快照里的 {@code Inventory} 是玩家存档格式（主背包 36 格），平时只躺在 {@code bodyState} 里，
     * 不吐出来的话这具身体一死，里面的东西就跟着蒸发了。
     * <p>
     * 盔甲不归这里管：它已经被真正穿在实体槽位上（见 {@link #onSyncedDataUpdated}），
     * 由 {@code Mob.dropCustomDeathLoot} 按构造函数里设的 2.0 掉率必定掉出来。
     */
    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);

        if (this.bodyState == null || this.bodyState.getBody() == null) {
            return;
        }

        ValueInput input = TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(),
                this.bodyState.getBody().asCompoundTag());
        for (ItemStackWithSlot entry : input.listOrEmpty("Inventory", ItemStackWithSlot.CODEC)) {
            if (!entry.stack().isEmpty()) {
                this.drop(entry.stack().copy(), false, false);
            }
        }
    }

    /**
     * 分身消失（被夺舍 / 死亡 / 丢弃）后：从身体索引里注销，并刷新 owner 的列表。
     */
    @Override
    public void remove(RemovalReason reason) {
        super.remove(reason);
        if (this.level().isClientSide() || !(this.level() instanceof ServerLevel level)) return;

        ServerPlayer owner = this.ownerPlayer();
        if (owner != null) {
            ShellBodyIndex.remove(owner, new ShellBodyIndex.Entry(
                    level.dimension().identifier(), this.blockPosition(), this.getUUID()));
            CorpseNetwork.refreshShellStates(owner);
        }
    }

    @Nullable
    private ServerPlayer ownerPlayer() {
        UUID ownerUuid = this.getOwnerUuid();
        if (ownerUuid == null || !(this.level() instanceof ServerLevel level) || level.getServer() == null) {
            return null;
        }
        return level.getServer().getPlayerList().getPlayer(ownerUuid);
    }

    // ==================== 服务端 tick ====================

    /** 最近一次写进身体索引的区块：分身跨区块时更新索引坐标，区块卸载后也能被按需加载找到 */
    private long indexedChunk = Long.MIN_VALUE;

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide() || !this.isActive()) {
            return;
        }

        // 每 5 秒补发一次尸兄状态（客户端重连后自愈）
        if (this.tickCount % 100 == 0) {
            this.syncBodyCorpseData(false);
        }

        // 龙右身体：像尸王那样自己放次声波
        this.tickInfrasound();

        BlockPos pos = this.blockPosition();
        long chunk = ChunkPos.pack(pos);
        if (chunk == this.indexedChunk) {
            return;
        }
        this.indexedChunk = chunk;

        ServerPlayer owner = this.ownerPlayer();
        if (owner != null) {
            ShellBodyIndex.upsert(owner, new ShellBodyIndex.Entry(
                    this.level().dimension().identifier(), pos, this.getUUID()));
        }
    }

    // ==================== 作为转移目标 ====================

    /**
     * 把这具分身的身体应用给玩家（完全意识转移）。
     * 具体 apply 逻辑放在 ServerShell 里，这里只提供数据。
     */
    public boolean canBeTransferred() {
        return this.isActive() && this.bodyState != null;
    }

    /**
     * 转移完成后，分身实体本身应该被移除（身体已被玩家占用）。
     */
    public void consumeOnTransfer(ServerLevel level) {
        this.bodyState = null;
        this.discard();
    }

    @Override
    public UUID ownerUuid() {
        return this.getOwnerUuid();
    }

    @Override
    public ShellState snapshot() {
        return this.bodyState;
    }

    @Override
    public boolean ready() {
        return this.canBeTransferred();
    }

    @Override
    public void consume(ServerLevel level) {
        this.consumeOnTransfer(level);
    }
}