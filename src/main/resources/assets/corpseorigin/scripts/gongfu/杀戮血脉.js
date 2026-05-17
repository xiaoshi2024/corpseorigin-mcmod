// 杀戮血脉 - 终极异能功法
function activate(player, world, data) {
    // 1. 激活杀戮血脉效果
    for (var i = 0; i < 60; i++) {
        world.spawnParticle(
            "minecraft:dragon_breath",
            player.getX() + (Math.random() - 0.5) * 5,
            player.getY() + player.getEyeHeight() + (Math.random() - 0.5) * 3,
            player.getZ() + (Math.random() - 0.5) * 5,
            (Math.random() - 0.5) * 0.5,
            (Math.random() - 0.5) * 0.5,
            (Math.random() - 0.5) * 0.5
        );
    }

    // 2. 给玩家添加强力增益效果
    player.addEffect(new Packages.net.minecraft.world.effect.MobEffectInstance(
        Packages.net.minecraft.world.effect.MobEffects.DAMAGE_BOOST,
        300, // 15秒
        2,   // 等级：III级
        false, false
    ));
    
    player.addEffect(new Packages.net.minecraft.world.effect.MobEffectInstance(
        Packages.net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED,
        300, // 15秒
        2,   // 等级：III级
        false, false
    ));

    // 3. 对周围所有敌对生物造成毁灭性伤害
    var targets = world.getEntitiesOfClass(
        Java.type("net.minecraft.world.entity.LivingEntity").class,
        player.getBoundingBox().inflate(10)
    );

    for (var i = 0; i < targets.size(); i++) {
        var target = targets.get(i);
        if (target != player && !target.isAlliedTo(player)) {
            target.hurt(world.damageSources().magic(), 40.0);
            
            // 添加恐惧效果
            target.addEffect(new Packages.net.minecraft.world.effect.MobEffectInstance(
                Packages.net.minecraft.world.effect.MobEffects.WEAKNESS,
                200, // 10秒
                2,   // 等级：III级
                false, false
            ));
        }
    }

    // 4. 播放震撼音效
    world.playSound(
        null, player.getX(), player.getY(), player.getZ(),
        "minecraft:entity.ender_dragon.death",
        "players", 1.5, 0.7
    );

    return true;
}