package xiaoshi2022.corpseorigin.skill.chapter;

import net.fabricmc.fabric.api.attachment.v1.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.tags.ItemTags;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.character.CharacterManager;
import xiaoshi2022.corpseorigin.component.PlayerCorpseComponent;
import xiaoshi2022.corpseorigin.limb.LimbSlots;

public final class BodySkillState {
    public static final AttachmentType<Long> FOREARM_UNTIL=AttachmentRegistry.create(CorpseOrigin.id("forearm_until"),
            b->b.initializer(()->0L).syncWith(ByteBufCodecs.VAR_LONG,AttachmentSyncPredicate.all()));
    public static final AttachmentType<ItemStack> SALMON_SWORD=AttachmentRegistry.create(CorpseOrigin.id("salmon_sword"),
            b->b.initializer(()->ItemStack.EMPTY).persistent(ItemStack.OPTIONAL_CODEC).copyOnDeath()
                    .syncWith(ItemStack.OPTIONAL_STREAM_CODEC,AttachmentSyncPredicate.all()));
    public static boolean missingForearm(Entity p){return p.getAttachedOrCreate(FOREARM_UNTIL)>p.level().getGameTime();}
    public static boolean hasArm(ServerPlayer p){
        return !missingForearm(p) && !PlayerCorpseComponent.get(p).readLimbs().isSevered(LimbSlots.RIGHT_ARM);
    }
    public static void register(){
        UseEntityCallback.EVENT.register((player,level,hand,entity,hit)->{
            if(!(entity instanceof net.minecraft.world.entity.player.Player fish)
                    || !"bianyi_guiyu".equals(fish.getAttachedOrCreate(ChapterActorState.ROLE)))return InteractionResult.PASS;
            ItemStack stack=player.getItemInHand(hand);
            if(!stack.is(ItemTags.SWORDS) && !(stack.isEmpty() && player.isShiftKeyDown()))return InteractionResult.PASS;
            if(!(player instanceof ServerPlayer p))return InteractionResult.SUCCESS;
            if(!p.mayBuild() || p.isSpectator())return InteractionResult.FAIL;
            var sword=fish.getAttachedOrCreate(SALMON_SWORD);
            if(stack.is(ItemTags.SWORDS) && sword.isEmpty()){
                fish.setAttached(SALMON_SWORD,stack.copyWithCount(1));if(!p.isCreative())stack.shrink(1);
            }else if(stack.isEmpty() && p.isShiftKeyDown() && !sword.isEmpty()){
                if(!p.addItem(sword.copy()))p.drop(sword.copy(),false);fish.setAttached(SALMON_SWORD,ItemStack.EMPTY);
            }else return InteractionResult.FAIL;
            return InteractionResult.SUCCESS;
        });
        ServerTickEvents.END_SERVER_TICK.register(server->{
            for(var p:server.getPlayerList().getPlayers()){
                var role=CharacterManager.getInstance().getPlayerCharacterId(p);
                if(!p.isAlive() || !"heixiaofei".equals(role))p.setAttached(FOREARM_UNTIL,0L);
                if(!"bianyi_guiyu".equals(role)){
                    var sword=p.getAttachedOrCreate(SALMON_SWORD);
                    if(!sword.isEmpty()){if(!p.addItem(sword.copy()))p.drop(sword.copy(),false);p.setAttached(SALMON_SWORD,ItemStack.EMPTY);}
                    continue;
                }
                if(!p.isAlive() || p.getAttachedOrCreate(SALMON_SWORD).isEmpty() || !p.isInWater()
                        || p.getDeltaMovement().horizontalDistanceSqr()<.006 || p.tickCount%3!=0)continue;
                var base=p.blockPosition();
                for(var pos:net.minecraft.core.BlockPos.betweenClosed(base.offset(-1,0,-1),base.offset(1,2,1))){
                    if(p.level().getFluidState(pos.below()).is(net.minecraft.tags.FluidTags.WATER))
                        ImpactTerrain.breakBlock(p,pos,true);
                }
            }
        });
    }
}
