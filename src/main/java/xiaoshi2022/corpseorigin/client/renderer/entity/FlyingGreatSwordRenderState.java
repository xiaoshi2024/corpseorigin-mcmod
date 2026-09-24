package xiaoshi2022.corpseorigin.client.renderer.entity;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

public class FlyingGreatSwordRenderState extends EntityRenderState {
    public ItemStack itemStack = ItemStack.EMPTY;

    public float syncedYaw;
    public byte phase;
    public float qiAge;
    public float syncedPitch;

    /** 0 = 剑身立起沿飞行方向；90 = 横躺 */
    public float roll = 0f;
    /** 0 = 剑尖朝 +Z；180 = 剑尖朝 -Z；±90 = 侧面 */
    public float modelYawOffset = 0f;
    /** 剑面俯仰修正 */
    public float modelPitchOffset = 0f;

    public float renderScale = 2f;

    public Entity sourceEntity;
}
