package xiaoshi2022.corpseorigin.client.renderer.blockentity;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.Direction;

import java.util.UUID;

public class ShellStorageRenderState extends BlockEntityRenderState {
    public boolean hasBody;
    public UUID ownerUuid;
    public Direction facing = Direction.NORTH;
    public float progress;
}