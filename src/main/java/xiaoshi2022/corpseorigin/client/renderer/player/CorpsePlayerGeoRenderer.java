package xiaoshi2022.corpseorigin.client.renderer.player;

import com.geckolib.renderer.GeoReplacedEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.RenderPassInfo;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
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
            boolean stumpOnly = severed && progress < 0.0F;
            // 藤蔓血管只在"正在长"的时候爬出来
            boolean growing = severed && progress >= 0.0F;

            // 承载方块的显示骨（right_arm2 这类，不是空挂点 right_arm）
            for (String bone : LimbSlots.LIMB_BONES[slot]) {
                snapshots.ifPresent(bone, snapshot -> {
                    snapshot.skipRender(stumpOnly);
                    // 没断的肢体显式复位缩放。再生动画用的是 hold_on_last_frame，
                    // 它会把骨骼的 scale 定格在最后一帧（例如 "scale": 0 的隐藏状态）；
                    // 万一那帧状态被带到了下一段动画，完好/别的部位就会莫名其妙少一块。
                    // 这里每帧写成 1 是幂等的，只是把"该正常显示"这件事说明确。
                    if (!severed) {
                        snapshot.setScale(1.0F, 1.0F, 1.0F);
                    }
                });
            }
            snapshots.ifPresent(LimbSlots.STUMP_BONES[slot],
                    snapshot -> snapshot.skipRender(!severed));
            // 血管是"根骨 → vein_1 → vein_2 → vein_3"的链，藏根骨的子骨就能整条藏掉；
            // 一只肢体可能挂了好几组，逐个处理
            for (String bone : LimbSlots.VEIN_BONES[slot]) {
                snapshots.ifPresent(bone, snapshot -> snapshot.skipChildrenRender(!growing));
            }
        }
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
