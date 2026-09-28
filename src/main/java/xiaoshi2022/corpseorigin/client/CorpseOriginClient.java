package xiaoshi2022.corpseorigin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderingRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSources;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.block.CloneChamberBlock;
import xiaoshi2022.corpseorigin.block.entity.CloneChamberBlockEntity;
import xiaoshi2022.corpseorigin.client.aps.APSInkSceneManager;
import xiaoshi2022.corpseorigin.client.aps.APSInkSceneRenderer;
import xiaoshi2022.corpseorigin.client.camera.PersistentCameraEntity;
import xiaoshi2022.corpseorigin.client.camera.PersistentCameraEntityGoal;
import xiaoshi2022.corpseorigin.client.gui.CameraBlackoutScreen;
import xiaoshi2022.corpseorigin.client.gui.CloneChamberScreen;
import xiaoshi2022.corpseorigin.client.hud.InfectionHudOverlay;
import xiaoshi2022.corpseorigin.client.hud.SkillHotbarOverlay;
import xiaoshi2022.corpseorigin.client.render.CorpsePlayerRenderHandler;
import xiaoshi2022.corpseorigin.client.render.laser.BloodLotusLaserManager;
import xiaoshi2022.corpseorigin.client.render.thunder.ThunderFxManager;
import xiaoshi2022.corpseorigin.client.renderer.blockentity.CNChessZbrsRenderer;
import xiaoshi2022.corpseorigin.client.renderer.blockentity.CloneChamberRenderer;
import xiaoshi2022.corpseorigin.client.renderer.entity.*;
import xiaoshi2022.corpseorigin.client.skin.clone.ClientSkinCache;
import xiaoshi2022.corpseorigin.event.client.ClientEntityEventHandler;
import xiaoshi2022.corpseorigin.network.*;
import xiaoshi2022.corpseorigin.registry.*;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CorpseOriginClient implements ClientModInitializer {

    /** 瀹㈡埛绔彲杞Щ韬綋鍒楄〃锛圲I 鏄剧ず鐢紝鍙惈杞婚噺淇℃伅锛?*/
    public static final java.util.List<ClientShellEntry> clientShellEntries =
            new java.util.concurrent.CopyOnWriteArrayList<>();

    /** 瀹㈡埛绔交閲忚韩浣撴潯鐩?*/
    public record ClientShellEntry(
            java.util.UUID uuid,
            java.util.UUID ownerUuid,
            String world,
            int x, int y, int z,
            float progress
    ) {
    }

    // 鉁?瀹㈡埛绔案鍏勬暟鎹紦瀛橈紙鐢?UUID 浣滀负閿級
    public static final java.util.Map<UUID, ClientCorpseData> corpseDataCache = new ConcurrentHashMap<>();

    /**
     * 鍏嬮殕韬綋鐨勮鑹插瑙傜紦瀛橈紙閿?= 韬綋 UUID锛氫粨鍐呮槸韬綋绋冲畾 UUID锛岃嫃閱掑悗鏄垎韬疄浣?UUID锛夈€?
     * <p>
     * 缈呰唨/楸奸硟绛?瑙掕壊闄勫姞楠ㄩ"鎸夎繖浠芥暟鎹湪鍒嗚韩涓庝粨鍐呭厠闅嗕汉涓婃墜缁橈紝
     * 鍜?{@link #corpseDataCache}锛堝案鍏勫楠ㄩ锛夊垎寮€瀛樻斁銆?
     */
    public record ClientCloneBody(String characterId, net.minecraft.nbt.CompoundTag evolutionParts,
                                  boolean infant, int bearArms) {
        public boolean hasTrait(String trait) {
            return this.evolutionParts != null && this.evolutionParts.getBooleanOr(trait, false);
        }
    }

    public static final java.util.Map<UUID, ClientCloneBody> cloneBodyDataCache = new ConcurrentHashMap<>();

    /**
     * Flashback 鍥炴斁涓撶敤锛氬揩鐓ч檮浠跺寘鍒拌揪鏃剁帺瀹跺疄浣撳彲鑳借繕娌￠噸寤哄畬锛?
     * 鍏堟寜 UUID 鏆傚瓨 evolution_parts 闄勪欢 NBT锛屾瘡 tick 閲嶈瘯鍥炲～銆?
     */
    public static final Map<UUID, net.minecraft.nbt.CompoundTag> pendingReplayBodies = new ConcurrentHashMap<>();

    /** 鉁?涓存椂绾㈢溂鐘舵€侊細UUID 鈫?鍓╀綑 tick */
    public static final Map<UUID, Integer> tempRedEyeTicks = new ConcurrentHashMap<>();

    /** 鉁?澶╃嚎瀹濆疂灏稿厔鍚搁鐘舵€侊細鏂芥湳鑰?UUID 鈫?姝ｅ湪鍚哥殑瀵硅薄涓庡墿浣?tick */
    public static final Map<UUID, AntennaSuck> antennaSucks = new ConcurrentHashMap<>();

    /** 涓€娆¤繘琛屼腑鐨勫惛椋燂細鐩爣瀹炰綋 id + 鍓╀綑 tick锛坽@code targetEntityId < 0} = 鐩爣鏈煡锛屽彧鎾姩鐢讳笉杞悜锛?*/
    public record AntennaSuck(int targetEntityId, int ticks, int totalTicks) {
    }

    /** 杩欎綅鐜╁鐜板湪鏄惁姝ｅ湪鍚搁锛堢洈鐢叉覆鏌撴椂璇诲畠鍐冲畾鎾笉鎾?absorb锛?*/
    public static boolean isAntennaSucking(UUID uuid) {
        AntennaSuck suck = uuid == null ? null : antennaSucks.get(uuid);
        return suck != null && suck.ticks() > 0;
    }

    /**
     * 杩欐鍚搁宸茬粡杩涜浜嗗灏?tick锛?1 = 娌″湪鍚革級銆?
     * <p>
     * 鐩旂敳娓叉煋鎷垮畠鍒ゆ柇"鐜板湪鎾埌鍔ㄧ敾鐨勫摢涓€娈? 鈥斺€?鍔ㄧ敾鍚庡崐娈碉紙鍒哄嚭鍘婚偅鍑犲抚锛夎鎶婅Е鎵?
     * 绮剧‘鎻掕繘鐩爣鑴戦棬锛屽緱鐭ラ亾杩涘害鎵嶈兘瀵逛笂鍔ㄧ敾鑷繁鐨勮妭濂忋€?
     */
    public static int getAntennaSuckElapsed(UUID casterUuid) {
        AntennaSuck suck = casterUuid == null ? null : antennaSucks.get(casterUuid);
        return suck == null ? -1 : Math.max(0, suck.totalTicks() - suck.ticks());
    }

    /** 鉁?澶╃嚎瀹濆疂灏稿厔鐨刵et.minecraft.client.resources.language.I18n.get("gui.corpseorigin.label.097")绐楀彛锛氱帺瀹?UUID 鈫?鍓╀綑 tick锛堟湇鍔＄骞挎挱杩囨潵鐨勶紝鍙奖鍝嶈〃鐜帮級 */
    public static final Map<UUID, Integer> antennaBlocks = new ConcurrentHashMap<>();

    /** 杩欎綅鐜╁鐜板湪鏄惁澶勪簬鏍兼尅鍔ㄧ敾绐楀彛锛堢洈鐢叉覆鏌撴椂璇诲畠鍐冲畾鎾笉鎾牸鎸″姩鐢伙級 */
    public static boolean isAntennaBlocking(UUID uuid) {
        Integer ticks = uuid == null ? null : antennaBlocks.get(uuid);
        return ticks != null && ticks > 0;
    }

    /**
     * 鉁?寮€鑳冨ザ銆岃強鑺辩浘銆嶇殑鏍兼尅绐楀彛锛氱帺瀹?UUID 鈫?鍓╀綑 tick锛堟湇鍔＄骞挎挱杩囨潵鐨勶紝鍙奖鍝嶈〃鐜帮級銆?
     * <p>
     * 绐楀彛鍐呰儗鍚庨偅濂?{@code niunaix} 鑳屾寕鎾?{@code parry}锛堣姳鐡ｅ紶寮€鎴愮浘锛夛紝骞堕厤鍚堟湇鍔＄
     * {@code KaiWeiNaiEventHandler} 鐨勭鐭㈠弽寮广€?
     */
    public static final Map<UUID, Integer> niunaiParries = new ConcurrentHashMap<>();

    /** 杩欎綅鐜╁鐜板湪鏄惁鍦ㄨ強鑺辩浘鏍兼尅绐楀彛鍐咃紙鑳屾寕娓叉煋鏃惰瀹冨喅瀹氭挱涓嶆挱 parry锛?*/
    public static boolean isNiunaiParrying(UUID uuid) {
        Integer ticks = uuid == null ? null : niunaiParries.get(uuid);
        return ticks != null && ticks > 0;
    }

    /**
     * 鉁?寮€鑳冨ザ銆屾嫤鑵版柀鏂€嶇獥鍙ｏ細鐜╁ UUID 鈫?鍓╀綑 tick锛堟湇鍔＄骞挎挱杩囨潵鐨勶紝鍙奖鍝嶈〃鐜帮級銆?
     * <p>
     * 绐楀彛鍐呮暣韬ā鍨嬫崲鎴?{@code niunai_link_player}锛氬厛鎾?{@code broken_off}锛堟嫤鑵版柀鏂級骞朵繚鎸侊紝
     * 鏈€鍚?{@link xiaoshi2022.corpseorigin.character.KaiWeiNai#NIUNAI_LINK_RESTORE_TICKS} 閭ｆ鎾?
     * {@code link}锛堟帴鍥烇級銆備笉姝诲垽瀹氬叏鍦ㄦ湇鍔＄銆?
     */
    public static final Map<UUID, Integer> niunaiLinks = new ConcurrentHashMap<>();

    /** 杩欎綅鐜╁鐜板湪鏄惁澶勪簬鎷﹁叞鏂╂柇绐楀彛鍐咃紙娓叉煋鏃惰瀹冨喅瀹氳涓嶈鏁磋韩鎹㈡ā鍨嬶級 */
    public static boolean isNiunaiLink(UUID uuid) {
        Integer ticks = uuid == null ? null : niunaiLinks.get(uuid);
        return ticks != null && ticks > 0;
    }

    /** 鎷﹁叞鏂╂柇杩樺墿澶氬皯 tick锛?1 = 涓嶅湪绐楀彛鍐咃級 */
    public static int niunaiLinkRemaining(UUID uuid) {
        Integer ticks = uuid == null ? null : niunaiLinks.get(uuid);
        return ticks == null || ticks <= 0 ? -1 : ticks;
    }

    /**
     * 灏稿发涔嬪瓙銆屽崈鐪间竾鐩€嶇殑鍑濊绐楀彛锛堝墿浣?tick锛夈€?
     * <p>
     * 鐢?{@code ShiChaoSpecialSyncS2C} 鍐欏叆锛屾覆鏌撴椂璇诲畠鍐冲畾瑕佷笉瑕佹敼鎾?{@code special} 鍔ㄧ敾锛?
     * 璋佹槸"琚畾浣忕殑"鏄湇鍔＄绠楃殑锛坽@code ThousandEyesHandler}锛夛紝瀹㈡埛绔彧鐪嬪姩鐢汇€?
     */
    public static final Map<UUID, Integer> shiChaoSpecials = new ConcurrentHashMap<>();

    /** 杩欎綅鐜╁鐜板湪鏄惁姝ｅ湪鏀惧崈鐪间竾鐩紙娓叉煋鏃惰瀹冨垏鍔ㄧ敾锛?*/
    public static boolean isShiChaoSpecial(UUID uuid) {
        Integer ticks = uuid == null ? null : shiChaoSpecials.get(uuid);
        return ticks != null && ticks > 0;
    }

    /**
     * 鍙栥€屾鍦ㄨ杩欎綅鐜╁鍚搁鐨勭洰鏍囧疄浣撱€嶏紝娌℃湁 / 涓嶅湪瀹㈡埛绔紙鏈姞杞姐€佸凡姝伙級鏃惰繑鍥?null銆?
     * <p>
     * 鐩旂敳娓叉煋瑕侀潬瀹冪畻瑙︽墜杞悜鐨勮搴︼紝鎵€浠ヨ繖閲屽彧鍋氳В鏋愶紝涓嶅仛浠讳綍閫昏緫鍒ゅ畾銆?
     */
    public static LivingEntity getAntennaSuckTarget(UUID casterUuid) {
        AntennaSuck suck = casterUuid == null ? null : antennaSucks.get(casterUuid);
        if (suck == null || suck.ticks() <= 0 || suck.targetEntityId() < 0) {
            return null;
        }

        var level = Minecraft.getInstance().level;
        if (level == null) {
            return null;
        }

        Entity entity = level.getEntity(suck.targetEntityId());
        return entity instanceof LivingEntity living && living.isAlive() ? living : null;
    }


    @Override
    public void onInitializeClient() {
        RealmGrowthScreen.register();
        RawMeatTooltip.init();
        OrganClient.register();
        HeartRecoveryScreen.register();
        // 1. 鎸夐敭缁戝畾
        CorpseKeyBindings.register();
        ClientPlayNetworking.registerGlobalReceiver(NestRadarPayload.TYPE, (payload, context) ->
                context.client().execute(() -> {
                    if (payload.open()) context.client().gui.setScreen(new NestRadarScreen(payload.contacts()));
                    else if (context.client().gui.screen() instanceof NestRadarScreen radar) radar.update(payload.contacts());
                }));

        // 2. 瀹炰綋娓叉煋鍣?
        EntityRendererRegistry.register(ModEntities.LOWER_LEVEL_ZB, LowerLevelZbRenderer::new);
        EntityRendererRegistry.register(ModEntities.ZISHU_ROBOT, xiaoshi2022.corpseorigin.client.renderer.entity.ZishuRobotRenderer::new);
        EntityRendererRegistry.register(ModEntities.ZISHU_ION_BALL, context -> new net.minecraft.client.renderer.entity.ThrownItemRenderer<>(context, 1.5f, true));
        EntityRendererRegistry.register(ModEntities.CORPSE_MAGGOT, xiaoshi2022.corpseorigin.client.renderer.entity.CorpseMaggotRenderer::new);
        EntityRendererRegistry.register(ModEntities.BLOOD_WING_BEAM,
                xiaoshi2022.corpseorigin.client.renderer.entity.BloodWingBeamRenderer::new);
        EntityRendererRegistry.register(ModEntities.AOTUMAN_ZB, AotumanZbRenderer::new);
        EntityRendererRegistry.register(ModEntities.JUQUE_BEAM, JuQueBeamRenderer::new);
        EntityRendererRegistry.register(ModEntities.FLYING_GREAT_SWORD, FlyingGreatSwordRenderer::new);
        EntityRendererRegistry.register(ModEntities.CLONE_AVATAR,
                context -> new CloneAvatarRenderer(context, false));
        EntityRendererRegistry.register(ModEntities.COCO_PENGUIN, CocoPenguinRenderer::new);
        EntityRendererRegistry.register(ModEntities.HAM, HamRenderer::new);
        EntityRendererRegistry.register(ModEntities.RED_FIRE_ANT, c->new xiaoshi2022.corpseorigin.client.renderer.entity.CorpseAntRenderer(c,"red_fire_ant"));
        EntityRendererRegistry.register(ModEntities.BULLET_ANT, c->new xiaoshi2022.corpseorigin.client.renderer.entity.CorpseAntRenderer(c,"bullet_ant"));
        ChapterCinematics.register();
        ClientPlayNetworking.registerGlobalReceiver(xiaoshi2022.corpseorigin.network.ChameleonDisguisePayload.Result.TYPE,(payload,context)->context.client().execute(()->{
            if(context.client().gui.screen() instanceof ChameleonDisguiseScreen screen)screen.result(payload.error());
        }));
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT.register((handler,client)->xiaoshi2022.corpseorigin.client.skin.ChameleonSkins.clear());
        EntityRendererRegistry.register(ModEntities.VAMPIRE_BAT, net.minecraft.client.renderer.entity.BatRenderer::new);
        EntityRendererRegistry.register(ModEntities.CHAPTER_BOMB, net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
        EntityRendererRegistry.register(ModEntities.GREAT_TENGU, xiaoshi2022.corpseorigin.client.renderer.entity.GreatTenguRenderer::new);
        EntityRendererRegistry.register(ModEntities.BLACK_GOLD_HEART,c->new xiaoshi2022.corpseorigin.client.renderer.entity.SkillConstructRenderer(c,"black_gold_heart"));
        EntityRendererRegistry.register(ModEntities.VINE_BIND,c->new xiaoshi2022.corpseorigin.client.renderer.entity.SkillConstructRenderer(c,"vine_bind"));
        EntityRendererRegistry.register(ModEntities.BLOOD_LOTUS_PETAL,c->new xiaoshi2022.corpseorigin.client.renderer.entity.SkillConstructRenderer(c,"blood_lotus_petal"));
        EntityRendererRegistry.register(ModEntities.BEE_WHEEL,c->new xiaoshi2022.corpseorigin.client.renderer.entity.SkillConstructRenderer(c,"bee_wheel"));
        EntityRendererRegistry.register(ModEntities.SLAUGHTER_INCARNATION,c->new xiaoshi2022.corpseorigin.client.renderer.entity.SkillConstructRenderer(c,"slaughter_incarnation"));
        EntityRendererRegistry.register(ModEntities.ZBR_GOURD,xiaoshi2022.corpseorigin.client.renderer.entity.GourdOrganRenderer::new);
        EntityRendererRegistry.register(ModEntities.SEVERED_FOREARM,c->new xiaoshi2022.corpseorigin.client.renderer.entity.SkillConstructRenderer(c,"severed_forearm"));
        EntityRendererRegistry.register(ModEntities.TIANGANG_HALO,c->new xiaoshi2022.corpseorigin.client.renderer.entity.SkillConstructRenderer(c,"tiangang_halo"));
        EntityRendererRegistry.register(ModEntities.CORPSE_FISH_EGG, CorpseFishEggRenderer::new);
        EntityRendererRegistry.register(ModEntities.ZBR_FISH, xiaoshi2022.corpseorigin.client.renderer.entity.ZbrFishRenderer::new);
        EntityRendererRegistry.register(ModEntities.COCO_ZOMBIE, CocoZombieRenderer::new);
        EntityRendererRegistry.register(ModEntities.COCO_ZOMBIE_X, CocoZombieXRenderer::new);
        EntityRendererRegistry.register(ModEntities.UNCLE, UncleRenderer::new);
        EntityRendererRegistry.register(ModEntities.ZB_WORM, ZbWormRenderer::new);
        EntityRendererRegistry.register(ModEntities.MIKU_ZB, MikuZbRenderer::new);
        EntityRendererRegistry.register(ModEntities.LEEK_PROJECTILE, LeekProjectileRenderer::new);
        EntityRendererRegistry.register(ModEntities.OSMIUM_ICE_SPEAR, OsmiumIceSpearRenderer::new);
        // 宸︽姢娉曡洘榫欑殑鑺傜鎾炵锛氶殣褰㈠疄浣擄紝鍙渶瑕佷竴涓?浠€涔堥兘涓嶇敾"鐨勭粯鍒跺櫒
        EntityRendererRegistry.register(ModEntities.GUARDIAN_PART, GuardianPartRenderer::new);
        // 灏歌洘榫欙紙宸︽姢娉?鑴辩"鍚庢斁鍑烘潵鐨勫疇鐗?BOSS锛?
        EntityRendererRegistry.register(ModEntities.ZUO_FLOOD_LONG, ZuoFloodLongRenderer::new);
        // 榛戣壊鐏嚎鍏嬮殕浠撴柟鍧楀疄浣撴覆鏌撳櫒
        BlockEntityRendererRegistry.register(
                ModBlockEntities.CLONE_CHAMBER,
                CloneChamberRenderer::new
        );
        // 灏稿厔鑲夊潡锛圙eckoLib 鍔ㄧ敾鏂瑰潡锛?
        
        // 璞℃灏稿厔锛圙eckoLib 鍔ㄧ敾鏂瑰潡瀹炰綋锛?
        BlockEntityRendererRegistry.register(
                ModBlockEntities.CN_CHESS_ZBRS,
                CNChessZbrsRenderer::new
        );

        // 3. 妯″瀷灞傛敞鍐?
        ModModelLayers.register();

        // 鉁?澶╃嚎瀹濆疂鐩旂敳鍔ㄧ敾鐢ㄥ埌鐨?query.target_*_rotation锛圙eckoLib 娌″唴缃紝寰楄嚜宸辨敞鍐岋級
        xiaoshi2022.corpseorigin.client.renderer.armor.AntennaZBRitemRenderer.registerMolangQueries();

        // 4. 娴佷綋绾圭悊
        registerFluidTextures();

        // 5. 鐜╁灏稿厔娓叉煋灞?
        CorpsePlayerRenderHandler.register();

        // 鉁?娉ㄥ唽瀹㈡埛绔敾鍑讳簨浠剁洃鍚?
        // 鉁?娉ㄥ唽瀹㈡埛绔疄浣撲簨浠?
        ClientEntityEventHandler.register();

        // 鉁?娉ㄥ唽 HUD
        InfectionHudOverlay.register();
        xiaoshi2022.corpseorigin.client.hud.ThermalHudOverlay.register();
        SkillHotbarOverlay.register();

        // 6. 缃戠粶鎺ユ敹
        ClientPlayNetworking.registerGlobalReceiver(CorpsePayloads.CharacterSyncS2C.TYPE, (payload, context) -> {
            context.client().execute(() ->
                    CharacterManagerBridge.setCharacter(payload.characterId()));
        });

        ClientPlayNetworking.registerGlobalReceiver(ShellStateSyncS2C.TYPE, (payload, context) ->
                context.client().execute(() -> {
                    CorpseOriginClient.clientShellEntries.clear();
                    for (ShellStateSyncS2C.Entry e : payload.entries()) {
                        CorpseOriginClient.clientShellEntries.add(new ClientShellEntry(
                                e.uuid(), e.ownerUuid(), e.world(),
                                e.x(), e.y(), e.z(), e.progress()));
                    }
                }));

        ClientPlayNetworking.registerGlobalReceiver(SynchronizationResponsePacket.TYPE, (payload, context) ->
                context.client().execute(() -> {
                    Minecraft client = context.client();

                    // 鏂囧瓧鎻愮ず锛堝け璐ュ師鍥犮€佹浜¤嚜鍔ㄥず鑸嶇殑鎻愮ず涔嬬被锛?
                    if (!payload.message().equals(Component.empty()) && client.player != null) {
                        client.player.sendOverlayMessage(payload.message());
                    }

                    if (!payload.success()) {
                        // 鎹㈣韩浣撳け璐ワ細鍒啀绛夎惤浣嶄簡锛屾妸闀滃ご浜よ繕缁欑帺瀹?
                        PersistentCameraEntity.unset(client);
                        return;
                    }

                    // 鍙彁绀轰笉杩囧満鐨勫寘锛堟病鏈夌洰鏍囪韩浣撴暟鎹級锛屽埌姝や负姝?
                    if (!payload.cameraCutscene()) {
                        return;
                    }

                    var player = client.player;
                    if (player == null) return;

                    // 鈽?Flashback 鍏煎锛氬洖鏀句細鎶婂綍鍒跺埌鐨勫寘鍘熸牱閲嶆斁涓€閬嶏紙鍖呮嫭杩欎釜鍚屾鍝嶅簲鍖咃級锛?
                    //   鍥炴斁涓粷涓嶈兘鎶㈢浉鏈猴紝鍚﹀垯瑙嗚浼氳鎷借繘鎰忚瘑杞Щ鍔ㄧ敾
                    if (PersistentCameraEntity.isReplayPlaying()) {
                        CorpseOrigin.LOGGER.debug("鍥炴斁涓紝璺宠繃鎰忚瘑杞Щ鐩告満杩囧満");
                        return;
                    }

                    BlockPos startPos = payload.fromPos();
                    Direction startFacing = payload.fromFacing();
                    BlockPos targetPos = payload.toPos();
                    Direction targetFacing = payload.toFacing();

                    // 鈽?鐩磋鍒嗘敮锛堝案鐜嬫崲韬細閲戣潐鑴卞３ / 琛€鑲夐噸濉?/ 鍙抽敭鍥炴棫韬綋锛夛細
                    //   鍘熷湴鍨傜洿鎶捣 鈫?90掳 鎷愬集妯潃鐢╁嚭鍘伙紝鍏ㄧ▼鐩栦竴灞傞粦鍦猴紱鎾畬鎵嶈鏈嶅姟绔湡姝ｆ崲韬?
                    if (payload.cameraStyle() == SynchronizationResponsePacket.CameraStyle.RIGHT_ANGLE) {
                        if (PersistentCameraEntity.isLocalPlayerFirstPersonView(client)) {
                            CameraBlackoutScreen.fadeIn(client);
                            PersistentCameraEntity.setup(client, PersistentCameraEntityGoal.rightAngleExit(
                                    startPos, startFacing, targetPos,
                                    __ -> finishCameraDirect(payload.targetStateUuid(), targetPos,
                                            payload.toWorld())));
                        } else {
                            // 涓嶆帴绠¤瑙掞紙鏃佽/绗笁浜虹О锛変篃蹇呴』鍥炲寘锛屽惁鍒欐湇鍔＄涓€鐩寸瓑锛岃韩浣撴案杩滄崲涓嶈繃鏉?
                            finishCameraDirect(payload.targetStateUuid(), targetPos, payload.toWorld());
                        }
                        return;
                    }

                    // 鈽?杩囧満鍙拡瀵广€屽綋鍓嶇帺瀹惰嚜宸辩殑绗竴浜虹О瑙嗚銆嶏細鐩告満琚埆浜烘帴绠★紙鏃佽/鍒囪瑙掞級鎴栫涓変汉绉版椂
                    //   涓嶆挱杩囧満锛屼絾浠嶇劧绔嬪埢鍥炲寘锛屽惁鍒欐湇鍔＄浼氫竴鐩寸瓑 CameraDonePacket锛岃韩浣撴案杩滄崲涓嶈繃鏉?
                    if (!PersistentCameraEntity.isLocalPlayerFirstPersonView(client)) {
                        finishCamera(payload.targetStateUuid(), startPos, startFacing, targetPos, targetFacing,
                                payload.toWorld());
                        return;
                    }

                    boolean sameWorld = payload.fromWorld().equals(payload.toWorld());

                    PersistentCameraEntityGoal cameraGoal = player.isDeadOrDying()
                            ? PersistentCameraEntityGoal.limbo(startPos, startFacing, targetPos,
                            __ -> finishCamera(payload.targetStateUuid(), startPos, startFacing, targetPos,
                                    targetFacing, payload.toWorld()))
                            : PersistentCameraEntityGoal.stairwayToHeaven(startPos, startFacing, targetPos,
                            __ -> finishCamera(payload.targetStateUuid(), startPos, startFacing, targetPos,
                                    targetFacing, payload.toWorld()));

                    PersistentCameraEntity.setup(client, cameraGoal);
                }));
        
        // 鉁?鎺ユ敹鐜╁灏稿厔鏁版嵁鍚屾锛堢敤 UUID锛?
        ClientPlayNetworking.registerGlobalReceiver(CorpsePayloads.PlayerCorpseSyncS2C.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                // 鈽?椤轰究鍒锋柊涓€娆＄毊鑲ょ紦瀛?
                ClientSkinCache.resolve(payload.playerUuid());

                ClientCorpseData data = new ClientCorpseData(
                        payload.isCorpse(),
                        payload.corpseType(),
                        payload.corpseData()
                );
                corpseDataCache.put(payload.playerUuid(), data);  // 鉁?鐢?UUID
                // 鈽?灏稿发涔嬪瓙浜岄樁娈电殑 30 鏍肩鎾炵鏄鍐?Player#getDimensions 寰楀埌鐨勶紙瑙?PlayerDimensionsMixin锛夛紝
                //   瀹㈡埛绔篃璇昏繖浠界紦瀛樺垽瀹氾紝鎵€浠ュ舰鎬佷竴鍙樺氨寰楄绠卞瓙閲嶇畻涓€娆★紝鍚﹀垯瀹㈡埛绔繕鍋滃湪 1.8 鏍笺€?
                if (Minecraft.getInstance().level != null) {
                    Player synced = Minecraft.getInstance().level.getPlayerByUUID(payload.playerUuid());
                    if (synced != null) {
                        synced.refreshDimensions();
                    }
                }
                CorpseOrigin.LOGGER.debug("鏀跺埌鐜╁灏稿厔鏁版嵁: uuid={}, isCorpse={}",
                        payload.playerUuid(), payload.isCorpse());
            });
        });

        // 鉁?鎺ユ敹鍏嬮殕韬綋鐨勮鑹插瑙傦紙缈呰唨/楸奸硟/瑙掕壊鏍囪锛屾寜韬綋 UUID 缂撳瓨锛?
        ClientPlayNetworking.registerGlobalReceiver(CorpsePayloads.CloneBodySyncS2C.TYPE, (payload, context) ->
                context.client().execute(() -> cloneBodyDataCache.put(payload.bodyUuid(),
                        new ClientCloneBody(payload.characterId(), payload.evolutionParts(),
                                payload.infant(), payload.bearArms()))));

        // 鉁?Flashback 鍥炴斁锛氬揩鐓цˉ鍙戠殑 evolution_parts 闄勪欢锛屾寜 UUID 鍥炲～鍒伴噸寤哄嚭鐨勭帺瀹跺疄浣?
        ClientPlayNetworking.registerGlobalReceiver(CorpsePayloads.ReplayPlayerBodyS2C.TYPE, (payload, context) ->
                context.client().execute(() -> applyReplayBody(payload.playerUuid(), payload.body())));

        // 鉁?鎺ユ敹杩涘寲/宸插鎶€鑳藉悓姝?
        ClientPlayNetworking.registerGlobalReceiver(CorpsePayloads.EvolutionSyncS2C.TYPE, (payload, context) ->
                context.client().execute(() ->
                        ClientState.applyEvolution(
                                payload.earnedPoints(),
                                payload.availablePoints(),
                                payload.kills(),
                                payload.learnedSkills(), payload.level(), payload.pointsToNext())));

        // 鉁?鎺ユ敹鎶€鑳藉喎鍗村悓姝?
        ClientPlayNetworking.registerGlobalReceiver(CorpsePayloads.CooldownSyncS2C.TYPE, (payload, context) ->
                context.client().execute(() ->
                        ClientState.applyCooldown(payload.skillPath(), payload.ticks())));

        ClientPlayNetworking.registerGlobalReceiver(CorpsePayloads.InfectionSyncS2C.TYPE, (payload, context) ->
                context.client().execute(() ->
                        ClientState.infection = payload.infection()));

        // 鉁?鎺ユ敹鍐呭姏鍚屾
        ClientPlayNetworking.registerGlobalReceiver(CorpsePayloads.InnerPowerSyncS2C.TYPE, (payload, context) ->
                context.client().execute(() -> {
                    ClientState.innerPower = payload.current();
                    ClientState.maxInnerPower = payload.max();
                }));

        ClientPlayNetworking.registerGlobalReceiver(CorpsePayloads.TempRedEyeSyncS2C.TYPE, (payload, context) ->
                context.client().execute(() ->
                        tempRedEyeTicks.put(payload.playerUuid(), payload.durationTicks())));

        // 鉁?澶╃嚎瀹濆疂灏稿厔鍚搁鐘舵€侊紙0 鍙婁互涓?= 绔嬪埢缁撴潫锛岀敤浜庤鎵撴柇锛?
        ClientPlayNetworking.registerGlobalReceiver(CorpsePayloads.AntennaSuckSyncS2C.TYPE, (payload, context) ->
                context.client().execute(() -> {
                    if (payload.durationTicks() <= 0) {
                        antennaSucks.remove(payload.playerUuid());
                    } else {
                        antennaSucks.put(payload.playerUuid(),
                                new AntennaSuck(payload.targetEntityId(), payload.durationTicks(),
                                        payload.durationTicks()));
                    }
                }));

        // 鉁?澶╃嚎瀹濆疂灏稿厔銆屾牸鎸°€嶅姩鐢讳俊鍙凤紙0 鍙婁互涓?= 绔嬪埢缁撴潫锛?
        ClientPlayNetworking.registerGlobalReceiver(CorpsePayloads.AntennaBlockSyncS2C.TYPE, (payload, context) ->
                context.client().execute(() -> {
                    if (payload.durationTicks() <= 0) {
                        antennaBlocks.remove(payload.playerUuid());
                    } else {
                        antennaBlocks.put(payload.playerUuid(), payload.durationTicks());
                    }
                }));

        // 鉁?寮€鑳冨ザ銆岃強鑺辩浘銆嶆牸鎸＄獥鍙ｏ紙0 鍙婁互涓?= 绔嬪埢缁撴潫锛?
        ClientPlayNetworking.registerGlobalReceiver(CorpsePayloads.NiunaiParrySyncS2C.TYPE, (payload, context) ->
                context.client().execute(() -> {
                    if (payload.durationTicks() <= 0) {
                        niunaiParries.remove(payload.playerUuid());
                    } else {
                        niunaiParries.put(payload.playerUuid(), payload.durationTicks());
                    }
                }));

        // 鉁?寮€鑳冨ザ銆屾嫤鑵版柀鏂€嶈〃鐜扮獥鍙ｏ紙0 鍙婁互涓?= 绔嬪埢缁撴潫锛?
        ClientPlayNetworking.registerGlobalReceiver(CorpsePayloads.NiunaiLinkSyncS2C.TYPE, (payload, context) ->
                context.client().execute(() -> {
                    if (payload.durationTicks() <= 0) {
                        niunaiLinks.remove(payload.playerUuid());
                    } else {
                        niunaiLinks.put(payload.playerUuid(), payload.durationTicks());
                    }
                }));

        // 鉁?灏稿发涔嬪瓙銆屽崈鐪间竾鐩€嶅嚌瑙嗙獥鍙ｏ紙0 鍙婁互涓?= 绔嬪埢缁撴潫锛?
        ClientPlayNetworking.registerGlobalReceiver(CorpsePayloads.ShiChaoSpecialSyncS2C.TYPE, (payload, context) ->
                context.client().execute(() -> {
                    if (payload.durationTicks() <= 0) {
                        shiChaoSpecials.remove(payload.playerUuid());
                    } else {
                        shiChaoSpecials.put(payload.playerUuid(), payload.durationTicks());
                    }
                }));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (CorpseKeyBindings.openSkillWheel.consumeClick()) {
                // 1. 鍛ㄥ洿鏈夊厠闅嗕粨 鈫?鎵撳紑鍏嬮殕浠?UI
                BlockPos nearby = findNearbyCloneChamber();
                if (nearby != null) {
                    client.gui.setScreen(new CloneChamberScreen(nearby));
                    continue;
                }

                // 2. 鍚﹀垯璧版妧鑳借疆鐩?
                if (client.gui.screen() instanceof SkillWheelScreen) {
                    client.gui.setScreen(null);
                } else if (client.gui.screen() == null) {
                    client.gui.setScreen(new SkillWheelScreen());
                }
            }

            // 鎶€鑳芥爲淇濇寔"鎸変竴涓嬫墦寮€"
            while (CorpseKeyBindings.openSkillTree.consumeClick()) {
                client.gui.setScreen(new SkillTreeScreen());
            }
            while (CorpseKeyBindings.toggleHud.consumeClick()) {
                if (client.player != null && client.hitResult instanceof net.minecraft.world.phys.BlockHitResult hit
                        && client.level != null
                        && client.level.getBlockState(hit.getBlockPos()).is(xiaoshi2022.corpseorigin.registry.ModBlocks.ZBR_FLESH)) {
                    ClientPlayNetworking.send(new CorpsePayloads.CorpseNestTeleportC2S(hit.getBlockPos()));
                    continue;
                }
                ClientState.hudVisible = !ClientState.hudVisible;
                if (Minecraft.getInstance().player != null) {
                    Minecraft.getInstance().player.sendOverlayMessage(
                            Component.translatable(ClientState.hudVisible
                                    ? "hud.corpseorigin.toggle.on"
                                    : "hud.corpseorigin.toggle.off")
                    );
                }
            }

            while (CorpseKeyBindings.openHudSettings.consumeClick()) {
                if (client.gui.screen() == null) {
                    client.gui.setScreen(new HudSettingsScreen());
                }
            }
            while(CorpseKeyBindings.openOrganEditor.consumeClick()) {
                if(client.gui.screen()==null && client.player!=null)client.gui.setScreen(new OrganEditorScreen());
            }
            while(CorpseKeyBindings.organAbility.consumeClick()){
                if(client.gui.screen()==null&&client.player!=null)ClientPlayNetworking.send(new xiaoshi2022.corpseorigin.growth.OrganEvolutionPayload("",client.player.isShiftKeyDown()?"water_drink":"water_fire"));
            }

            for (int slot = 0; slot < CorpseKeyBindings.quickSkills.length; slot++) {
                while (CorpseKeyBindings.quickSkills[slot].consumeClick()) {
                    if (client.gui.screen() == null && client.player != null) {
                        var skill = SkillHotbarState.getSkill(slot);
                        if (skill != null) {
                            ChameleonDisguiseScreen.activate(skill.getId().getPath());
                        }
                    }
                }
            }

            // 鉁?绾㈢溂璁℃椂鑷噺锛堝畨鍏ㄥ啓娉曪級
            if (!tempRedEyeTicks.isEmpty()) {
                tempRedEyeTicks.replaceAll((k, v) -> v - 1);
                tempRedEyeTicks.entrySet().removeIf(e -> e.getValue() <= 0);
            }

            // 鉁?Flashback 鍥炴斁锛氬疄浣撻噸寤哄彲鑳芥櫄浜庡揩鐓ч檮浠跺寘锛屾瘡 tick 灏濊瘯鎶婃殏瀛樼殑闄勪欢鍥炲～
            if (!pendingReplayBodies.isEmpty() && client.level != null) {
                pendingReplayBodies.entrySet().removeIf(e -> {
                    Player player = client.level.getPlayerByUUID(e.getKey());
                    if (player == null) return false;
                    player.setAttached(xiaoshi2022.corpseorigin.growth.SurvivalGrowth.BODY, e.getValue());
                    return true;
                });
            }

            // 鉁?鍚搁璁℃椂鑷噺
            if (!antennaSucks.isEmpty()) {
                antennaSucks.replaceAll((k, v) ->
                        new AntennaSuck(v.targetEntityId(), v.ticks() - 1, v.totalTicks()));
                antennaSucks.entrySet().removeIf(e -> e.getValue().ticks() <= 0);
            }

            // 鉁?鏍兼尅绐楀彛璁℃椂鑷噺
            if (!antennaBlocks.isEmpty()) {
                antennaBlocks.replaceAll((k, v) -> v - 1);
                antennaBlocks.entrySet().removeIf(e -> e.getValue() <= 0);
            }

            // 鉁?寮€鑳冨ザ鑿婅姳鐩剧獥鍙ｈ鏃惰嚜鍑?
            if (!niunaiParries.isEmpty()) {
                niunaiParries.replaceAll((k, v) -> v - 1);
                niunaiParries.entrySet().removeIf(e -> e.getValue() <= 0);
            }

            // 鉁?寮€鑳冨ザ鎷﹁叞鏂╂柇绐楀彛璁℃椂鑷噺锛堝噺鍒?0 灏辫嚜鐒跺垏鍥炴櫘閫氭ā鍨嬶級
            if (!niunaiLinks.isEmpty()) {
                niunaiLinks.replaceAll((k, v) -> v - 1);
                niunaiLinks.entrySet().removeIf(e -> e.getValue() <= 0);
            }

            // 鉁?灏稿发涔嬪瓙鍗冪溂涓囩洰绐楀彛璁℃椂鑷噺锛堝噺鍒?0 灏卞垏鍥?idle / walk锛?
            if (!shiChaoSpecials.isEmpty()) {
                shiChaoSpecials.replaceAll((k, v) -> v - 1);
                shiChaoSpecials.entrySet().removeIf(e -> e.getValue() <= 0);
            }
        });

        // 娉ㄥ唽婵€鍏?+ 姘村ⅷ娓叉煋
        LevelRenderEvents.COLLECT_SUBMITS.register(context -> {
            PoseStack poseStack = context.poseStack();
            SubmitNodeCollector collector = context.submitNodeCollector();
            xiaoshi2022.corpseorigin.client.render.QiAuraRenderer.render(poseStack,collector);
            xiaoshi2022.corpseorigin.client.render.SwordImpactRenderer.render(poseStack,collector);
            BloodLotusLaserManager.getInstance().render(poseStack, collector);
            // 鉁?灏哥帇闆风數锛堢传鑹诧級
            ThunderFxManager.getInstance().render(poseStack, collector);

            // 鉁?姘村ⅷ鎰忓
            Minecraft mc = Minecraft.getInstance();
            if (mc.gameRenderer != null && mc.gameRenderer.mainCamera() != null) {
                Vec3 cameraPos = mc.gameRenderer.mainCamera().position();
                APSInkSceneRenderer.render(poseStack, collector, cameraPos);
            }
        });

// 鉁?鎺ユ敹澶氱洰鏍囬摼鏉″寘
        ClientPlayNetworking.registerGlobalReceiver(BloodLotusLaserMultiPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                BloodLotusLaserManager.getInstance().addChains(
                        payload.getStart(),
                        payload.targetUuids(),
                        payload.durationTicks()
                );
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(TianGangBeamPayload.TYPE, (payload, context) ->
                context.client().execute(() -> xiaoshi2022.corpseorigin.client.render.laser.TianGangBeamState.accept(payload)));

        xiaoshi2022.corpseorigin.client.render.SwordImpactRenderer.register();
        ClientPlayNetworking.registerGlobalReceiver(xiaoshi2022.corpseorigin.network.SwordImpactPayload.TYPE,(payload,context)->context.client().execute(()->xiaoshi2022.corpseorigin.client.render.SwordImpactRenderer.accept(payload)));
        ClientPlayNetworking.registerGlobalReceiver(QiAuraPayload.TYPE,(payload,context)->context.client().execute(()->
                xiaoshi2022.corpseorigin.client.render.QiAuraRenderer.accept(payload)));
        ClientPlayNetworking.registerGlobalReceiver(BloodLotusAuraPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                var entity=context.client().level==null?null:context.client().level.getEntity(payload.playerUuid());
                if(entity!=null)xiaoshi2022.corpseorigin.client.render.QiAuraRenderer.accept(new QiAuraPayload(
                        entity.getId(),"lotus_lamp",entity.position(),0xcc184f,2,payload.durationTicks()));
            });
        });

        // 鉁?鎺ユ敹绱壊闆风數鐗规晥锛堣惤闆?/ 鐞冪姸闂數鐢靛姬锛?
        ClientPlayNetworking.registerGlobalReceiver(CorpsePayloads.ThunderBoltFxS2C.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                ThunderFxManager.getInstance().addBolt(
                        payload.getFrom(),
                        payload.getTo(),
                        payload.durationTicks(),
                        payload.width()
                );
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(APSInkScenePayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                if (payload.open()) {
                    APSInkSceneManager.open(
                            payload.casterId(),
                            payload.x(), payload.y(), payload.z(),
                            payload.riverDirX(), payload.riverDirZ());
                } else {
                    APSInkSceneManager.close();
                }
            });
        });
        // 鉁?鏂板锛氭帴鏀惰瘲鐗岃惤涓?
        ClientPlayNetworking.registerGlobalReceiver(APSInkPoemPayload.TYPE, (payload, context) -> {
            context.client().execute(() ->
                    APSInkSceneManager.dropPoem(payload.lineIndex()));
        });

// 姣?tick 鏇存柊
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            BloodLotusLaserManager.getInstance().tick();
            xiaoshi2022.corpseorigin.client.render.laser.TianGangBeamState.tick();
            xiaoshi2022.corpseorigin.client.render.QiAuraRenderer.tick();
            ThunderFxManager.getInstance().tick();
        });

        CorpseOrigin.LOGGER.debug("CorpseOrigin client initialized");
    }

    private static void finishCamera(java.util.UUID targetUuid, BlockPos startPos, Direction startFacing,
                                     BlockPos targetPos, Direction targetFacing, Identifier targetWorld) {
        // 绗竴娈碉紙鐏甸瓊涓婂ぉ锛夋斁瀹岋細閫氱煡鏈嶅姟绔紑濮嬫崲韬綋锛岄暅澶村厛鐣欏湪澶╀笂锛?
        // 绛夌帺瀹剁湡姝ｈ惤鍒版柊韬綋鍚庡啀鎾浜屾銆屼笅钀介檮韬€嶏紙瑙?beginHandoff锛?
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(
                new CameraDonePacket(targetUuid));
        PersistentCameraEntity.beginHandoff(startPos, startFacing, targetPos, targetFacing, targetWorld);
    }

    /**
     * 鐩磋鍒嗘敮锛堝案鐜嬫崲韬級鐨勬敹灏撅細闀滃ご涓嶉澶╀篃涓嶄笅钀?鈥斺€?鍙洖鍖呰鏈嶅姟绔崲韬紝
     * 鐒跺悗绛夌帺瀹惰惤鍒扮洰鏍囦綅缃紝鍦ㄩ粦骞曢噷鎶婅瑙掍氦杩橈紙瑙?{@code beginDirectRelease}锛夈€?
     */
    private static void finishCameraDirect(java.util.UUID targetUuid, BlockPos targetPos, Identifier targetWorld) {
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(
                new CameraDonePacket(targetUuid));
        PersistentCameraEntity.beginDirectRelease(targetPos, targetWorld);
    }

    /** 鎵剧帺瀹跺懆鍥?3 鏍煎唴鏈€杩戠殑鍏嬮殕浠擄紙鍙涓嬪崐鏍硷級锛岃繑鍥炲叾鏂瑰潡鍧愭爣 */
    public static BlockPos findNearbyCloneChamber() {
        var player = Minecraft.getInstance().player;
        if (player == null || player.level() == null) {
            return null;
        }

        BlockPos origin = player.blockPosition();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int dx = -3; dx <= 3; dx++) {
            for (int dy = -3; dy <= 3; dy++) {
                for (int dz = -3; dz <= 3; dz++) {
                    pos.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    if (player.level().getBlockEntity(pos)
                            instanceof CloneChamberBlockEntity chamber
                            && CloneChamberBlock.isLower(chamber.getBlockState())) {
                        return chamber.getBlockPos().immutable();
                    }
                }
            }
        }
        return null;
    }

    // ==================== 瀹㈡埛绔暟鎹被 ====================

    /**
     * Flashback 鍥炴斁锛氭妸蹇収鎼哄甫鐨?evolution_parts 闄勪欢 NBT 鍥炲～缁欐寚瀹?UUID 鐨勭帺瀹躲€?
     * <p>
     * 蹇収鐨勮嚜瀹氫箟鍖呭湪 viewer 鐨?sendLevelInfo 闃舵缁熶竴閫佽揪锛岀粷澶у鏁扮帺瀹跺疄浣撻噸寤哄凡瀹屾垚锛?
     * 涓囦竴杩樻病瑙佸埌瀹炰綋锛堟湰鍦扮帺瀹剁殑鍒涘缓鍖呴『搴忎笉鍚岋級锛屽厛鏀捐繘 {@link #pendingReplayBodies}
     * 鐢卞鎴风 tick 缁х画灏濊瘯銆?
     */
    public static void applyReplayBody(UUID uuid, net.minecraft.nbt.CompoundTag body) {
        if (uuid == null || body == null) return;
        Minecraft client = Minecraft.getInstance();
        Player player = client.level == null ? null : client.level.getPlayerByUUID(uuid);
        if (player != null) {
            player.setAttached(xiaoshi2022.corpseorigin.growth.SurvivalGrowth.BODY, body);
            pendingReplayBodies.remove(uuid);
        } else {
            pendingReplayBodies.put(uuid, body);
        }
    }

    private static void registerFluidTextures() {
        Identifier stillId = Identifier.fromNamespaceAndPath(CorpseOrigin.MOD_ID, "block/infected_water_still");
        Identifier flowingId = Identifier.fromNamespaceAndPath(CorpseOrigin.MOD_ID, "block/infected_water_flow");
        Identifier overlayId = Identifier.fromNamespaceAndPath(CorpseOrigin.MOD_ID, "block/infected_water_overlay");

        Material still = new Material(stillId);
        Material flowing = new Material(flowingId);
        Material overlay = new Material(overlayId);

        FluidRenderingRegistry.register(
                ModFluids.INFECTED_WATER,
                ModFluids.FLOWING_INFECTED_WATER,
                new FluidModel.Unbaked(
                        still,
                        flowing,
                        overlay,
                        BlockTintSources.constant(ARGB.opaque(0x884422))
                )
        );

        CorpseOrigin.LOGGER.debug("Infected water textures registered");
    }
}
