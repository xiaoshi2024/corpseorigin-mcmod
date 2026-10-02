package xiaoshi2022.corpseorigin.client.model.entity;

import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.model.DefaultedEntityGeoModel;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.Identifier;
import xiaoshi2022.corpseorigin.CorpseOrigin;

/**
 * 蚊子尸兄模型（{@code mosquito_zbr} geo 资源，贴图 {@code textures/entity/mosquito_zbr.png}）。
 * <p>
 * <b>蚊群核心</b>（MosquitoZbrEntity）和<b>环绕蚊子</b>（MosquitoSwarmEntity）共用同一套
 * geo / 动画 / 贴图——子蚊子只是渲染尺寸缩小（见 {@code MosquitoSwarmRenderer} 的缩放），
 * 与"多形态共用一套 geo 模型"的惯例一致。动画键：idle / fly / suck（look 备用）。
 *
 * @param <T> 蚊群核心或环绕蚊子
 */
@Environment(EnvType.CLIENT)
public class MosquitoZbrModel<T extends GeoAnimatable> extends DefaultedEntityGeoModel<T> {

    public MosquitoZbrModel() {
        super(Identifier.fromNamespaceAndPath(CorpseOrigin.MOD_ID, "mosquito_zbr"));
    }
}
