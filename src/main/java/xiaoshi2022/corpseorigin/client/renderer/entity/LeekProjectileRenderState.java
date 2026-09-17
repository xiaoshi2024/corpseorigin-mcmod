package xiaoshi2022.corpseorigin.client.renderer.entity;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

/** 投掷大葱的渲染状态 */
public class LeekProjectileRenderState extends EntityRenderState {
    public ItemStack itemStack = ItemStack.EMPTY;

    public float syncedYaw;
    public float syncedPitch;

    public Entity sourceEntity;
}
