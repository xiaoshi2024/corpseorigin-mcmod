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
                checkOrganAttachment();
                checkOrganCaches();
                checkCloneOrganSnapshot();
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

    private static void checkOrganAttachment() {
        PlayerModel model = model(false);
        com.mojang.blaze3d.vertex.PoseStack root = new com.mojang.blaze3d.vertex.PoseStack();
        model.body.resetPose();
        var origin = xiaoshi2022.corpseorigin.client.renderer.player.NiunaiXRenderer.bodyPose(root, model)
                .last().pose().transformPosition(new org.joml.Vector3f(0, 1.5F, 0));
        require(origin.length() < 0.0001F, "GEO shoulder must align with vanilla body pivot");
        // The shoulder remains on the body joint through crouching, attack twist and roll.
        model.body.setPos(2, 3, -4);
        model.body.xRot = .6F;
        model.body.yRot = -.7F;
        model.body.zRot = .3F;
        var mounted = xiaoshi2022.corpseorigin.client.renderer.player.NiunaiXRenderer.bodyPose(root, model).last().pose();
        var shoulder = mounted.transformPosition(new org.joml.Vector3f(0, 1.5F, 0));
        require(shoulder.distance(new org.joml.Vector3f(2 / 16F, 3 / 16F, -4 / 16F)) < .0001F,
                "Back mount detached from translated/rotated body pivot");
        // Compare against ModelPart's rotation order, independently of the attachment's origin compensation.
        var joint = new com.mojang.blaze3d.vertex.PoseStack();
        model.body.translateAndRotate(joint);
        var expected = joint.last().pose().transformDirection(new org.joml.Vector3f(0, 0, -1));
        require(mounted.transformDirection(new org.joml.Vector3f(0, 0, 1)).distance(expected) < .0001F,
                "Back mount must inherit all three body rotations");
        require(root.last().pose().equals(new org.joml.Matrix4f()), "Attachment mutated the shared pose stack");
    }

    private static void checkOrganCaches() {
        var host = new xiaoshi2022.corpseorigin.client.limb.PlayerGeoAnimatable() {
            final xiaoshi2022.corpseorigin.client.limb.PlayerLayerAnimationCache cache =
                    new xiaoshi2022.corpseorigin.client.limb.PlayerLayerAnimationCache(this);
            public void registerControllers(com.geckolib.animatable.manager.AnimatableManager.ControllerRegistrar controllers) {}
            public com.geckolib.animatable.instance.AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
        };
        var cache = host.cache;
        long first = Long.MIN_VALUE + 1, second = first + 1, gourd = Long.MIN_VALUE + 17;
        Object modelA = new Object(), modelB = new Object();
        cache.prepareOrgan(first, modelA);
        var a = cache.getManagerForId(first);
        var b = cache.getManagerForId(second);
        var g = cache.getManagerForId(gourd);
        var body = cache.getManagerForId(123);
        require(a != b && a != g && b != g && body != a && body != g, "Organ animation managers overlap");
        cache.prepareOrgan(first, modelA);
        require(a == cache.getManagerForId(first), "Unchanged organ loses its animation each frame");
        cache.prepareOrgan(first, modelB);
        require(a != cache.getManagerForId(first) && b == cache.getManagerForId(second)
                && g == cache.getManagerForId(gourd), "Changing one organ resets another organ");
        require(xiaoshi2022.corpseorigin.client.limb.PlayerGeoAnimatable.class.isAssignableFrom(
                xiaoshi2022.corpseorigin.entity.CloneAvatarEntity.class), "Clone animation mixin not applied");
    }

    private static void checkCloneOrganSnapshot() {
        for (boolean corpse : new boolean[] {false, true}) {
            for (String role : new String[] {"xiaojingang", "kaiweinai"}) {
                var original = new net.minecraft.nbt.CompoundTag();
                original.putString("CharacterId", role);
                var parts = new net.minecraft.nbt.CompoundTag();
                String loadout = "[{\"organ\":\"test\",\"joint\":\"body\"}]";
                parts.putString(xiaoshi2022.corpseorigin.growth.OrganLibrary.BODY_KEY, loadout);
                parts.putInt("wings", 3);
                parts.putBoolean("gourd_detached", true);
                parts.putString("gourd_entity", java.util.UUID.randomUUID().toString());
                parts.putBoolean("gourd_dead", true);
                original.put("EvolutionParts", parts);
                var skills = new net.minecraft.nbt.ListTag();
                skills.add(net.minecraft.nbt.StringTag.valueOf("role_skill"));
                original.put("LearnedSkills", skills);
                var tag = new net.minecraft.nbt.CompoundTag();
                tag.put("Data", original);
                var component = new xiaoshi2022.corpseorigin.shell.CharacterShellStateComponent();
                component.readNbt(tag);
                component.applyEvolutionPartsClone(net.minecraft.util.RandomSource.create(7), corpse, 1);
                component.clearCharacterId();
                var saved = new net.minecraft.nbt.CompoundTag();
                component.writeNbt(saved);
                var restored = new xiaoshi2022.corpseorigin.shell.CharacterShellStateComponent();
                restored.readNbt(saved);
                require(restored.getCharacterId().equals(xiaoshi2022.corpseorigin.character.MortalCharacter.ID),
                        "Cosmetic organs must not restore role identity");
                require(saved.getCompound("Data").orElseThrow().getList("LearnedSkills").orElseThrow().isEmpty(),
                        "Cosmetic organs must not retain role skills");
                require(restored.getEvolutionParts().getStringOr("clone_organ_role", "").equals(role),
                        "Role organ appearance lost through cloning/save-load");
                require(restored.getEvolutionParts().getStringOr("organ_loadout", "").equals(loadout),
                        "Custom organ loadout lost through cloning/save-load");
                require(parts.getIntOr("wings", 0) == 3, "Cloning changed source body");
                require(!restored.getEvolutionParts().contains("gourd_entity")
                        && !restored.getEvolutionParts().getBooleanOr("gourd_detached", false),
                        "Cultivated gourd retained donor entity/detachment");
                if (corpse) require(restored.getEvolutionParts().getBooleanOr("gourd_dead", false),
                        "Cultivation unexpectedly revived a destroyed donor organ");
                require(parts.getBooleanOr("gourd_detached", false), "Cloning recalled donor gourd");
                if (!corpse) require(!restored.getEvolutionParts().contains("wings"), "Clean-water clone inherited mutation");
            }
        }
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
