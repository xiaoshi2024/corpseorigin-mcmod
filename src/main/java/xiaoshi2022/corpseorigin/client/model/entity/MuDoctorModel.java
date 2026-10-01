package xiaoshi2022.corpseorigin.client.model.entity;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.Identifier;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.entity.MuDoctorEntity;

/**
 * 穆博士模型 —— 二阶段（金属进化）通过自定义 {@link DataTicket} 切换贴图。
 * <p>
 * 用法：{@link xiaoshi2022.corpseorigin.client.renderer.entity.MuDoctorRenderer#extractRenderState}
 * 在每帧把 {@code entity.getPhase() == PHASE_METAL} 写进 {@link #METAL_TICKET}，
 * 本类 {@link #getTextureResource(GeoRenderState)} 读 ticket 切到金属贴图。
 * <p>
 * 金属贴图 {@code mu_doctor_metal.png} 缺失时 GeckoLib 会回退主贴图（仅 warn，不崩），
 * 二阶段至少还能靠 haseye（第三只眼）+ 电弧火花表现区分。
 */
@Environment(EnvType.CLIENT)
public class MuDoctorModel extends DefaultedEntityGeoModel<MuDoctorEntity> {

    public static final DataTicket<Boolean> METAL_TICKET =
            DataTicket.create(CorpseOrigin.MOD_ID + ":mu_doctor_metal", Boolean.class);

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(CorpseOrigin.MOD_ID, "textures/entity/mu_doctor.png");
    private static final Identifier TEXTURE_METAL =
            Identifier.fromNamespaceAndPath(CorpseOrigin.MOD_ID, "textures/entity/mu_doctor_metal.png");

    public MuDoctorModel() {
        super(Identifier.fromNamespaceAndPath(CorpseOrigin.MOD_ID, "mu_doctor"));
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        Boolean metal = renderState.getGeckolibData(METAL_TICKET);
        return Boolean.TRUE.equals(metal) ? TEXTURE_METAL : TEXTURE;
    }
}