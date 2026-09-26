package xiaoshi2022.corpseorigin.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * 凹凸曼尸兄 —— 低阶尸兄的"原皮"兄弟。
 * <p>
 * 外观上它和 {@link LowerLevelZbEntity} 共用同一套几何与动画，区别只在贴图：
 * 低阶尸兄是"玩家皮肤 + 尸化骨骼叠加层"现合成的动态纹理，凹凸曼直接吃静态贴图
 * {@code textures/entity/aotuman.png}，<b>因此脸上没有那只尸眼</b>
 * （尸眼是叠加层画的，见 {@code AotumanZbRenderer} 里被跳过的几根骨头）。
 * <p>
 * 数值上是只"虚弱、没有攻击性"的尸兄：血少、移速慢、打不动人，
 * 并且<b>连一个攻击/索敌目标都不注册</b> —— 不主动打人，被打也不还手。
 */
public class AotumanZbEntity extends LowerLevelZbEntity {

    // ==================== 虚弱数值 ====================
    // 这几个数同时被 createAttributes()（首次生成）和 updateAttributesForEvolution()（读档）使用，
    // 免得两边写飘 —— 父类那条"越进化越强"的曲线对凹凸曼不适用。

    private static final double MAX_HEALTH = 8.0D;
    private static final double MOVEMENT_SPEED = 0.16D;
    private static final double ATTACK_DAMAGE = 0.0D;
    private static final double FOLLOW_RANGE = 12.0D;
    private static final double ARMOR = 0.0D;

    public AotumanZbEntity(EntityType<? extends AotumanZbEntity> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 2;   // 这么弱，打了也没什么赚头
    }

    /** 虚弱版属性：血少、走得慢、攻击力 0、没有护甲 */
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, MAX_HEALTH)
                .add(Attributes.MOVEMENT_SPEED, MOVEMENT_SPEED)
                .add(Attributes.ATTACK_DAMAGE, ATTACK_DAMAGE)
                .add(Attributes.FOLLOW_RANGE, FOLLOW_RANGE)
                .add(Attributes.ARMOR, ARMOR);
    }

    /**
     * 凹凸曼是固定强度的特殊形态，不参与父类那条"按游戏日掷进化等级"，
     * 免得掷出高阶却既不涨强度、又长出它模型上没有的器官。
     */
    @Override
    protected boolean rollsSpawnEvolution() {
        return false;
    }

    /**
     * 覆盖父类的"越进化越强"曲线。
     * <p>
     * 父类这个方法会把血量/攻击/移速按 {@code evolutionLevel} 重新写一遍，
     * 而它会在<b>读档时</b>被调用（{@code readAdditionalSaveData} → {@code setEvolutionLevel}）——
     * 不覆盖的话，凹凸曼存一次档、读一次档就变回一只正常强度的低阶尸兄了。
     */
    @Override
    protected void updateAttributesForEvolution() {
        if (this.getAttribute(Attributes.MAX_HEALTH) != null) {
            this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(MAX_HEALTH);
        }
        if (this.getAttribute(Attributes.ATTACK_DAMAGE) != null) {
            this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(ATTACK_DAMAGE);
        }
        if (this.getAttribute(Attributes.MOVEMENT_SPEED) != null) {
            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(MOVEMENT_SPEED);
        }
        if (this.getAttribute(Attributes.ARMOR) != null) {
            this.getAttribute(Attributes.ARMOR).setBaseValue(ARMOR);
        }
        this.setHealth(this.getMaxHealth());
    }

    /**
     * 只保留"活着"的几条行为：浮水、慢慢晃、看玩家、东张西望。
     * <p>
     * 刻意<b>不调 super</b> —— 父类的近战、被攻击反击、索敌（含饥饿时吃同类）一概不要，
     * 这样它既不会主动攻击，也不会被打了就追着人打。
     */
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.6D));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
    }

    /** 打不动人：即使被别的逻辑推进攻击动作，也造不成任何伤害、不进食 */
    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        return false;
    }

    /**
     * 凹凸曼不吃"名字皮肤"。
     * <p>
     * 它用的是固定贴图 {@code aotuman.png}（见 {@code AotumanZbRenderer}），
     * 名字对它没有任何渲染作用 —— 关掉这个开关，自然生成时就不会白分一个 ID、也不会白跑一遍皮肤查询。
     */
    @Override
    protected boolean usesNamedSkin() {
        return false;
    }

    /** 永远不处于"饿极了"状态 —— 它没胃口也不进食，别顶着怒气粒子晃悠 */
    @Override
    public boolean isStarving() {
        return false;
    }
}
