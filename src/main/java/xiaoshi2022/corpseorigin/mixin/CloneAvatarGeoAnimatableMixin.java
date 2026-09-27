package xiaoshi2022.corpseorigin.mixin;

import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.instance.InstancedAnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.state.AnimationTest;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import xiaoshi2022.corpseorigin.client.limb.PlayerGeoAnimatable;
import xiaoshi2022.corpseorigin.client.renderer.player.CloneGourdRenderer;
import xiaoshi2022.corpseorigin.client.renderer.player.CreaturePlayerRenderer;
import xiaoshi2022.corpseorigin.client.renderer.player.NiunaiXRenderData;
import xiaoshi2022.corpseorigin.entity.CloneAvatarEntity;

/**
 * 让克隆分身成为「角色专属 GEO 外观」的动画宿主。
 * <p>
 * 玩家侧那套挂在 {@code AbstractClientPlayer} 上（见 {@code ClientPlayerGeoAnimatableMixin}），
 * 分身不是玩家、类型对不上，所以单独在 {@link CloneAvatarEntity} 上挂一份精简版：
 * 只保留分身会渲染的两条控制器 —— <b>角色全身模型</b>（小金刚尸兄等）与
 * <b>开胃奶背挂</b>（{@code niunaix}）。
 * <p>
 * 两条控制器都靠 {@code null} 判定"这一帧不是我的形态"（渲染另一套模型、或压根不渲染时，
 * 那份 render state 上不会有自己的 ticket）→ 原样 {@code CONTINUE}，绝不碰内部状态；
 * 明确读到"不是我的形态"（{@code false} / 空串）时才 {@code reset()}，
 * 免得重名的 idle / attack 残留在共享的动画宿主上被别的模型读到。
 * <p>
 * 动画缓存用独立的 {@link InstancedAnimatableInstanceCache}，与玩家的完全隔开 ——
 * 分身的动作不会影响任何一个玩家的姿势。
 */
@Mixin(CloneAvatarEntity.class)
public abstract class CloneAvatarGeoAnimatableMixin implements PlayerGeoAnimatable {

    @Unique
    private AnimatableInstanceCache corpseorigin$cloneCache;

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // 角色全身模型：MODEL 有值就播对应 clip（idle / walk / attack / cast …）
        controllers.add(new AnimationController<PlayerGeoAnimatable>("clone_creature", 2, test -> {
            String model = test.getData(CreaturePlayerRenderer.MODEL);
            if (model == null) {
                return PlayState.STOP;      // 不是我的回合
            }
            if (model.isEmpty()) {
                test.controller().reset();      // 这一帧不做角色模型 → 整条停掉并清干净
                return PlayState.STOP;
            }
            String clip = test.getData(CreaturePlayerRenderer.CLIP);
            return test.setAndContinue(RawAnimation.begin().thenLoop(clip == null ? "idle" : clip));
        }));

        // 开胃奶背挂：idle（收拢贴在背后）/ attack（尖刺伸出）
        controllers.add(new AnimationController<PlayerGeoAnimatable>("clone_niunai", 0, test -> {
            Boolean active = test.getDataOrDefault(NiunaiXRenderData.ACTIVE, null);
            if (active == null) {
                return PlayState.STOP;      // 不是我的回合
            }
            if (!active) {
                test.controller().reset();
                return PlayState.STOP;
            }
            boolean attacking = Boolean.TRUE.equals(
                    test.getDataOrDefault(NiunaiXRenderData.ATTACKING, Boolean.FALSE));
            if (Boolean.TRUE.equals(test.getDataOrDefault(NiunaiXRenderData.PARRYING, Boolean.FALSE)))
                return test.setAndContinue(RawAnimation.begin().thenLoop("parry"));
            return test.setAndContinue(attacking
                    ? RawAnimation.begin().thenPlay("attack")
                    : RawAnimation.begin().thenLoop("idle"));
        }));

        // 葫芦（葫芦小金刚）：idle / eyez / hand / snake / power … 与玩家侧同一条规则
        controllers.add(new AnimationController<PlayerGeoAnimatable>("clone_gourd", 4, test -> {
            String clip = test.getData(CloneGourdRenderer.CLIP);
            if (clip == null) {
                return PlayState.STOP;      // 不是我的回合
            }
            if (clip.isEmpty()) {
                test.controller().reset();      // 这一帧不做葫芦 → 整条停掉并清干净
                return PlayState.STOP;
            }
            return test.setAndContinue(RawAnimation.begin().thenLoop(clip));
        }));

        // 自定义器官：每个器官用自己的 manager（见 CloneOrganRenderer#getInstanceId），
        // 这里只按各器官自己那份 render state 上的 CLIP 播对应动画（与玩家侧 custom_organs 同规则）
        controllers.add(new AnimationController<PlayerGeoAnimatable>("clone_organs", 4, test -> {
            String clip = test.getData(xiaoshi2022.corpseorigin.client.render.layer.CustomOrganLayer.CLIP);
            return clip == null ? PlayState.STOP
                    : test.setAndContinue(RawAnimation.begin().thenLoop(clip));
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        if (corpseorigin$cloneCache == null) {
            corpseorigin$cloneCache = new xiaoshi2022.corpseorigin.client.limb.PlayerLayerAnimationCache(this);
        }
        return corpseorigin$cloneCache;
    }
}
