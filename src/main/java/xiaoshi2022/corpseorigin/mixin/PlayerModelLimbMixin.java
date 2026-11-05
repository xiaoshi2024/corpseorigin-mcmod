package xiaoshi2022.corpseorigin.mixin;

import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xiaoshi2022.corpseorigin.client.limb.LimbRenderData;
import xiaoshi2022.corpseorigin.limb.LimbSlots;

/**
 * 断肢形态下，原版玩家模型只隐藏<b>断掉的那几肢</b>，其余部位照常渲染。
 * <p>
 * 这是"分肢体替换"的一半：<b>原版模型继续画躯干、头、完好的四肢</b>（以及挂在它们上面的盔甲、
 * 皮肤外层、其它模组的层），断掉的那一肢才交给 corpse_player 模型去画残桩与血管
 * （见 {@code CorpsePlayerGeoRenderer}）。相比"整具身体换模型"，这样盔甲与皮肤的参照系完全一致，
 * 不需要任何位置/旋转补偿。
 * <p>
 * 头部在 HEAD 恢复（原版不重置它），TAIL 只追加断肢隐藏，不强制显示部件。
 * 保留皮肤外层开关、旁观者和 PAL 第一视角的可见性；优先级排在 PAL 2001 之后。
 */
@Mixin(value = PlayerModel.class, priority = 2100)
public abstract class PlayerModelLimbMixin {

    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V",
            at = @At("HEAD"))
    private void corpseorigin$resetHead(AvatarRenderState state, CallbackInfo ci) {
        // Vanilla resets the other body parts, but not the head. Reset before
        // animation mods apply their first-person visibility, never after them.
        ((PlayerModel) (Object) this).head.visible = true;
    }

    @Inject(
            method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V",
            at = @At("TAIL")
    )
    private void corpseorigin$hideSeveredLimbs(AvatarRenderState state, CallbackInfo ci) {
        PlayerModel self = (PlayerModel) (Object) this;
        var level=net.minecraft.client.Minecraft.getInstance().level;
        var actor=level==null?null:level.getEntity(state.id);
        if(actor!=null) {
            String action=actor.getAttachedOrCreate(xiaoshi2022.corpseorigin.skill.chapter.ChapterScenes.ACTION);
            if(actor.getAttachedOrCreate(xiaoshi2022.corpseorigin.skill.chapter.ChapterScenes.UNTIL)>level.getGameTime()) {
                float phase=(float)Math.sin(state.ageInTicks*.35);
                switch(action) {
                    case "round_dance" -> {self.rightArm.zRot=1.1f;self.leftArm.zRot=-1.1f;self.rightLeg.xRot=-.7f;self.leftLeg.xRot=.7f;}
                    case "tiangang_right","tiangang_combo" -> {self.rightArm.xRot=-1.6f;self.body.yRot=-.3f;}
                    case "tiangang_left" -> {self.leftArm.xRot=-1.6f;self.body.yRot=.3f;}
                    case "tiangang_slam" -> {self.rightArm.xRot=-2.8f;self.leftArm.xRot=-2.8f;self.rightLeg.xRot=-.6f;self.leftLeg.xRot=-.6f;}
                    case "drain" -> {self.head.xRot=.5f;self.body.xRot=.15f;self.rightArm.zRot=.18f+phase*.05f;self.leftArm.zRot=-.18f-phase*.05f;}
                    case "knockback" -> {self.head.xRot=-.5f;self.rightArm.xRot=-1.4f;self.leftArm.xRot=-1.4f;self.rightLeg.xRot=.7f;self.leftLeg.xRot=.5f;}
                    case "entrance" -> {self.rightArm.xRot=-1.6f;self.leftArm.zRot=-.35f;self.head.xRot=-.15f;}
                    case "charge" -> {self.rightArm.xRot=-2.2f;self.leftArm.xRot=-2.2f;self.rightArm.zRot=.4f;self.leftArm.zRot=-.4f;}
                    case "release" -> {self.rightArm.xRot=-1.6f;self.leftArm.xRot=-1.6f;}
                    case "ambush" -> {self.rightArm.xRot=-1.7f;self.body.yRot=-.35f;}
                    case "guigun_sweep" -> {self.rightArm.xRot=-1.3f;self.rightArm.zRot=-.6f;self.body.yRot=.2f;}
                    case "guigun_guard" -> {self.rightArm.xRot=-.7f;self.leftArm.xRot=-.9f;self.rightArm.zRot=.3f;self.leftArm.zRot=-.3f;}
                    // 强化药剂扎心：双手内扣按住胸口，低头盯着针头压进心脏，针头入体时轻微发抖
                    case "s_agent_press_right", "s_agent_press_left" -> {
                        float t=xiaoshi2022.corpseorigin.client.render.SagentInjectionPose.approach(
                                actor,state.ageInTicks-(float)Math.floor(state.ageInTicks));
                        boolean right=action.endsWith("right");
                        var injecting=right?self.rightArm:self.leftArm;
                        injecting.xRot=-1.35f*t;
                        injecting.yRot=(right?-.85f:.3f)*t;
                        injecting.zRot=(right?-.65f:.25f)*t;
                        var supporting=right?self.leftArm:self.rightArm;
                        supporting.xRot=-.7f*t;
                        supporting.zRot=(right?.2f:-.3f)*t;
                        self.head.xRot=.35f*t;
                    }
                    case "guigun_resonance" -> {self.rightArm.xRot=-2.0f;self.leftArm.xRot=-2.0f;self.head.xRot=-.2f;}
                    case "guigun_crush" -> {self.rightArm.xRot=-2.9f;self.leftArm.xRot=-2.9f;self.rightLeg.xRot=-.6f;self.leftLeg.xRot=-.6f;}
                    case "wuchou_blade" -> {self.rightArm.xRot=-1.8f;self.body.yRot=-.3f;}
                    case "wuchou_step" -> {self.body.xRot=.25f;self.rightArm.xRot=.4f;self.leftArm.xRot=-.8f;}
                    case "wusheng_twin" -> {self.rightArm.xRot=-1.5f;self.leftArm.xRot=-1.2f;self.body.yRot=.25f;}
                    case "wusheng_cross" -> {self.rightArm.xRot=-1.6f;self.leftArm.xRot=-1.6f;self.rightArm.zRot=.5f;self.leftArm.zRot=-.5f;}
                }
            }
        }
        Integer mask = state.getGeckolibData(LimbRenderData.LIMB_MASK);
        int severed = mask == null ? 0 : (mask & LimbSlots.MASK_ALL);
        if (actor instanceof net.minecraft.world.entity.player.Player player
                && xiaoshi2022.corpseorigin.skill.chapter.GourdInheritance.disguised(player)) return;

        boolean head = severed(severed, LimbSlots.HEAD);
        self.head.visible &= !head;
        self.hat.visible &= !head && state.showHat;

        boolean rightArm = severed(severed, LimbSlots.RIGHT_ARM)
                || (actor!=null && xiaoshi2022.corpseorigin.skill.chapter.BodySkillState.missingForearm(actor));
        self.rightArm.visible &= !rightArm;
        self.rightSleeve.visible &= !rightArm && state.showRightSleeve;

        boolean leftArm = severed(severed, LimbSlots.LEFT_ARM);
        self.leftArm.visible &= !leftArm;
        self.leftSleeve.visible &= !leftArm && state.showLeftSleeve;

        boolean rightLeg = severed(severed, LimbSlots.RIGHT_LEG);
        self.rightLeg.visible &= !rightLeg;
        self.rightPants.visible &= !rightLeg && state.showRightPants;

        boolean leftLeg = severed(severed, LimbSlots.LEFT_LEG);
        self.leftLeg.visible &= !leftLeg;
        self.leftPants.visible &= !leftLeg && state.showLeftPants;

        // 躯干（body / jacket）永远由原版渲染：它上面挂着胸甲，位置必须和盔甲一致

        // ======== 第3期：半截葫芦小金刚（两条腿都断了，腰部半截爬着走） ========
        if (actor instanceof net.minecraft.world.entity.player.Player player) {
            String role = player.getAttachedOrCreate(
                    xiaoshi2022.corpseorigin.skill.chapter.ChapterActorState.ROLE);
            if ("xiaojingang_lame".equals(role)) {
                // 两条腿 + 裤腿全藏掉 → 只剩上半身
                self.rightLeg.visible = false;
                self.leftLeg.visible  = false;
                self.rightPants.visible = false;
                self.leftPants.visible  = false;
                // 身体整体往下趴：xRot 前倾 + body 根部 Y 轴下移，让屁股贴近地面
                self.body.xRot -= 0.35f;     // 身体前倾 20°，上半身趴平
                self.body.y = 4.5f;           // 身体根部原 Y=24（站立），这里压到 4.5，半截屁股贴地
                // 两只手撑地：手臂向前伸 + 下垂，像用胳膊拖着半截身体爬
                self.rightArm.xRot = -2.2f;   // 右臂往前趴平（接近地面）
                self.rightArm.zRot = 0.25f;   // 手臂稍微外撇，不要贴身体
                self.leftArm.xRot  = -2.2f;   // 左臂同
                self.leftArm.zRot  = -0.25f;
                // 头稍微抬起来点（别让脸贴地）
                self.head.xRot = Math.min(self.head.xRot + 0.2f, 0.4f);
                // 走路时手臂前后摆动由原版 walk animation 负责
            }
        }

        // ======== 第3期：捆绑姿态（被穆博士捆在地上躺着的晓佳/小惠） ========
        // GeckoLib 场景模型（xiaohui_scene）在 CreaturePlayerRenderer 里切，
        // 这里 PlayerModelLimbMixin 处理没切场景模型的普通玩家被 GM 指令捆时的躺着姿态。
        if (actor instanceof net.minecraft.world.entity.player.Player player
                && xiaoshi2022.corpseorigin.client.render.BoundPose.active(actor)) {
            // 整体躺平：身体轴线水平贴地（xRot = -PI/2）
            self.body.xRot = (float) (-Math.PI / 2);
            // 头稍微抬起来（别让脸贴地）
            self.head.xRot = 0.2f;
            self.head.yRot = 0;
            self.head.zRot = 0;
            // 两条腿也跟着身体躺平
            self.rightLeg.xRot = (float) (-Math.PI / 2);
            self.rightLeg.zRot = 0.2f;     // 稍微分开点
            self.leftLeg.xRot  = (float) (-Math.PI / 2);
            self.leftLeg.zRot  = -0.2f;
            // 双手放在身上/两侧被捆住（不做自由摆动）
            self.rightArm.xRot = (float) (-Math.PI / 2);
            self.rightArm.zRot = 0.6f;     // 右手放在身上
            self.leftArm.xRot  = (float) (-Math.PI / 2);
            self.leftArm.zRot  = -0.6f;    // 左手同
            self.body.yRot = 0;
            // 挣扎态：躺着扭身体想挣脱
            if ("bound_struggle".equals(xiaoshi2022.corpseorigin.client.render.BoundPose.pose(actor))) {
                float t = (float) Math.sin(state.ageInTicks * 0.45);
                self.body.zRot += t * 0.08f;   // 左右扭动
                self.head.yRot += t * 0.15f;   // 头跟着晃
                self.rightArm.zRot += t * 0.2f;
                self.leftArm.zRot  -= t * 0.2f;
            }
        }
    }

    private static boolean severed(int mask, int slot) {
        return (mask & (1 << slot)) != 0;
    }
}
