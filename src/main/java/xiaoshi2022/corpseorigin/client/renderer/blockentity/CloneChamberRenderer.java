package xiaoshi2022.corpseorigin.client.renderer.blockentity;

import com.geckolib.renderer.GeoArmorRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
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
import net.minecraft.world.entity.LivingEntity;
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
import xiaoshi2022.corpseorigin.client.render.layer.CloneRoleGeoLayer;
import xiaoshi2022.corpseorigin.client.render.layer.ExoskeletonRenderLayer;
import xiaoshi2022.corpseorigin.client.renderer.CloneArmorSupport;
import xiaoshi2022.corpseorigin.client.skin.clone.ClientSkinCache;
import xiaoshi2022.corpseorigin.registry.ModBlocks;
import xiaoshi2022.corpseorigin.registry.ModModelLayers;

import java.util.List;

public class CloneChamberRenderer
        implements BlockEntityRenderer<CloneChamberBlockEntity, CloneChamberRenderState> {

    // ===== 6 涓缃?BlockState =====
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

    /** 鎵撳嵃瀹屾垚闃堝€硷紝鍜?CloneState.COMPLETE_PROGRESS 涓€鑷?*/
    private static final float COMPLETE_PROGRESS = 0.96F;

    private final BlockModelResolver modelResolver;
    private final PlayerModel cloneModel;
    private final VoxelModel voxelModel;
    /** 浠撳唴鍏嬮殕浜虹殑缈呰唨 / 楸奸硟锛堝拰瀹炰綋涓婄殑 EvolutionPartsLayer 鍚屼竴濂楁墜缁樻ā鍨嬶級 */
    private final ModelPart leftWingPart;
    private final ModelPart rightWingPart;
    private final ModelPart gillsPart;
    @Nullable
    private final ExoskeletonModel exoskeletonModel;
    @Nullable
    private HumanoidArmorLayer<AvatarRenderState, PlayerModel, PlayerModel> armorLayer;

    private static ModelPart bakeExtraPart(String name, CubeListBuilder cubes) {
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild(name, cubes, PartPose.ZERO);
        return LayerDefinition.create(mesh, 16, 16).bakeRoot().getChild(name);
    }

    public CloneChamberRenderer(BlockEntityRendererProvider.Context context) {
        this.modelResolver = context.blockModelResolver();

        EntityModelSet entityModels = Minecraft.getInstance().getEntityModels();
        this.cloneModel = new PlayerModel(
                entityModels.bakeLayer(ModModelLayers.CLONE_DUMMY), false);
        this.voxelModel = new VoxelModel(this.cloneModel);

        // 瑙掕壊涓撳睘澶栬鐢ㄧ殑 GEO 娓叉煋鍣ㄧ敱 CloneAvatarRenderer 鍦ㄥ疄浣撴覆鏌撳櫒娉ㄥ唽鏃跺垱寤?
        // 锛堝鎴风鍚姩灏变細寤猴紝鏃╀簬浠讳綍浠撴覆鏌擄級锛涜繖閲屼笉閲嶅鍒濆鍖栥€?

        this.leftWingPart = bakeExtraPart("wing_left", CubeListBuilder.create()
                .addBox(1, 1, 2.5f, 13, 1, 1).addBox(3, 2, 2.7f, 10, 4, .6f)
                .addBox(4, 6, 2.7f, 7, 4, .6f).addBox(5, 10, 2.7f, 4, 3, .6f));
        this.rightWingPart = bakeExtraPart("wing_right", CubeListBuilder.create()
                .addBox(-14, 1, 2.5f, 13, 1, 1).addBox(-13, 2, 2.7f, 10, 4, .6f)
                .addBox(-11, 6, 2.7f, 7, 4, .6f).addBox(-9, 10, 2.7f, 4, 3, .6f));
        CubeListBuilder fins = CubeListBuilder.create();
        for (int i = 0; i < 3; i++) {
            fins.addBox(-4.8f, 2 + i * 2, -1, .8f, 1, 4);
            fins.addBox(4, 2 + i * 2, -1, .8f, 1, 4);
        }
        this.gillsPart = bakeExtraPart("gills", fins);

        ExoskeletonModel baked = null;
        try {
            baked = new ExoskeletonModel(entityModels.bakeLayer(ModModelLayers.EXOSKELETON));
        } catch (Exception e) {
            CorpseOrigin.LOGGER.warn("浠撳唴鍏嬮殕浜哄楠ㄩ妯″瀷鐑樼剻澶辫触锛屽皢涓嶆覆鏌撳楠ㄩ: {}", e.getMessage());
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
        state.entityType = chamber.getCloneEntityType();
        state.entityData = chamber.getCloneEntityData();
        state.partialTick = partialTick;

        // 鈽?"鍒殑妯＄粍鐨勬恫浣?鍦ㄦ柟鍧楃姸鎬侀噷鍙兘璁版垚 OTHER锛屽瑙傛敼鐢辨覆鏌撳櫒鑷粯锛?
        //   璐村浘鐢ㄥ師鐗堟按/鐔斿博鐨勶紝棰滆壊鍙栨祦浣撹嚜宸辩儤鐒欐ā鍨嬩笂鐨勬煋鑹?
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

    /** 娌″畾涔夋煋鑹茬殑姘寸郴娴佷綋鐨勫厹搴曡壊锛氳娑茬孩锛堝案鍏勬ā缁勭殑鍩瑰吇娑插氨鏄锛?*/
    private static final int BLOOD_TINT = 0xFF8A0303;

    /**
     * 娴佷綋鏌撹壊锛氬彇娴佷綋鑷繁鐑樼剻妯″瀷涓婄殑 tint 婧愩€?
     * <p>
     * 26.2 鐨勬祦浣撴ā鍨嬭嚜甯?tint 瀹氫箟锛堝父閲忚壊/鐢熺墿缇ょ郴鑹茬瓑锛夛紝
     * 璇箟瀵瑰簲 NeoForge {@code IClientFluidTypeExtensions.getTintColor}銆?
     */
    private static int fluidTint(CloneChamberBlockEntity chamber, FluidState fluidState) {
        FluidModel model = Minecraft.getInstance().getModelManager().getFluidStateModelSet().get(fluidState);
        // 娴佷綋娌″畾涔夋煋鑹叉椂 tintSource 涓?null锛堝 BOP 琛€娑诧級锛?
        // 鐔斿博绯绘寜鐧借壊锛堢啍宀╄创鍥炬湰韬甫鑹诧級锛屾按绯诲厹搴曟垚琛€娑茬孩
        BlockTintSource tintSource = model.tintSource();
        if (tintSource == null) {
            return fluidState.is(FluidTags.LAVA) ? -1 : BLOOD_TINT;
        }
        int tint;
        if (chamber.getLevel() instanceof ClientLevel clientLevel) {
            tint = tintSource.colorInWorld(chamber.getBlockState(), clientLevel, chamber.getBlockPos());
        } else {
            tint = tintSource.color(chamber.getBlockState());
        }
        // 甯搁噺鑹查€氬父鎸?RGB 璁帮紝alpha 浣嶄负 0锛涜ˉ鎴愪笉閫忔槑锛屼笉鐒舵暣鐗囨恫浣撲細琚?alpha=0 鐢绘病
        if ((tint & 0xFF000000) == 0) {
            tint |= 0xFF000000;
        }
        return tint;
    }

    /** 鍙︿竴鍗婁粨鏍兼湁娌℃湁娑蹭綋锛堝喅瀹氳涓嶈鍓旈櫎涓ゅ崐涔嬮棿鐨勬帴瑙﹂潰锛?*/
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

        // ===== 1. 鏈綋 =====
        BlockState bodyState = state.lowerHalf ? STATE_BODY_LOWER : STATE_BODY_UPPER;
        BlockModelRenderState bodyRenderState = new BlockModelRenderState();
        modelResolver.update(bodyRenderState, bodyState, displayContext);
        bodyRenderState.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);

        // ===== 2. 闂紙鍚戝寮€锛氶棬鍙剁粫閾伴摼杞埌浠撳锛?=====
        float angle = state.doorOpen * 90.0F;

        // ===== 3. 鍏嬮殕浜猴紙鍙湪涓嬪崐鏍兼覆鏌擄級 =====
        if (state.lowerHalf && state.hasClone) {
            renderClone(pose, collector, state, camera);
        }

        // 鍙抽棬锛氶摪閾?origin [15.5, 16, 1]
        pose.pushPose();
        pose.translate(15.5F / 16.0F, 0.0F, 1.0F / 16.0F);
        pose.mulPose(Axis.YP.rotationDegrees(-angle));
        pose.translate(-15.5F / 16.0F, 0.0F, -1.0F / 16.0F);
        BlockState rightDoorState = state.lowerHalf ? STATE_DOOR_RIGHT_LOWER : STATE_DOOR_RIGHT_UPPER;
        BlockModelRenderState rightDoorRenderState = new BlockModelRenderState();
        modelResolver.update(rightDoorRenderState, rightDoorState, displayContext);
        rightDoorRenderState.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        pose.popPose();

        // 宸﹂棬锛氶摪閾?origin [1.5, 16, 1]
        pose.pushPose();
        pose.translate(1.5F / 16.0F, 0.0F, 1.0F / 16.0F);
        pose.mulPose(Axis.YP.rotationDegrees(angle));
        pose.translate(-1.5F / 16.0F, 0.0F, -1.0F / 16.0F);
        BlockState leftDoorState = state.lowerHalf ? STATE_DOOR_LEFT_LOWER : STATE_DOOR_LEFT_UPPER;
        BlockModelRenderState leftDoorRenderState = new BlockModelRenderState();
        modelResolver.update(leftDoorRenderState, leftDoorState, displayContext);
        leftDoorRenderState.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        pose.popPose();

        // ===== 4. 娑蹭綋锛氬叾瀹冩ā缁勭殑娑蹭綋鑷繁鐢伙紙鍘熺増鐢讳笉浜嗘柟鍧楃姸鎬侀噷娌¤鐨勬祦浣擄級 =====
        // 鏀惧湪鏈濆悜鏃嬭浆涔嬪唴锛氭恫浣撶洅瀵归綈鐨勬槸妯″瀷鍧愭爣绯婚噷鐨勪粨鍐呯┖鑵旓紝瑕佽窡浠撲綋涓€璧疯浆
        if (state.customFluid != null) {
            renderCustomFluid(state, pose, collector);
        }

        pose.popPose();
    }

    /**
     * 浠撳唴娑蹭綋鐨勮嚜缁樸€?
     * <p>
     * 涓嶈蛋鍘熺増 {@code FluidRenderer}锛堝畠杈撳嚭鐨勬槸鍖哄潡鍒嗗尯灞€閮ㄥ潗鏍囷紝涓旀寜鏂瑰潡鐘舵€佸墧闄ら偦闈紝
     * 鐢ㄥ湪璐濋洉娓叉煋鍣ㄩ噷浣嶇疆鍜屽墧闄ら兘瀵逛笉涓婏級锛屾敼涓虹洿鎺ョ敤鍘熺増姘?鐔斿博鐨勮创鍥?
     * 鐢讳竴涓创鐫€浠撳唴绌鸿厰鐨勬恫浣撶洅锛氳创鍥句笌娓叉煋灞傜収鍘熺増姘?鐔斿博妯℃澘閫夛紝棰滆壊鍙栨祦浣撹嚜宸辨ā鍨嬩笂鐨勬煋鑹层€?
     */
    private void renderCustomFluid(CloneChamberRenderState state, PoseStack pose, SubmitNodeCollector collector) {
        Minecraft minecraft = Minecraft.getInstance();
        if (state.customFluid == null) {
            return;
        }

        FluidStateModelSet modelSet = minecraft.getModelManager().getFluidStateModelSet();
        // 璐村浘/娓叉煋灞傜敤鍘熺増姘存垨鐔斿博鐨勬ā鏉?
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
     * 鐢讳竴涓创鐫€浠撳唴绌鸿厰鐨勬恫浣撶洅锛堢┖蹇冨３锛岄潰閮藉湪绌鸿厰杈圭晫涓婏級銆?
     * <p>
     * 绌鸿厰瀵归綈 {@code CloneChamberBlock} 鐨勭鎾炲舰鐘讹紙妯″瀷鏈濆寳鐨勫潗鏍囩郴锛岄殢浠撲綋涓€璧锋棆杞級锛?
     * x 0.0625~0.94375銆亃 0.00625~0.94375锛涗笅鍗婁粠鍦版澘椤堕潰 0.0625 璧凤紝
     * 涓婂崐鍒伴《鐩栦笅娌?0.91875 姝€傛暣浣撳啀鍐呯缉涓€涓濓紝閬垮厤鍜屼粨澹佽〃闈?z-fighting 绌挎ā銆?
     * <p>
     * 涓婁笅涓ゅ崐鍚勮嚜鐢诲崐娈碉紝涓ゅ崐涔嬮棿鐨勬帴瑙﹂潰鍓旈櫎鎺夛紝鏁存煴娑蹭綋涓棿灏变笉浼氬鍑轰竴鏉℃恫闈€?
     */
    private static void drawFluidBox(PoseStack.Pose pose, VertexConsumer consumer, TextureAtlasSprite sprite,
                                     int tint, int light, boolean lowerHalf, boolean connected) {
        float pad = 0.001F;
        float x0 = 0.0625F + pad, x1 = 0.94375F - pad;
        float z0 = 0.00625F + pad, z1 = 0.94375F - pad;
        float y0 = lowerHalf ? 0.0625F + pad : pad;
        float y1 = lowerHalf ? 1.0F : 0.91875F - pad;

        if (!(lowerHalf && connected)) {
            face(pose, consumer, sprite, tint, light, Direction.DOWN, x0, x1, y0, y1, z0, z1);
        }
        if (!(!lowerHalf && connected)) {
            face(pose, consumer, sprite, tint, light, Direction.UP, x0, x1, y0, y1, z0, z1);
        }
        face(pose, consumer, sprite, tint, light, Direction.NORTH, x0, x1, y0, y1, z0, z1);
        face(pose, consumer, sprite, tint, light, Direction.SOUTH, x0, x1, y0, y1, z0, z1);
        face(pose, consumer, sprite, tint, light, Direction.WEST, x0, x1, y0, y1, z0, z1);
        face(pose, consumer, sprite, tint, light, Direction.EAST, x0, x1, y0, y1, z0, z1);
    }

    /** 鐢绘恫浣撶殑涓€涓潰锛氬洓涓鎸夐潰鍐呭钩闈㈠彇鍧愭爣锛堜晶闈?u 娌挎按骞炽€乿 娌块珮搴︼紝椤?搴曢潰 u/v 娌夸袱鏍规按骞宠酱锛?*/
    private static void face(PoseStack.Pose pose, VertexConsumer consumer, TextureAtlasSprite sprite,
                             int tint, int light, Direction direction,
                             float x0, float x1, float y0, float y1, float z0, float z1) {
        float[] xs, ys, zs, us, vs;
        float nx = 0.0F, ny = 0.0F, nz = 0.0F;
        switch (direction) {
            case DOWN -> {
                xs = new float[]{x0, x0, x1, x1}; ys = new float[]{y0, y0, y0, y0}; zs = new float[]{z0, z1, z1, z0};
                us = new float[]{0, 0, 1, 1}; vs = new float[]{0, 1, 1, 0};
                ny = -1.0F;
            }
            case UP -> {
                xs = new float[]{x0, x0, x1, x1}; ys = new float[]{y1, y1, y1, y1}; zs = new float[]{z0, z1, z1, z0};
                us = new float[]{0, 0, 1, 1}; vs = new float[]{0, 1, 1, 0};
                ny = 1.0F;
            }
            case NORTH -> {
                xs = new float[]{x0, x1, x1, x0}; ys = new float[]{y0, y0, y1, y1}; zs = new float[]{z0, z0, z0, z0};
                us = new float[]{0, 1, 1, 0}; vs = new float[]{0, 0, 1, 1};
                nz = -1.0F;
            }
            case SOUTH -> {
                xs = new float[]{x0, x1, x1, x0}; ys = new float[]{y0, y0, y1, y1}; zs = new float[]{z1, z1, z1, z1};
                us = new float[]{0, 1, 1, 0}; vs = new float[]{0, 0, 1, 1};
                nz = 1.0F;
            }
            case WEST -> {
                xs = new float[]{x0, x0, x0, x0}; ys = new float[]{y0, y0, y1, y1}; zs = new float[]{z0, z1, z1, z0};
                us = new float[]{0, 1, 1, 0}; vs = new float[]{0, 0, 1, 1};
                nx = -1.0F;
            }
            default -> { // EAST
                xs = new float[]{x1, x1, x1, x1}; ys = new float[]{y0, y0, y1, y1}; zs = new float[]{z0, z1, z1, z0};
                us = new float[]{0, 1, 1, 0}; vs = new float[]{0, 0, 1, 1};
                nx = 1.0F;
            }
        }

        // movingBlock 娓叉煋灞傚紑鑳岄潰鍓旈櫎锛岀粫搴忓啓鍙嶆暣闈㈠氨娌′簡锛?
        // 涓ょ缁曞簭鍚勭敾涓€閬嶏紝浠讳綍瑙嗚閮芥湁涓€涓€氳繃鍓旈櫎锛堝叡闈紝鍙︿竴浠借鍓旓紝涓嶄細鍙犲姞娣峰悎锛?
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

    // ==================== 鍏嬮殕浜烘覆鏌?====================

    /**
     * 浠撳唴绌洪棿鍦ㄦ柟鍧楀唴鐨勪腑蹇冦€?
     * <p>
     * 瀵归綈 {@code CloneChamberBlock} 鐨勭鎾炲舰鐘讹細鍐呴儴 x 0.0625~0.94375銆亃 0.00625~0.94375锛?
     * 鍦伴潰鏉块《闈㈠湪 y 0.0625銆傜洿鎺ョ敤鏂瑰潡涓績(0.5)浼氳鍏嬮殕浜哄亸鍚戝悗澹併€?
     */
    private static final float INTERIOR_CENTER_X = (0.0625F + 0.94375F) / 2.0F;
    private static final float INTERIOR_CENTER_Z = (0.00625F + 0.94375F) / 2.0F;
    /** 浠撳唴鍦版澘椤堕潰楂樺害 */
    private static final float INTERIOR_FLOOR_Y = 0.0625F;
    /** 妯″瀷鎶崌閲忥細鍦?Y 缈昏浆涔嬪悗鐨勫潗鏍囩郴閲屾妸妯″瀷鑴氬簳鎶埌钀界偣涓婏紙璐熷€?= 涓栫晫閲岀殑鍚戜笂锛?*/
    private static final float MODEL_LIFT = -1.40F;

    // ===== 涓ゅ褰㈡€佸悇鑷嫭绔嬬殑钀界偣锛屾敼涓€杈逛笉褰卞搷鍙︿竴杈?=====

    /** 鍩硅偛涓紙浣撶礌褰㈡€侊級锛氳嚜瀹氫箟鍑犱綍鐩存帴鎸夋ā鍨嬫柟鍧楀潗鏍囬噸寤猴紝鍗曠嫭瀹氫綅 */
    private static final float VOXEL_X = 0.6F;
    private static final float VOXEL_Y = INTERIOR_FLOOR_Y;
    private static final float VOXEL_Z = INTERIOR_CENTER_Z;

    /** 鎴愮啛鍚庯紙瀹屾暣妯″瀷锛岃蛋 submitModel 绠＄嚎锛?*/
    private static final float BODY_X = INTERIOR_CENTER_X;
    private static final float BODY_Y = INTERIOR_FLOOR_Y;
    private static final float BODY_Z = INTERIOR_CENTER_Z;

    private void renderClone(PoseStack pose, SubmitNodeCollector collector, CloneChamberRenderState state, CameraRenderState camera) {
        // 鈽?鐢熺墿鍏嬮殕浣擄細鐢ㄥ搴旂敓鐗╃殑娓叉煋鍣ㄧ敾鍑哄拰鍘熷疄浣撲竴鏍风殑澶栬
        if (state.entityType != null) {
            renderEntityClone(pose, collector, state, camera);
            return;
        }

        float progress = state.cloneProgress;
        PlayerSkin skin = ClientSkinCache.resolve(state.ownerUuid);
        boolean grown = progress >= 1.0F;   // getCloneProgress() 宸叉寜杩欏叿韬綋鑷繁鐨勫畬鎴愬害褰掍竴鍖?

        pose.pushPose();
        if (grown) {
            pose.translate(BODY_X, BODY_Y, BODY_Z);
        } else {
            pose.translate(VOXEL_X, VOXEL_Y, VOXEL_Z);
        }

        if (grown) {
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

            // 鈽?鐩旂敳锛氬彇杩欏叿韬綋鑷繁绌跨殑閭ｅ
            avatar.headEquipment = equipmentAt(state.equipment, 0);
            avatar.chestEquipment = equipmentAt(state.equipment, 1);
            avatar.legsEquipment = equipmentAt(state.equipment, 2);
            avatar.feetEquipment = equipmentAt(state.equipment, 3);

            // 鈽?瑙掕壊涓撳睘澶栬锛堝皬閲戝垰灏稿厔 / 寮€鑳冨ザ鑳屾寕鈥︹€︼級锛氫笌瀹炰綋涓婃槸鍚屼竴濂?GEO 绠＄嚎锛?
            //   鍙槸鏁版嵁婧愭崲鎴?CloneBodySyncS2C 鐨勭紦瀛橈紙鍩硅偛涓殑韬綋杩樻病鎴愪负瀹炰綋锛夈€?
            //   鈿狅笍 蹇呴』鎺掑湪涓嬮潰鐨?scale(-1,-1,1) 涔嬪墠锛欸EO 璧扮殑鏄師鐗堝疄浣撴覆鏌撻偅濂楀彉鎹紝
            //   鑷繁浼氬仛缈昏浆涓庝綅绉伙紝澶氱炕涓€娆℃ā鍨嬪氨鍙嶄簡銆?
            boolean roleGeo = false;
            try {
                CloneRoleGeoLayer.extractForChamber(state.bodyUuid, avatar, 0.0F);
                // 鑳屾寕闀垮湪鑳屽悗锛屽厛鎻愪氦锛堟繁搴︽祴璇曚細璁╄韩浣撴甯告尅浣忓畠锛?
                roleGeo = !CloneRoleGeoLayer.replacesBody(avatar)
                        && CloneRoleGeoLayer.submitReplacing(avatar, pose, collector, camera);
            } catch (Throwable t) {
                CorpseOrigin.LOGGER.warn("[CorpseOrigin] 浠撳唴瑙掕壊澶栬娓叉煋澶辫触锛屽凡鍥為€€鍘熸牱: {}", t.toString());
            }
            if (roleGeo) {
                pose.scale(-1.0F, -1.0F, 1.0F);
                pose.translate(0.0F, MODEL_LIFT, 0.0F);
                this.cloneModel.setupAnim(avatar);
                CloneRoleGeoLayer.submitOrgans(avatar, pose, collector, this.cloneModel);
                // 鏁磋韩鏇挎崲鍨嬶細鍘熺増鍏嬮殕浜?/ 鐩旂敳 / 灏稿厔闆朵欢鍏ㄩ儴涓嶇敾锛堜笌鐜╁渚у悓涓€鍙栬垗锛?
                pose.popPose();
                return;
            }

            pose.scale(-1.0F, -1.0F, 1.0F);
            pose.translate(0.0F, MODEL_LIFT, 0.0F);

            if (CloneRoleGeoLayer.replacesBody(avatar)) {
                this.cloneModel.setupAnim(avatar);
                CloneRoleGeoLayer.submitOrgans(avatar, pose, collector, this.cloneModel);
                pose.popPose();
                return;
            }
            collector.submitModel(
                    this.cloneModel,
                    avatar,
                    pose,
                    RenderTypes.entityTranslucent(skin.body().texturePath()),
                    state.lightCoords,
                    OverlayTexture.NO_OVERLAY,
                    -1,
                    null);

            // 鈽?灞傦細鍏堢洈鐢诧紝鍐嶅案鍏勫楠ㄩ/绾㈢溂锛堟斁鍦?submitModel 涔嬪悗锛屾ā鍨嬪Э鍔垮凡鎽嗗ソ锛?
            HumanoidArmorLayer<AvatarRenderState, PlayerModel, PlayerModel> armor =
                    this.armorLayer();
            if (armor != null) {
                // 鈽?GeckoLib 鐨?geo 鐩旂敳瑕佹湁"姣忔Ы浣嶆覆鏌撴暟鎹?鎵嶄細鎺ョ锛岃€岄偅浠芥暟鎹彧鍦ㄥ疄浣撴覆鏌撶姸鎬?
                //   鍒涘缓鏃剁敱 GeckoLib 鐨?EntityRendererMixin 濉?鈥斺€?鏂瑰潡瀹炰綋娓叉煋娌℃湁閭ｄ竴姝ワ紝
                //   浜庢槸鐩旂敳浼氭帀鍥炲師鐗堥€氶亾銆佹寜 ArmorMaterial 鐢绘垚閽荤煶鐢层€?
                //   杩欓噷鎷夸竴鍏风灞忓亣韬紙瑁呭宸插杩涚湡瀹炴Ы浣嶏級鎵嬪姩琛ヤ竴娆★紝涔嬪悗鐩旂敳灞傝嚜宸卞氨浼氳蛋 geo 閫氶亾銆?
                LivingEntity dummy = CloneArmorSupport.dummyWearer(state.equipment);
                if (dummy != null) {
                    GeoArmorRenderer.captureRenderStates(avatar, dummy, 0.0F,
                            (ignored, slot) -> this.cloneModel,
                            slot -> copyRenderState(avatar));
                }
                armor.submit(pose, collector, state.lightCoords, avatar, 0.0F, 0.0F);
            }
            renderCorpseParts(pose, collector, state, avatar);
            // 鈽?钁姦锛堣懌鑺﹀皬閲戝垰锛夛細鎸傚湪 body 楠ㄩ涓婏紝鎵€浠ヨ鍦ㄦā鍨嬪Э鍔挎憜濂戒箣鍚庛€?
            //   鐢?妯″瀷鏍?绌洪棿鐨?pose 鎻愪氦锛堜笌瀹炰綋涓婄殑灞傝蛋鍚屼竴鏉￠敋鐐瑰彉鎹級銆?
            //   鍏堟樉寮忔憜涓€娆″Э鍔匡紝淇濊瘉璇诲埌鐨?body 楠ㄩ浣嶇疆灏辨槸杩欎竴甯х殑銆?
            this.cloneModel.setupAnim(avatar);
            CloneRoleGeoLayer.submitBackMount(avatar, pose, collector, this.cloneModel, camera);
            CloneRoleGeoLayer.submitGourd(avatar, pose, collector, this.cloneModel, camera);
            CloneRoleGeoLayer.submitOrgans(avatar, pose, collector, this.cloneModel);
        } else {
            pose.scale(-1.0F, -1.0F, 1.0F);
            pose.translate(0.0F, MODEL_LIFT, 0.0F);

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
        }

        pose.popPose();
    }

    // ==================== 鐢熺墿鍏嬮殕浣撴覆鏌?====================

    /** 缂撳瓨鐨勭灞忕敓鐗╁疄浣擄紙鎸夊疄浣撶被鍨?ID 缂撳瓨锛岄伩鍏嶆瘡甯ч噸寤猴級 */
    private static final java.util.Map<Identifier, net.minecraft.world.entity.Entity> ENTITY_CACHE = new java.util.HashMap<>();

    /** 绂诲睆瀹炰綋鐨?id 鍙戝彿鍣細鐢ㄩ€掑噺鐨勮礋鏁帮紝淇濊瘉闈?0 涓斾笉涓庢湇鍔＄鍒嗛厤鐨勬鏁?id 鎾炶溅 */
    private static int nextOffscreenId = -1;

    /**
     * 娓叉煋鐢熺墿鍏嬮殕浣擄細鐢ㄨ鐢熺墿绫诲瀷鑷繁鐨勬覆鏌撳櫒鐢诲嚭鍜屽師瀹炰綋瀹屽叏涓€鑷寸殑澶栬銆?
     * <p>
     * 鏂规锛氬鎴风鍒涘缓涓€鍏峰悓绫诲瀷鐨勭灞忓疄浣擄紝鍔犺浇瀛樺偍鐨?NBT锛堜繚鐣欒澶?澶栬/鐘舵€侊級锛?
     * 鍐嶇敤 {@code EntityRenderDispatcher.submit} 鐢诲埌浠撳唴銆傚疄浣撲笉鍔犲叆涓栫晫锛岀函娓叉煋鐢ㄣ€?
     */
    private void renderEntityClone(PoseStack pose, SubmitNodeCollector collector, CloneChamberRenderState state, CameraRenderState camera) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || state.entityType == null) {
            return;
        }

        // 1. 鍙栵紙鎴栧垱寤猴級绂诲睆瀹炰綋
        net.minecraft.world.entity.Entity entity = ENTITY_CACHE.get(state.entityType);
        boolean created = false;
        if (entity == null || entity.level() != mc.level) {
            var ref = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.get(state.entityType).orElse(null);
            if (ref == null) return;
            net.minecraft.world.entity.EntityType<?> type = ref.value();
            entity = type.create(mc.level, net.minecraft.world.entity.EntitySpawnReason.LOAD);
            if (entity == null) return;
            created = true;
            ENTITY_CACHE.put(state.entityType, entity);
        }

        // 2. 鍔犺浇 NBT锛堜繚鐣欏瑙?瑁呭/鐘舵€佹晥鏋滐級
        if (state.entityData != null) {
            net.minecraft.nbt.CompoundTag data = state.entityData.copy();
            entity.load(net.minecraft.world.level.storage.TagValueInput.create(
                    net.minecraft.util.ProblemReporter.DISCARDING,
                    mc.level.registryAccess(),
                    data));
        }

        // 鈽?瀹炰綋娌″姞鍏ヤ笘鐣屽氨娌℃湁 id锛坕d == 0锛夛紝鑰?GeckoLib 鎻愬彇娓叉煋鐘舵€佹椂浼氱粰鎵嬫寔鐗?
        //   璋?ItemModelResolver.updateForLiving 鈫?Entity.getId()锛岄偅閲屽 id == 0 鐩存帴鎶?
        //   "Tried to access entity ID before ID assignment"銆?
        //   鏀惧湪 load 涔嬪悗琛ワ紝鍏嶅緱琚?NBT 閲岀殑瀛楁鐩栨帀锛涘彧鍦ㄦ柊寤洪偅涓€甯цˉ涓€娆★紝缂撳瓨鐨勫疄浣撴部鐢ㄣ€?
        if (created) {
            entity.setId(nextOffscreenId--);
        }

        // 3. 瀹氫綅鍒颁粨鍐呬腑蹇冿紝閫傚綋缂╂斁閫傚簲浠撲綋
        pose.pushPose();
        pose.translate(BODY_X, BODY_Y, BODY_Z);
        float scale = 0.9F;
        pose.scale(scale, scale, scale);
        // 鏈濆悜鐜╁锛堥潰鏈濅粨闂級
        pose.mulPose(Axis.YP.rotationDegrees(180.0F));

        // 4. 鎻愬彇瀹炰綋娓叉煋鐘舵€佸苟鎻愪氦缁樺埗
        @SuppressWarnings({"rawtypes", "unchecked"})
        net.minecraft.client.renderer.entity.EntityRenderer renderer =
                mc.getEntityRenderDispatcher().getRenderer(entity);

        // GeckoLib 鐨?createRenderState() 杩斿洖 null锛屽繀椤荤敤 createRenderState(animatable, partialTick)
        // 瀹冨唴閮ㄤ細鍚屾椂瀹屾垚鍒涘缓 + extractRenderState + finalizeRenderState
        net.minecraft.client.renderer.entity.state.EntityRenderState renderState;
        if (renderer instanceof com.geckolib.renderer.GeoEntityRenderer geoRenderer) {
            renderState = geoRenderer.createRenderState(entity, state.partialTick);
        } else {
            renderState = renderer.createRenderState();
            if (renderState != null) {
                renderer.extractRenderState(entity, renderState, state.partialTick);
            }
        }

        if (renderState == null) {
            pose.popPose();
            return;
        }

        // 鈽?琛ュ厜鐓э細姝ｅ父璧?EntityRenderDispatcher 鏃跺畠浼氬厛鎸夊疄浣撴墍鍦ㄤ綅缃畻濂?lightCoords
        //   鍐?extract锛屾垜浠繖閲屾槸鎵嬪姩 extract 鐨勶紝涓嶈ˉ杩欎竴鍙ュ厜鐓у氨鎭掍负 0 鈥斺€?鐢诲嚭鏉ヤ竴鐗囨榛戙€?
        //   鐩存帴鐢ㄤ粨鏍艰嚜宸辩殑鍏夌収鍗冲彲锛堝厠闅嗕綋灏卞湪浠撻噷锛夈€?
        renderState.lightCoords = state.lightCoords;

        mc.getEntityRenderDispatcher().submit(
                renderState, camera, 0, 0, 0, pose, collector);

        pose.popPose();
    }

    /**
     * 澶嶅埗涓€浠戒粨鍐呭厠闅嗕汉鐨勬覆鏌撶姸鎬併€?
     * <p>
     * GeckoLib 缁欐瘡涓洈鐢叉Ы浣嶅悇瑕佷竴浠?render state锛堝畠闈?{@code CURRENT_SLOT} 鍐冲畾鎶婄┛鎴磋€呯殑
     * 鍝簺閮ㄤ綅濮垮娍鎷峰埌鐩旂敳楠ㄧ殑鍝簺娈典笂锛夛紝鎵€浠ヨ繖閲屾瘡涓Ы浣嶉兘鏂板缓涓€浠斤紝
     * 鑰屼笉鏄洓浠剁洈鐢插叡鐢ㄥ悓涓€涓璞°€?
     */
    private static AvatarRenderState copyRenderState(AvatarRenderState source) {
        AvatarRenderState copy = new AvatarRenderState();
        copy.skin = source.skin;
        copy.lightCoords = source.lightCoords;
        copy.isSpectator = source.isSpectator;
        copy.showHat = source.showHat;
        copy.showJacket = source.showJacket;
        copy.showLeftPants = source.showLeftPants;
        copy.showRightPants = source.showRightPants;
        copy.showLeftSleeve = source.showLeftSleeve;
        copy.showRightSleeve = source.showRightSleeve;
        copy.showCape = source.showCape;
        copy.headEquipment = source.headEquipment;
        copy.chestEquipment = source.chestEquipment;
        copy.legsEquipment = source.legsEquipment;
        copy.feetEquipment = source.feetEquipment;
        return copy;
    }

    private static ItemStack equipmentAt(List<ItemStack> equipment, int index) {
        return index < equipment.size() ? equipment.get(index) : ItemStack.EMPTY;
    }

    // ==================== 鐩旂敳 / 灏稿厔澶栭楠?====================

    @Nullable
    private HumanoidArmorLayer<AvatarRenderState, PlayerModel, PlayerModel> armorLayer() {
        if (this.armorLayer == null) {
            this.armorLayer = CloneArmorSupport.armorLayer(new ChamberLayerParent(this.cloneModel));
        }
        return this.armorLayer;
    }

    /** 鐩旂敳灞傝姹備竴涓?RenderLayerParent锛岃繖閲屾妸瀹冩寚鍚戜粨鍐呰繖濂楁ā鍨?*/
    private record ChamberLayerParent(PlayerModel model)
            implements RenderLayerParent<AvatarRenderState, PlayerModel> {

        @Override
        public PlayerModel getModel() { return this.model; }
    }

    /**
     * 浠撳唴鍏嬮殕浜虹殑灏稿厔澶栭楠间笌绾㈢溂銆?
     * <p>
     * 鍜岀湡鐜╁鐢ㄧ殑鏄悓涓€濂楁暟鎹紙瀹㈡埛绔紦瀛樼殑灏稿厔鏁版嵁鎸?owner uuid 璁帮級涓庡悓涓€寮犺创鍥撅紝
     * 浣嗚礉闆曟覆鏌撳櫒娌℃湁瀹炰綋娓叉煋灞傜殑绠￠亾锛屾墍浠ヨ繖閲屾墜鍔ㄦ彁浜ゆā鍨嬮儴浠躲€?
     */
    private void renderCorpseParts(PoseStack pose, SubmitNodeCollector collector,
                                   CloneChamberRenderState state, AvatarRenderState avatar) {
        if (state.ownerUuid == null) {
            return;
        }
        // 灏稿厔鐘舵€佹寜"杩欏叿韬綋"鍙栵紙鏈嶅姟绔寜韬綋 uuid 鍗曠嫭鍚屾杩囷級
        xiaoshi2022.corpseorigin.client.ClientCorpseData corpseData =
                state.bodyUuid == null ? null : CorpseOriginClient.corpseDataCache.get(state.bodyUuid);
        boolean corpse = corpseData != null && corpseData.isCorpse && !corpseData.isDisguised();
        // 绾㈢溂鏄帺瀹惰嚜宸辩殑鎴樻枟鐘舵€侊紝璺熺潃璐﹀彿璧?
        int redEye = CorpseOriginClient.tempRedEyeTicks.getOrDefault(state.ownerUuid, 0);
        if (!corpse && redEye <= 0) {
            return;
        }

        if (corpse && corpseData.showsCorpseEye() && this.exoskeletonModel != null) {
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

        // 缈呰唨 / 楸奸硟锛氳鑹插瑙傚寘鎸夎韩浣?UUID 缂撳瓨锛涢敋鍦?cloneModel.body 涓婃墜缁?
        if (corpse && state.bodyUuid != null) {
            CorpseOriginClient.ClientCloneBody cloneBody =
                    CorpseOriginClient.cloneBodyDataCache.get(state.bodyUuid);
            if (cloneBody != null) {
                boolean wings = cloneBody.hasTrait("wings");
                boolean hasGills = cloneBody.hasTrait("gills");
                if (wings || hasGills) {
                    this.cloneModel.setupAnim(avatar);
                    pose.pushPose();
                    this.cloneModel.body.translateAndRotate(pose);
                    ClientLevel clientLevel = Minecraft.getInstance().level;
                    float time = clientLevel == null ? 0F : clientLevel.getGameTime();
                    if (wings) {
                        float flap = (float) Math.sin(time * .12) * .35f;
                        this.leftWingPart.yRot = .35f + flap;
                        this.rightWingPart.yRot = -.35f - flap;
                        collector.order(2).submitModelPart(this.leftWingPart, pose,
                                RenderTypes.entityCutout(CorpseOrigin.id("textures/entity/chapter_blood.png")),
                                state.lightCoords, OverlayTexture.NO_OVERLAY, null);
                        collector.order(2).submitModelPart(this.rightWingPart, pose,
                                RenderTypes.entityCutout(CorpseOrigin.id("textures/entity/chapter_blood.png")),
                                state.lightCoords, OverlayTexture.NO_OVERLAY, null);
                    }
                    if (hasGills) {
                        collector.order(2).submitModelPart(this.gillsPart, pose,
                                RenderTypes.entityCutout(CorpseOrigin.id("textures/entity/chapter_xuanwu.png")),
                                state.lightCoords, OverlayTexture.NO_OVERLAY, null);
                    }
                    pose.popPose();
                }
            }
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
