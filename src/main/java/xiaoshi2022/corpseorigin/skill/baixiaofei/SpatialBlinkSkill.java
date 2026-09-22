package xiaoshi2022.corpseorigin.skill.baixiaofei;

import xiaoshi2022.corpseorigin.skill.AbstractSkill;
import xiaoshi2022.corpseorigin.skill.SkillType;

/**
 * 白小飞·空间异能（短距瞬移）
 * <p>
 * 设定效果：闪现 8–16 格，落点产生空间涟漪。
 * 冷却：15 秒。特效：紫/黑色粒子，瞬移音效。
 * <p>
 * TODO 实装：视线落点检测 → 距离上限 8–16 格 → 瞬移 + 落点空间涟漪粒子与音效。
 */
public class SpatialBlinkSkill extends AbstractSkill {

    public static final String PATH = "spatial_blink";

    public SpatialBlinkSkill() {
        super(PATH, SkillType.UTILITY, 300);   // 15s
    }
    private net.minecraft.world.phys.Vec3 destination(net.minecraft.server.level.ServerPlayer p) {
        var level=p.level();var start=p.position();
        var hit=level.clip(new net.minecraft.world.level.ClipContext(p.getEyePosition(),p.getEyePosition().add(p.getLookAngle().scale(16)),
                net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,p));
        double distance=Math.min(16,p.getEyePosition().distanceTo(hit.getLocation())-.6);
        for(double d=distance;d>=1;d-=.5) {
            var end=start.add(p.getLookAngle().scale(d));var pos=net.minecraft.core.BlockPos.containing(end);
            if(!level.hasChunkAt(pos)||!level.getWorldBorder().isWithinBounds(pos)
                    ||end.y<level.getMinY() ||end.y+p.getBbHeight()>level.getMaxY()
                    ||!level.noCollision(p,p.getBoundingBox().move(end.subtract(start))))continue;
            return end;
        }
        return null;
    }
    @Override public net.minecraft.network.chat.Component checkUsable(net.minecraft.server.level.ServerPlayer p) {
        return destination(p)==null?net.minecraft.network.chat.Component.literal("前方没有安全的空间落点"):null;
    }
    @Override public void onActivate(net.minecraft.server.level.ServerPlayer p) {
        var end=destination(p);if(end==null)return;
        p.teleportTo(end.x,end.y,end.z);
        ((net.minecraft.server.level.ServerLevel)p.level()).sendParticles(net.minecraft.core.particles.ParticleTypes.PORTAL,end.x,end.y+1,end.z,40,.5,.8,.5,.15);
    }
}
