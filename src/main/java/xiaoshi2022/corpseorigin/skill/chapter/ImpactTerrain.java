package xiaoshi2022.corpseorigin.skill.chapter;

import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
import net.minecraft.tags.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import xiaoshi2022.corpseorigin.character.CharacterManager;
import java.util.*;

/** Bounded collision corridors; never loads chunks or deletes containers/unbreakable blocks. */
public final class ImpactTerrain {
    private record Flight(ServerPlayer owner, LivingEntity target, ServerLevel level, String role, Vec3 direction, int started) {}
    private static final Map<UUID,Flight> FLIGHTS=new HashMap<>();
    public static boolean breakBlock(ServerPlayer p,BlockPos pos,boolean woodOnly){
        var level=(ServerLevel)p.level();
        if(p.isSpectator() || !p.mayBuild() || !level.hasChunkAt(pos) || !level.getWorldBorder().isWithinBounds(pos) || !level.mayInteract(p,pos))return false;
        BlockState state=level.getBlockState(pos);
        if(state.isAir() || state.hasBlockEntity() || !state.getFluidState().isEmpty()
                || state.getDestroySpeed(level,pos)<0 || state.getDestroySpeed(level,pos)>6)return false;
        if(woodOnly && !state.is(BlockTags.LOGS) && !state.is(BlockTags.PLANKS) && !state.is(BlockTags.WOODEN_SLABS))return false;
        if(!PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(level,p,pos,state,null))return false;
        boolean broken=level.destroyBlock(pos,true,p);
        if(broken)PlayerBlockBreakEvents.AFTER.invoker().afterBlockBreak(level,p,pos,state,null);
        return broken;
    }
    public static void launch(ServerPlayer p,LivingEntity target,double speed){
        Vec3 direction=new Vec3(p.getLookAngle().x,0,p.getLookAngle().z).normalize();
        if(direction.lengthSqr()<.01)return;
        FLIGHTS.put(target.getUUID(),new Flight(p,target,(ServerLevel)p.level(),
                CharacterManager.getInstance().getPlayerCharacterId(p),direction,p.tickCount));
        target.setDeltaMovement(direction.scale(speed).add(0,.35,0));target.hurtMarked=true;
    }
    public static void register(){
        ServerLifecycleEvents.SERVER_STOPPED.register(s->FLIGHTS.clear());
        ServerTickEvents.START_SERVER_TICK.register(server->{
            FLIGHTS.values().removeIf(f->!f.owner.isAlive() || f.owner.isRemoved() || !f.target.isAlive()
                    || f.target.isRemoved() || f.owner.level()!=f.level || f.target.level()!=f.level
                    || f.owner.tickCount-f.started>=20 || !f.role.equals(CharacterManager.getInstance().getPlayerCharacterId(f.owner))
                    || !ChapterCombat.canHit(f.owner,f.target));
            for(var f:FLIGHTS.values()){
                Vec3 step=f.direction.scale(1.2);
                AABB corridor=f.target.getBoundingBox().expandTowards(step).inflate(.12);
                int budget=24;
                for(BlockPos pos:BlockPos.betweenClosed(BlockPos.containing(corridor.minX,corridor.minY,corridor.minZ),
                        BlockPos.containing(corridor.maxX,corridor.maxY,corridor.maxZ))){
                    if(budget--<=0)break;
                    breakBlock(f.owner,pos,false);
                }
                f.target.setDeltaMovement(step.add(0,Math.max(-.2,Math.min(.1,f.target.getDeltaMovement().y)),0));
                f.target.hurtMarked=true;
            }
        });
    }
}
