/**
 * 尸兄肉块方块实体类 - 处理肉块的动画渲染
 *
 * 【功能说明】
 * 1. 动画控制：播放idle呼吸脉动动画，模拟活体组织的特性
 * 2. 使用GeckoLib实现复杂的3D动画效果
 *
 * 【动画系统】
 * - idle动画：持续的呼吸脉动效果
 * - 动画循环播放，模拟血肉组织的生命感
 * - 使用GeckoLib的动画控制器管理动画状态
 *
 * 【尸巢背景】
 * 尸巢是漫画《尸兄》中的核心地点，是由巨型尸兄形成的肉山建筑。
 * 尸兄肉块是组成尸巢的基本单元，具有生物般的脉动特性。
 *
 * 【关联系统】
 * - ZBRFleshBlock: 控制方块基本属性和行为
 * - GeckoLib: 提供动画渲染支持
 *
 * @author Phagens
 * @version 1.0
 */
package com.phagens.corpseorigin.block.entity;

import com.phagens.corpseorigin.register.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * 尸兄肉块方块实体
 * 实现GeoBlockEntity接口以支持GeckoLib动画
 */
public class ZBRFleshBlockEntity extends BlockEntity implements GeoBlockEntity {

    /** idle动画定义 - 呼吸脉动效果 */
    protected static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");

    /** GeckoLib动画实例缓存 */
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    /** 击杀数 - 用于触发结构生成 */
    private int kills = 0;
    /** 生成结构所需的击杀数 */
    public static final int REQUIRED_KILLS = 10;
    /** 尸巢主人UUID */
    private java.util.UUID owner = null;

    /**
     * 构造函数
     *
     * @param pos 方块位置
     * @param state 方块状态
     */
    public ZBRFleshBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.ZBR_FLESH.get(), pos, state);
    }

    /**
     * 获取当前击杀数
     */
    public int getKills() {
        return kills;
    }

    /**
     * 添加击杀数
     */
    public void addKills() {
        kills++;
        setChanged();
        com.phagens.corpseorigin.CorpseOrigin.LOGGER.info("尸巢肉块击杀数增加，当前击杀数: {}/{}", kills, REQUIRED_KILLS);
    }

    /**
     * 获取尸巢主人
     */
    public java.util.UUID getOwner() {
        return owner;
    }

    /**
     * 设置尸巢主人
     */
    public void setOwner(java.util.UUID owner) {
        this.owner = owner;
        setChanged();
    }

    /**
     * 注册动画控制器
     * 实现GeoBlockEntity接口的方法
     *
     * @param controllers 动画控制器注册器
     */
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, this::idleAnimController));
    }

    /**
     * idle动画控制器
     * 持续播放呼吸脉动动画
     *
     * @param state 动画状态
     * @return 动画播放状态
     */
    protected <E extends ZBRFleshBlockEntity> PlayState idleAnimController(final AnimationState<E> state) {
        return state.setAndContinue(IDLE);
    }

    /**
     * 获取动画实例缓存
     * 实现GeoBlockEntity接口的方法
     *
     * @return 动画实例缓存
     */
    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }


}
