package xiaoshi2022.corpseorigin.client.renderer;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import xiaoshi2022.corpseorigin.client.skin.ZbSkinState;
import xiaoshi2022.corpseorigin.entity.LowerLevelZbEntity;

public class LowerLevelZbRenderState extends LivingEntityRenderState {
    public LowerLevelZbEntity entity;
    public Identifier customSkinTexture;
    public ZbSkinState skinState = ZbSkinState.NOT_LOADED;
}