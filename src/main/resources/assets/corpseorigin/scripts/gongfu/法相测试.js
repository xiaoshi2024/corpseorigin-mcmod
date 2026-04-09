function activate(entity, world, data) {
    var Vec3 = Packages.net.minecraft.world.phys.Vec3;
    var FaxiangFactory = Packages.com.phagens.corpseorigin.GongFU.JSskill.Factory.FaxiangFactory;

    if (!world.isClientSide) {
        var pos = entity.position();

        var faxiang = FaxiangFactory.spawnFaxiang(
            entity,
            pos,
            "corpseorigin:geo/entity/guigun.geo.json",
            "corpseorigin:textures/entity/guigun.png",
            "corpseorigin:animations/entity/guigun.animation.json",
            1200,
            3.0,
            300.0,
            20.0,
            10.0,
            2.0,
            48.0
        );

        if (faxiang != null) {
            gongfuLog("法相已召唤！生命：300 攻击：20 持续60秒");
        }
    }

    return true;
}
