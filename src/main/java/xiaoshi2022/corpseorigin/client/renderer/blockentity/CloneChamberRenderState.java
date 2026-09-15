package xiaoshi2022.corpseorigin.client.renderer.blockentity;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.UUID;

public class CloneChamberRenderState extends BlockEntityRenderState {
    public boolean lowerHalf;
    public float doorOpen;
    public float cloneProgress;
    /** 这具克隆体自己的完成度（0.80~0.99）：达到它就算成熟，改用完整模型渲染 */
    public float cloneCompletion = 0.96F;
    public boolean hasClone;
    public Direction facing = Direction.NORTH;
    public UUID ownerUuid;
    /** 这具身体的标识（由维度+坐标推导）：客户端按它取这具身体自己的尸兄状态 */
    public UUID bodyUuid;
    /** 仓内身体穿的盔甲（顺序：头/胸/腿/脚），来自方块实体更新包 */
    public List<ItemStack> equipment = List.of();
}
