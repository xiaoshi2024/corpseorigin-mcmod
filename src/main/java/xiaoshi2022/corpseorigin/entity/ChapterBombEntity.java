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

public class ChapterBombEntity extends ThrowableItemProjectile {
    public ChapterBombEntity(EntityType<? extends ChapterBombEntity> type,Level level) { super(type,level); }
    @Override protected Item getDefaultItem() { return ModItems.PARCEL_BOMB; }
    @Override public void tick() {
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
        if(getOwner() instanceof ServerPlayer player && player.isAlive() && player.level()==level) {
            var center=hit.getLocation().subtract(getDeltaMovement().normalize().scale(.15));
            for(var target:level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,
                    new net.minecraft.world.phys.AABB(center,center).inflate(3))) {
                if(!ChapterCombat.canHit(player,target) || target.distanceToSqr(center)>9)continue;
                if(level.clip(new ClipContext(center,target.getEyePosition(),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,this)).getType()!=HitResult.Type.MISS)continue;
                target.hurtServer(level,damageSources().playerAttack(player),8);
            }
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION,center.x,center.y,center.z,3,.3,.3,.3,0);
            level.playSound(null,center.x,center.y,center.z,net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE.value(),net.minecraft.sounds.SoundSource.PLAYERS,.8f,1);
        }
        discard();
    }
    public static void launch(ServerPlayer player,Item item) {
        var bomb=new ChapterBombEntity(xiaoshi2022.corpseorigin.registry.ModEntities.CHAPTER_BOMB,player.level());
        bomb.setOwner(player); bomb.setItem(new net.minecraft.world.item.ItemStack(item));
        bomb.setPos(player.getEyePosition().add(0,-.15,0));
        bomb.setDeltaMovement(player.getLookAngle().scale(.9)); player.level().addFreshEntity(bomb);
        player.swing(net.minecraft.world.InteractionHand.MAIN_HAND,true);
    }
}
