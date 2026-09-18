package xiaoshi2022.corpseorigin.client.renderer.player;

import com.geckolib.renderer.GeoReplacedEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.RenderPassInfo;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.Identifier;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.client.limb.ClientLimbCache;
import xiaoshi2022.corpseorigin.client.limb.LimbRenderData;
import xiaoshi2022.corpseorigin.client.limb.PlayerGeoAnimatable;
import xiaoshi2022.corpseorigin.client.model.entity.CorpsePlayerGeoModel;
import xiaoshi2022.corpseorigin.limb.LimbSlots;

/**
 * 断肢形态玩家的 GeoLib 绘制器。
 * <p>
 * 它本身是个无状态绘制器，不通过 EntityRendererRegistry 替换注册 ——
 * 而是在 AvatarRenderer 初始化时建好存成静态单例，由 mixin 在 submit 时调用。
 * <p>
 * 骨骼的隐藏 / 缩放都在 {@link #adjustModelBonesForRender} 里做，
 * 该方法是 GeckoLib 注册的"最后一个骨骼更新器"，所以能覆盖动画写入的值。
 * <p>
 * ⚠️ 关于两个类型参数都是 {@code AbstractClientPlayer}：GeckoLib 的 Molang 查询会把
 * {@code animatable} 强转成 {@code LivingEntity}，而 {@code GeoReplacedEntityRenderer} 又禁止
 * {@code animatable} 是 {@code Entity} —— 所以构造时先塞 {@code null} 绕开那个检查，
 * 再由 {@link #fillRenderState} 把宿主换成真正的玩家实体。动画宿主本身由
 * {@code ClientPlayerGeoAnimatableMixin} 挂在 {@link AbstractClientPlayer} 上。
 */
@Environment(EnvType.CLIENT)
public class CorpsePlayerGeoRenderer
        extends GeoReplacedEntityRenderer<PlayerGeoAnimatable, AbstractClientPlayer, AvatarRenderState> {

    private static CorpsePlayerGeoRenderer instance;

    public CorpsePlayerGeoRenderer(EntityRendererProvider.Context context) {
        // 第三个参数传 null：见类注释，真正的宿主在 fillRenderState 里换
        super(context, new CorpsePlayerGeoModel(), null);
        this.shadowRadius = 0.5F;
    }

    /** 已初始化好的单例；AvatarRenderer 还没建过时为 null */
    public static CorpsePlayerGeoRenderer get() {
        return instance;
    }

    /** 在 AvatarRenderer 的构造里调用，只建一次 */
    public static CorpsePlayerGeoRenderer createIfAbsent(EntityRendererProvider.Context context) {
        if (instance == null) {
            instance = new CorpsePlayerGeoRenderer(context);
            CorpseOrigin.LOGGER.info("✅ 断肢玩家渲染器已创建");
        }
        return instance;
    }

    /**
     * 把动画宿主从 renderer 的字段（null）换成真正的玩家实体。
     * <p>
     * GeckoLib 会用它去建 {@code AnimatableManager}、以及给 Molang 查询提供 {@code LivingEntity}，
     * 传 null 的话开局就是 {@code ClassCastException} / {@code NPE}。
     */
    @Override
    public AvatarRenderState fillRenderState(PlayerGeoAnimatable animatable, AbstractClientPlayer entity,
                                            AvatarRenderState renderState, float partialTick) {
        return super.fillRenderState((PlayerGeoAnimatable) entity, entity, renderState, partialTick);
    }

    /** 同理，createRenderState 也别用那个 null 宿主 */
    @Override
    public AvatarRenderState createRenderState(PlayerGeoAnimatable animatable, AbstractClientPlayer entity) {
        return super.createRenderState((PlayerGeoAnimatable) entity, entity);
    }

    /**
     * 只决定"哪些骨骼该出现在画面上"，形状交给动画。
     * <p>
     * 肉芽长多大、残桩什么时候收掉、藤蔓血管怎么爬，全部在
     * {@code corpse_player.animation.json} 的 {@code regrow_*} 动画里描述。
     * 这里只处理三类状态：
     * <ul>
     *   <li>完好 → 显示骨 + 残桩 + 血管都由模型默认状态显示，无需干预；</li>
     *   <li>断了但<b>不会自愈</b>（{@link ClientLimbCache#PERMANENT}）→ 动画不播，所以兜底把那肢的
     *       显示骨整体藏掉、只留残桩；</li>
     *   <li>再生中 → 显示骨不藏（缩放由动画的 scale 关键帧负责），血管放开让它爬。</li>
     * </ul>
     */
    @Override
    public void adjustModelBonesForRender(RenderPassInfo<AvatarRenderState> renderPassInfo,
                                          BoneSnapshots snapshots) {
        AvatarRenderState state = renderPassInfo.renderState();
        Integer mask = state.getGeckolibData(LimbRenderData.LIMB_MASK);
        if (mask == null) {
            return;   // 四肢完好 → 走原版玩家渲染，Geo 模型根本不参与
        }

        for (int slot = 0; slot < LimbSlots.COUNT; slot++) {
            boolean severed = (mask & (1 << slot)) != 0;
            float progress = state.getOrDefaultGeckolibData(
                    LimbRenderData.REGROW_BY_SLOT.get(slot), ClientLimbCache.INTACT);
            // 藤蔓血管只在"正在长"的时候爬出来
            boolean growing = severed && progress >= 0.0F;

            // ★ 尸体模型<b>只负责断掉的那几肢</b>，而且只出"残桩 + 血管"：
            //   - 显示骨（right_arm2 这类，画整条肢体的方块）整根不画 —— 断肢形态下那条肢体由原版模型
            //     负责隐藏（见 PlayerModelLimbMixin），长好之后又由原版模型重新出画；
            //   - 残桩只在断着的时候画，血管只在正在长的时候爬；
            //   - 躯干/头/完好的四肢也全部不画（见方法末尾），一律交回原版模型 ——
            //     这样盔甲、皮肤、别的模组挂在玩家身上的层全都天然对齐，不需要任何补偿。
            for (String bone : LimbSlots.LIMB_BONES[slot]) {
                snapshots.ifPresent(bone, snapshot -> snapshot.skipRender(true));
            }
            snapshots.ifPresent(LimbSlots.STUMP_BONES[slot],
                    snapshot -> snapshot.skipRender(!severed));
            // 血管是"根骨 → vein_1 → vein_2 → vein_3"的链，藏根骨的子骨就能整条藏掉；
            // 一只肢体可能挂了好几组，逐个处理
            for (String bone : LimbSlots.VEIN_BONES[slot]) {
                snapshots.ifPresent(bone, snapshot -> snapshot.skipChildrenRender(!growing));
            }
        }

        // 躯干交给原版模型（它的方块在 geo 里叫 body）
        snapshots.ifPresent("body", snapshot -> {
            snapshot.skipRender(true);
            snapshot.skipChildrenRender(true);
        });

        corpseorigin$followVanillaPose(state, snapshots, mask);
    }

    // ==================== 残桩跟随原版骨架 ====================

    /** 部位下标 → geo 里的"挂点"骨名（顺序同 {@link LimbSlots}）：残桩、血管都挂在这些骨下面是 */
    private static final String[] VANILLA_POSE_BONES =
            {"right_arm", "left_arm", "right_leg", "left_leg", "head"};

    /** 只借它算姿势的原版玩家模型（自己 bake 一份，不动渲染器里那个） */
    private PlayerModel corpseorigin$vanillaPose;

    /**
     * 把<b>断掉那一肢</b>的挂点骨姿势对齐到原版骨架。
     * <p>
     * 残桩和血管都是挂点骨（{@code right_arm} / {@code right_leg} …）的子骨，所以把挂点骨按原版
     * 肩/胯的姿势摆好，残桩就会跟着原版那条肢体的摆动一起动 —— 和旁边照常渲染的躯干、盔甲对得上。
     * 完好的部位不用管：它们在 geo 里根本不画（由原版模型出画）。
     */
    private void corpseorigin$followVanillaPose(AvatarRenderState state, BoneSnapshots snapshots, int mask) {
        PlayerModel base = corpseorigin$vanillaPose();
        if (base == null) {
            return;
        }
        base.setupAnim(state);

        for (int slot = 0; slot < LimbSlots.COUNT; slot++) {
            if ((mask & (1 << slot)) == 0) {
                continue;
            }
            corpseorigin$copyPose(snapshots, VANILLA_POSE_BONES[slot], corpseorigin$partOf(base, slot));
        }
    }

    private PlayerModel corpseorigin$vanillaPose() {
        if (this.corpseorigin$vanillaPose == null) {
            var modelSet = Minecraft.getInstance().getEntityModels();
            if (modelSet == null) {
                return null;   // 还没进世界
            }
            this.corpseorigin$vanillaPose =
                    new PlayerModel(modelSet.bakeLayer(ModelLayers.PLAYER), false);
        }
        return this.corpseorigin$vanillaPose;
    }

    private static ModelPart corpseorigin$partOf(PlayerModel base, int slot) {
        return switch (slot) {
            case LimbSlots.RIGHT_ARM -> base.rightArm;
            case LimbSlots.LEFT_ARM -> base.leftArm;
            case LimbSlots.RIGHT_LEG -> base.rightLeg;
            case LimbSlots.LEFT_LEG -> base.leftLeg;
            default -> base.head;
        };
    }

    /**
     * 把一个原版部位的姿势写进同名骨骼。
     * <p>
     * 符号约定照抄 GeckoLib 给盔甲骨骼做的那套转换（它对 {@code xRot}、{@code yRot} 取负、{@code zRot} 不变）：
     * 原版模型的 y 轴朝下、Geo 模型的 y 轴朝上，所以 X/Y 旋转与 Y 平移要取负。
     * <p>
     * 平移只写<b>相对静止姿势的偏移</b>：骨骼自己的枢轴位置已经写在模型里了，
     * 直接写原版的绝对坐标会把肢体整体挪走。
     */
    private static void corpseorigin$copyPose(BoneSnapshots snapshots, String bone, ModelPart part) {
        PartPose rest = part.getInitialPose();
        snapshots.ifPresent(bone, snapshot -> snapshot
                .setRotation(-part.xRot, -part.yRot, part.zRot)
                .setTranslation(part.x - rest.x(), -(part.y - rest.y()), part.z - rest.z()));
    }

    @Override
    public Identifier getTextureLocation(AvatarRenderState renderState) {
        // 模型那边也会要纹理，这里保持一致返回玩家本人皮肤
        if (renderState.skin != null) {
            return renderState.skin.body().texturePath();
        }
        return DefaultPlayerSkin.getDefaultTexture();
    }

    @Override
    public RenderType getRenderType(AvatarRenderState renderState, Identifier texture) {
        return RenderTypes.entityCutout(texture);
    }
}
