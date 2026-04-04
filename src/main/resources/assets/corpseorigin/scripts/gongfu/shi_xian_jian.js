function activate(player, world, data) {
    var gongfuLog = function(msg) {
        print("[GongFu Test] " + msg);
    };

    gongfuLog("========== 开始测试飞行道具系统 ==========");

    // 使用 Java 层预计算的值（通过 Packages 创建 Vec3）
    var Vec3 = Packages.net.minecraft.world.phys.Vec3;

    // 从预计算的变量构建朝向向量
    var lookDir = new Vec3(lookX, lookY, lookZ);

    gongfuLog("朝向向量: (" + lookX.toFixed(3) + ", " + lookY.toFixed(3) + ", " + lookZ.toFixed(3) + ")");

    // 计算起始位置（玩家前方 2 格）
    var startPos = new Vec3(
        playerX + lookX * 2,
        playerY + 1.5,
        playerZ + lookZ * 2
    );

    gongfuLog("起始位置: (" + startPos.x.toFixed(2) + ", " + startPos.y.toFixed(2) + ", " + startPos.z.toFixed(2) + ")");

    // ==================== 测试 1: 粒子模拟模式 ====================
    gongfuLog("测试 1: 创建粒子剑气");

    ProjectileManager.createProjectile(
        player,           // 射击者
        startPos,         // 起始位置
        lookDir,          // 方向
        25.0,             // 伤害
        15.0,             // 射程
        0.8,              // 速度
        "enchanted_hit",  // 粒子类型
        1.5,              // 碰撞箱
        "minecraft:entity.player.attack.sweep",  // 音效
        function(target) {
            // 命中回调 - 使用击退效果代替点燃
            gongfuLog("粒子剑气命中目标: " + target.getName().getString());
            target.push(lookX * 2.0, 1.0, lookZ * 2.0);  // 击退
        }
    );

    // ==================== 测试 2: 原版实体模式（箭矢）====================
    gongfuLog("测试 2: 发射附魔箭矢");

    var EntityType = Packages.net.minecraft.world.entity.EntityType;

    ProjectileManager.spawnEntityProjectile(
        player,
        EntityType.ARROW,
        new Vec3(playerX, eyeY, playerZ),
        lookDir,
        3.0,  // 威力
        null  // 暂时不配置（避免 Nashorn 调用问题）
    );

    gongfuLog("已发射箭矢");

    // ==================== 测试 3: 多重射击 ====================
    gongfuLog("测试 3: 扇形多重剑气");

    for (var i = -1; i <= 1; i++) {
        var angle = i * 0.3;  // 偏移角度
        var dirX = lookX * Math.cos(angle) - lookZ * Math.sin(angle);
        var dirZ = lookX * Math.sin(angle) + lookZ * Math.cos(angle);

        var direction = new Vec3(dirX, lookY, dirZ);

        ProjectileManager.createProjectile(
            player,
            startPos,
            direction,
            20.0,
            15.0,
            0.8,
            "flame",  // 火焰粒子
            1.5,
            null,
            function(target) {
                gongfuLog("扇形剑气命中: " + target.getName().getString());
            }
        );
    }

    // ==================== 测试 4: 不同粒子类型 ====================
    gongfuLog("测试 4: 测试不同粒子效果");

    var particleTypes = ["crit", "cloud", "heart", "spark"];

    for (var i = 0; i < particleTypes.length; i++) {
        var offset = (i - 1.5) * 0.5;
        var dirX = lookX + offset * 0.2;
        var dirZ = lookZ + offset * 0.2;

        var direction = new Vec3(dirX, lookY, dirZ);

        ProjectileManager.createProjectile(
            player,
            startPos,
            direction,
            15.0,
            10.0,
            0.6,
            particleTypes[i],  // 不同粒子
            1.0,
            null,
            null
        );
    }

    // ==================== 测试 5: 带状态效果的剑气 ====================
    gongfuLog("测试 5: 创建带虚弱效果的剑气");

    var MobEffectInstance = Packages.net.minecraft.world.effect.MobEffectInstance;
    var MobEffects = Packages.net.minecraft.world.effect.MobEffects;

    ProjectileManager.createProjectile(
        player,
        startPos,
        lookDir,
        30.0,
        20.0,
        1.0,
        "cloud",
        2.0,
        "minecraft:entity.generic.explode",
        function(target) {
            // 添加虚弱效果
            target.addEffect(new MobEffectInstance(
                MobEffects.WEAKNESS,
                200,  // 10 秒
                1     // II 级
            ));

            // 添加缓慢效果
            target.addEffect(new MobEffectInstance(
                MobEffects.MOVEMENT_SLOWDOWN,
                200,
                2     // III 级
            ));

            gongfuLog("已给目标添加虚弱和缓慢效果");
        }
    );

    // ==================== 测试 6: 高速穿透剑气 ====================
    gongfuLog("测试 6: 创建高速剑气");

    ProjectileManager.createProjectile(
        player,
        startPos,
        lookDir,
        35.0,  // 高伤害
        25.0,  // 远射程
        1.5,   // 高速度
        "spark",
        1.0,
        "minecraft:entity.player.attack.knockback",
        function(target) {
            // 强力击退
            target.push(lookX * 5.0, 2.0, lookZ * 5.0);
            gongfuLog("高速剑气命中并击退目标");
        }
    );

    gongfuLog("========== 测试完成！共创建 11 个投射物 ==========");
    gongfuLog("提示: 观察粒子效果、命中回调、状态效果是否正常");

    return true;
}

// 返回包含 activate 函数的对象
({
    activate: activate
});