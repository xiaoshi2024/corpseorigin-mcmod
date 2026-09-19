package xiaoshi2022.corpseorigin.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import xiaoshi2022.corpseorigin.component.MutantForm;
import xiaoshi2022.corpseorigin.config.CorpseConfig;

/**
 * 左护法「蛟龙」身上的一节碰撞箱（仿原版末影龙的 {@code EnderDragonPart}）。
 * <p>
 * 变异体形态下玩家自己的原版箱子被设成"不可被射线选中"，挨打改由这些节承担：
 * <ul>
 *   <li>每节都是真实体（会同步到客户端），所以<b>近战射线能选中它</b> —— 这一点必须走实体，
 *       客户端选不中的目标根本发不出攻击包；</li>
 *   <li>进游戏开 {@code F3+B} 看得见这些箱子，方便对着模型调偏移；</li>
 *   <li>打到任意一节 → {@link #hurtServer} 把伤害原样转给主人（玩家本体），
 *       所以护甲、药水、击退、击杀归属全都跟"直接打到人"一致；</li>
 *   <li>不物理、不下坠、不推人、不挡路，也不参与存档（主人没了就销毁）。</li>
 * </ul>
 * ⚠️ 位置只能按"相对主人、按朝向旋转"的固定偏移摆（{@link CorpseConfig.MutantBody.Hitboxes.Segment}）——
 * 蛇身到底在哪是客户端 GeckoLib 动画算的，服务端拿不到，所以这套偏移是要手调的。
 */
public class GuardianPartEntity extends Entity implements OwnerBound {

    /** 主人 UUID（同步给客户端：本人的客户端要跳过自己身体的箱子，否则自己砍不到敌人） */
    private static final EntityDataAccessor<String> DATA_OWNER_UUID =
            SynchedEntityData.defineId(GuardianPartEntity.class, EntityDataSerializers.STRING);

    /** 主人实体；只有服务端有值（节由服务端生成） */
    private Player owner;

    /** 这一节在配置里的名字，只用于日志/命令 */
    private String partName = "segment";

    private float width = 1.0F;
    private float height = 1.0F;
    private EntityDimensions dimensions = EntityDimensions.scalable(1.0F, 1.0F);

    /** 配置里这一节的偏移（相对主人、按主人朝向旋转） */
    private float forward;
    private float up;
    private float right;

    public GuardianPartEntity(EntityType<? extends GuardianPartEntity> type, Level level) {
        super(type, level);
        // 只是个"挨打的靶子"：不物理、不下坠、不推人
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    // ==================== 配置 ====================

    /** 把配置里这一节的偏移与尺寸套上来（尺寸变了才重算包围盒） */
    public void applySegment(CorpseConfig.MutantBody.Hitboxes.Segment segment) {
        this.partName = segment.name;
        this.forward = segment.forward;
        this.up = segment.up;
        this.right = segment.right;
        if (this.width != segment.width || this.height != segment.height) {
            this.width = segment.width;
            this.height = segment.height;
            this.dimensions = EntityDimensions.scalable(this.width, this.height);
            this.refreshDimensions();
        }
    }

    public String getPartName() {
        return this.partName;
    }

    public void setOwner(Player owner) {
        this.owner = owner;
        this.entityData.set(DATA_OWNER_UUID, owner.getUUID().toString());
    }

    /**
     * 摆到"相对主人、按主人朝向旋转"的位置上。
     * <p>
     * 箱子本身是轴对齐的（原版 AABB 没法转），所以只挪中心点 —— 也正因为如此，
     * 节用"前 / 上 / 右"三个偏移来描述就够了。
     */
    public void followOwner(Player owner) {
        double yaw = Math.toRadians(owner.getYRot());
        double forwardX = -Math.sin(yaw);
        double forwardZ = Math.cos(yaw);
        // 右手方向 = 前方向转 90°
        double rightX = -forwardZ;
        double rightZ = forwardX;

        double x = owner.getX() + forwardX * this.forward + rightX * this.right;
        double z = owner.getZ() + forwardZ * this.forward + rightZ * this.right;
        double y = owner.getY() + this.up;   // 实体的 y 是箱子底面
        this.setPos(x, y, z);
    }

    // ==================== 挨打 ====================

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        Player owner = this.owner;
        if (owner == null || !owner.isAlive() || owner.level() != level) {
            return false;
        }
        // ① 自己打自己（本人近战 / 自己射出去的箭）→ 不生效，否则等于自杀
        if (source.getEntity() == owner || source.getDirectEntity() == owner) {
            return false;
        }
        // ② 只转"人或生物造成的伤害"：火、摔落、闪电这类环境伤害不转
        if (!(source.getEntity() instanceof LivingEntity)) {
            return false;
        }
        // ③ 爆炸也不转：爆炸是范围伤害，玩家自己的箱子已经吃过一次，再转就成了双倍
        if (source.is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION)) {
            return false;
        }
        // 原样转给本体：护甲、无敌帧、击退、击杀归属都跟直接打到人一致
        return owner.hurtOrSimulate(source, amount);
    }

    /** 这一节是不是属于某个实体（UUID 比对，客户端也能用 —— 那边没有 owner 实体引用） */
    @Override
    public boolean isOwnedBy(Entity entity) {
        if (entity == null) {
            return false;
        }
        if (this.owner != null && this.owner == entity) {
            return true;
        }
        String uuid = this.entityData.get(DATA_OWNER_UUID);
        return !uuid.isEmpty() && uuid.equals(entity.getUUID().toString());
    }

    /**
     * 能不能被射线选中。
     * <p>
     * 服务端恒 true（自己打自己由 {@link #hurtServer} 挡）；客户端要把<b>本地玩家自己</b>身体的箱子让开，
     * 否则站在自己蛇身上时准星永远对着自己的箱子，一刀都砍不到敌人。
     */
    @Override
    public boolean isPickable() {
        return !this.level().isClientSide() || !corpseorigin$isLocalPlayersBody();
    }

    @Environment(EnvType.CLIENT)
    private boolean corpseorigin$isLocalPlayersBody() {
        String uuid = this.entityData.get(DATA_OWNER_UUID);
        Player local = Minecraft.getInstance().player;
        return local != null && !uuid.isEmpty() && uuid.equals(local.getUUID().toString());
    }

    // ==================== 生命周期 ====================

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            return;   // 客户端只跟着服务端同步过来的位置走
        }
        Player owner = this.owner;
        if (owner == null || owner.isRemoved() || !owner.isAlive()
                || owner.level() != this.level()
                || !MutantForm.isMutantWithHitboxes(owner)) {
            this.discard();
        }
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    /** 不当垫脚石：别的实体（含主人自己）直接穿过去 */
    @Override
    public boolean canBeCollidedWith(Entity entity) {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return this.dimensions;
    }

    /** 像末影龙的分体一样：问到它等于问到主人（"这是不是那个玩家"） */
    @Override
    public boolean is(Entity entity) {
        return this == entity || this.owner == entity;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_OWNER_UUID, "");
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
    }
}
