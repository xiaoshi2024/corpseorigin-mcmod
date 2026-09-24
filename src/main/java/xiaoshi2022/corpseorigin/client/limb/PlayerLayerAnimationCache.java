package xiaoshi2022.corpseorigin.client.limb;

import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.animatable.instance.InstancedAnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;

/** Keep the existing body manager intact; reserve one separate manager for evolution overlays. */
public final class PlayerLayerAnimationCache extends InstancedAnimatableInstanceCache {
    public static final long EVOLUTION = Long.MIN_VALUE;
    private final InstancedAnimatableInstanceCache evolution;
    private final java.util.Map<Long,InstancedAnimatableInstanceCache> organs = new java.util.HashMap<>();
    private final java.util.Map<Long,Object> organModels = new java.util.HashMap<>();
    public void prepareOrgan(long id, Object model) {
        if (organModels.put(id, model) != model) organs.remove(id);
    }
    public PlayerLayerAnimationCache(GeoAnimatable player) {
        super(player);
        evolution = new InstancedAnimatableInstanceCache(player);
    }
    @Override public AnimatableManager<?> getManagerForId(long id) {
        if(id>EVOLUTION && id<=EVOLUTION+16)
            return organs.computeIfAbsent(id,k->new InstancedAnimatableInstanceCache(animatable)).getManagerForId(id);
        return id == EVOLUTION ? evolution.getManagerForId(id) : super.getManagerForId(id);
    }
}
