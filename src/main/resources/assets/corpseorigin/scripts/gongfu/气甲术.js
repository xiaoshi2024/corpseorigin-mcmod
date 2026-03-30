// 气甲术 - 防御型功法
(function() {
    return {
        activate: function(player, world, data) {
            try {
                var SkillEffects = Packages.com.phagens.corpseorigin.GongFU.JSskill.SkillEffects;

                // 1. 创建气甲护盾效果
                for (var i = 0; i < 30; i++) {
                    SkillEffects.spawnParticles(
                        player,
                        "minecraft:cloud",
                        1,
                        player.x + (Math.random() - 0.5) * 3,
                        player.y + player.eyeHeight + (Math.random() - 0.5) * 2,
                        player.z + (Math.random() - 0.5) * 3,
                        0.5, 0.5, 0.5,
                        0.1
                    );
                }

                // 2. 给玩家添加临时护甲效果
                SkillEffects.addEffect(player, "minecraft:resistance", 200, 1);

                // 3. 播放音效
                SkillEffects.playSound(player, "minecraft:block.anvil.place", 0.8, 1.2);

                return true;
            } catch (e) {
                java.lang.System.out.println("气甲术执行失败了：" + e);

                                throw e;
            }
        }
    };
})();
