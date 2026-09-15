package xiaoshi2022.corpseorigin.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.block.FluidStateModelSet;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.block.CloneChamberBlock;
import xiaoshi2022.corpseorigin.block.FluidKind;
import xiaoshi2022.corpseorigin.block.entity.CloneChamberBlockEntity;
import xiaoshi2022.corpseorigin.client.CorpseOriginClient;
import xiaoshi2022.corpseorigin.client.model.ExoskeletonModel;
import xiaoshi2022.corpseorigin.client.model.clone.VoxelModel;
import xiaoshi2022.corpseorigin.client.renderer.CloneArmorSupport;
import xiaoshi2022.corpseorigin.client.render.layer.ExoskeletonRenderLayer;
import xiaoshi2022.corpseorigin.client.skin.clone.ClientSkinCache;
import xiaoshi2022.corpseorigin.registry.ModBlocks;
import xiaoshi2022.corpseorigin.registry.ModModelLayers;

import java.util.List;

public class CloneChamberRenderer
        implements BlockEntityRenderer<CloneChamberBlockEntity, CloneChamberRenderState> {

    // ===== 6 个预置 BlockState =====
    private static final BlockState STATE_BODY_LOWER =
            ModBlocks.CLONE_CHAMBER.defaultBlockState()
                    .setValue(CloneChamberBlock.PART, CloneChamberBlock.Part.BODY_LOWER);

    private static final BlockState STATE_BODY_UPPER =
            ModBlocks.CLONE_CHAMBER.defaultBlockState()
                    .setValue(CloneChamberBlock.PART, CloneChamberBlock.Part.BODY_UPPER);

    private static final BlockState STATE_DOOR_LEFT_LOWER =
            ModBlocks.CLONE_CHAMBER.defaultBlockState()
                    .setValue(CloneChamberBlock.PART, CloneChamberBlock.Part.DOOR_LEFT_LOWER);

    private static final BlockState STATE_DOOR_LEFT_UPPER =
            ModBlocks.CLONE_CHAMBER.defaultBlockState()
                    .setValue(CloneChamberBlock.PART, CloneChamberBlock.Part.DOOR_LEFT_UPPER);

    private static final BlockState STATE_DOOR_RIGHT_LOWER =
            ModBlocks.CLONE_CHAMBER.defaultBlockState()
                    .setValue(CloneChamberBlock.PART, CloneChamberBlock.Part.DOOR_RIGHT_LOWER);

    private static final BlockState STATE_DOOR_RIGHT_UPPER =
            ModBlocks.CLONE_CHAMBER.defaultBlockState()
                    .setValue(CloneChamberBlock.PART, CloneChamberBlock.Part.DOOR_RIGHT_UPPER);

    /** 打印完成阈值，和 CloneState.COMPLETE_PROGRESS 一致 */
    private static final float COMPLETE_PROGRESS = 0.96F;

    private final BlockModelResolver modelResolver;
    private final PlayerModel cloneModel;
    private final VoxelModel voxelModel;
    @Nullable
    private final ExoskeletonModel exoskeletonModel;
    @Nullable
    private HumanoidArmorLayer<AvatarRenderState, PlayerModel, PlayerModel> armorLayer;

    public CloneChamberRenderer(BlockEntityRendererProvider.Context context) {
        this.modelResolver = context.blockModelResolver();

        EntityModelSet entityModels = Minecraft.getInstance().getEntityModels();
        this.cloneModel = new PlayerModel(
                entityModels.bakeLayer(ModModelLayers.CLONE_DUMMY), false);
        this.voxelModel = new VoxelModel(this.cloneModel);

        ExoskeletonModel baked = null;
        try {
            baked = new ExoskeletonModel(entityModels.bakeLayer(ModModelLayers.EXOSKELETON));
        } catch (Exception e) {
            CorpseOrigin.LOGGER.warn("仓内克隆人外骨骼模型烘焙失败，将不渲染外骨骼: {}", e.getMessage());
        }
        this.exoskeletonModel = baked;
    }

    @Override
    public CloneChamberRenderState createRenderState() {
        return new CloneChamberRenderState();
    }

    @Override
    public void extractRenderState(CloneChamberBlockEntity chamber, CloneChamberRenderState state,
                                   float partialTick, Vec3 cameraPos,
                                   net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay overlay) {
        BlockEntityRenderState.extractBase(chamber, state, overlay);
        state.lowerHalf = chamber.getBlockState().getValue(CloneChamberBlock.HALF) == DoubleBlockHalf.LOWER;
        state.facing = chamber.getBlockState().getValue(CloneChamberBlock.FACING);
        state.doorOpen = chamber.getDoorOpenProgress(partialTick);
        state.cloneProgress = chamber.getCloneProgress();
        state.cloneCompletion = chamber.getCloneCompletion();
        state.hasClone = chamber.hasClone();
        state.ownerUuid = chamber.getOwnerUuid();
        state.bodyUuid = chamber.bodyUuid();
        state.equipment = chamber.getEquipment();

        // ★ "别的模组的液体"在方块状态里只能记成 OTHER，外观改由渲染器自绘：
        //   贴图用原版水/熔岩的，颜色取流体自己烘焙模型上的染色
        Fluid storedFluid = chamber.getBlockState().getValue(CloneChamberBlock.FLUID).isUnknown()
                ? chamber.storedFluid()
                : null;
        state.customFluid = storedFluid == null ? null : storedFluid.defaultFluidState();
        if (state.customFluid != null) {
            state.customLavaLike = state.customFluid.is(FluidTags.LAVA);
            state.customTint = fluidTint(chamber, state.customFluid);
            state.fluidConnected = otherHalfHasFluid(chamber);
        }
    }

    /**
     * 流体染色：取流体自己烘焙模型上的 tint 源。
     * <p>
     * 26.2 的流体模型自带 tint 定义（常量色/生物群系色等），
     * 语义对应 NeoForge {@code IClientFluidTypeExtensions.getTintColor}。
     */
    private static int fluidTint(CloneChamberBlockEntity chamber, FluidState fluidState) {
        FluidModel model = Minecraft.getInstance().getModelManager().getFluidStateModelSet().get(fluidState);
        int tint;
        if (chamber.getLevel() instanceof ClientLevel clientLevel) {
            tint = model.tintSource().colorInWorld(chamber.getBlockState(), clientLevel, chamber.getBlockPos());
        } else {
            tint = model.tintSource().color(chamber.getBlockState());
        }
        // 常量色通常按 RGB 记，alpha 位为 0；补成不透明，不然整片液体会被 alpha=0 画没
        if ((tint & 0xFF000000) == 0) {
            tint |= 0xFF000000;
        }
        return tint;
    }

    /** 另一半仓格有没有液体（决定要不要剔除两半之间的接触面） */
    private static boolean otherHalfHasFluid(CloneChamberBlockEntity chamber) {
        Level level = chamber.getLevel();
        if (level == null) {
            return false;
        }
        BlockPos otherPos = chamber.getBlockState().getValue(CloneChamberBlock.HALF) == DoubleBlockHalf.LOWER
                ? chamber.getBlockPos().above()
                : chamber.getBlockPos().below();
        BlockState otherState = level.getBlockState(otherPos);
        return otherState.hasProperty(CloneChamberBlock.FLUID)
                && otherState.getValue(CloneChamberBlock.FLUID) != FluidKind.NONE;
    }

    @Override
    public void submit(CloneChamberRenderState state, PoseStack pose,
                       SubmitNodeCollector collector, CameraRenderState camera) {

        BlockDisplayContext displayContext = BlockDisplayContext.create();

        pose.pushPose();
        pose.translate(0.5F, 0.5F, 0.5F);
        pose.mulPose(Axis.YP.rotationDegrees(state.facing.getOpposite().toYRot()));
        pose.translate(-0.5F, -0.5F, -0.5F);

        // ===== 1. 本体 =====
        BlockState bodyState = state.lowerHalf ? STATE_BODY_LOWER : STATE_BODY_UPPER;
        BlockModelRenderState bodyRenderState = new BlockModelRenderState();
        modelResolver.update(bodyRenderState, bodyState, displayContext);
        bodyRenderState.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);

        // ===== 2. 门（向外开：门叶绕铰链转到仓外） =====
        float angle = state.doorOpen * 90.0F;

        // ===== 3. 克隆人（只在下半格渲染） =====
        if (state.lowerHalf && state.hasClone) {
            renderClone(pose, collector, state);
        }

        // 右门：铰链 origin [15.5, 16, 1]
        pose.pushPose();
        pose.translate(15.5F / 16.0F, 0.0F, 1.0F / 16.0F);
        pose.mulPose(Axis.YP.rotationDegrees(-angle));
        pose.translate(-15.5F / 16.0F, 0.0F, -1.0F / 16.0F);
        BlockState rightDoorState = state.lowerHalf ? STATE_DOOR_RIGHT_LOWER : STATE_DOOR_RIGHT_UPPER;
        BlockModelRenderState rightDoorRenderState = new BlockModelRenderState();
        modelResolver.update(rightDoorRenderState, rightDoorState, displayContext);
        rightDoorRenderState.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        pose.popPose();

        // 左门：铰链 origin [1.5, 16, 1]
        pose.pushPose();
        pose.translate(1.5F / 16.0F, 0.0F, 1.0F / 16.0F);
        pose.mulPose(Axis.YP.rotationDegrees(angle));
        pose.translate(-1.5F / 16.0F, 0.0F, -1.0F / 16.0F);
        BlockState leftDoorState = state.lowerHalf ? STATE_DOOR_LEFT_LOWER : STATE_DOOR_LEFT_UPPER;
        BlockModelRenderState leftDoorRenderState = new BlockModelRenderState();
        modelResolver.update(leftDoorRenderState, leftDoorState, displayContext);
        leftDoorRenderState.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        pose.popPose();

        pose.popPose();

        // ===== 4. 液体：其它模组的液体自己画（原版画不了方块状态里没记的流体） =====
        if (state.customFluid != null) {
            renderCustomFluid(state, pose, collector);
        }
    }

    /**
     * 仓内液体的自绘。
     * <p>
     * 不走原版 {@code FluidRenderer}（它输出的是区块分区局部坐标，且按方块状态剔除邻面，
     * 用在贝雕渲染器里位置和剔除都对不上），改为直接用原版水/熔岩的贴图画满格液体盒：
     * 贴图与渲染层照原版水/熔岩模板选，颜色取流体自己模型上的染色。
     */
    private void renderCustomFluid(CloneChamberRenderState state, PoseStack pose, SubmitNodeCollector collector) {
        Minecraft minecraft = Minecraft.getInstance();
        if (state.customFluid == null) {
            return;
        }

        FluidStateModelSet modelSet = minecraft.getModelManager().getFluidStateModelSet();
        // 贴图/渲染层用原版水或熔岩的模板
        FluidModel template = modelSet.get(state.customLavaLike
                ? Fluids.LAVA.defaultFluidState()
                : Fluids.WATER.defaultFluidState());
        TextureAtlasSprite sprite = template.stillMaterial().sprite();

        RenderType renderType = switch (template.layer()) {
            case SOLID -> RenderTypes.solidMovingBlock();
            case CUTOUT -> RenderTypes.cutoutMovingBlock();
            case TRANSLUCENT -> RenderTypes.translucentMovingBlock();
        };

        collector.submitCustomGeometry(pose, renderType, (poseEntry, consumer) -> {
            drawFluidBox(poseEntry, consumer, sprite, state.customTint, state.lightCoords,
                    state.lowerHalf, state.fluidConnected);
        });
    }

    /**
     * 画一个 0..1 的满格液体盒。
     * <p>
     * 上下两半各自画半段，两半之间的接触面剔除掉，整柱液体中间就不会多出一条液面。
     */
    private static void drawFluidBox(PoseStack.Pose pose, VertexConsumer consumer, TextureAtlasSprite sprite,
                                     int tint, int light, boolean lowerHalf, boolean connected) {
        if (!(lowerHalf && connected)) {
            face(pose, consumer, sprite, tint, light, Direction.DOWN);
        }
        if (!(!lowerHalf && connected)) {
            face(pose, consumer, sprite, tint, light, Direction.UP);
        }
        face(pose, consumer, sprite, tint, light, Direction.NORTH);
        face(pose, consumer, sprite, tint, light, Direction.SOUTH);
        face(pose, consumer, sprite, tint, light, Direction.WEST);
        face(pose, consumer, sprite, tint, light, Direction.EAST);
    }

    /** 画液体的一个面：四个角按面内平面取坐标（侧面 u 沿水平、v 沿高度，顶/底面 u/v 沿两根水平轴） */
    private static void face(PoseStack.Pose pose, VertexConsumer consumer, TextureAtlasSprite sprite,
                             int tint, int light, Direction direction) {
        float[] xs, ys, zs, us, vs;
        float nx = 0.0F, ny = 0.0F, nz = 0.0F;
        switch (direction) {
            case DOWN -> {
                xs = new float[]{0, 0, 1, 1}; ys = new float[]{0, 0, 0, 0}; zs = new float[]{0, 1, 1, 0};
                us = new float[]{0, 0, 1, 1}; vs = new float[]{0, 1, 1, 0};
                ny = -1.0F;
            }
            case UP -> {
                xs = new float[]{0, 0, 1, 1}; ys = new float[]{1, 1, 1, 1}; zs = new float[]{0, 1, 1, 0};
                us = new float[]{0, 0, 1, 1}; vs = new float[]{0, 1, 1, 0};
                ny = 1.0F;
            }
            case NORTH -> {
                xs = new float[]{0, 1, 1, 0}; ys = new float[]{0, 0, 1, 1}; zs = new float[]{0, 0, 0, 0};
                us = new float[]{0, 1, 1, 0}; vs = new float[]{0, 0, 1, 1};
                nz = -1.0F;
            }
            case SOUTH -> {
                xs = new float[]{0, 1, 1, 0}; ys = new float[]{0, 0, 1, 1}; zs = new float[]{1, 1, 1, 1};
                us = new float[]{0, 1, 1, 0}; vs = new float[]{0, 0, 1, 1};
                nz = 1.0F;
            }
            case WEST -> {
                xs = new float[]{0, 0, 0, 0}; ys = new float[]{0, 0, 1, 1}; zs = new float[]{0, 1, 1, 0};
                us = new float[]{0, 1, 1, 0}; vs = new float[]{0, 0, 1, 1};
                nx = -1.0F;
            }
            default -> { // EAST
                xs = new float[]{1, 1, 1, 1}; ys = new float[]{0, 0, 1, 1}; zs = new float[]{0, 1, 1, 0};
                us = new float[]{0, 1, 1, 0}; vs = new float[]{0, 0, 1, 1};
                nx = 1.0F;
            }
        }

        // movingBlock 渲染层开背面剔除，绕序写反整面就没了；
        // 两种绕序各画一遍，任何视角都有一个通过剔除（共面，另一份被剔，不会叠加混合）
        for (int pass = 0; pass < 2; pass++) {
            for (int i = 0; i < 4; i++) {
                int idx = pass == 0 ? i : 3 - i;
                consumer.addVertex(pose, xs[idx], ys[idx], zs[idx])
                        .setColor(tint)
                        .setUv(sprite.getU(us[idx]), sprite.getV(vs[idx]))
                        .setOverlay(OverlayTexture.NO_OVERLAY)
                        .setLight(light)
                        .setNormal(pose, nx, ny, nz);
            }
        }
    }

    // ==================== 克隆人渲染 ====================

    /**
     * 仓内空间在方块内的中心。
     * <p>
     * 对齐 {@code CloneChamberBlock} 的碰撞形状：内部 x 0.0625~0.94375、z 0.00625~0.94375，
     * 地面板顶面在 y 0.0625。直接用方块中心(0.5)会让克隆人偏向后壁。
     */
    private static final float INTERIOR_CENTER_X = (0.0625F + 0.94375F) / 2.0F;
    private static final float INTERIOR_CENTER_Z = (0.00625F + 0.94375F) / 2.0F;
    /** 仓内地板顶面高度 */
    private static final float INTERIOR_FLOOR_Y = 0.0625F;
    /** 模型抬升量：在 Y 翻转之后的坐标系里把模型脚底抬到落点上（负值 = 世界里的向上） */
    private static final float MODEL_LIFT = -1.40F;

    // ===== 两套形态各自独立的落点，改一边不影响另一边 =====

    /** 培育中（体素形态）：自定义几何直接按模型方块坐标重建，单独定位 */
    private static final float VOXEL_X = 0.6F;
    private static final float VOXEL_Y = INTERIOR_FLOOR_Y;
    private static final float VOXEL_Z = INTERIOR_CENTER_Z;

    /** 成熟后（完整模型，走 submitModel 管线） */
    private static final float BODY_X = INTERIOR_CENTER_X;
    private static final float BODY_Y = INTERIOR_FLOOR_Y;
    private static final float BODY_Z = INTERIOR_CENTER_Z;

    private void renderClone(PoseStack pose, SubmitNodeCollector collector, CloneChamberRenderState state) {
        float progress = state.cloneProgress;
        PlayerSkin skin = ClientSkinCache.resolve(state.ownerUuid);
        boolean grown = progress >= 1.0F;   // getCloneProgress() 已按这具身体自己的完成度归一化

        pose.pushPose();
        if (grown) {
            pose.translate(BODY_X, BODY_Y, BODY_Z);
        } else {
            pose.translate(VOXEL_X, VOXEL_Y, VOXEL_Z);
        }
        pose.scale(-1.0F, -1.0F, 1.0F);
        pose.translate(0.0F, MODEL_LIFT, 0.0F);

        if (!grown) {
            this.voxelModel.completeness = Math.min(1.0F, progress);

            collector.submitCustomGeometry(
                    pose,
                    RenderTypes.entityCutout(skin.body().texturePath()),
                    (poseEntry, consumer) -> {
                        PoseStack local = new PoseStack();
                        local.last().pose().set(poseEntry.pose());
                        local.last().normal().set(poseEntry.normal());
                        this.voxelModel.render(local, consumer,
                                state.lightCoords, OverlayTexture.NO_OVERLAY, -1);
                    });
        } else {
            AvatarRenderState avatar = new AvatarRenderState();
            avatar.skin = skin;
            avatar.lightCoords = state.lightCoords;
            avatar.isSpectator = false;
            avatar.showHat = true;
            avatar.showJacket = true;
            avatar.showLeftPants = true;
            avatar.showRightPants = true;
            avatar.showLeftSleeve = true;
            avatar.showRightSleeve = true;
            avatar.showCape = false;

            // ★ 盔甲：取这具身体自己穿的那套
            avatar.headEquipment = equipmentAt(state.equipment, 0);
            avatar.chestEquipment = equipmentAt(state.equipment, 1);
            avatar.legsEquipment = equipmentAt(state.equipment, 2);
            avatar.feetEquipment = equipmentAt(state.equipment, 3);

            collector.submitModel(
                    this.cloneModel,
                    avatar,
                    pose,
                    RenderTypes.entityTranslucent(skin.body().texturePath()),
                    state.lightCoords,
                    OverlayTexture.NO_OVERLAY,
                    -1,
                    null);

            // ★ 层：先盔甲，再尸兄外骨骼/红眼（放在 submitModel 之后，模型姿势已摆好）
            HumanoidArmorLayer<AvatarRenderState, PlayerModel, PlayerModel> armor =
                    this.armorLayer();
            if (armor != null) {
                armor.submit(pose, collector, state.lightCoords, avatar, 0.0F, 0.0F);
            }
            renderCorpseParts(pose, collector, state, avatar);
        }

        pose.popPose();
    }

    private static ItemStack equipmentAt(List<ItemStack> equipment, int index) {
        return index < equipment.size() ? equipment.get(index) : ItemStack.EMPTY;
    }

    // ==================== 盔甲 / 尸兄外骨骼 ====================

    @Nullable
    private HumanoidArmorLayer<AvatarRenderState, PlayerModel, PlayerModel> armorLayer() {
        if (this.armorLayer == null) {
            this.armorLayer = CloneArmorSupport.armorLayer(new ChamberLayerParent(this.cloneModel));
        }
        return this.armorLayer;
    }

    /** 盔甲层要求一个 RenderLayerParent，这里把它指向仓内这套模型 */
    private record ChamberLayerParent(PlayerModel model)
            implements RenderLayerParent<AvatarRenderState, PlayerModel> {

        @Override
        public PlayerModel getModel() { return this.model; }
    }

    /**
     * 仓内克隆人的尸兄外骨骼与红眼。
     * <p>
     * 和真玩家用的是同一套数据（客户端缓存的尸兄数据按 owner uuid 记）与同一张贴图，
     * 但贝雕渲染器没有实体渲染层的管道，所以这里手动提交模型部件。
     */
    private void renderCorpseParts(PoseStack pose, SubmitNodeCollector collector,
                                   CloneChamberRenderState state, AvatarRenderState avatar) {
        if (state.ownerUuid == null) {
            return;
        }
        // 尸兄状态按"这具身体"取（服务端按身体 uuid 单独同步过）
        CorpseOriginClient.ClientCorpseData corpseData =
                state.bodyUuid == null ? null : CorpseOriginClient.corpseDataCache.get(state.bodyUuid);
        boolean corpse = corpseData != null && corpseData.isCorpse && !corpseData.isDisguised();
        // 红眼是玩家自己的战斗状态，跟着账号走
        int redEye = CorpseOriginClient.tempRedEyeTicks.getOrDefault(state.ownerUuid, 0);
        if (!corpse && redEye <= 0) {
            return;
        }

        if (corpse && this.exoskeletonModel != null) {
            this.exoskeletonModel.copyFromHead(this.cloneModel.head);
            this.exoskeletonModel.setupAnim(avatar);
            collector.order(0).submitModelPart(
                    this.exoskeletonModel.getShieye(),
                    pose,
                    RenderTypes.entityTranslucent(ExoskeletonRenderLayer.EXOSKELETON_TEXTURE),
                    state.lightCoords,
                    OverlayTexture.NO_OVERLAY,
                    null);
        }

        if (redEye > 0) {
            collector.order(1).submitModelPart(
                    this.cloneModel.head,
                    pose,
                    RenderTypes.eyes(ExoskeletonRenderLayer.RED_EYE_OVERLAY),
                    state.lightCoords,
                    OverlayTexture.NO_OVERLAY,
                    null);
        }
    }

}