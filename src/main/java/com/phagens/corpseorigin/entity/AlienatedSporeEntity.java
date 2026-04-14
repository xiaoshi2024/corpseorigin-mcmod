package com.phagens.corpseorigin.entity;

import com.phagens.corpseorigin.register.BlockRegistry;
import com.phagens.corpseorigin.register.EntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class AlienatedSporeEntity extends Entity {
    private static final int MAX_LIFETIME = 6000;
    private int lifetime = 0;
    
    public AlienatedSporeEntity(EntityType<? extends AlienatedSporeEntity> type, Level level) {
        super(type, level);
        this.noPhysics = false;
    }
    
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }
    
    @Override
    public void tick() {
        super.tick();
        
        if (!this.level().isClientSide) {
            lifetime++;
            if (lifetime > MAX_LIFETIME) {
                this.discard();
                return;
            }
        }
        
        Vec3 currentPos = this.position();
        Vec3 motion = this.getDeltaMovement();
        
        motion = motion.add(0, -0.03, 0);
        this.setDeltaMovement(motion);
        
        Vec3 nextPos = currentPos.add(motion);
        
        BlockHitResult hitResult = this.level().clip(new ClipContext(
            currentPos,
            nextPos,
            ClipContext.Block.COLLIDER,
            ClipContext.Fluid.NONE,
            this
        ));
        
        if (hitResult.getType() != HitResult.Type.MISS) {
            BlockPos hitPos = hitResult.getBlockPos();
            BlockState currentState = this.level().getBlockState(hitPos);
            
            if (!currentState.isAir() && !this.level().isClientSide) {
                BlockState alienatedFragment = BlockRegistry.ALIENATED_FRAGMENT.get().defaultBlockState();
                this.level().setBlock(hitPos, alienatedFragment, 3);
                
                if (this.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(
                        ParticleTypes.EXPLOSION,
                        hitPos.getX() + 0.5,
                        hitPos.getY() + 0.5,
                        hitPos.getZ() + 0.5,
                        1,
                        0.0, 0.0, 0.0,
                        0.0
                    );
                    
                    BlockState sporeBlockState = BlockRegistry.ALIENATED_FRAGMENT.get().defaultBlockState();
                    serverLevel.sendParticles(
                        new BlockParticleOption(ParticleTypes.FALLING_DUST, sporeBlockState),
                        hitPos.getX() + 0.5,
                        hitPos.getY() + 1.0,
                        hitPos.getZ() + 0.5,
                        15,
                        0.5, 0.3, 0.5,
                        0.1
                    );
                }
                
                this.discard();
                return;
            }
        }
        
        this.setPos(nextPos);
        
        if (this.level().isClientSide) {
            BlockState sporeBlockState = BlockRegistry.ALIENATED_FRAGMENT.get().defaultBlockState();
            this.level().addParticle(
                new BlockParticleOption(ParticleTypes.FALLING_DUST, sporeBlockState),
                this.getX(),
                this.getY(),
                this.getZ(),
                0.0, 0.0, 0.0
            );
        }
    }
    
    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Lifetime", lifetime);
    }
    
    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        lifetime = tag.getInt("Lifetime");
    }
    
    public static AlienatedSporeEntity create(Level level, Vec3 startPos, Vec3 direction) {
        AlienatedSporeEntity spore = new AlienatedSporeEntity(EntityRegistry.ALIENATED_SPORE.get(), level);
        spore.setPos(startPos.x, startPos.y, startPos.z);
        
        double speed = 0.6 + level.random.nextDouble() * 0.5;
        spore.setDeltaMovement(
            direction.x * speed,
            direction.y * speed + 0.25,
            direction.z * speed
        );
        
        return spore;
    }
    
    public static void spawnSporeBurst(ServerLevel level, Vec3 center, int count) {
        for (int i = 0; i < count; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2;
            double pitch = (level.random.nextDouble() - 0.5) * Math.PI * 0.8;
            
            double dx = Math.cos(angle) * Math.cos(pitch);
            double dy = Math.sin(pitch) + 0.3;
            double dz = Math.sin(angle) * Math.cos(pitch);
            
            Vec3 direction = new Vec3(dx, dy, dz).normalize();
            AlienatedSporeEntity spore = create(level, center.add(0, 0.5, 0), direction);
            level.addFreshEntity(spore);
        }
    }
}
