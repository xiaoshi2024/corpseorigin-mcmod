package xiaoshi2022.corpseorigin.entity;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.*;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;
import xiaoshi2022.corpseorigin.character.CharacterManager;
import xiaoshi2022.corpseorigin.skill.chapter.*;
import java.util.UUID;

/** Short-lived skill geometry with server-side ownership and swept projectile hits. */
public class SkillConstructEntity extends PathfinderMob implements GeoEntity {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation SPAWN_IDLE = RawAnimation.begin().thenPlay("spawn").thenLoop("idle");
    private static final RawAnimation SHOOT = RawAnimation.begin().thenPlayAndHold("shoot");
    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("attack");
    private static final net.minecraft.network.syncher.EntityDataAccessor<Integer> ANCHOR =
            SynchedEntityData.defineId(SkillConstructEntity.class,net.minecraft.network.syncher.EntityDataSerializers.INT);
    private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
    private UUID owner,anchor;
    private String role="";
    private int life;
    private int specialTicks, specialCooldown;
    private Vec3 specialDirection=Vec3.ZERO;
    public SkillConstructEntity(EntityType<? extends SkillConstructEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
        setInvulnerable(true);
        setSilent(true);
        xpReward = 0;
        setPersistenceRequired();  // 防止被原版逻辑自动清除
    }
    public String kind(){return BuiltInRegistries.ENTITY_TYPE.getKey(getType()).getPath();}
    public boolean ownedBy(ServerPlayer p){return p.getUUID().equals(owner);}
    public boolean ownedBy(CloneAvatarEntity clone){return clone.getUUID().equals(owner);}
    public static SkillConstructEntity findOwned(CloneAvatarEntity clone, String kind) {
        return clone.level().getEntitiesOfClass(SkillConstructEntity.class, clone.getBoundingBox().inflate(64),
                e -> e.isAlive() && e.ownedBy(clone) && e.kind().equals(kind)).stream().findFirst().orElse(null);
    }
    public static SkillConstructEntity spawn(CloneAvatarEntity clone, EntityType<SkillConstructEntity> type, Entity anchor, int ticks) {
        var entity = type.create(clone.level(), EntitySpawnReason.TRIGGERED);
        if (entity == null) throw new IllegalStateException("Cannot create clone skill construct");
        entity.owner = clone.getUUID();
        entity.role = clone.getBodyRole();
        entity.anchor = anchor == null ? null : anchor.getUUID();
        if (anchor != null) entity.entityData.set(ANCHOR, anchor.getId());
        entity.life = ticks;
        entity.setPos(anchor == null ? clone.getEyePosition() : anchor.position());
        entity.setYRot(clone.getYRot());
        if (entity instanceof BeeWheelEntity wheel) wheel.initializeClone(clone);
        clone.level().addFreshEntity(entity);
        return entity;
    }
    public static SkillConstructEntity spawn(ServerPlayer p,EntityType<SkillConstructEntity> type,Entity anchor,int ticks) {
        var e=type.create(p.level(),EntitySpawnReason.TRIGGERED);
        if(e==null)throw new IllegalStateException("Cannot create skill construct");
        e.owner=p.getUUID();e.anchor=anchor==null?null:anchor.getUUID();
        if(anchor!=null)e.entityData.set(ANCHOR,anchor.getId());
        e.role=CharacterManager.getInstance().getPlayerCharacterId(p);e.life=ticks;
        e.setPos(anchor==null?p.getEyePosition():anchor.position());e.setYRot(p.getYRot());
        if(e instanceof BeeWheelEntity wheel)wheel.initialize(p);
        p.level().addFreshEntity(e);return e;
    }
    @Override public void tick(){
        super.tick();
        // Retire old world-following hearts. Inventory previews are never added to or ticked by the level.
        if (!level().isClientSide() && kind().equals("black_gold_heart")) { discard(); return; }
        if(level().isClientSide()){
            var attached=level().getEntity(entityData.get(ANCHOR));
            if(attached!=null)follow(attached);
            return;
        }
        if(!(level() instanceof ServerLevel level))return;
        if (owner != null && level.getEntity(owner) instanceof CloneAvatarEntity clone) {
            if (--life <= 0 || !clone.isAlive() || !clone.isActive() || !role.equals(clone.getBodyRole())) { discard(); return; }
            tickClone(level, clone);
            return;
        }
        if(--life<=0 || owner==null || !(level.getEntity(owner) instanceof ServerPlayer p)
                || !p.isAlive() || !role.equals(CharacterManager.getInstance().getPlayerCharacterId(p))){discard();return;}
        String kind=kind();
        if(tickConstruct(level,p))return;
        if(kind.equals("black_gold_heart") && !xiaoshi2022.corpseorigin.character.PlayerCharacterData.get(p).hasLearned(p.getUUID(),"black_gold_heart")){discard();return;}
        if(kind.equals("tiangang_halo") && !xiaoshi2022.corpseorigin.skill.longyou.TianGangCombat.isShen(p)){discard();return;}
        if(anchor!=null){
            var target=level.getEntity(anchor);if(target==null || !target.isAlive()){discard();return;}
            if(kind.equals("vine_bind") && (!(target instanceof LivingEntity living) || !ChapterCombat.canHit(p,living))){discard();return;}
            follow(target);
            if(kind.equals("slaughter_incarnation")){
                tickIncarnation(p);
                if(tickCount%5==0)QiEffects.aura(this,"incarnation",0xb51236,specialTicks>0?5:3,12);
            }
            if(kind.equals("slaughter_incarnation") && specialTicks==0 && tickCount%20==0){
                SkillRework.area(p,8,32,1,true);
                // Let the 2.4-second entrance finish before overlaying an attack.
                if(tickCount>=60)triggerAnim("action","attack");
            }
            return;
        }
        Vec3 start=position(),end=start.add(getDeltaMovement());
        if(!level.hasChunkAt(net.minecraft.core.BlockPos.containing(end))){discard();return;}
        var block=level.clip(new ClipContext(start,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,this));
        var hit=ProjectileUtil.getEntityHitResult(this,start,block.getLocation(),new AABB(start,end).inflate(.4),
                e->e instanceof LivingEntity l && ChapterCombat.canHit(p,l),start.distanceToSqr(block.getLocation()));
        if(hit!=null){
            var target=(LivingEntity)hit.getEntity();
            float damage=kind.equals("blood_lotus_petal")?40:kind.equals("bee_wheel")?28:26;
            if(target.hurtServer(level,kind.equals("blood_lotus_petal") ? xiaoshi2022.corpseorigin.skill.QiSkillDamageSource.wrap(p.damageSources().playerAttack(p)) : p.damageSources().playerAttack(p),damage)){
                var v=getDeltaMovement().normalize();target.push(v.x,.3,v.z);target.hurtMarked=true;
            }
            discard();return;
        }
        if(block.getType()!=HitResult.Type.MISS){discard();return;}
        setPos(end);setYRot(getYRot()+25);
        if(kind.equals("blood_lotus_petal")){
            if(tickCount%3==0)QiEffects.cloud(level,end,0xc51a36,1.2f,12);
            if(tickCount%8==0)level.playSound(null,getX(),getY(),getZ(),net.minecraft.sounds.SoundEvents.FIRE_EXTINGUISH,
                    net.minecraft.sounds.SoundSource.PLAYERS,.35f,1.6f);
        }
    }
    protected boolean tickConstruct(ServerLevel level,ServerPlayer player){return false;}
    private void tickClone(ServerLevel level, CloneAvatarEntity clone) {
        if (this instanceof BeeWheelEntity wheel) { wheel.tickCloneWheel(level, clone); return; }
        if (anchor != null) {
            var attached = level.getEntity(anchor);
            if (!(attached instanceof LivingEntity living) || !living.isAlive()) { discard(); return; }
            if (kind().equals("vine_bind")) {
                if (!ChapterCombat.canHit(clone, living)) { discard(); return; }
                living.setDeltaMovement(living.getDeltaMovement().multiply(.1, 1, .1));
                living.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.SLOWNESS, 10, 4));
            }
            follow(attached);
            if (kind().equals("slaughter_incarnation") && tickCount >= 48 && tickCount % 20 == 0) {
                for (var target : level.getEntitiesOfClass(LivingEntity.class, clone.getBoundingBox().inflate(8),
                        t -> ChapterCombat.canHit(clone, t) && clone.hasLineOfSight(t) && clone.distanceToSqr(t) <= 64))
                    target.hurtServer(level, clone.damageSources().mobAttack(clone), 32);
                triggerAnim("action", "attack");
            }
            return;
        }
        Vec3 start = position(), end = start.add(getDeltaMovement());
        if (!level.hasChunkAt(net.minecraft.core.BlockPos.containing(end))) { discard(); return; }
        var wall = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        var hit = ProjectileUtil.getEntityHitResult(this, start, wall.getLocation(), new AABB(start, end).inflate(.4),
                e -> e instanceof LivingEntity living && ChapterCombat.canHit(clone, living), start.distanceToSqr(wall.getLocation()));
        if (hit != null) {
            var target = (LivingEntity) hit.getEntity();
            target.hurtServer(level, clone.damageSources().mobAttack(clone), kind().equals("blood_lotus_petal") ? 40 : 26);
            discard();
        } else if (wall.getType() != HitResult.Type.MISS) discard();
        else { setPos(end); setYRot(getYRot() + 25); }
    }
    public static SkillConstructEntity findOwned(ServerPlayer player,String kind){
        return player.level().getEntitiesOfClass(SkillConstructEntity.class,player.getBoundingBox().inflate(64),
                e->e.isAlive() && e.ownedBy(player) && e.kind().equals(kind)).stream().findFirst().orElse(null);
    }
    public boolean activateSpecial(ServerPlayer player){
        if(!ownedBy(player) || !kind().equals("slaughter_incarnation"))return false;
        if(tickCount<48 || specialTicks>0 || specialCooldown>0 || life<65){
            player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("skill.corpseorigin.killing_incarnation.special_wait"));
            return false;
        }
        if (!xiaoshi2022.corpseorigin.skill.SkillResources.pay(player,
                new xiaoshi2022.corpseorigin.skill.SkillResourceRules.Cost(15, 15))) return false;
        specialDirection=player.getLookAngle().normalize();
        specialTicks=64;specialCooldown=120;
        stopTriggeredAnim("action","attack");
        triggerAnim("special","special");
        return true;
    }
    private void tickIncarnation(ServerPlayer player){
        if(specialCooldown>0)specialCooldown--;
        if(specialTicks<=0)return;
        // Separate wind-up, strike and recovery; never overlap the ordinary pulse.
        if(--specialTicks==32){
            var level=player.level();
            Vec3 origin=player.getEyePosition();
            for(var target:level.getEntitiesOfClass(LivingEntity.class,player.getBoundingBox().inflate(12),
                    t->ChapterCombat.canHit(player,t))){
                Vec3 offset=target.getBoundingBox().getCenter().subtract(origin);
                if(offset.lengthSqr()>144 || offset.normalize().dot(specialDirection)<.5 || !player.hasLineOfSight(target))continue;
                if(target.hurtServer(level,xiaoshi2022.corpseorigin.skill.QiSkillDamageSource.wrap(player.damageSources().playerAttack(player)),64)){
                    target.push(specialDirection.x*1.5,.4,specialDirection.z*1.5);target.hurtMarked=true;
                }
            }
            for(int i=1;i<=12;i++){
                Vec3 point=origin.add(specialDirection.scale(i));
                var wall=level.clip(new ClipContext(origin,point,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,player));
                if(wall.getType()!=HitResult.Type.MISS)break;
                if(i%2==0)QiEffects.cloud(level,point,0xc51a36,.5f+i*.12f,16);
            }
            level.playSound(null,player.blockPosition(),net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE.value(),
                    net.minecraft.sounds.SoundSource.PLAYERS,.8f,.8f);
        }
    }
    @Override
    protected void registerGoals() {
        // 技能实体不需要 AI
    }

    private void follow(Entity target){
        String kind=kind();
        double height=kind.equals("slaughter_incarnation")?target.getBbHeight()+.3:kind.equals("black_gold_heart")?1:0;
        setPos(target.position().add(0,height,0));
        setYRot(target instanceof LivingEntity living?living.yBodyRot:target.getYRot());
    }
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);  // 关键：先让父类注册其字段
        builder.define(ANCHOR, -1);        // 再注册自己的字段
    }
    @Override protected void addAdditionalSaveData(ValueOutput out){} // Ephemeral constructs never resume an attack after reload.
    @Override protected void readAdditionalSaveData(ValueInput in){life=0;}
    @Override public boolean hurtServer(ServerLevel level,net.minecraft.world.damagesource.DamageSource source,float amount){return false;}
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers){
        RawAnimation animation = switch(kind()){
            case "blood_lotus_petal", "slaughter_incarnation" -> SPAWN_IDLE;
            case "bee_wheel" -> SHOOT;
            case "severed_forearm", "vine_bind" -> IDLE;
            default -> null;
        };
        if(animation!=null)controllers.add(new AnimationController<SkillConstructEntity>(
                "main",0,test->test.setAndContinue(animation)));
        if(kind().equals("slaughter_incarnation"))controllers.add(new AnimationController<SkillConstructEntity>(
                "action",0,test->PlayState.STOP)
                // Fit the 1.6-second attack inside the existing one-second damage cadence.
                .setAnimationSpeed(2).triggerableAnim("attack",ATTACK));
        if(kind().equals("slaughter_incarnation"))controllers.add(new AnimationController<SkillConstructEntity>(
                "special",0,test->PlayState.STOP).triggerableAnim("special",RawAnimation.begin().thenPlay("special")));
        if(kind().equals("tiangang_halo"))controllers.add(new com.geckolib.animation.AnimationController<SkillConstructEntity>(
                "eyes",0,test->test.setAndContinue(com.geckolib.animation.RawAnimation.begin().thenLoop("openeye"))));
        if(kind().equals("black_gold_heart"))controllers.add(new com.geckolib.animation.AnimationController<SkillConstructEntity>(
                "pulse",0,test->test.setAndContinue(com.geckolib.animation.RawAnimation.begin().thenLoop("heartbeat"))));
    }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
}
