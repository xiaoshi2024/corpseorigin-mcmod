function activate(entity, world, data) {
    var ceng = data.getCeng();
    var finalDamage = calculateFinalDamage(10.0, ceng);
    var cengNum = getCengNumber(ceng);

    var lookDir = entity.getLookAngle();
    var startPos = entity.getEyePosition();

    var PM = Packages.com.phagens.corpseorigin.GongFU.JSskill.Factory.ProjectileManager;
     var SkillEffects = Packages.com.phagens.corpseorigin.GongFU.JSskill.SkillEffects;
        var Vec3 = Packages.net.minecraft.world.phys.Vec3;
    for (var i = 0; i < cengNum; i++) {
           var angle = (i - (cengNum - 1) / 2) * 0.3;
           var dirX = lookDir.x * Math.cos(angle) - lookDir.z * Math.sin(angle);
           var dirZ = lookDir.x * Math.sin(angle) + lookDir.z * Math.cos(angle);
           var direction = new Packages.net.minecraft.world.phys.Vec3(dirX, lookDir.y, dirZ);

           PM.getInstance().createHomingProjectile(
               entity,
               startPos,
               direction,
               finalDamage,
               200.0,
               1.0,
               "flame",
               1,
               "minecraft:entity.generic.explode",
                function(target) {
                           var level = target.level();

                           // 1. 生成爆炸粒子
                           level.sendParticles(
                               Packages.net.minecraft.core.particles.ParticleTypes.EXPLOSION,
                               target.getX(), target.getY(), target.getZ(),
                               30, 1.0, 1.0, 1.0, 0.2
                           );

                           // 2. 播放爆炸音效
                           var soundEvent = Packages.net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT
                               .get(Packages.net.minecraft.resources.ResourceLocation.parse("minecraft:entity.generic.explode"));
                           if (soundEvent != null) {
                               level.playSound(null, target.getX(), target.getY(), target.getZ(),
                                   soundEvent, Packages.net.minecraft.sounds.SoundSource.PLAYERS, 1.0, 1.0);
                           }

                           // 3. 区域伤害（使用 Java 工具方法）
                           var centerPos = new Vec3(target.getX(), target.getY(), target.getZ());
                           SkillEffects.createExplosionDamage(entity, centerPos, 3.0, finalDamage, 0.5);
                       },

                0.12,    // turnRate: 转向灵敏度
                       50,       // particleCount: 每tick 5个粒子
                       0.3,     // offsetX: X轴扩散0.3格
                       0.3,     // offsetY: Y轴扩散0.3格
                       0.3,     // offsetZ: Z轴扩散0.3格
                       0.05     // speed: 粒子向外扩散速度
           );
       }

    return true;
}

