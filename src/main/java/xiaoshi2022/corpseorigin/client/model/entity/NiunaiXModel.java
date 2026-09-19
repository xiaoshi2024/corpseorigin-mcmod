package xiaoshi2022.corpseorigin.client.model.entity;

import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.Identifier;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.client.limb.PlayerGeoAnimatable;

/**
 * 开胃奶尸兄化后挂在背后的 {@code niunaix}（触角 + 捆仙索 + 菊花盾）。
 * <p>
 * geo / 动画都直接取
 * {@code geckolib/models/entity/niunaix.geo.json} 与
 * {@code geckolib/animations/entity/niunaix.animation.json}（同一个 id 派生，不用另外配置）。
 * <p>
 * 这套骨骼是<b>独立贴图</b>：不跟玩家皮肤走，而是模型自带的
 * {@code textures/entity/niunaix.png} —— 与断肢（贴玩家皮肤）正好相反。
 */
@Environment(EnvType.CLIENT)
public class NiunaiXModel extends DefaultedEntityGeoModel<PlayerGeoAnimatable> {

    /** 背挂自己的贴图（模型 128×128，与玩家皮肤无关） */
    public static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(CorpseOrigin.MOD_ID, "textures/entity/niunaix.png");

    public NiunaiXModel() {
        super(Identifier.fromNamespaceAndPath(CorpseOrigin.MOD_ID, "niunaix"));
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        return TEXTURE;
    }
}
