/**
 * 尸兄肉块模型类 - 定义肉块的几何模型和纹理
 *
 * 【功能说明】
 * 1. 模型加载：从 geo/block/zbr_flesh.geo.json 加载几何模型
 * 2. 纹理绑定：绑定血肉纹理
 * 3. 渲染类型：使用 entityCutoutNoCull 确保正确渲染
 *
 * 【尸巢背景】
 * 尸兄肉块是组成尸巢的基本单元，具有生物般的脉动特性。
 * 模型包含多个层次：外层肉块、内层核心，模拟活体组织。
 *
 * 【关联系统】
 * - ZBRFleshBlockEntity: 提供动画状态
 * - ZBRFleshRenderer: 使用此模型进行渲染
 * - GeckoLib: 提供模型加载支持
 *
 * @author Phagens
 * @version 1.0
 */
package com.phagens.corpseorigin.client.Models.block;

import com.phagens.corpseorigin.block.entity.ZBRFleshBlockEntity;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedBlockGeoModel;

import static com.phagens.corpseorigin.CorpseOrigin.MODID;

/**
 * 尸兄肉块模型
 * 继承 DefaultedBlockGeoModel 以支持 GeckoLib 动画模型
 */
public class ZBRFleshModel extends DefaultedBlockGeoModel<ZBRFleshBlockEntity> {

    // 显式定义纹理路径
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MODID, "textures/block/zbr_flesh.png");

    /**
     * 构造函数
     * 指定模型资源路径为 zbr_flesh
     * 会自动加载 geo/block/zbr_flesh.geo.json 和 animations/block/zbr_flesh.animation.json
     */
    public ZBRFleshModel() {
        super(ResourceLocation.fromNamespaceAndPath(MODID, "zbr_flesh"));
    }

    /**
     * 获取纹理资源
     *
     * @param animatable 方块实体
     * @return 纹理资源位置
     */
    @Override
    public ResourceLocation getTextureResource(ZBRFleshBlockEntity animatable) {
        return TEXTURE;
    }

    /**
     * 获取渲染类型
     * 使用 entityCutoutNoCull 确保正确渲染，不进行面剔除
     *
     * @param animatable 方块实体
     * @param texture 纹理资源
     * @return 渲染类型
     */
    @Override
    public RenderType getRenderType(ZBRFleshBlockEntity animatable, ResourceLocation texture) {
        return RenderType.entityCutoutNoCull(texture);
    }
}
