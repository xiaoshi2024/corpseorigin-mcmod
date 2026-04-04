var activate = function(player, world, data) {
    // 1. 获取层级信息
    var ceng = data.getCeng();
    var baseDamage = 25.0;
    var finalDamage = calculateFinalDamage(baseDamage, ceng);

    gongfuLog("霸道势 - 层级: " + ceng + ", 伤害: " + finalDamage);

    var lookDir = player.getLookAngle();

    // 2. 计算范围和距离（随层级变化）
    var visualRange = calculateFinalRange(15.0, ceng);  // 粒子特效显示范围
    var attackRange = calculateFinalRange(15.0, ceng);  // 实际攻击判定范围

    // 如果想要粒子比攻击范围稍短（更真实），可以这样：
    // var visualRange = attackRange * 0.9;  // 粒子显示为攻击范围的 90%

    // 3. 生成 3 道连续的斩击
    for (var i = 0; i < 3; i++) {
        // 斩击起始位置
        var startX = player.getX() + lookDir.x * 2;
        var startY = player.getY() + 1.0;
        var startZ = player.getZ() + lookDir.z * 2;

        // 斩击方向和距离（使用视觉范围）
        var dirX = lookDir.x;
        var dirZ = lookDir.z;

        // 使用 SkillEffects 生成斩击特效
        Packages.com.phagens.corpseorigin.GongFU.JSskill.SkillEffects.spawnSlashingAttack(
            player,
            startX, startY, startZ,
            dirX, dirZ, visualRange,  // ✅ 使用视觉范围
            5.0,   // 宽度
            5.0,   // 高度
            "minecraft:sweep_attack",  // 粒子类型
            90,    // 分段数
            0.1    // 速度
        );
    }

    // 4. 对前方锥形范围造成伤害（使用攻击范围）
    var targets = world.getEntitiesOfClass(
        Java.type("net.minecraft.world.entity.LivingEntity").class,
        player.getBoundingBox().inflate(attackRange)  // ✅ 使用攻击范围
    );
};

(function() {
    return {
        activate: activate
    };
})();