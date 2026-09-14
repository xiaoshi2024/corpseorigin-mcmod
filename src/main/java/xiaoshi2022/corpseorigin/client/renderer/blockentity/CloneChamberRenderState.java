package xiaoshi2022.corpseorigin.client.renderer.blockentity;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.Direction;

import java.util.UUID;

public class CloneChamberRenderState extends BlockEntityRenderState {
    public boolean lowerHalf;
    public float doorOpen;
    public float cloneProgress;
    public boolean hasClone;
    public Direction facing = Direction.NORTH;
    public UUID ownerUuid;
    /** 分身已激活：BER 不再渲染假人 */
    public boolean avatarActive;
}