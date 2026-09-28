package xiaoshi2022.corpseorigin.client.renderer.entity;

import com.geckolib.constant.dataticket.DataTicket;
import com.google.common.reflect.TypeToken;
import net.minecraft.resources.Identifier;
import xiaoshi2022.corpseorigin.client.skin.ZbSkinState;
import xiaoshi2022.corpseorigin.entity.LowerLevelZbEntity;

public final class RenderStateData {
    public static final DataTicket<Boolean> CORPSE_EYE = DataTicket.create("corpse_eye",Boolean.class);
    public static final DataTicket<Boolean> CRACKED = DataTicket.create("corpse_cracked",Boolean.class);
    public static final DataTicket<Identifier> CUSTOM_SKIN_TEXTURE =
            DataTicket.create("custom_skin_texture", new TypeToken<Identifier>() {});
    public static final DataTicket<ZbSkinState> SKIN_STATE =
            DataTicket.create("skin_state", new TypeToken<ZbSkinState>() {});
    public static final DataTicket<LowerLevelZbEntity> ENTITY =
            DataTicket.create("entity", new TypeToken<LowerLevelZbEntity>() {});

    /** 初音尸兄是否处于「消化不良」大肚子状态 */
    public static final DataTicket<Boolean> MIKU_OVERFULL =
            DataTicket.create("miku_overfull", Boolean.class);

    private RenderStateData() {}
}
