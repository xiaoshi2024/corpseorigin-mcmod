package xiaoshi2022.corpseorigin.client.limb;

import com.geckolib.animatable.GeoAnimatable;

/**
 * 断肢模型的动画宿主标记接口。
 * <p>
 * 存在的唯一原因是"编译期可见性"：真正的实现是 {@code ClientPlayerGeoAnimatableMixin} 在运行时
 * 把 {@link GeoAnimatable} 挂到 {@code AbstractClientPlayer} 上的，编译器看不到这一步，
 * 所以泛型里写 {@code AbstractClientPlayer} 会报"不在 T 的范围内"。
 * 声明这个空接口、让 renderer / model 的泛型参数写它，编译期就通了，
 * 运行时靠 mixin 保证实体确实是它的实现。
 */
public interface PlayerGeoAnimatable extends GeoAnimatable {
}
