package xiaoshi2022.corpseorigin.entity;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.*;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import xiaoshi2022.corpseorigin.growth.ThermalSurvey;

/** Craftable companion: forehead scanner and charged ion discharge, with owner-safe targeting. */
public final class ZishuRobotEntity extends TamableAnimal implements GeoEntity,RangedAttackMob {
    private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
    private static final RawAnimation IDLE=RawAnimation.begin().thenLoop("idle"),WALK=RawAnimation.begin().thenLoop("walk");
    public ZishuRobotEntity(EntityType<? extends TamableAnimal> type,Level l){super(type,l);}
    public static AttributeSupplier.Builder createAttributes(){return createMobAttributes().add(Attributes.MAX_HEALTH,80).add(Attributes.ARMOR,12).add(Attributes.MOVEMENT_SPEED,.3).add(Attributes.FOLLOW_RANGE,24);}
    public boolean canEngage(LivingEntity target){return target!=null&&target.isAlive()&&!(target instanceof Player)&&target!=getOwner()&&!isAlliedTo(target)
            &&!(target instanceof TamableAnimal pet&&pet.getOwner()!=null&&pet.getOwner()==getOwner())&&ThermalSurvey.isCorpse(target);}
    @Override protected void registerGoals(){
        goalSelector.addGoal(0,new FloatGoal(this));goalSelector.addGoal(1,new SitWhenOrderedToGoal(this));
        goalSelector.addGoal(2,new RangedAttackGoal(this,1.1,60,20));goalSelector.addGoal(3,new FollowOwnerGoal(this,1.15,6,2));
        goalSelector.addGoal(4,new LookAtPlayerGoal(this,Player.class,8));goalSelector.addGoal(5,new RandomLookAroundGoal(this));
        targetSelector.addGoal(1,new NearestAttackableTargetGoal<LivingEntity>(this,LivingEntity.class,10,true,false,(e,l)->!isOrderedToSit()&&canEngage(e)));
    }
    @Override public void performRangedAttack(LivingEntity target,float distance){
        if(!(level() instanceof ServerLevel server)||isOrderedToSit()||!canEngage(target)||!hasLineOfSight(target))return;
        triggerAnim("action","ion");
        var orb=new ZishuIonBallEntity(server,this,target);server.addFreshEntity(orb);
    }
    @Override public InteractionResult mobInteract(Player player,InteractionHand hand){
        if(!isOwnedBy(player))return InteractionResult.PASS;
        if(!level().isClientSide()){
            if(player.getItemInHand(hand).is(Items.REDSTONE)&&getHealth()<getMaxHealth()){heal(16);player.getItemInHand(hand).consume(1,player);}
            else if(player.isShiftKeyDown()&&player instanceof net.minecraft.server.level.ServerPlayer sp)ThermalSurvey.report(sp);
            else{setOrderedToSit(!isOrderedToSit());getNavigation().stop();setTarget(null);}
        }return InteractionResult.SUCCESS;
    }
    @Override public boolean isFood(ItemStack s){return false;}
    @Override public AgeableMob getBreedOffspring(ServerLevel l,AgeableMob other){return null;}
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar c){
        c.add(new AnimationController<ZishuRobotEntity>("body",3,t->t.setAndContinue(t.isMoving()&&!isOrderedToSit()?WALK:IDLE)));
        c.add(new AnimationController<ZishuRobotEntity>("action",2,t->com.geckolib.animation.object.PlayState.STOP).triggerableAnim("ion",RawAnimation.begin().thenPlay("ion")));
    }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
}
