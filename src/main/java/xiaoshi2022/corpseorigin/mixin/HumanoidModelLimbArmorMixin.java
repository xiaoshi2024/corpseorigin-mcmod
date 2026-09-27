package xiaoshi2022.corpseorigin.mixin;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xiaoshi2022.corpseorigin.client.limb.LimbRenderData;
import xiaoshi2022.corpseorigin.limb.LimbSlots;

/**
 * 断掉的那一肢<b>不画它自己的盔甲部件</b>，其余盔甲照常渲染。
 * <p>
 * 现在身体是"分肢体替换"的：完好的部位由原版模型出画（盔甲天然贴合），只有断掉的那一肢换成
 * corpse_player 的残桩/血管 —— 所以只需要把那一肢的盔甲部件藏掉（否则会有一条袖子挂在空中，
 * 也会把正在长的肉芽盖住）。躯干、头、没断的四肢一律不动。
 * <p>
 * ⚠️ 注入点必须是 {@code setupAnim} 的 TAIL：盔甲模型的姿势是在 {@code submitModel} 阶段
 * 由这个方法设置的，早写会被原版覆盖掉。另外模型实例是跨实体复用的，状态记在实例自己身上。
 */
@Mixin(HumanoidModel.class)
public abstract class HumanoidModelLimbArmorMixin {

    /** 被我们藏起来的部位位掩码 —— 只还原自己藏过的，别去碰原版自己的显隐状态 */
    @Unique
    private int corpseorigin$hiddenMask;

    @Inject(
            method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V",
            at = @At("TAIL")
    )
    private void corpseorigin$hideSeveredLimbArmor(HumanoidRenderState state, CallbackInfo ci) {
        if (!(state instanceof AvatarRenderState avatar)) {
            return;   // 只有玩家（含克隆分身）才有断肢这回事
        }
        HumanoidModel<?> self = (HumanoidModel<?>) (Object) this;
        // PlayerModel has its own per-frame reset and skin-layer-aware mask.
        // Restoring its hat here would override skin settings after regrowth.
        if (self instanceof net.minecraft.client.model.player.PlayerModel) return;
        Integer mask = avatar.getGeckolibData(LimbRenderData.LIMB_MASK);
        int severedMask = mask == null ? 0 : (mask & LimbSlots.MASK_ALL);
        var level=net.minecraft.client.Minecraft.getInstance().level;
        var actor=level==null?null:level.getEntity(avatar.id);
        if(actor!=null && xiaoshi2022.corpseorigin.skill.chapter.BodySkillState.missingForearm(actor))
            severedMask |= 1 << LimbSlots.RIGHT_ARM;

        for (int slot = 0; slot < LimbSlots.COUNT; slot++) {
            int bit = 1 << slot;
            boolean severed = (severedMask & bit) != 0;
            boolean hidden = (this.corpseorigin$hiddenMask & bit) != 0;

            if (severed == hidden) {
                continue;   // 该藏的藏了 / 该露的露着，这一帧不用动它
            }
            this.corpseorigin$setLimbVisible(self, slot, !severed);
            this.corpseorigin$hiddenMask = severed
                    ? this.corpseorigin$hiddenMask | bit
                    : this.corpseorigin$hiddenMask & ~bit;
        }
    }

    /** 部位下标（见 {@link LimbSlots}）→ 原版模型上对应的部件 */
    @Unique
    private void corpseorigin$setLimbVisible(HumanoidModel<?> model, int slot, boolean visible) {
        switch (slot) {
            case LimbSlots.RIGHT_ARM -> model.rightArm.visible = visible;
            case LimbSlots.LEFT_ARM -> model.leftArm.visible = visible;
            case LimbSlots.RIGHT_LEG -> model.rightLeg.visible = visible;
            case LimbSlots.LEFT_LEG -> model.leftLeg.visible = visible;
            // 头盔外层的方块挂在 hat 上，只藏 head 的话会留下一顶空头盔
            case LimbSlots.HEAD -> {
                model.head.visible = visible;
                model.hat.visible = visible;
            }
            default -> {
            }
        }
    }
}
