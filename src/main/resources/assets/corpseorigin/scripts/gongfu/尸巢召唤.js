// 尸巢召唤 - 尸兄玩家的终极技能
var skill = {
    activate: function(entity, world, data) {
        if (!world.isClientSide) {
            var serverLevel = world;

            for (var i = 0; i < 100; i++) {
                var spawnX = entity.getX() + (Math.random() - 0.5) * 8;
                var spawnY = entity.getY() + (Math.random() - 0.5) * 6;
                var spawnZ = entity.getZ() + (Math.random() - 0.5) * 8;

                world.sendParticles(
                    Packages.net.minecraft.core.particles.ParticleTypes.DRAGON_BREATH,
                    spawnX, spawnY, spawnZ,
                    1, (Math.random() - 0.5) * 0.3, (Math.random() - 0.5) * 0.3, (Math.random() - 0.5) * 0.3, 0
                );
            }

            world.playSound(
                null, entity.getX(), entity.getY(), entity.getZ(),
                Packages.net.minecraft.sounds.SoundEvents.SLIME_BLOCK_PLACE,
                Packages.net.minecraft.sounds.SoundSource.PLAYERS, 2.0, 0.5
            );

            var CorpseNestFactory = Java.type("com.phagens.corpseorigin.GongFU.JSskill.Factory.CorpseNestFactory");
            var BlockPos = Packages.net.minecraft.core.BlockPos;

            var centerPos = new BlockPos(entity.blockPosition().getX(), entity.blockPosition().getY() - 1, entity.blockPosition().getZ());
            var ownerUUID = entity.getUUID();

            CorpseNestFactory.generateSemiCircle(serverLevel, centerPos, 8, ownerUUID, 5);

            var searchBox = new Packages.net.minecraft.world.phys.AABB(
                entity.getX() - 64, entity.getY() - 64, entity.getZ() - 64,
                entity.getX() + 64, entity.getY() + 64, entity.getZ() + 64
            );

            var zbEntities = world.getEntitiesOfClass(
                Java.type("com.phagens.corpseorigin.entity.LowerLevelZbEntity").class,
                searchBox
            );

            var totalKillsAdded = 0;
            var isCreative = false;
            var isPlayer = entity instanceof Packages.net.minecraft.server.level.ServerPlayer;

            if (isPlayer) {
                isCreative = entity.isCreative();
            }

            for (var i = 0; i < zbEntities.size(); i++) {
                var zb = zbEntities.get(i);

                var isPlayerServant = true;

                if (isPlayer) {
                    try {
                        var masterField = zb.getClass().getDeclaredField("masterUUID");
                        masterField.setAccessible(true);
                        var master = masterField.get(zb);
                        if (master && master.equals(entity.getUUID())) {
                            isPlayerServant = true;
                        } else {
                            isPlayerServant = false;
                        }
                    } catch (e) {
                        continue;
                    }
                }

                if (isPlayer && !isCreative && !isPlayerServant) {
                    continue;
                }

                for (var j = 0; j < 20; j++) {
                    var effectX = zb.getX() + (Math.random() - 0.5) * 2;
                    var effectY = zb.getY() + zb.getBbHeight() / 2 + (Math.random() - 0.5) * 2;
                    var effectZ = zb.getZ() + (Math.random() - 0.5) * 2;

                    world.sendParticles(
                        Packages.net.minecraft.core.particles.ParticleTypes.DRAGON_BREATH,
                        effectX, effectY, effectZ,
                        1, (Math.random() - 0.5) * 0.2, (Math.random() - 0.5) * 0.2, (Math.random() - 0.5) * 0.2, 0
                    );
                }

                zb.hurt(world.damageSources().magic(), 9999);
                totalKillsAdded++;

                world.playSound(
                    null, zb.getX(), zb.getY(), zb.getZ(),
                    Packages.net.minecraft.sounds.SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR,
                    Packages.net.minecraft.sounds.SoundSource.PLAYERS, 1.0, 0.8
                );
            }

            world.playSound(
                null, entity.getX(), entity.getY(), entity.getZ(),
                Packages.net.minecraft.sounds.SoundEvents.ENDER_DRAGON_GROWL,
                Packages.net.minecraft.sounds.SoundSource.PLAYERS, 1.5, 0.8
            );

            if (isPlayer) {
                entity.sendSystemMessage(
                    Packages.net.minecraft.network.chat.Component.literal("§a尸巢已召唤！生成了半圆形尸巢结构，半径8格")
                );
            }
        }

        return true;
    }
};

skill;