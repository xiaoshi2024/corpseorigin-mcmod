function activate(entity, world, data) {
    var ceng = data.getCeng();
    var finalDamage = calculateFinalDamage(15.0, ceng);
    var cengNum = getCengNumber(ceng);

    var lookDir = entity.getLookAngle();
    var startPos = entity.getEyePosition();

    var PM = Packages.com.phagens.corpseorigin.GongFU.JSskill.Factory.ProjectileManager;
    var SkillEffects = Packages.com.phagens.corpseorigin.GongFU.JSskill.SkillEffects;
    var Vec3 = Packages.net.minecraft.world.phys.Vec3;

    for (var i = 0; i < cengNum; i++) {
        var angle = (i - (cengNum - 1) / 2) * 0.4;
        var dirX = lookDir.x * Math.cos(angle) - lookDir.z * Math.sin(angle);
        var dirZ = lookDir.x * Math.sin(angle) + lookDir.z * Math.cos(angle);
        var direction = new Vec3(dirX, lookDir.y, dirZ);

        PM.getInstance().createProjectile(
            entity,
            startPos,
            direction,
            finalDamage,
            25.0,
            1.8,
            "crit",
            1.5,
            "minecraft:entity.player.attack.sweep",
             function(target) {
                            var level = target.level();

                            level.sendParticles(
                                Packages.net.minecraft.core.particles.ParticleTypes.CRIT,
                                target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                                20, 0.5, 0.5, 0.5, 0.1
                            );

                            level.sendParticles(
                                Packages.net.minecraft.core.particles.ParticleTypes.SWEEP_ATTACK,
                                target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                                10, 0.8, 0.8, 0.8, 0.05
                            );

                            var soundEvent = Packages.net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT
                                .get(Packages.net.minecraft.resources.ResourceLocation.parse("minecraft:entity.player.attack.knockback"));
                            if (soundEvent != null) {
                                level.playSound(null, target.getX(), target.getY(), target.getZ(),
                                    soundEvent, Packages.net.minecraft.sounds.SoundSource.PLAYERS, 1.0, 1.0);
                            }
                        }
                    );
    }

    return true;
}
