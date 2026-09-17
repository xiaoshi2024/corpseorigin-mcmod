package xiaoshi2022.corpseorigin.client.renderer.armor;

import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.constant.DataTickets;
import com.geckolib.renderer.GeoArmorRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import xiaoshi2022.corpseorigin.item.armor.AntennaZBRitem;
import xiaoshi2022.corpseorigin.mixin.LevelRendererAccessor;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

/**
 * 首人称：用天线宝宝盔甲自己的模型画手臂，替掉原版那条光手。
 * <p>
 * 参考的是 NeoForge 那边 {@code RenderArmEvent} 的做法（拿盔甲渲染器 + 只留手臂骨骼 + 藏掉其它部位），
 * 但 Fabric 没有对应事件，所以注入点落在 {@code ItemInHandRenderer.renderPlayerArm} 的 HEAD：
 * 原版只在"手上没东西、玩家不隐身、不在开镜"时才会调用它 —— "空手才替换"这个条件天然成立。
 * <p>
 * 和参考实现一样，这里是<b>先接管、再自己复刻原版的手臂变换</b>：NeoForge 的事件在变换之后触发，
 * 我们只能在那之前插进去，所以 {@link #applyVanillaArmTransform} 把
 * {@code ItemInHandRenderer.renderPlayerArm} 里的常量原样抄了一份，
 * 画出来的盔甲手臂才会落在原版手的位置上。
 */
public final class AntennaArmFirstPerson {

    private AntennaArmFirstPerson() {
    }

    /**
     * @return true = 已经用盔甲手臂画好了，调用方要取消原版手臂渲染；
     *         false = 条件不满足（没穿胸甲 / 拿不到渲染状态等），走原版
     */
    public static boolean render(PoseStack poseStack, SubmitNodeCollector collector, int packedLight,
                                 float equipProgress, float swingProgress, HumanoidArm arm) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null) {
            return false;
        }

        // 手臂属于胸甲那件（GeckoLib 的 CHEST 段包含左右手臂）
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!(chest.getItem() instanceof AntennaZBRitem)) {
            return false;
        }

        GeoRenderProvider provider = GeoRenderProvider.of(chest);
        if (provider == GeoRenderProvider.DEFAULT) {
            return false;
        }

        @SuppressWarnings("unchecked")
        GeoArmorRenderer<?, AvatarRenderState> renderer =
                (GeoArmorRenderer<?, AvatarRenderState>) provider.getGeoArmorRenderer(chest, EquipmentSlot.CHEST);
        if (renderer == null) {
            return false;
        }

        CameraRenderState camera = ((LevelRendererAccessor) client.levelRenderer)
                .corpseorigin$levelRenderState().cameraRenderState;
        if (camera == null) {
            return false;   // 拿不到相机状态就别硬画，老实退回原版手臂
        }

        AvatarRenderer<?> playerRenderer = client.getEntityRenderDispatcher().getPlayerRenderer(player);
        if (playerRenderer == null) {
            return false;
        }
        PlayerModel armorModel = playerRenderer.getModel();

        // 造一份当帧渲染状态交给 GeckoLib：它会自己从玩家真实槽位读这件盔甲，
        // 并把 baseModel（玩家模型）对应部位的姿势拷到盔甲骨的对应段上。
        // 这里状态是"静止"的（walk/attack 全 0），所以手臂骨拿到的就是绑定姿势 ——
        // 和参考实现里 right.updateRotation(0,0,0) 是同一个意思，摆位交给下面的 poseStack 变换。
        AvatarRenderState root = new AvatarRenderState();
        root.lightCoords = packedLight;
        root.skin = player.getSkin();
        renderer.captureRenderStates(root, player,
                client.getDeltaTracker().getGameTimeDeltaPartialTick(false),
                (state, slot) -> armorModel,
                slot -> copyOf(root));

        AvatarRenderState chestState = chestState(root);
        if (chestState == null) {
            return false;
        }

        // 只留正在画的这条手臂：其余段（头/身/腿/脚）加另一条手臂，连子树一起跳过
        List<String> hidden = new ArrayList<>(GeoArmorRenderer.ArmorSegment.values().length - 1);
        for (GeoArmorRenderer.ArmorSegment segment : GeoArmorRenderer.ArmorSegment.values()) {
            if (segment != armSegment(arm)) {
                hidden.add(renderer.getBoneNameForSegment(chestState, segment));
            }
        }

        poseStack.pushPose();
        applyVanillaArmTransform(poseStack, equipProgress, swingProgress, arm);

        renderer.performRenderPass(chestState, poseStack, collector, camera,
                List.of((info, bones) -> {
                    for (String name : hidden) {
                        bones.ifPresent(name, snap -> snap.skipRender(true).skipChildrenRender(true));
                    }
                }));

        poseStack.popPose();
        return true;
    }

    /** 取胸甲那一份 per-slot 渲染状态（手臂段在它上面） */
    private static AvatarRenderState chestState(AvatarRenderState root) {
        EnumMap<EquipmentSlot, ? extends net.minecraft.client.renderer.entity.state.HumanoidRenderState> perSlot =
                root.getGeckolibData(DataTickets.PER_SLOT_RENDER_DATA);
        if (perSlot == null) {
            return null;
        }
        return perSlot.get(EquipmentSlot.CHEST) instanceof AvatarRenderState state ? state : null;
    }

    private static GeoArmorRenderer.ArmorSegment armSegment(HumanoidArm arm) {
        return arm == HumanoidArm.RIGHT
                ? GeoArmorRenderer.ArmorSegment.RIGHT_ARM
                : GeoArmorRenderer.ArmorSegment.LEFT_ARM;
    }

    /**
     * 复制一份渲染状态给每个盔甲槽位。
     * <p>
     * GeckoLib 靠每份状态里的 {@code CURRENT_SLOT} 决定把穿戴者哪些部位的姿势拷到盔甲骨的哪些段上，
     * 所以槽位之间不能共用同一个对象。
     */
    private static AvatarRenderState copyOf(AvatarRenderState source) {
        AvatarRenderState copy = new AvatarRenderState();
        copy.skin = source.skin;
        copy.lightCoords = source.lightCoords;
        copy.isSpectator = false;
        copy.showHat = true;
        copy.showJacket = true;
        copy.showLeftPants = true;
        copy.showRightPants = true;
        copy.showLeftSleeve = true;
        copy.showRightSleeve = true;
        copy.showCape = false;
        copy.headEquipment = source.headEquipment;
        copy.chestEquipment = source.chestEquipment;
        copy.legsEquipment = source.legsEquipment;
        copy.feetEquipment = source.feetEquipment;
        return copy;
    }

    /**
     * 原版 {@code ItemInHandRenderer.renderPlayerArm} 的手臂变换，照抄一遍。
     * <p>
     * 抄的理由见类注释：我们注入在方法 HEAD（变换之前），而 Fabric 没有"变换之后"的事件点。
     * 这些常量与原版一一对应，改游戏版本时对着原版核对一下即可。
     */
    private static void applyVanillaArmTransform(PoseStack poseStack, float equipProgress,
                                                 float swingProgress, HumanoidArm arm) {
        boolean right = arm != HumanoidArm.LEFT;
        float f = right ? 1.0F : -1.0F;
        float sqrtSwing = Mth.sqrt(swingProgress);
        float f2 = -0.3F * Mth.sin(sqrtSwing * (float) Math.PI);
        float f3 = 0.4F * Mth.sin(sqrtSwing * ((float) Math.PI * 2.0F));
        float f4 = -0.4F * Mth.sin(swingProgress * (float) Math.PI);

        poseStack.translate(f * (f2 + 0.64000005F),
                f3 - 0.6F + equipProgress * -0.6F,
                f4 - 0.71999997F);
        poseStack.mulPose(Axis.YP.rotationDegrees(f * 45.0F));

        float f5 = Mth.sin(swingProgress * swingProgress * (float) Math.PI);
        float f6 = Mth.sin(sqrtSwing * (float) Math.PI);
        poseStack.mulPose(Axis.YP.rotationDegrees(f * f6 * 70.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(f * f5 * -20.0F));

        poseStack.translate(f * -1.0F, 3.6F, 3.5F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(f * 120.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(200.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(f * -135.0F));
        poseStack.translate(f * 5.6F, 0.0F, 0.0F);
    }
}
