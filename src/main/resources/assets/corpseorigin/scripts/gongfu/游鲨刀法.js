function activate(entity, world, data) {
    var ceng = data.getCeng();
    var finalDamage = calculateFinalDamage(15.0, ceng);

    var lookDir = entity.getLookAngle();
    var startPos = entity.getEyePosition();

    var PM = Packages.com.phagens.corpseorigin.GongFU.JSskill.Factory.ProjectileManager;
    var Vec3 = Packages.net.minecraft.world.phys.Vec3;

    PM.getInstance().createProjectile(
        entity,
        startPos,
        lookDir,
        finalDamage,
        25.0,
        2.0,
        "crit",
        0.2,
        "minecraft:entity.player.attack.sweep",
        null,
        8,
        1.5,
        0.1,
        0.1,
        0.02
    );

    return true;
}