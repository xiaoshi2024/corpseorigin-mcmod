package xiaoshi2022.corpseorigin.skill.chapter;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import xiaoshi2022.corpseorigin.growth.RealmProgression;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** A moving excavation front, globally budgeted. Does not generate/load distant chunks. */
public final class SwordRift {
    private SwordRift() {}
    private static final ArrayDeque<Cut> CUTS=new ArrayDeque<>();
    private static final Map<UUID,Long> NEXT=new HashMap<>();
    private static long removed;
    public static long removedBlocks(){return removed;}
    public static int activeCuts(){return CUTS.size();}
    public static boolean start(ServerPlayer p,Vec3 at,Vec3 direction,int tier) {
        var c=RealmProgression.config();
        if(!c.swordTerrainDestruction || tier<10 || !p.mayBuild() || p.isSpectator())return false;
        if(CUTS.size()>=c.swordRiftConcurrent || CUTS.stream().anyMatch(j->j.player==p))return false;
        long now=p.level().getGameTime();
        if(NEXT.getOrDefault(p.getUUID(),0L)>now)return false;
        Vec3 horizontal=new Vec3(direction.x,0,direction.z);
        if(horizontal.lengthSqr()<.01)horizontal=new Vec3(p.getLookAngle().x,0,p.getLookAngle().z);
        if(horizontal.lengthSqr()<.01)horizontal=new Vec3(0,0,1);
        CUTS.add(new Cut(p,at,horizontal.normalize(),tier));
        NEXT.put(p.getUUID(),now+c.swordRiftCooldownTicks);
        SwordImpact.send(p.level(),at,horizontal,tier,1,-1,p.getId());
        return true;
    }
    public static void register(){
        ServerLifecycleEvents.SERVER_STOPPED.register(s->{CUTS.clear();NEXT.clear();removed=0;});
        ServerTickEvents.END_SERVER_TICK.register(s->{
            var c=RealmProgression.config();
            if(!c.swordTerrainDestruction){CUTS.clear();return;}
            long deadline=System.nanoTime()+c.swordRiftMillisPerTick*1000000L;
            int scans=0,broken=0;
            while(!CUTS.isEmpty() && scans<c.swordRiftScansPerTick && broken<c.swordRiftBlocksPerTick && System.nanoTime()<deadline){
                Cut cut=CUTS.removeFirst();
                if(!cut.valid())continue;
                for(int n=0;n<64 && scans<c.swordRiftScansPerTick && broken<c.swordRiftBlocksPerTick;n++){
                    if(System.nanoTime()>=deadline)break;
                    int result=cut.step();scans++;
                    if(result<0){cut.done=true;break;}
                    if(result>0){broken++;removed++;}
                }
                if(!cut.done)CUTS.addLast(cut);
            }
        });
    }
    private static final class Cut {
        final ServerPlayer player;final ServerLevel level;final Vec3 origin,forward,side;
        final int tier,length,width,height,minY;final long started;
        int along,y,across,total;boolean done;
        Cut(ServerPlayer p,Vec3 at,Vec3 dir,int tier){
            player=p;level=p.level();origin=at;forward=dir;side=new Vec3(-dir.z,0,dir.x);this.tier=tier;
            var c=RealmProgression.config();
            length=Math.min(c.swordRiftMaxLength,SwordQiRules.riftLength(tier));width=SwordQiRules.riftWidth(tier);
            minY=Math.max(level.getMinY(),(int)Math.floor(at.y)-SwordQiRules.riftHeight(tier)/3);
            height=tier>=12?level.getMaxY()-minY+1:Math.min(SwordQiRules.riftHeight(tier),level.getMaxY()-minY+1);
            across=-SwordQiRules.halfWidth(width,0,height);started=level.getGameTime();
        }
        boolean valid(){return !done && player.isAlive() && !player.isRemoved() && player.level()==level
                && player.mayBuild() && !player.isSpectator() && level.getGameTime()-started<12000;}
        int step(){
            if(along>=length || total>=RealmProgression.config().swordRiftMaxBlocks)return -1;
            Vec3 center=origin.add(forward.scale(along));
            if(!level.hasChunkAt(BlockPos.containing(center)))return -1;
            int half=SwordQiRules.halfWidth(width,y,height);
            BlockPos pos=BlockPos.containing(center.add(side.scale(across)).x,minY+y,center.add(side.scale(across)).z);
            if(++across>half){across=-SwordQiRules.halfWidth(width,y+1,height);if(++y>=height){
                y=0;across=-SwordQiRules.halfWidth(width,0,height);along++;
                if(along%12==0)SwordImpact.send(level,center,forward,tier,2,-1,player.getId());
                if(along%24==0)GroundShockwave.spawn(player,center.add(side.scale(width*.6)),6,1.5);
            }}
            if(!level.hasChunkAt(pos) || !level.getWorldBorder().isWithinBounds(pos) || !level.mayInteract(player,pos))return 0;
            var state=level.getBlockState(pos);
            if(state.isAir() || state.hasBlockEntity() || !state.getFluidState().isEmpty() || state.getDestroySpeed(level,pos)<0)return 0;
            if(!PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(level,player,pos,state,null))return 0;
            // No item/particle entity per block; one visual front represents each section.
            if(!level.setBlock(pos,Blocks.AIR.defaultBlockState(),2|16))return 0;
            PlayerBlockBreakEvents.AFTER.invoker().afterBlockBreak(level,player,pos,state,null);
            total++;return 1;
        }
    }
}
