package xiaoshi2022.corpseorigin.mixin;

import com.geckolib.renderer.GeoArmorRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.RenderPassInfo;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xiaoshi2022.corpseorigin.client.limb.LimbRenderData;
import xiaoshi2022.corpseorigin.limb.LimbSlots;

/**
 * 断掉的那一肢，把<b>模组自己的 GeoLib 套装</b>（龙右长袍、天线宝宝套装……）对应的骨段藏掉。
 * <p>
 * 这些套装不走原版盔甲层，走的是 {@code GeoArmorRenderer}：它把穿戴者的原版 {@code HumanoidModel}
 * 的姿势拷到自己的 {@code armorHead / armorBody / armorRightArm …} 骨段上。
 * 身体那边断掉的那一肢现在是 corpse_player 出的残桩/血管，所以这一段的套装得让开，
 * 否则袖子会挂在空中、也会把正在长的肉芽盖住。
 * <p>
 * 骨段名由 GeckoLib 自己的 {@link GeoArmorRenderer#getBoneNameForSegment} 解析，不用记每套模型叫什么；
 * 模型里没有那根骨时自动空操作。其余骨段（头/躯干/没断的四肢）一概不动，照常跟随原版骨架。
 */
@Mixin(GeoArmorRenderer.class)
public abstract class GeoArmorRendererLimbMixin {

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Inject(method = "adjustModelBonesForRender", at = @At("TAIL"))
    private void corpseorigin$hideSeveredLimbArmor(RenderPassInfo<?> renderPassInfo, BoneSnapshots snapshots,
                                                   CallbackInfo ci) {
        if (!(renderPassInfo.renderState() instanceof AvatarRenderState avatar)) {
            return;   // 只有玩家（含克隆分身）才有断肢这回事
        }
        Integer mask = avatar.getGeckolibData(LimbRenderData.LIMB_MASK);
        int severedMask = mask == null ? 0 : (mask & LimbSlots.MASK_ALL);
        if (severedMask == 0) {
            return;
        }

        GeoArmorRenderer self = (GeoArmorRenderer) (Object) this;
        for (GeoArmorRenderer.ArmorSegment segment : GeoArmorRenderer.ArmorSegment.values()) {
            int limbSlot = corpseorigin$limbSlot(segment);
            if (limbSlot < 0 || (severedMask & (1 << limbSlot)) == 0) {
                continue;   // 躯干 / 没断的部位：不动
            }
            String bone = self.getBoneNameForSegment(avatar, segment);
            if (bone == null || bone.isEmpty()) {
                continue;
            }
            snapshots.get(bone).ifPresent(snapshot -> {
                // 方块往往挂在骨段的子骨上（armorRightArm → "Right Arm"），所以两层都要藏
                snapshot.skipRender(true);
                snapshot.skipChildrenRender(true);
            });
        }
    }

    /** 盔甲骨段对应的断肢位（躯干返回 -1） */
    private static int corpseorigin$limbSlot(GeoArmorRenderer.ArmorSegment segment) {
        return switch (segment) {
            case HEAD -> LimbSlots.HEAD;
            case RIGHT_ARM -> LimbSlots.RIGHT_ARM;
            case LEFT_ARM -> LimbSlots.LEFT_ARM;
            // 腿断了，护腿和靴子一起让开
            case RIGHT_LEG, RIGHT_FOOT -> LimbSlots.RIGHT_LEG;
            case LEFT_LEG, LEFT_FOOT -> LimbSlots.LEFT_LEG;
            default -> -1;
        };
    }
}
