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

    /**
     * 空间异能是白小飞在《尸巢之战篇》后期才觉醒的异能，门槛抬到「天级」：
     * {@link #getRequiredLevel()} = <b>9</b> —— 绝对进化等级 9，也就是 {@code EvolutionTier.TIAN}。
     * <p>
     * 这一条同时管住技能树与自由角色（凡人 / 尸兄）的「发现」：
     * {@code BalanceRules.discoveryLevel} 取 requiredLevel 与 9 的较大值，
     * 所以天级之前连随机机遇都刷不出这条技能。
     */
    @Override
    public int getRequiredLevel() {
        return 9;
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
        return destination(p)==null?net.minecraft.network.chat.Component.translatable("message.corpseorigin.spatial_blink_skill.text_01"):null;
    }
    @Override public void onActivate(net.minecraft.server.level.ServerPlayer p) {
        var end=destination(p);if(end==null)return;
        p.teleportTo(end.x,end.y,end.z);
        xiaoshi2022.corpseorigin.skill.chapter.QiEffects.burst((net.minecraft.server.level.ServerLevel)p.level(),end.x,end.y+1,end.z,0x8a3fd6,40,.8);
    }
}
