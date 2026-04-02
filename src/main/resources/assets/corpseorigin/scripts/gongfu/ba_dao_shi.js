var activate = function(player, world, data) {
    var lookDir = player.getLookAngle();

    // 生成 3 道连续的斩击
    for (var i = 0; i < 3; i++) {
        // 斩击起始位置
        var startX = player.getX() + lookDir.x * 2;
        var startY = player.getY() + 1.0;
        var startZ = player.getZ() + lookDir.z * 2;

        // 斩击方向和距离
        var dirX = lookDir.x;
        var dirZ = lookDir.z;
        var distance = 15.0;

        // 使用 SkillEffects 提供的工厂方法生成斩击特效
        Packages.com.phagens.corpseorigin.GongFU.JSskill.SkillEffects.spawnSlashingAttack(
            player,
            startX, startY, startZ,
            dirX, dirZ, distance,
            5.0,   // 宽度
            5.0,   // 高度
            "minecraft:sweep_attack",  // 粒子类型
            90,    // 分段数
            0.1    // 速度
        );
    }

    // 对前方锥形范围造成伤害
    var targets = world.getEntitiesOfClass(
        Java.type("net.minecraft.world.entity.LivingEntity").class,
        player.getBoundingBox().inflate(15)
    );

    for (var i = 0; i < targets.size(); i++) {
        var target = targets.get(i);
        if (target != player && !target.isAlliedTo(player)) {
            // 判断是否在前方锥形范围内
            var dx = target.getX() - player.getX();
            var dz = target.getZ() - player.getZ();
            var dot = dx * lookDir.x + dz * lookDir.z;

            if (dot > 0) { // 在前方
                target.hurt(world.damageSources().playerAttack(player), 25.0);
                // 强力击退
                target.push(lookDir.x * 3.0, 1.5, lookDir.z * 3.0);
            }
        }
    }
    return true;
};

(function() {
    return {
        activate: activate
    };
})();