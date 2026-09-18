package xiaoshi2022.corpseorigin.client.renderer.entity;

import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.builtin.ItemInHandGeoLayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import xiaoshi2022.corpseorigin.client.model.entity.AotumanZbModel;
import xiaoshi2022.corpseorigin.entity.AotumanZbEntity;

/**
 * 凹凸曼尸兄渲染器：与低阶尸兄同一套模型与动画，贴图固定为 {@code aotuman.png}。
 * <p>
 * 与低阶尸兄渲染器的两点不同：
 * <ol>
 *   <li>不走 {@code CombinedSkinBuilder}（不合成"玩家皮肤 + 尸化骨骼层"），直接用静态贴图；</li>
 *   <li><b>跳过尸化骨骼</b> —— 见 {@link #GHOUL_BONES}。</li>
 * </ol>
 */
public class AotumanZbRenderer extends GeoEntityRenderer<AotumanZbEntity, LivingEntityRenderState> {

    /**
     * 尸化骨骼：脸上那只尸眼（{@code shieye}）和脑后那串飘带（{@code group2} … {@code group7}）。
     * <p>
     * 这些骨头的 UV 全部落在"尸化叠加层"真正画了东西的那块区域（贴图左下角），
     * 低阶尸兄靠叠加层把它们画成尸眼/飘带；换成干净的凹凸曼皮肤之后，
     * 那里只是普通的躯干皮肤像素，画出来就是几块错位的皮肉 —— 所以整根不画。
     */
    private static final String[] GHOUL_BONES =
            {"shieye", "group2", "group7", "group3", "group4", "group5", "group6"};

    public AotumanZbRenderer(EntityRendererProvider.Context context) {
        super(context, new AotumanZbModel());
        this.shadowRadius = 0.5f;
        this.withRenderLayer(new ItemInHandGeoLayer<>(context, this));
    }

    @Override
    public void adjustModelBonesForRender(RenderPassInfo<LivingEntityRenderState> renderPassInfo,
                                          BoneSnapshots snapshots) {
        super.adjustModelBonesForRender(renderPassInfo, snapshots);
        for (String bone : GHOUL_BONES) {
            snapshots.ifPresent(bone, snapshot -> snapshot.skipRender(true));
        }
    }

    @Override
    public Identifier getTextureLocation(LivingEntityRenderState renderState) {
        return AotumanZbModel.TEXTURE;
    }

    @Override
    public RenderType getRenderType(LivingEntityRenderState renderState, Identifier texture) {
        return RenderTypes.entityCutout(texture);
    }
}
