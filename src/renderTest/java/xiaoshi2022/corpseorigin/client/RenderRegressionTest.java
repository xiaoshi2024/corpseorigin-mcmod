package xiaoshi2022.corpseorigin.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.item.equipment.ArmorType;
import xiaoshi2022.corpseorigin.client.compat.FirstPersonArmorCompat;
import xiaoshi2022.corpseorigin.client.limb.LimbRenderData;
import xiaoshi2022.corpseorigin.limb.LimbSlots;
import xiaoshi2022.corpseorigin.registry.ArmorMaterialRegistry;

/** Runs in an isolated client directory; does not create or load a world. */
public final class RenderRegressionTest implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        if (!Boolean.getBoolean("corpseorigin.renderRegression")) return;
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            try {
                for (boolean slim : new boolean[] {false, true}) checkSkin(slim);
                checkMaterials();
                boolean pal = FabricLoader.getInstance().isModLoaded("player_animation_library");
                if (pal) PalChecks.run();
                else require(FirstPersonArmorCompat.prepare(new AvatarRenderState(), new AvatarRenderState()) == -1,
                        "Optional bridge must work without PAL");
                System.out.println("CORPSEORIGIN_RENDER_REGRESSION_PASS pal=" + pal);
                client.stop();
            } catch (Throwable failure) {
                failure.printStackTrace();
                System.exit(1);
            }
        });
    }

    private static PlayerModel model(boolean slim) {
        return new PlayerModel(LayerDefinition.create(PlayerModel.createMesh(CubeDeformation.NONE, slim), 64, 64).bakeRoot(), slim);
    }

    private static void checkSkin(boolean slim) {
        PlayerModel model = model(slim);
        AvatarRenderState state = new AvatarRenderState();
        for (boolean visible : new boolean[] {true, false, true, false}) {
            state.showHat = state.showJacket = state.showLeftSleeve = state.showRightSleeve
                    = state.showLeftPants = state.showRightPants = visible;
            state.addGeckolibData(LimbRenderData.LIMB_MASK, 0);
            model.setupAnim(state);
            require(model.hat.visible == visible && model.jacket.visible == visible
                    && model.leftSleeve.visible == visible && model.rightSleeve.visible == visible
                    && model.leftPants.visible == visible && model.rightPants.visible == visible, "Skin toggles overridden");
            state.addGeckolibData(LimbRenderData.LIMB_MASK, LimbSlots.MASK_ALL);
            model.setupAnim(state);
            require(!model.head.visible && !model.rightArm.visible && !model.leftArm.visible
                    && !model.rightLeg.visible && !model.leftLeg.visible, "Severed limbs shown");
            state.addGeckolibData(LimbRenderData.LIMB_MASK, 0);
            model.setupAnim(state);
            require(model.head.visible && model.leftArm.visible && model.rightArm.visible, "Regrown limbs still hidden");
            require(model.hat.visible == visible && model.leftSleeve.visible == visible, "Regrowth overrides skin toggles");
        }
        state.isSpectator = true;
        model.setupAnim(state);
        require(!model.leftArm.visible && !model.rightLeg.visible, "Spectator body restored");
    }

    private static void checkMaterials() {
        var light = ArmorMaterialRegistry.XIAOLU_ARMOR_MATERIAL;
        var king = ArmorMaterialRegistry.LONGYOU_ARMOR_MATERIAL;
        int lightTotal = 0, kingTotal = 0;
        for (ArmorType type : new ArmorType[] {ArmorType.HELMET, ArmorType.CHESTPLATE, ArmorType.LEGGINGS}) {
            lightTotal += light.defense().get(type);
            kingTotal += king.defense().get(type);
        }
        require(lightTotal == 13 && kingTotal == 20, "Incorrect armor totals");
        require(light.toughness() == 0 && king.toughness() == 4, "Incorrect toughness");
        require(light.knockbackResistance() == 0 && king.knockbackResistance() == .1F, "Incorrect knockback resistance");
    }

    private static final class PalChecks {
        static void run() {
            var config = new com.zigythebird.playeranimcore.api.firstPerson.FirstPersonConfiguration(true, false, true, true, true);
            var manager = new com.zigythebird.playeranim.animation.AvatarAnimManager(null) {
                @Override public com.zigythebird.playeranimcore.api.firstPerson.FirstPersonConfiguration getFirstPersonConfiguration() {
                    return config;
                }
            };
            AvatarRenderState root = new AvatarRenderState(), slot = new AvatarRenderState();
            var from = (com.zigythebird.playeranim.accessors.IAvatarAnimationState) root;
            var to = (com.zigythebird.playeranim.accessors.IAvatarAnimationState) slot;
            from.playerAnimLib$setAnimManager(manager);
            from.playerAnimLib$setFirstPersonPass(true);
            require(FirstPersonArmorCompat.prepare(root, slot) == FirstPersonArmorCompat.RIGHT_ARM, "Wrong first-person arm mask");
            require(to.playerAnimLib$isFirstPersonPass() && to.playerAnimLib$getAnimManager() == manager, "Slot animation state not propagated");
            PlayerModel model = model(false);
            model.setupAnim(slot);
            require(!model.head.visible && !model.body.visible && !model.leftLeg.visible
                    && !model.rightLeg.visible && model.rightArm.visible && !model.leftArm.visible,
                    "First-person hidden body restored by limb mixin");
            slot.addGeckolibData(LimbRenderData.LIMB_MASK, 1 << LimbSlots.RIGHT_ARM);
            model.setupAnim(slot);
            require(!model.rightArm.visible, "PAL restored a severed arm");
            config.setShowArmor(false);
            require(FirstPersonArmorCompat.prepare(root, slot) == 0, "Hidden armor enabled");
            var perSlot = new java.util.EnumMap<net.minecraft.world.entity.EquipmentSlot, AvatarRenderState>(net.minecraft.world.entity.EquipmentSlot.class);
            perSlot.put(net.minecraft.world.entity.EquipmentSlot.CHEST, slot);
            root.addGeckolibData(com.geckolib.constant.DataTickets.PER_SLOT_RENDER_DATA, perSlot);
            for (var item : new net.minecraft.world.item.Item[] {
                    xiaoshi2022.corpseorigin.registry.ModItems.LONGYOU_CLOTH_CHESTPLATE.get(),
                    xiaoshi2022.corpseorigin.registry.ModItems.XIAOLU_ARMOR_CHESTPLATE.get(),
                    xiaoshi2022.corpseorigin.registry.ModItems.ANTENNA_ZBR_ARMOR_CHESTPLATE.get()}) {
                require(com.geckolib.renderer.GeoArmorRenderer.tryRenderGeoArmorPiece(
                        (state, equipmentSlot) -> model, new com.mojang.blaze3d.vertex.PoseStack(), null,
                        new net.minecraft.world.item.ItemStack(net.minecraft.core.Holder.direct(item)),
                        net.minecraft.world.entity.EquipmentSlot.CHEST, 0, root),
                        "First-person geo armor was not consumed by the visibility filter");
            }
            from.playerAnimLib$setFirstPersonPass(false);
            require(FirstPersonArmorCompat.prepare(root, slot) == -1 && !to.playerAnimLib$isFirstPersonPass(), "First-person state leaked into third person");
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
