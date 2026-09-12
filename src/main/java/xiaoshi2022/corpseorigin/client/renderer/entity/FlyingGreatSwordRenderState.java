package xiaoshi2022.corpseorigin.client.renderer.entity;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

public class FlyingGreatSwordRenderState extends EntityRenderState {

    public float renderScale = 0f;

    public ItemStack itemStack = ItemStack.EMPTY;
    public float syncedYaw;
    public float syncedPitch;
    /** updateForNonLiving 需要非 null 的 entity（它会调用 entity.level()） */
    public Entity sourceEntity;
}