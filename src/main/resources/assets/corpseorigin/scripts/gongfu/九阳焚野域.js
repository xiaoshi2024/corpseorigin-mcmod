
function activate(player, world, data) {
    var DomainFactory = Packages.com.phagens.corpseorigin.GongFU.Domain.DomainFactory;
    var SkillEffects = Packages.com.phagens.corpseorigin.GongFU.JSskill.SkillEffects;
    var Vec3 = Packages.net.minecraft.world.phys.Vec3;

    // 获取功法层数和倍率
    var ceng = data.getCeng();
    var cengMultiplier = getCengMultiplier(ceng);
    var cengNum = getCengNumber(ceng);

    // 获取玩家属性
    var attackDamage = player.getAttributeValue(Packages.net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
    var maxHealth = player.getAttributeValue(Packages.net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);

    // ⭐ 动态计算技能参数（基于玩家属性和层数）
    // 基础伤害 = 玩家攻击力 × 0.6 × 层数倍率
    var baseDamage = attackDamage * 0.6;
    var finalDamage = baseDamage * cengMultiplier;

    // 领域半径 = 基础4格 + 玩家生命值加成 + 层数加成
    var domainRadius = 4.0 + (maxHealth * 0.05) + (cengNum * 1.0);

    // 持续时间 = 基础100tick + 玩家生命值加成 + 层数加成（单位：tick）
    var duration = 100 + Math.floor(maxHealth * 1.5) + (cengNum * 15);
    if (duration > 400) duration = 400; // 最大20秒

    // ⭐ 粒子配置（根据层数调整）
    var particleType = cengNum >= 6 ? Packages.net.minecraft.core.particles.ParticleTypes.FLAME :
                      (cengNum >= 3 ? Packages.net.minecraft.core.particles.ParticleTypes.ENCHANT :
                       Packages.net.minecraft.core.particles.ParticleTypes.END_ROD);

    var particleCount = 40 + cengNum * 5; // 粒子数量随层数增加
    var particleSpreadX = 0.05;
    var particleSpreadY = 0.1 + cengNum * 0.02; // Y轴扩散随层数增加
    var particleSpreadZ = 0.05;

    if (!world.isClientSide) {
        var pos = player.position();

        // ⭐ 召唤领域实体（使用完整版自定义粒子配置）
        var domain = DomainFactory.spawnDomain(
            player,                    // 召唤者
            pos,                       // 位置
            duration,                  // 持续时间（tick）
            finalDamage,               // 每次伤害
            domainRadius,              // 领域半径
            10,                        // 每10tick造成伤害
            particleType,              // 粒子类型
            particleCount,             // 每tick粒子数量
            particleSpreadX,           // X轴扩散
            particleSpreadY,           // Y轴扩散
            particleSpreadZ,           // Z轴扩散
            0.02,                      // 粒子速度
            true,                      // 使用环形排列
            function(target) {
                // ⭐ 命中回调：点燃敌人（使用 tick 单位，1秒=20tick）
                target.setRemainingFireTicks((2 + cengNum) * 20);

                // ⭐ 每3重天添加压制负面效果（虚弱+缓慢）
                if (cengNum % 3 === 0) {
                    var debuffLevel = Math.floor(cengNum / 3); // 3重天=1级，6重天=2级，9重天=3级

                    // 虚弱效果（降低攻击力）
                    SkillEffects.addEffect(target, "minecraft:weakness", 20, debuffLevel - 1);

                    // 缓慢效果（降低移动速度）
                    SkillEffects.addEffect(target, "minecraft:slowness", 20, debuffLevel - 1);

                    // 生成压制特效粒子
                    SkillEffects.spawnParticles(
                        player,
                        "minecraft:smoke",
                        5,
                        target.getX(),
                        target.getY() + target.getBbHeight() / 2,
                        target.getZ(),
                        0.4, 0.4, 0.4,
                        0.03
                    );

                    gongfuLog("九阳压制 - 目标: " + target.getName().getString() +
                             ", 等级: " + debuffLevel);
                }
            }
        );

        if (domain != null) {
            gongfuLog("九阳领域已激活！半径: " + domainRadius.toFixed(1) +
                     "格, 伤害: " + finalDamage.toFixed(1) +
                     ", 持续: " + (duration / 20).toFixed(1) + "秒");
        }
    }

    // ⭐ 初始爆发特效（一次性视觉效果）
    SkillEffects.spawnDomainExpansion(
        player,
        player.x, player.y, player.z,
        domainRadius,
        3,                     // 3层壳
        cengNum >= 6 ? "minecraft:flame" : "minecraft:enchant",
        25 + cengNum * 3       // 每层粒子数
    );

    // 生成升腾的阳气柱
    SkillEffects.spawnAscendingSwirl(
        player,
        player.x, player.y, player.z,
        4.0 + cengNum * 0.8,   // 高度
        2.0 + cengNum * 0.3,   // 半径
        "minecraft:flame",
        40 + cengNum * 5,      // 粒子数量
        0.15 * cengNum         // 漩涡强度
    );

    // ⭐ 根据层数添加额外效果
    if (cengNum >= 3) {
        // 三重天以上：给玩家添加防火效果
        SkillEffects.addEffect(player, "minecraft:fire_resistance", duration, 0);
    }

    if (cengNum >= 6) {
        // 六重天以上：给玩家添加力量效果
        SkillEffects.addEffect(player, "minecraft:strength", duration, Math.floor(cengNum / 3));

        // 生成爆炸冲击波特效
        SkillEffects.spawnShockwave(
            player,
            player.x, player.y, player.z,
            domainRadius,
            "minecraft:explosion",
            20
        );
    }

    if (cengNum >= 9) {
        // 九重天：终极效果 - 再生能力
        SkillEffects.addEffect(player, "minecraft:regeneration", 100, 2);
    }

    // 播放音效
    SkillEffects.playSound(player, "minecraft:entity.blaze.shoot", 1.0, 0.8 + cengNum * 0.05);

    // 记录调试信息
    gongfuLog("九阳焚野域激活 - 层数: " + ceng + ", 倍率: " + cengMultiplier +
             ", 玩家攻击力: " + attackDamage.toFixed(1) +
             ", 最终伤害: " + finalDamage.toFixed(1) +
             ", 领域半径: " + domainRadius.toFixed(1) + "格" +
             ", 持续时间: " + (duration / 20).toFixed(1) + "秒");

    return true;
}