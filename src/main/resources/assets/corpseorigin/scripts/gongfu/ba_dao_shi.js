var skill = {
    activate: function(entity, world, data) {
        if (!world.isClientSide) {
            var SkillEffects = Java.type("com.phagens.corpseorigin.GongFU.JSskill.SkillEffects");

            // ⭐ 一行代码搞定
            SkillEffects.spawnLaserBeam(entity, 10.0);

            // 播放音效和粒子（可选）
            world.playSound(
                null, entity.getX(), entity.getY(), entity.getZ(),
                Packages.net.minecraft.sounds.SoundEvents.BEACON_ACTIVATE,
                Packages.net.minecraft.sounds.SoundSource.PLAYERS,
                1.0, 1.5
            );
        }

        return true;
    }
};

skill;