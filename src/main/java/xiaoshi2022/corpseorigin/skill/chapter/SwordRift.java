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
        // 创造模式豁免等级门槛；天级（9）即可开剑气（仅约束生存/冒险玩家）
        if(!c.swordTerrainDestruction || (!p.isCreative() && tier<9) || !p.mayBuild() || p.isSpectator())return false;
        // 同一玩家可同时维持多道剑气（劈山连开），只受全局并发上限与个人冷却约束
        if(CUTS.size()>=c.swordRiftConcurrent)return false;
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
                    // -2 = 压制轮休眠（等下个节流窗口），不是完成
                    if(result==-1){cut.done=true;break;}
                    if(result>0){broken++;removed++;}
                }
                if(!cut.done)CUTS.addLast(cut);
            }
        });
    }
    private static final class Cut {
        final ServerPlayer player;final ServerLevel level;final Vec3 origin,forward,side;
        final int tier,length,width,height,minY;final long started;
        /** tier>=12（神级顶端）：劈开的液体格在压制期（600 tick）内不会被回流的水重新填满 */
        final long sealUntil;
        /** pass 0 = 首轮开槽；pass>=1 = 液体压制轮（只清槽内回流的水，不动实体方块） */
        int along,y,across,total,pass;long nextRescan;boolean done;
        Cut(ServerPlayer p,Vec3 at,Vec3 dir,int tier){
            player=p;level=p.level();origin=at;forward=dir;side=new Vec3(-dir.z,0,dir.x);this.tier=tier;
            var c=RealmProgression.config();
            length=Math.min(c.swordRiftMaxLength,SwordQiRules.riftLength(tier));width=SwordQiRules.riftWidth(tier);
            minY=Math.max(level.getMinY(),(int)Math.floor(at.y)-SwordQiRules.riftHeight(tier)/3);
            height=tier>=12?level.getMaxY()-minY+1:Math.min(SwordQiRules.riftHeight(tier),level.getMaxY()-minY+1);
            across=-SwordQiRules.halfWidth(width,0,height);started=level.getGameTime();
            sealUntil=tier>=12?started+600:0;
        }
        boolean valid(){return !done && player.isAlive() && !player.isRemoved() && player.level()==level
                && player.mayBuild() && !player.isSpectator() && level.getGameTime()-started<12000;}
        int step(){
            if(along>=length || total>=RealmProgression.config().swordRiftMaxBlocks){
                // 全槽扫完：tier>=12 进入液体压制轮，节流重扫（每 40 tick 一遍）防止海水回填沟道
                long now=level.getGameTime();
                if(tier>=12 && now<sealUntil){
                    if(now<nextRescan)return -2;
                    along=0;y=0;across=-SwordQiRules.halfWidth(width,0,height);pass++;nextRescan=now+40;
                } else return -1;
            }
            Vec3 center=origin.add(forward.scale(along));
            if(!level.hasChunkAt(BlockPos.containing(center)))return pass==0?-1:-2;
            int half=SwordQiRules.halfWidth(width,y,height);
            BlockPos pos=BlockPos.containing(center.add(side.scale(across)).x,minY+y,center.add(side.scale(across)).z);
            if(++across>half){across=-SwordQiRules.halfWidth(width,y+1,height);if(++y>=height){
                y=0;across=-SwordQiRules.halfWidth(width,0,height);along++;
                if(along%12==0)SwordImpact.send(level,center,forward,tier,2,-1,player.getId());
                if(along%24==0)GroundShockwave.spawn(player,center.add(side.scale(width*.6)),6,1.5);
            }}
            if(!level.hasChunkAt(pos) || !level.getWorldBorder().isWithinBounds(pos) || !level.mayInteract(player,pos))return 0;
            var state=level.getBlockState(pos);
            if(pass>0){
                // 压制轮：只清回流进槽道的液体，实体方块（玩家后放的）一概不动
                if(state.isAir() || state.getFluidState().isEmpty())return 0;
                return level.setBlock(pos,Blocks.AIR.defaultBlockState(),2|16)?1:0;
            }
            boolean fluid=!state.getFluidState().isEmpty();
            if(state.isAir() || state.hasBlockEntity())return 0;
            if(fluid){
                // 神级顶端（12）：剑气把液体连同水面一起劈开，直接清成空气
                return level.setBlock(pos,Blocks.AIR.defaultBlockState(),2|16)?1:0;
            }
            if(state.getDestroySpeed(level,pos)<0)return 0;
            if(!PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(level,player,pos,state,null))return 0;
            // No item/particle entity per block; one visual front represents each section.
            if(!level.setBlock(pos,Blocks.AIR.defaultBlockState(),2|16))return 0;
            PlayerBlockBreakEvents.AFTER.invoker().afterBlockBreak(level,player,pos,state,null);
            total++;return 1;
        }
    }
}
