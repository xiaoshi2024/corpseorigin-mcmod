package xiaoshi2022.corpseorigin.registry;

import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import xiaoshi2022.corpseorigin.CorpseOrigin;

/**
 * 模组自定义粒子类型注册。
 */
public final class ModParticles {

    private static Identifier id(String name) {
        return Identifier.fromNamespaceAndPath(CorpseOrigin.MOD_ID, name);
    }

    /**
     * 黑色羽毛粒子：乌鸦尸兄"随机掉羽毛"的视觉表现——飘落、摇摆、旋转的小黑羽。
     * 贴图取自原版鸡毛逐像素换色（textures/particle/black_feather.png）。
     */
    public static final SimpleParticleType BLACK_FEATHER =
            Registry.register(BuiltInRegistries.PARTICLE_TYPE, id("black_feather"),
                    FabricParticleTypes.simple());

    private ModParticles() {
    }

    /** 静态字段初始化即完成注册；保留 init() 与其他注册类的调用习惯一致 */
    public static void init() {
    }
}
