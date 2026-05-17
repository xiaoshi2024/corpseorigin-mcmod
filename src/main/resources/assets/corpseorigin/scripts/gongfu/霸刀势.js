
function activate(entity, world, data) {
    var SkillEffects = Packages.com.phagens.corpseorigin.GongFU.JSskill.SkillEffects;
    var PM = Packages.com.phagens.corpseorigin.GongFU.JSskill.Factory.ProjectileManager;
    var Vec3 = Packages.net.minecraft.world.phys.Vec3;

    // 获取功法层数和倍率
    var ceng = data.getCeng();
    var cengMultiplier = getCengMultiplier(ceng);
    var cengNum = getCengNumber(ceng);

    // 获取玩家属性
    var attackDamage = entity.getAttributeValue(Packages.net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
    var maxHealth = entity.getAttributeValue(Packages.net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);
    var movementSpeed = entity.getAttributeValue(Packages.net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);

    // 计算基础伤害（基于玩家攻击力）
    var baseDamage = attackDamage * 0.8; // 基础伤害为玩家攻击力的80%

    // 根据功法层数计算最终伤害
    var finalDamage = baseDamage * cengMultiplier;

    // 根据层数调整激光参数
    var laserRange = calculateFinalRange(10.0, ceng); // 使用工具函数计算射程
    var laserDamage = finalDamage;

    // ⭐ 生成霸刀势专属粒子特效
    var lookDir = entity.getLookAngle();
    var startPos = entity.getEyePosition();

    // 生成环绕玩家的刀气势场粒子 - 使用旋转魔法阵
    SkillEffects.spawnRotatingMagicCircle(
        entity,
        entity.x, entity.y, entity.z,
        2.0 + cengNum * 0.2, // 半径随层数增加
        3, // 3层环
        cengNum * 30, // 旋转角度
        "minecraft:enchanted_hit",
        15 + cengNum * 3, // 每层粒子数
        0.05 * cengNum // 旋转速度随层数增加
    );

    // 生成升腾漩涡效果增强气势
    SkillEffects.spawnAscendingSwirl(
        entity,
        entity.x, entity.y, entity.z,
        3.0 + cengNum * 0.5, // 高度
        1.5 + cengNum * 0.2, // 半径
        cengNum >= 6 ? "minecraft:portal" : "minecraft:spell",
        30 + cengNum * 5, // 粒子数量
        0.1 * cengNum // 漩涡强度
    );

    // ⭐ 创建激光投射物（带命中回调和跟随粒子）
    PM.getInstance().createProjectile(
        entity,
        startPos,
        lookDir,
        laserDamage,
        laserRange * 20.0, // 最大飞行距离（转换为tick单位）
        3.0, // 飞行速度
        cengNum >= 6 ? "dragon_breath" : (cengNum >= 3 ? "flame" : "crit"),
        0.3, // 粒子密度
        "minecraft:entity.player.attack.sweep",
        function(target) {
            // 命中回调：击中目标时触发范围伤害
            if (cengNum >= 6) {
                var centerPos = new Vec3(target.getX(), target.getY(), target.getZ());

                // 范围伤害半径 = 层数 × 0.8格
                var explosionRadius = cengNum * 0.8;

                // 造成范围伤害（使用SkillEffects工具方法）
                SkillEffects.createExplosionDamage(
                    entity,
                    centerPos,
                    explosionRadius,
                    laserDamage * 0.4, // 范围伤害为直接伤害的40%
                    0.6 // 距离衰减系数
                );

                // 生成爆炸冲击波特效
                SkillEffects.spawnShockwave(
                    entity,
                    target.getX(), target.getY(), target.getZ(),
                    explosionRadius,
                    "minecraft:explosion",
                    15
                );

                gongfuLog("霸刀势命中 - 范围半径: " + explosionRadius.toFixed(1) + "格");
            }
        },
        10, // 穿透次数
        1.2, // 碰撞箱大小
        0.02, // X扩散（减小使粒子更集中）
        0.02, // Y扩散（减小使粒子更集中）
        0 // 重力
    );

    // 生成跟随投射物的细长垂直粒子轨迹
    for (var i = 0; i < 40 + cengNum * 8; i++) {
        var progress = i / (40 + cengNum * 8);
        var particleX = startPos.x + lookDir.x * laserRange * progress;
        var particleY = startPos.y + lookDir.y * laserRange * progress;
        var particleZ = startPos.z + lookDir.z * laserRange * progress;

        // 选择粒子类型
        var particleType = "minecraft:end_rod"; // 使用末地烛粒子（细长垂直）
        if (cengNum >= 6) {
            particleType = "minecraft:dragon_breath";
        } else if (cengNum >= 3) {
            particleType = "minecraft:flame";
        }

        // 生成垂直细长的粒子柱
        for (var j = 0; j < 3; j++) {
            SkillEffects.spawnParticles(
                entity,
                particleType,
                1,
                particleX,
                particleY + j * 0.3, // 垂直排列
                particleZ,
                0.05, 0.05, 0.05, // 极小扩散，形成细线
                0.1 // 极低速度
            );
        }
    }

    // 播放增强的音效
    world.playSound(
        null, entity.getX(), entity.getY(), entity.getZ(),
        Packages.net.minecraft.sounds.SoundEvents.BEACON_ACTIVATE,
        Packages.net.minecraft.sounds.SoundSource.PLAYERS,
        1.0, 1.0 + (cengNum * 0.1) // 层数越高，音调越高
    );

    // 根据层数添加额外效果
    if (cengNum >= 3) {
        // 三重天以上：添加力量效果
        SkillEffects.addEffect(entity, "minecraft:strength", 100, Math.floor(cengNum / 3));
    }

    // 记录调试信息
    gongfuLog("霸刀势激活 - 层数: " + ceng + ", 倍率: " + cengMultiplier +
             ", 玩家攻击力: " + attackDamage.toFixed(1) +
             ", 最终伤害: " + laserDamage.toFixed(1) +
             ", 射程: " + laserRange.toFixed(1));

    return true;
}