package xiaoshi2022.corpseorigin.item;

import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

import java.util.function.Consumer;

/** Port of the 1.21.1 redstone-powered dryer; all gameplay resolves on the server. */
public final class HairDryerItem extends Item implements GeoItem {
    private static final net.minecraft.world.level.ExplosionDamageCalculator SHORT_CIRCUIT_DAMAGE = new net.minecraft.world.level.ExplosionDamageCalculator(){
        @Override public float getEntityDamageAmount(net.minecraft.world.level.Explosion explosion,Entity target,float exposure){
            float normal=super.getEntityDamageAmount(explosion,target,exposure);
            // A close, unobstructed electrical blast kills a normal 25 HP corpse,
            // while cover, armor and stronger evolved enemies still matter.
            return target instanceof LivingEntity && target.distanceToSqr(explosion.center())<=9
                    ?Math.max(normal,40*Math.clamp(exposure,0,1)):normal;
        }
    };
    private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
    public HairDryerItem(Properties p){super(p);GeoItem.registerSyncedAnimatable(this);}
    public static boolean waterDamaged(ItemStack s){return s.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getBooleanOr("WaterDamaged",false);}
    public static boolean powered(Level level,BlockPos center){
        for(BlockPos pos:BlockPos.betweenClosed(center.offset(-5,-5,-5),center.offset(5,5,5))){
            if(!level.hasChunkAt(pos))continue;
            var state=level.getBlockState(pos);
            if(state.is(Blocks.REDSTONE_BLOCK) || level.getBestNeighborSignal(pos)>0
                    || state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT)
                    && state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT)
                    && (state.is(Blocks.REDSTONE_TORCH)||state.is(Blocks.REDSTONE_WALL_TORCH)||state.is(Blocks.REDSTONE_LAMP)))return true;
        }return false;
    }
    @Override public InteractionResult use(Level level,Player p,InteractionHand hand){
        var stack=p.getItemInHand(hand);
        if(stack.isEmpty()||!stack.is(this))return InteractionResult.FAIL;
        if(!powered(level,p.blockPosition())){if(!level.isClientSide())p.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("item.corpseorigin.hair_dryer.no_power"));return InteractionResult.FAIL;}
        if(waterDamaged(stack)){
            if(level.isClientSide())return InteractionResult.SUCCESS;
            stack.shrink(1);
            level.explode(p,null,SHORT_CIRCUIT_DAMAGE,p.position(),3,true,Level.ExplosionInteraction.TNT);
            p.hurtServer((ServerLevel)level,p.damageSources().explosion(p,null),4);p.igniteForSeconds(3);
            return InteractionResult.SUCCESS;
        }
        p.startUsingItem(hand);
        if(level instanceof ServerLevel server)triggerAnim(p,GeoItem.getOrAssignId(stack,server),"body","charge");
        return InteractionResult.SUCCESS;
    }
    @Override public ItemUseAnimation getUseAnimation(ItemStack s){return ItemUseAnimation.BOW;}
    @Override public int getUseDuration(ItemStack s,LivingEntity e){return 100;}
    @Override public void onUseTick(Level l,LivingEntity e,ItemStack s,int remaining){
        if(l instanceof ServerLevel server && remaining%5==0)server.sendParticles(ParticleTypes.ELECTRIC_SPARK,e.getX(),e.getEyeY(),e.getZ(),3,.3,.2,.3,.03);
    }
    @Override public boolean releaseUsing(ItemStack s,Level l,LivingEntity e,int remaining){return finishCharge(s,l,e,100-remaining);}
    @Override public ItemStack finishUsingItem(ItemStack s,Level l,LivingEntity e){finishCharge(s,l,e,100);return s;}
    public boolean finishCharge(ItemStack s,Level l,LivingEntity e,int ticks){
        if(l instanceof ServerLevel server)stopTriggeredAnim(e,GeoItem.getOrAssignId(s,server),"body","charge");
        if(!(l instanceof ServerLevel server)||!(e instanceof Player p)||ticks<5||waterDamaged(s)||!powered(l,p.blockPosition()))return false;
        float f=Math.clamp(ticks/100f,0,1);f=(f*f+2*f)/3;
        p.addEffect(new MobEffectInstance(MobEffects.LUCK,(int)(100+f*200),(int)(f*2)));
        l.playSound(null,p.blockPosition(),net.minecraft.sounds.SoundEvents.ITEM_PICKUP,net.minecraft.sounds.SoundSource.PLAYERS,.5f+f*.5f,1);
        triggerAnim(p,GeoItem.getOrAssignId(s,server),"body","use");
        s.hurtAndBreak(Math.max(1,(int)(f*3)),p,p.getUsedItemHand()==InteractionHand.MAIN_HAND?EquipmentSlot.MAINHAND:EquipmentSlot.OFFHAND);return true;
    }
    public static void tickDropped(ItemEntity item){
        if(item.isRemoved()||!(item.level() instanceof ServerLevel level)||item.tickCount%10!=0||!(item.getItem().getItem() instanceof HairDryerItem)||waterDamaged(item.getItem()))return;
        var pos=item.blockPosition();boolean wet=false;
        for(BlockPos p:BlockPos.betweenClosed(pos.offset(-1,-1,-1),pos.offset(1,1,1)))if(level.getFluidState(p).is(FluidTags.WATER)){wet=true;break;}
        if(!wet)return;
        if(powered(level,pos)){
            for(var target:level.getEntitiesOfClass(LivingEntity.class,item.getBoundingBox().inflate(8))){
                boolean nearWater=target.isInWater();
                for(BlockPos p:BlockPos.betweenClosed(target.blockPosition().offset(-2,-1,-2),target.blockPosition().offset(2,2,2)))if(level.getFluidState(p).is(FluidTags.WATER)){nearWater=true;break;}
                if(nearWater){target.hurtServer(level,level.damageSources().lightningBolt(),16);target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS,60,2));target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,100,1));}
            }
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK,item.getX(),item.getY(),item.getZ(),30,1,1,1,.15);
            level.playSound(null,pos,net.minecraft.sounds.SoundEvents.LIGHTNING_BOLT_IMPACT,net.minecraft.sounds.SoundSource.BLOCKS,1.5f,1);
        }
        var stack=item.getItem().copy();CustomData.update(DataComponents.CUSTOM_DATA,stack,t->t.putBoolean("WaterDamaged",true));
        int damage=stack.getDamageValue()+30+level.getRandom().nextInt(51);
        if(level.getRandom().nextBoolean()||damage>=stack.getMaxDamage())item.discard();
        else{stack.setDamageValue(damage);item.setItem(stack);}
        level.sendParticles(ParticleTypes.SMOKE,item.getX(),item.getY(),item.getZ(),10,.3,.3,.3,.03);
        level.playSound(null,pos,net.minecraft.sounds.SoundEvents.GENERIC_SPLASH,net.minecraft.sounds.SoundSource.BLOCKS,1,1);
    }
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,net.minecraft.world.item.component.TooltipDisplay display,Consumer<net.minecraft.network.chat.Component> out,TooltipFlag flag){
        out.accept(net.minecraft.network.chat.Component.translatable(waterDamaged(stack)?"item.corpseorigin.hair_dryer.damaged":"item.corpseorigin.hair_dryer.hint"));
    }
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar c){c.add(new AnimationController<HairDryerItem>("body",0,t->t.setAndContinue(RawAnimation.begin().thenLoop("idle"))).triggerableAnim("charge",RawAnimation.begin().thenPlay("charge")).triggerableAnim("use",RawAnimation.begin().thenPlay("use")));}
    @Override public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
    @Override public void createGeoRenderer(Consumer<GeoRenderProvider> consumer){consumer.accept(new GeoRenderProvider(){
        private com.geckolib.renderer.GeoItemRenderer<HairDryerItem> renderer;
        @Override public com.geckolib.renderer.GeoItemRenderer<HairDryerItem> getGeoItemRenderer(){
            if(renderer==null)renderer=new com.geckolib.renderer.GeoItemRenderer<>(new com.geckolib.model.DefaultedItemGeoModel<>(xiaoshi2022.corpseorigin.CorpseOrigin.id("hair_dryer")));return renderer;
        }
    });}
}
