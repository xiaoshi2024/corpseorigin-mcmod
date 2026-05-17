// 水球人异能 - 水系控制功法
function activate(player, world, data) {
    // 1. 创建水球环绕效果
    for (var i = 0; i < 50; i++) {
        var angle = (i / 50) * Math.PI * 2;
        var radius = 2.5;
        
        world.spawnParticle(
            "minecraft:dripping_water",
            player.getX() + Math.cos(angle) * radius,
            player.getY() + player.getEyeHeight() + Math.sin(i * 0.2) * 0.5,
            player.getZ() + Math.sin(angle) * radius,
            0, 0.1, 0
        );
    }

    // 2. 给玩家添加水系增益效果
    player.addEffect(new Packages.net.minecraft.world.effect.MobEffectInstance(
        Packages.net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE,
        200, // 10秒
        1,   // 等级：II级
        false, false
    ));
    
    player.addEffect(new Packages.net.minecraft.world.effect.MobEffectInstance(
        Packages.net.minecraft.world.effect.MobEffects.REGENERATION,
        100, // 5秒
        0,   // 等级：I级
        false, false
    ));

    // 3. 对周围生物造成水系伤害
    var targets = world.getEntitiesOfClass(
        Java.type("net.minecraft.world.entity.LivingEntity").class,
        player.getBoundingBox().inflate(8)
    );

    for (var i = 0; i < targets.size(); i++) {
        var target = targets.get(i);
        if (target != player && !target.isAlliedTo(player)) {
            target.hurt(world.damageSources().drown(), 20.0);
            
            // 添加减速效果（被水困住）
            target.addEffect(new Packages.net.minecraft.world.effect.MobEffectInstance(
                Packages.net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN,
                120, // 6秒
                1,   // 等级：II级
                false, false
            ));
        }
    }

    // 4. 播放水声效果
    world.playSound(
        null, player.getX(), player.getY(), player.getZ(),
        "minecraft:entity.dolphin.splash",
        "players", 1.2, 0.9
    );

    return true;
}