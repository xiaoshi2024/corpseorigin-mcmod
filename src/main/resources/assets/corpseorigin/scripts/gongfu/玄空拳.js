// 玄空拳 - 攻击型功法
function activate(player, world, data) {
    // 1. 获取玩家朝向
    var look = player.getLookAngle();

    // 2. 创建拳风效果
    for (var i = 0; i < 20; i++) {
        world.spawnParticle(
            "minecraft:sweep_attack",
            player.getX() + look.x * i * 0.5,
            player.getY() + player.getEyeHeight() + look.y * i * 0.5,
            player.getZ() + look.z * i * 0.5,
            look.x * 0.5, look.y * 0.5, look.z * 0.5
        );
    }

    // 3. 对前方生物造成伤害
    var targets = world.getEntitiesOfClass(
        Java.type("net.minecraft.world.entity.LivingEntity").class,
        player.getBoundingBox().expandTowards(look.scale(15)).inflate(3)
    );

    for (var i = 0; i < targets.size(); i++) {
        var target = targets.get(i);
        if (target != player && !target.isAlliedTo(player)) {
            target.hurt(world.damageSources().playerAttack(player), 12.0);
            target.push(look.x * 2, look.y * 0.5, look.z * 2);
        }
    }

    // 4. 播放音效
    world.playSound(
        null, player.getX(), player.getY(), player.getZ(),
        "minecraft:entity.player.attack.sweep",
        "players", 1.0, 0.9
    );

    return true;
}