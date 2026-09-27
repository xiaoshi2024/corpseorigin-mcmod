package xiaoshi2022.corpseorigin.client.compat;

import com.geckolib.constant.dataticket.DataTicket;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;

/** Optional PAL bridge, also used by Better Combat's first-person attack pass. */
public final class FirstPersonArmorCompat {
    public static final int RIGHT_ARM = 1;
    public static final int LEFT_ARM = 2;
    public static final DataTicket<Integer> ARM_MASK = DataTicket.create(
            "corpseorigin_first_person_armor_arms", Integer.class);
    private static final boolean PAL_LOADED = FabricLoader.getInstance().isModLoaded("player_animation_library");

    private FirstPersonArmorCompat() {}

    /** -1 means a normal pass; 0 means the animation explicitly hides armor. */
    public static int prepare(HumanoidRenderState source, HumanoidRenderState target) {
        int mask = PAL_LOADED ? PalBridge.prepare(source, target) : -1;
        target.addGeckolibData(ARM_MASK, mask);
        return mask;
    }

    // Keep optional API linkage in a class that is only loaded when PAL exists.
    private static final class PalBridge {
        private static int prepare(HumanoidRenderState source, HumanoidRenderState target) {
            if (!(source instanceof com.zigythebird.playeranim.accessors.IAvatarAnimationState from)
                    || !(target instanceof com.zigythebird.playeranim.accessors.IAvatarAnimationState to)) return -1;
            boolean firstPerson = from.playerAnimLib$isFirstPersonPass();
            var manager = from.playerAnimLib$getAnimManager();
            // PAL marks the root state AFTER GeckoLib extracts its per-slot states.
            // Copy at submission time so setupAnim uses the same animation/pass.
            to.playerAnimLib$setFirstPersonPass(firstPerson);
            to.playerAnimLib$setAnimManager(manager);
            if (!firstPerson) return -1;
            if (manager == null) return 0;
            var config = manager.getFirstPersonConfiguration();
            if (!config.isShowArmor()) return 0;
            return (config.isShowRightArm() ? RIGHT_ARM : 0)
                    | (config.isShowLeftArm() ? LEFT_ARM : 0);
        }
    }
}
