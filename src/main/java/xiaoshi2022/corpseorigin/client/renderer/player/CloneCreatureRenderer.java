package xiaoshi2022.corpseorigin.client.renderer.player;

import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoReplacedEntityRenderer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.client.limb.PlayerGeoAnimatable;
import xiaoshi2022.corpseorigin.entity.CloneAvatarEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 克隆分身的「角色全身模型」渲染器 —— {@link CreaturePlayerRenderer} 的分身版。
 * <p>
 * 原版那套的实体类型参数是 {@code AbstractClientPlayer}（玩家身上由 mixin 提供
 * {@link PlayerGeoAnimatable}），分身不是玩家、类型对不上，所以这里另建一份：
 * 泛型主机改成 {@link CloneAvatarEntity}，模型 / 动画资源与玩家侧完全同名同源
 * （{@code geckolib/models/entity/<角色id>.geo.json}），所以小金刚尸兄、青蛙尸兄、虎姐……
 * 在分身上画出来的就是同一具模型。
 * <p>
 * 每个角色一个实例（{@code DefaultedEntityGeoModel} 的动画资源是按 id 推导的，
 * 没法在一个实例里按 state 切换整份动画文件 —— 这一点与玩家侧的做法一致）。
 * <p>
 * ⚠️ 两个类型参数分开的原因同 {@link NiunaiXRenderer}：GeckoLib 的 Molang 查询会把
 * animatable 强转成 {@code LivingEntity}，而 {@code GeoReplacedEntityRenderer} 的构造函数
 * 又禁止 animatable 是 {@code Entity} —— 构造时先塞 null 绕开，再由 {@link #fillRenderState}
 * 换成真正的分身实体。
 */
@Environment(EnvType.CLIENT)
public final class CloneCreatureRenderer
        extends GeoReplacedEntityRenderer<PlayerGeoAnimatable, CloneAvatarEntity, AvatarRenderState> {

    /** 与 {@link CreaturePlayerRenderer} 同一份模型名单 */
    public static final List<String> MODELS = List.of(
            "qingwa_zb", "hujie", "chongmu", "jingang_zb", "jingang_infant",
            "xiongxing_zb", "red_fire_ant", "bullet_ant", "xiaohui_scene");

    private static final Map<String, CloneCreatureRenderer> RENDERERS = new HashMap<>();

    private CloneCreatureRenderer(EntityRendererProvider.Context context, String id) {
        super(context, new DefaultedEntityGeoModel<>(CorpseOrigin.id(id)), null);
        this.shadowRadius = 0.7F;
    }

    /** 在克隆分身的渲染器构造里调用一次即可（每个角色模型建一个实例）。 */
    public static void create(EntityRendererProvider.Context context) {
        if (!RENDERERS.isEmpty()) {
            return;
        }
        for (String id : MODELS) {
            RENDERERS.put(id, new CloneCreatureRenderer(context, id));
        }
    }

    public static CloneCreatureRenderer get(String id) {
        return id == null ? null : RENDERERS.get(id);
    }

    public static boolean has(String id) {
        return id != null && RENDERERS.containsKey(id);
    }

    /** 动画宿主不能用构造器里那个 null，改用实体自身（同玩家侧的写法）。 */
    @Override
    public AvatarRenderState fillRenderState(PlayerGeoAnimatable animatable, CloneAvatarEntity entity,
                                            AvatarRenderState state, float partialTick) {
        return super.fillRenderState((PlayerGeoAnimatable) entity, entity, state, partialTick);
    }

    @Override
    public AvatarRenderState createRenderState(PlayerGeoAnimatable animatable, CloneAvatarEntity entity) {
        return super.createRenderState((PlayerGeoAnimatable) entity, entity);
    }
}
