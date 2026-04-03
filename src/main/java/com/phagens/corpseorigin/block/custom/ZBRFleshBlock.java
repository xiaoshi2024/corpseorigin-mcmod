/**
 * 尸兄肉块方块类 - 尸巢的建筑方块
 *
 * 【功能说明】
 * 1. 动画效果：具有呼吸般的脉动动画，模拟活体组织的特性
 * 2. 尸巢组件：作为尸巢的基本构成单元，可以被尸兄玩家召唤生成
 * 3. 生物特性：虽然是方块，但具有类似生物的视觉效果
 *
 * 【尸巢背景】
 * 尸巢是漫画《尸兄》中的核心地点，是由巨型尸兄形成的肉山建筑。
 * 尸巢会吸引各方尸兄进入并通过吞噬他们来生长。
 * 尸兄玩家可以觉醒技能，当拥有足够多尸兄奴仆时，
 * 可以命令奴仆们自我吞噬生成肉块，逐渐组成尸巢。
 *
 * 【动画系统】
 * - idle动画：持续的呼吸脉动效果
 * - 使用GeckoLib实现复杂的3D动画效果
 *
 * 【关联系统】
 * - ZBRFleshBlockEntity: 处理肉块的动画渲染
 * - GeckoLib: 提供动画渲染支持
 *
 * @author Phagens
 * @version 1.0
 */
package com.phagens.corpseorigin.block.custom;

import com.phagens.corpseorigin.block.entity.ZBRFleshBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

/**
 * 尸兄肉块方块主类
 * 继承Block实现基础方块功能，实现EntityBlock接口以支持方块实体
 */
public class ZBRFleshBlock extends Block implements EntityBlock {

    /**
     * 构造函数
     */
    public ZBRFleshBlock() {
        super(BlockBehaviour.Properties.of()
                .strength(2.0f, 4.0f)       // 硬度2.0，爆炸抗性4.0
                .sound(SoundType.SLIME_BLOCK) // 史莱姆音效，模拟血肉质感
                .mapColor(MapColor.COLOR_RED) // 地图显示为红色
                .noOcclusion()               // 不遮挡光线
        );
    }

    /**
     * 获取渲染形状
     * 返回ENTITY_BLOCK_ANIMATED以支持GeckoLib动画
     */
    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    /**
     * 获取碰撞箱形状
     * 完整方块碰撞箱
     */
    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Block.box(0, 0, 0, 16, 16, 16);
    }

    /**
     * 获取碰撞箱形状
     */
    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Block.box(0, 0, 0, 16, 16, 16);
    }

    /**
     * 创建方块实体
     * 实现EntityBlock接口的方法
     *
     * @param pos 方块位置
     * @param state 方块状态
     * @return 新的方块实体实例
     */
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ZBRFleshBlockEntity(pos, state);
    }
}
