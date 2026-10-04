package xiaoshi2022.corpseorigin.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.util.RandomSource;

/**
 * 黑色羽毛粒子——乌鸦尸兄"随机掉羽毛"的视觉表现。
 * <p>
 * 模仿羽毛飘落：慢速下坠 + 左右摇摆 + 自旋，贴图取自原版鸡毛逐像素换色
 * （textures/particle/black_feather.png，经 particles/black_feather.json 进粒子图集）。
 */
public class BlackFeatherParticle extends SingleQuadParticle {

    /** 自旋角速度（正负随机，羽毛打转） */
    private final float spinSpeed;
    /** 摆动相位（不同羽毛错开） */
    private final float swayPhase;

    public BlackFeatherParticle(ClientLevel level, double x, double y, double z,
                                double vx, double vy, double vz, SpriteSet sprites, RandomSource random) {
        super(level, x, y, z, vx, vy, vz, sprites.get(random));
        this.friction = 0.96F;
        this.gravity = 0.012F;      // 比普通粒子轻得多，飘着落
        this.lifetime = 40 + this.random.nextInt(40);
        this.hasPhysics = true;     // 落地停住
        this.roll = this.random.nextFloat() * (float) (Math.PI * 2.0D);
        this.oRoll = this.roll;
        this.spinSpeed = (this.random.nextFloat() - 0.5F) * 0.25F;
        this.swayPhase = this.random.nextFloat() * (float) (Math.PI * 2.0D);
        // 初速稍微向上抛一下，更像被抖落
        this.yd = vy + 0.06D + this.random.nextDouble() * 0.04D;
        this.xd = vx * 0.3D;
        this.zd = vz * 0.3D;
    }

    @Override
    public void tick() {
        super.tick();
        // 自旋（先存上一帧角度供插值）+ 左右摇摆（正弦水平推力）
        this.oRoll = this.roll;
        this.roll += this.spinSpeed;
        double sway = Math.sin((this.age + this.swayPhase) * 0.35D) * 0.0035D;
        this.xd += sway;
        this.zd += Math.cos((this.age + this.swayPhase) * 0.28D) * 0.0035D;
    }

    @Override
    public Layer getLayer() {
        return Layer.OPAQUE;
    }
}
