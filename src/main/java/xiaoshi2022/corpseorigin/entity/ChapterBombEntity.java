package xiaoshi2022.corpseorigin.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import xiaoshi2022.corpseorigin.registry.ModItems;
import xiaoshi2022.corpseorigin.skill.chapter.ChapterCombat;
import xiaoshi2022.corpseorigin.skill.chapter.QiEffects;

public class ChapterBombEntity extends ThrowableItemProjectile {
    private int fuse=-1;
    private net.minecraft.world.phys.Vec3 blastCenter;
    public ChapterBombEntity(EntityType<? extends ChapterBombEntity> type,Level level) { super(type,level); }
    @Override protected Item getDefaultItem() { return ModItems.PARCEL_BOMB; }
    @Override public void tick() {
        if(fuse>=0) {
            if(level() instanceof ServerLevel && --fuse<=0)explode(blastCenter);
            return;
        }
        super.tick();
        if(!level().isClientSide() && tickCount>80) discard();
    }
    @Override protected boolean canHitEntity(net.minecraft.world.entity.Entity entity) {
        return super.canHitEntity(entity) && getOwner() instanceof ServerPlayer owner
                && entity instanceof net.minecraft.world.entity.LivingEntity target && ChapterCombat.canHit(owner,target);
    }
    @Override protected void onHit(HitResult hit) {
        super.onHit(hit);
        if(!(level() instanceof ServerLevel level))return;
        var center=hit.getLocation().subtract(getDeltaMovement().normalize().scale(.15));
        if(getItem().is(ModItems.BILLIARD_EIGHT)) {
            if(fuse>=0)return;
            blastCenter=center;setPos(center);setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
            setNoGravity(true);fuse=10+random.nextInt(51);return;
        }
        explode(center);
    }
    private void explode(net.minecraft.world.phys.Vec3 center) {
        if(!(level() instanceof ServerLevel level) || center==null){discard();return;}
        if(getOwner() instanceof ServerPlayer player && player.isAlive() && player.level()==level) {
            for(var target:level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,
                    new net.minecraft.world.phys.AABB(center,center).inflate(3))) {
                if(!ChapterCombat.canHit(player,target) || target.distanceToSqr(center)>9)continue;
                if(level.clip(new ClipContext(center,target.getEyePosition(),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,this)).getType()!=HitResult.Type.MISS)continue;
                target.hurtServer(level,damageSources().playerAttack(player),getItem().is(ModItems.BILLIARD_EIGHT)?24:16);
            }
            QiEffects.burst(level,center.x,center.y,center.z,0xd8552c,3,.3);
            level.playSound(null,center.x,center.y,center.z,net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE.value(),net.minecraft.sounds.SoundSource.PLAYERS,.8f,1);
        }
        discard();
    }
    public static void launch(ServerPlayer player,Item item) {
        var bomb=new ChapterBombEntity(xiaoshi2022.corpseorigin.registry.ModEntities.CHAPTER_BOMB,player.level());
        bomb.setOwner(player); bomb.setItem(new net.minecraft.world.item.ItemStack(item));
        bomb.setPos(player.getEyePosition().add(0,-.15,0));
        var direction=player.getLookAngle();
        if(item==ModItems.BILLIARD_EIGHT)direction=direction.add((player.getRandom().nextDouble()-.5)*.55,
                (player.getRandom().nextDouble()-.5)*.3,(player.getRandom().nextDouble()-.5)*.55).normalize();
        bomb.setDeltaMovement(direction.scale(.9)); player.level().addFreshEntity(bomb);
        player.swing(net.minecraft.world.InteractionHand.MAIN_HAND,true);
    }
}
