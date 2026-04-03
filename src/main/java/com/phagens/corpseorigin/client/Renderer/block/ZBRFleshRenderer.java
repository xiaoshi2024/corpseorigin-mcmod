/**
 * 尸兄肉块渲染器类 - 负责渲染尸兄肉块方块
 *
 * 【功能说明】
 * 1. 渲染尸兄肉块方块的3D模型
 * 2. 播放呼吸脉动动画
 * 3. 使用 GeckoLib 的 GeoBlockRenderer 实现复杂动画效果
 *
 * 【尸巢背景】
 * 尸兄肉块是组成尸巢的基本单元，具有生物般的脉动特性。
 * 渲染器负责将肉块的活体组织效果呈现在游戏中。
 *
 * 【关联系统】
 * - ZBRFleshBlockEntity: 提供动画状态
 * - ZBRFleshModel: 提供几何模型和纹理
 * - GeckoLib: 提供动画渲染支持
 *
 * @author Phagens
 * @version 1.0
 */
package com.phagens.corpseorigin.client.Renderer.block;

import com.phagens.corpseorigin.block.entity.ZBRFleshBlockEntity;
import com.phagens.corpseorigin.client.Models.block.ZBRFleshModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

/**
 * 尸兄肉块渲染器
 * 继承 GeoBlockRenderer 以支持 GeckoLib 动画渲染
 */
public class ZBRFleshRenderer extends GeoBlockRenderer<ZBRFleshBlockEntity> {

    /**
     * 构造函数
     *
     * @param context 渲染器提供上下文
     */
    public ZBRFleshRenderer(BlockEntityRendererProvider.Context context) {
        super(new ZBRFleshModel());
    }
}
