function activate(player, world, data) {
    var SkillEffects = Packages.com.phagens.corpseorigin.GongFU.JSskill.SkillEffects;

    // 获取功法层数和倍率
    var ceng = data.getCeng();
    var cengMultiplier = getCengMultiplier(ceng);
    var cengNum = getCengNumber(ceng);

    // 获取玩家属性
    var armor = player.getAttributeValue(Packages.net.minecraft.world.entity.ai.attributes.Attributes.ARMOR);
    var maxHealth = player.getAttributeValue(Packages.net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);

    // ⭐ 基于玩家护甲值和层数计算抗性等级（低稀有度功法数值保守）
    // 基础抗性等级 = 玩家护甲值 × 0.1 + 层数 × 0.3
    var resistanceLevel = Math.floor(armor * 0.1 + cengNum * 0.3);

    // 限制最大抗性等级为4（避免过强）
    if (resistanceLevel > 4) resistanceLevel = 4;
    if (resistanceLevel < 1) resistanceLevel = 1;

    // ⭐ 基于玩家生命值和层数计算持续时间
    // 基础持续时间 = 200 tick + 玩家最大生命值 × 2 + 层数 × 20
    var duration = 200 + Math.floor(maxHealth * 2) + cengNum * 20;

    // 限制最大持续时间为600 tick（30秒）
    if (duration > 600) duration = 600;

    // ⭐ 生成气甲护盾粒子特效
    var particleCount = 20 + cengNum * 3; // 粒子数量随层数增加

    for (var i = 0; i < particleCount; i++) {
        var angle = (i / particleCount) * Math.PI * 2;
        var radius = 1.5 + Math.random() * 0.5;

        SkillEffects.spawnParticles(
            player,
            "minecraft:cloud",
            1,
            player.x + Math.cos(angle) * radius,
            player.y + player.eyeHeight + (Math.random() - 0.5) * 2,
            player.z + Math.sin(angle) * radius,
            0.3, 0.3, 0.3,
            0.05
        );
    }

    // 生成垂直上升的气流粒子
    for (var i = 0; i < 10 + cengNum * 2; i++) {
        SkillEffects.spawnParticles(
            player,
            "minecraft:poof",
            1,
            player.x + (Math.random() - 0.5) * 2,
            player.y + Math.random() * 2,
            player.z + (Math.random() - 0.5) * 2,
            0.2, 0.5, 0.2,
            0.1
        );
    }

    // ⭐ 给玩家添加动态计算的抗性效果
    SkillEffects.addEffect(player, "minecraft:resistance", duration, resistanceLevel - 1);

    // 根据层数添加额外防御效果
    if (cengNum >= 3) {
        // 三重天以上：添加吸收效果（临时生命值）
        var absorptionAmount = 2.0 + cengNum * 1.5;
        SkillEffects.addEffect(player, "minecraft:absorption", duration, Math.floor(absorptionAmount / 4));
    }

    if (cengNum >= 6) {
        // 六重天以上：添加防火效果
        SkillEffects.addEffect(player, "minecraft:fire_resistance", duration, 0);
    }

    // 播放音效
    SkillEffects.playSound(player, "minecraft:block.anvil.place", 0.8, 1.0 + cengNum * 0.05);

    // 记录调试信息
    gongfuLog("气甲术激活 - 层数: " + ceng + ", 倍率: " + cengMultiplier +
             ", 玩家护甲: " + armor.toFixed(1) +
             ", 抗性等级: " + resistanceLevel +
             ", 持续时间: " + (duration / 20).toFixed(1) + "秒");

    return true;
}
