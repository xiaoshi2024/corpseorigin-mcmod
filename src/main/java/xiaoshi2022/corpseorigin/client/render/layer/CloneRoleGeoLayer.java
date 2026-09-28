package xiaoshi2022.corpseorigin.client.render.layer;

import com.geckolib.constant.DataTickets;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntitySpawnReason;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.character.KaiWeiNai;
import xiaoshi2022.corpseorigin.client.CorpseOriginClient;
import xiaoshi2022.corpseorigin.client.OrganClient;
import xiaoshi2022.corpseorigin.client.renderer.player.*;
import xiaoshi2022.corpseorigin.entity.CloneAvatarEntity;
import xiaoshi2022.corpseorigin.growth.OrganDefinition;
import xiaoshi2022.corpseorigin.growth.OrganLibrary;
import xiaoshi2022.corpseorigin.growth.OrganSlot;
import xiaoshi2022.corpseorigin.growth.SurvivalGrowth;
import xiaoshi2022.corpseorigin.registry.ModEntities;
import xiaoshi2022.corpseorigin.skill.chapter.ChapterActorState;
import xiaoshi2022.corpseorigin.skill.chapter.ChapterScenes;
import xiaoshi2022.corpseorigin.skill.chapter.CreatureAbilities;

import java.util.UUID;

/**
 * 克隆分身的「角色专属外观」层：把玩家身上那套 GEO 角色模型也画到分身上。
 * <p>
 * 覆盖两类（判定与玩家侧一致，数据都来自分身已同步的附件 / 网络缓存）：
 * <ol>
 *   <li><b>角色全身模型</b>（小金刚尸兄 {@code jingang_zb}、金刚婴儿、青蛙尸兄、虎姐、虫母……）——
 *       与玩家一样是<b>整身替换</b>：接管 submit，原版模型 / 盔甲 / 其他层都不画；</li>
 *   <li><b>开胃奶背挂</b>（{@code niunaix}：触角 / 捆仙索 / 菊花盾）—— 补画一层，原版模型照常渲染。</li>
 * </ol>
 * <p>
 * 为什么不是"再复制一个玩家渲染层"：玩家侧这些外观走的是 {@code GeoReplacedEntityRenderer}
 * 通道（整身替换 / 单独补画），不是 {@code RenderLayer}，所以复制层机制拿不到它们 ——
 * 只能在分身自己的渲染器里补一条等价管线。
 * <p>
 * 动画快照与玩家侧同路：extract 阶段把 ticket 写进一个独立子 state 并当场求值控制器，
 * submit 阶段直接提交这份 state。求值用的动画宿主是分身实体自身
 * （见 {@code CloneAvatarGeoAnimatableMixin}），与玩家的控制器各算各的、互不干扰。
 */
@Environment(EnvType.CLIENT)
public final class CloneRoleGeoLayer extends RenderLayer<AvatarRenderState, PlayerModel> {

    /** 葫芦小金刚的角色 id（葫芦骨骼只属于他） */
    private static final String GOURD_ROLE = "xiaojingang";

    /** 诊断用：每个分身只打一次"这具身体识别成了什么角色" */
    private static final java.util.Set<UUID> DIAGNOSED = new java.util.HashSet<>();

    /** 诊断用：每个分身每种外观只打一次"真的提交渲染了" */
    private static final java.util.Set<String> DIAGNOSED_SUBMIT = new java.util.HashSet<>();
    private static final java.util.Set<String> REPORTED_FAILURES = new java.util.HashSet<>();

    private static void reportFailure(String part, Exception error) {
        if (REPORTED_FAILURES.size() < 64 && REPORTED_FAILURES.add(part))
            CorpseOrigin.LOGGER.warn("Clone organ render failed: {}", part, error);
    }

    /** 这一帧要画的自定义器官（帧列表：槽位 + 渲染器 + 各自的子 render state） */
    public static final com.geckolib.constant.dataticket.DataTicket<java.util.List> SNAPSHOT_ORGAN =
            com.geckolib.constant.dataticket.DataTicket.create("clone_role_organ", java.util.List.class);

    /** 一个器官帧：槽位参数（决定挂在哪个骨骼、偏移多少）+ 渲染器 + 它自己的动画状态 */
    public static final com.geckolib.constant.dataticket.DataTicket<AvatarRenderState> SNAPSHOT_GOURD =
            com.geckolib.constant.dataticket.DataTicket.create("clone_gourd_snapshot", AvatarRenderState.class);

    public record OrganFrame(OrganSlot slot, CloneOrganRenderer renderer, AvatarRenderState state) {}

    /** 提交侧确证日志：能区分"根本没走到提交"和"提交了但看不见" */
    private static void diagnoseSubmit(AvatarRenderState state, String part) {
        if (state.id != 0 && DIAGNOSED_SUBMIT.add(part + ":" + state.id)) {
            CorpseOrigin.LOGGER.info("[CorpseOrigin] 克隆体外观提交: entityId={}, {}", state.id, part);
        }
    }

    public CloneRoleGeoLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent) {
        super(parent);
    }

    // ==================== 提取（每帧一次，由 CloneAvatarRenderer 调用） ====================

    /** Extract role appearance and independent snapshots for each attached organ. */
    public static void extract(CloneAvatarEntity entity, AvatarRenderState parent, float partialTick) {
        // 先显式关掉三条门控：读到 null 时控制器会"原样 CONTINUE"，
        // 上一条动画会残留在共享的快照里被别的模型读到（与玩家侧同样的坑）
        parent.addGeckolibData(CreaturePlayerRenderer.MODEL, "");
        parent.addGeckolibData(NiunaiXRenderData.ACTIVE, false);
        parent.addGeckolibData(CloneGourdRenderer.CLIP, "");
        parent.addGeckolibData(SNAPSHOT_GOURD, null);
        parent.addGeckolibData(SNAPSHOT_ORGAN, java.util.List.of());
        if (parent.isInvisible) {
            return;
        }
        String role = entity.getAttachedOrCreate(ChapterActorState.ROLE);
        String organRole = organRole(entity.getAttachedOrCreate(SurvivalGrowth.BODY), role);
        String action = entity.getAttachedOrCreate(ChapterScenes.ACTION);
        boolean acting = entity.swinging || !action.isEmpty();
        extractOrgans(entity, parent, partialTick);

        // ① 角色全身模型（整身替换：小金刚尸兄 / 金刚婴儿 / 青蛙尸兄……）
        String modelId = creatureModelFor(role, entity.getAttachedOrCreate(CreatureAbilities.INFANT));
        CloneCreatureRenderer creature = CloneCreatureRenderer.get(modelId);
        if (creature != null) {
            parent.addGeckolibData(CreaturePlayerRenderer.MODEL, modelId);
            parent.addGeckolibData(CreaturePlayerRenderer.CLIP, clipFor(entity, parent));
            parent.addGeckolibData(CreaturePlayerRenderer.ARMS, entity.getAttachedOrCreate(CreatureAbilities.BEAR_ARMS));
            parent.addGeckolibData(CreaturePlayerRenderer.INFANT, entity.getAttachedOrCreate(CreatureAbilities.INFANT));
            creature.extractRenderState(entity, parent, partialTick);
            diagnose(entity, role, modelId, false, false);
            return;
        }

        // ② 开胃奶背挂：这具身体的"外观角色"就是开胃奶 → 背挂（触角 / 捆仙索 / 菊花盾）是她的标志外观。
        //    ⚠️ 这里只认角色，不再额外要求尸兄数据里那套变种号：清水培育出来的身体 is_corpse=false、
        //       变种也跟着失效，但"看起来是开胃奶"这一点没变 —— 按玩家侧那套变种判定会把背挂整个漏掉。
        boolean backMount = false;
        CloneNiunaiRenderer niunai = CloneNiunaiRenderer.get();
        if (niunai != null && KaiWeiNai.ID.equals(organRole)) {
            parent.addGeckolibData(NiunaiXRenderData.ACTIVE, true);
            parent.addGeckolibData(NiunaiXRenderData.ATTACKING, acting);
            parent.addGeckolibData(NiunaiXRenderData.PARRYING, entity.getAttachedOrCreate(SurvivalGrowth.BODY)
                    .getLongOr("clone_niunai_shield_until", 0) > entity.level().getGameTime());
            parent.addGeckolibData(NiunaiXRenderData.AGE_TICKS, parent.ageInTicks);
            niunai.extractRenderState(entity, parent, partialTick);
            backMount = true;
        }

        // ③ 葫芦（葫芦小金刚背上那套骨骼）：玩家侧它是挂 body 上的"器官"，这里用同一份模型 / 动画复刻
        boolean gourd = false;
        CloneGourdRenderer gourdRenderer = CloneGourdRenderer.get();
        if (gourdRenderer != null && gourdVisible(entity)) {
            int form = gourdForm(entity);
            // 正在攻击 / 放技能：切到"发力"形态，附加骨骼跟着动起来
            AvatarRenderState gourdState = new AvatarRenderState();
            int activeForm = form != 0 ? form : acting ? 6 : 0;
            gourdState.addGeckolibData(CloneGourdRenderer.COLOR, gourdColor(activeForm));
            gourdState.addGeckolibData(CloneGourdRenderer.CLIP, gourdClip(activeForm, false));
            gourdRenderer.extractRenderState(entity, gourdState, partialTick);
            parent.addGeckolibData(SNAPSHOT_GOURD, gourdState);
            gourd = true;
        }

        // ④ 自定义器官：数据来自这具身体的 BODY 快照（培育 / 存身时抓下来的装配）。
        //    每个器官一个独立子 state —— 它们各自有模型与动画，共用一份 state 会互相串台
        //    （玩家侧也是这么做的：每个器官一个 render state + 一个独立动画管理器）。


        diagnose(entity, role, null, backMount, gourd);
    }

    /**
     * 解析这具身体 BODY 快照里的器官装配，逐个提取成帧。
     * <p>
     * 判定刻意<b>不做</b>玩家侧那套"创意模式 / 器官等级"校验 —— 快照里的装配本来就是玩家
     * 装配好并保存下来的，直接照画即可。
     */
    private static void extractOrgans(CloneAvatarEntity entity, AvatarRenderState parent, float partialTick) {
        if (!CloneOrganRenderer.ready()) {
            return;
        }
        var body = entity.getAttachedOrCreate(SurvivalGrowth.BODY);
        String saved = body.getStringOr(OrganLibrary.BODY_KEY, "");
        if (saved.isEmpty()) {
            return;
        }
        java.util.List<OrganSlot> slots;
        try {
            slots = OrganLibrary.parseSlots(saved);
        } catch (Exception e) {
            reportFailure("loadout", e);
            return;   // 失效的装配直接忽略
        }
        var resources = Minecraft.getInstance().getResourceManager();
        java.util.List<OrganFrame> frames = new java.util.ArrayList<>();
        String motion = entity.isInWater() ? "swim"
                : entity.swinging ? "attack"
                : !entity.onGround() ? (entity.isShiftKeyDown() ? "glide" : "fly")
                : entity.isShiftKeyDown() ? "crouch"
                : parent.walkAnimationSpeed > 0.02F ? "walk" : "idle";
        for (int i = 0; i < slots.size(); i++) {
            OrganSlot slot = slots.get(i);
            OrganDefinition def = OrganClient.catalog.stream()
                    .filter(d -> d.id().equals(slot.organ())).findFirst().orElse(null);
            if (def == null) {
                continue;
            }
            // 资源缺任一文件就跳过这只器官，不影响其他器官
            if (resources.getResource(Identifier.parse(def.model())).isEmpty()
                    || resources.getResource(Identifier.parse(def.texture())).isEmpty()
                    || resources.getResource(Identifier.parse(def.animation())).isEmpty()) {
                continue;
            }
            CloneOrganRenderer renderer = CloneOrganRenderer.get(def, i);
            if (renderer == null) {
                continue;
            }
            AvatarRenderState sub = new AvatarRenderState();
            sub.addGeckolibData(CustomOrganLayer.CLIP,
                    def.clips().getOrDefault(motion, def.clips().get("idle")));
            sub.addGeckolibData(DataTickets.PACKED_LIGHT, parent.lightCoords);
            try {
                renderer.extractRenderState(entity, sub, partialTick);
                frames.add(new OrganFrame(slot, renderer, sub));
            } catch (Exception error) {
                reportFailure("extract:" + def.id(), error);
            }
        }
        if (!frames.isEmpty()) {
            parent.addGeckolibData(SNAPSHOT_ORGAN, java.util.List.copyOf(frames));
        }
    }

    /** 每具分身只报一次"识别成了什么角色、画了什么"，方便确认整条管线有没有跑。 */
    private static void diagnose(CloneAvatarEntity entity, String role, String modelId,
                                boolean backMount, boolean gourd) {
        if (role == null || role.isEmpty() || !DIAGNOSED.add(entity.getUUID())) {
            return;   // 角色还没同步过来时先不记，等真正识别出角色再报
        }
        CorpseOrigin.LOGGER.info("[CorpseOrigin] 克隆体角色外观: uuid={}, role='{}', 全身模型={}, 背挂={}, 葫芦={}",
                entity.getUUID(), role, modelId == null ? "-" : modelId, backMount, gourd);
    }

    // ==================== 葫芦（小金刚）判定 ====================

    /** 这具分身该不该画葫芦：角色是小金刚，且葫芦没被放出去 / 没被打死（与玩家侧同一口径）。 */
    private static String organRole(net.minecraft.nbt.CompoundTag body, String role) {
        return body.getStringOr(xiaoshi2022.corpseorigin.shell.CharacterShellStateComponent.ORGAN_ROLE_KEY, role);
    }

    private static boolean gourdVisible(CloneAvatarEntity entity) {
        if (!GOURD_ROLE.equals(organRole(entity.getAttachedOrCreate(SurvivalGrowth.BODY),
                entity.getAttachedOrCreate(ChapterActorState.ROLE)))) {
            return false;
        }
        var body = entity.getAttachedOrCreate(SurvivalGrowth.BODY);
        return !body.getBooleanOr("gourd_detached", false) && !body.getBooleanOr("gourd_dead", false);
    }

    /** 葫芦形态：与 {@code GourdOrganState#form} 同规则（超过维持时间就回到 0）。 */
    private static int gourdForm(CloneAvatarEntity entity) {
        return gourdForm(entity.getAttachedOrCreate(SurvivalGrowth.BODY), entity.level().getGameTime());
    }

    private static int gourdForm(net.minecraft.nbt.CompoundTag body, long gameTime) {
        return gameTime < body.getLongOr("gourd_until", 0L) ? body.getIntOr("gourd_form", 0) : 0;
    }

    /** 贴图颜色：与 {@code GourdOrganState#color} 同表 */
    private static String gourdColor(int form) {
        return switch (form) {
            case 2, 6 -> "gold";
            case 4 -> "purple";
            case 5 -> "red";
            default -> "dark";
        };
    }

    /** 播哪条 clip：与 {@code GourdOrganState#clip} 同表 */
    private static String gourdClip(int form, boolean detached) {
        return switch (form) {
            case 1 -> "eyez";
            case 2 -> "hand";
            case 3 -> "snake";
            case 4 -> "snake_whater";
            case 5 -> "snake_fire";
            case 6 -> "power";
            default -> detached ? "snake" : "idle";
        };
    }

    /**
     * 分身这具身体用的是哪个角色全身模型；没有就返回 null。
     * <p>
     * 与 {@link CreaturePlayerRenderer#extract} 同一套映射，去掉只有玩家才会遇到的
     * 伪装 / 夺舍人类形态那几支 —— 分身不参与那些流程。
     */
    private static String creatureModelFor(String role, boolean infant) {
        if (role == null || role.isEmpty()) {
            return null;
        }
        String id = "chongqun".equals(role) ? "bullet_ant" : role;
        if ("jingang_zb".equals(role) && infant) {
            id = "jingang_infant";
        }
        return CloneCreatureRenderer.has(id) ? id : null;
    }

    /** 播哪条 clip：优先角色动作（施法 / 冲锋 / 扑击），否则按受击 / 挥击 / 走动 / 待机。 */
    private static String clipFor(CloneAvatarEntity entity, AvatarRenderState parent) {
        String action = entity.getAttachedOrCreate(ChapterScenes.ACTION);
        return switch (action) {
            case "cast", "threat" -> "cast";
            case "charge" -> "charge";
            case "pounce" -> "pounce";
            case "transform" -> "transform";
            case "attack" -> "attack";
            default -> entity.hurtTime > 0 ? "hurt"
                    : entity.swinging ? "attack"
                    : parent.walkAnimationSpeed > 0.02F ? "walk" : "idle";
        };
    }

    // ==================== 仓内（培育中的身体） ====================

    /** 仓内动画宿主：一具不加入世界的离屏分身，只用来算动画快照 */
    private static final java.util.Map<UUID, CloneAvatarEntity> CHAMBER_HOSTS = new java.util.LinkedHashMap<>();
    private static net.minecraft.client.multiplayer.ClientLevel chamberLevel;

    /** 离屏宿主的 id 发号器：递减负数，保证非 0 且不与世界里分配的 id 撞车 */
    private static int nextOffscreenId = -1;

    /** 拿（或重建）仓内的离屏动画宿主；玩家还没进世界时返回 null。 */
    private static CloneAvatarEntity chamberHost(UUID bodyUuid) {
        var level = Minecraft.getInstance().level;
        if (level == null) return null;
        if (chamberLevel != level) {
            CHAMBER_HOSTS.clear();
            chamberLevel = level;
        }
        CloneAvatarEntity host = CHAMBER_HOSTS.get(bodyUuid);
        if (host == null) {
            host = ModEntities.CLONE_AVATAR.create(level, EntitySpawnReason.LOAD);
            if (host == null) return null;
            host.setId(nextOffscreenId--);
            host.setUUID(bodyUuid);
            if (CHAMBER_HOSTS.size() >= 64) CHAMBER_HOSTS.remove(CHAMBER_HOSTS.keySet().iterator().next());
            CHAMBER_HOSTS.put(bodyUuid, host);
        }
        return host;
    }

    /**
     * 仓内版本：数据源换成 {@code CloneBodySyncS2C} 的客户端缓存（按身体 UUID），
     * 而不是实体附件 —— 培育中的身体还没成为实体。
     * <p>
     * 动画需要一个 {@code GeoAnimatable} 宿主，所以借用一具不加入世界的离屏分身：
     * 外观数据（角色 / 异兽能力）全部来自缓存，宿主只负责让控制器把动画快照算出来。
     */
    public static void extractForChamber(UUID bodyUuid, AvatarRenderState parent, float partialTick) {
        parent.addGeckolibData(CreaturePlayerRenderer.MODEL, "");
        parent.addGeckolibData(NiunaiXRenderData.ACTIVE, false);
        parent.addGeckolibData(CloneGourdRenderer.CLIP, "");
        parent.addGeckolibData(SNAPSHOT_GOURD, null);
        parent.addGeckolibData(SNAPSHOT_ORGAN, java.util.List.of());
        CorpseOriginClient.ClientCloneBody cached = bodyUuid == null ? null
                : CorpseOriginClient.cloneBodyDataCache.get(bodyUuid);
        if (cached == null) {
            return;   // 还没有外观数据（旧数据 / 包还没到）→ 按原样画普通克隆人
        }
        CloneAvatarEntity host = chamberHost(bodyUuid);
        if (host == null) {
            return;
        }
        host.setAttached(SurvivalGrowth.BODY, cached.evolutionParts().copy());
        parent.addGeckolibData(SNAPSHOT_ORGAN, java.util.List.of());
        extractOrgans(host, parent, partialTick);
        host.setAttached(ChapterActorState.ROLE, cached.characterId());
        host.setAttached(CreatureAbilities.INFANT, cached.infant());
        host.setAttached(CreatureAbilities.BEAR_ARMS, cached.bearArms());
        // 仓内没有实体渲染状态，光照只能自己塞（不补的话模型在全黑处会自带发光）
        parent.addGeckolibData(DataTickets.PACKED_LIGHT, parent.lightCoords);

        // ① 角色全身模型（仓内不播动作，统一用 idle）
        String modelId = creatureModelFor(cached.characterId(), cached.infant());
        CloneCreatureRenderer creature = CloneCreatureRenderer.get(modelId);
        if (creature != null) {
            parent.addGeckolibData(CreaturePlayerRenderer.MODEL, modelId);
            parent.addGeckolibData(CreaturePlayerRenderer.CLIP, "idle");
            parent.addGeckolibData(CreaturePlayerRenderer.ARMS, cached.bearArms());
            parent.addGeckolibData(CreaturePlayerRenderer.INFANT, cached.infant());
            creature.extractRenderState(host, parent, partialTick);
            return;
        }

        // ② 开胃奶背挂（判定与实体版同源，只是把角色换成身体缓存里的）
        CloneNiunaiRenderer niunai = CloneNiunaiRenderer.get();
        if (niunai != null && KaiWeiNai.ID.equals(organRole(cached.evolutionParts(), cached.characterId()))) {
            parent.addGeckolibData(NiunaiXRenderData.ACTIVE, true);
            parent.addGeckolibData(NiunaiXRenderData.ATTACKING, false);
            parent.addGeckolibData(NiunaiXRenderData.PARRYING, false);
            parent.addGeckolibData(NiunaiXRenderData.AGE_TICKS, (float) host.tickCount);
            niunai.extractRenderState(host, parent, partialTick);
        }

        // ③ 葫芦（葫芦小金刚）：仓内不播动作，按形态播常态 clip
        CloneGourdRenderer gourdRenderer = CloneGourdRenderer.get();
        if (gourdRenderer != null && GOURD_ROLE.equals(organRole(cached.evolutionParts(), cached.characterId()))) {
            var body = cached.evolutionParts();
            if (!body.getBooleanOr("gourd_detached", false) && !body.getBooleanOr("gourd_dead", false)) {
                int form = gourdForm(body, host.level().getGameTime());
                AvatarRenderState gourdState = new AvatarRenderState();
                gourdState.addGeckolibData(CloneGourdRenderer.COLOR, gourdColor(form));
                gourdState.addGeckolibData(CloneGourdRenderer.CLIP, gourdClip(form, false));
                gourdRenderer.extractRenderState(host, gourdState, partialTick);
                gourdState.addGeckolibData(DataTickets.PACKED_LIGHT, parent.lightCoords);
                parent.addGeckolibData(SNAPSHOT_GOURD, gourdState);
            }
        }
    }

    // ==================== 提交 ====================

    /** 整身替换型：提交角色模型并返回 true（调用方据此跳过原版模型）。 */
    public static boolean submitReplacing(AvatarRenderState state, PoseStack poses,
                                          SubmitNodeCollector collector, CameraRenderState camera) {
        String modelId = state.getGeckolibData(CreaturePlayerRenderer.MODEL);
        CloneCreatureRenderer renderer = CloneCreatureRenderer.get(modelId);
        if (renderer == null) {
            return false;
        }
        poses.pushPose();
        try {
            if (state.scale != 1.0F) {
                poses.scale(state.scale, state.scale, state.scale);
            }
            renderer.submit(state, poses, collector, camera);
            diagnoseSubmit(state, "整身模型 " + modelId);
        } finally {
            poses.popPose();
        }
        return true;
    }

    public static boolean replacesBody(AvatarRenderState state) {
        java.util.List<?> frames = state.getGeckolibData(SNAPSHOT_ORGAN);
        return frames != null && frames.stream().anyMatch(f -> f instanceof OrganFrame frame && frame.slot().replacesBody());
    }

    @Override
    public void submit(PoseStack poses, SubmitNodeCollector collector, int light,
                       AvatarRenderState state, float yaw, float pitch) {
        PlayerModel model = getParentModel();
        model.setupAnim(state);
        submitBackMount(state, poses, collector, model, new CameraRenderState());
        // 葫芦走层里的 pose —— 它与玩家侧器官层完全同构（层 pose 就是"模型根"），
        // 配合 CloneGourdRenderer 已把 adjustRenderPose / scaleModelForRender 置空，
        // performRenderPass 不会再多套一次实体变换。
        submitGourd(state, poses, collector, model, new CameraRenderState());
        // 自定义器官：按槽位的 joint 挂到对应骨骼上（与玩家侧同一套锚点与变换）
        submitOrgans(state, poses, collector, model);
    }

    /**
     * 提交自定义器官帧：每个器官按自己的槽位参数挂到对应骨骼上。
     * <p>
     * 变换完全复刻玩家侧的 {@code CustomOrganLayer.submitFrames}（joint 骨骼 → 槽位偏移 →
     * 槽位旋转 → 槽位缩放 + Y/Z 翻转），锚点参数来自装配时保存的 {@link OrganSlot}。
     */
    public static void submitOrgans(AvatarRenderState state, PoseStack poses, SubmitNodeCollector collector,
                                    PlayerModel model) {
        if (state == null || state.isInvisible || model == null) {
            return;
        }
        java.util.List<?> frames = state.getGeckolibData(SNAPSHOT_ORGAN);
        if (frames == null || frames.isEmpty()) {
            return;
        }
        for (Object o : frames) {
            if (!(o instanceof OrganFrame frame)) {
                continue;
            }
            OrganSlot slot = frame.slot();
            ModelPart joint = switch (slot.joint()) {
                case "head" -> model.head;
                case "left_arm" -> model.leftArm;
                case "right_arm" -> model.rightArm;
                case "left_leg" -> model.leftLeg;
                case "right_leg" -> model.rightLeg;
                default -> model.body;
            };
            PoseStack local = new PoseStack();
            local.last().set(poses.last());
            if (slot.replacesBody()) local.translate(0, 1.5, 0);
            else joint.translateAndRotate(local);
            local.translate(slot.x() / 16.0F, slot.y() / 16.0F, slot.z() / 16.0F);
            local.mulPose(Axis.XP.rotationDegrees(slot.rx()));
            local.mulPose(Axis.YP.rotationDegrees(slot.ry()));
            local.mulPose(Axis.ZP.rotationDegrees(slot.rz()));
            local.scale(slot.mirror() ? -slot.scale() : slot.scale(), -slot.scale(), -slot.scale());
            try {
                frame.renderer().performRenderPass(frame.state(), local, collector, new CameraRenderState());
            } catch (Exception error) {
                reportFailure("submit:" + slot.organ(), error);
            }
        }
    }

    /**
     * 提交葫芦骨骼（调用方负责 {@link PoseStack} 的 push/pop）。
     * <p>
     * ⚠️ 必须在<b>模型空间</b>调用（{@code pose} 处于模型根，即原版做完 {@code scale(-1,-1,1)}
     * 与垂直补偿之后）：葫芦是挂在 {@code body} 骨骼上的器官，锚点与玩家侧的
     * {@code CustomOrganLayer} 完全一致。
     * <p>
     * ⚠️ 这里走 {@code performRenderPass} 而不是 {@code submit}：后者会再应用一次实体变换
     * （旋转 / 翻转 / 位移），模型会被翻到别处去。
     */
    public static void submitGourd(AvatarRenderState state, PoseStack poses, SubmitNodeCollector collector,
                                   PlayerModel model, CameraRenderState camera) {
        if (state == null || state.isInvisible || model == null) {
            return;
        }
        AvatarRenderState gourdState = state.getGeckolibData(SNAPSHOT_GOURD);
        if (gourdState == null) return;
        String clip = gourdState.getGeckolibData(CloneGourdRenderer.CLIP);
        if (clip == null || clip.isEmpty()) {
            return;   // 这一帧没有葫芦
        }
        CloneGourdRenderer renderer = CloneGourdRenderer.get();
        if (renderer == null) {
            return;
        }
        // 锚点与玩家侧器官完全一致（OrganSlot: joint=body, x=0, y=14, z=5, scale=0.45）
        PoseStack local = new PoseStack();
        local.last().set(poses.last());
        model.body.translateAndRotate(local);
        local.translate(0.0F, 14.0F / 16.0F, 5.0F / 16.0F);
        local.scale(0.45F, -0.45F, -0.45F);
        try {
            renderer.performRenderPass(gourdState, local, collector, camera);
            diagnoseSubmit(state, "葫芦 " + clip);
        } catch (Exception error) {
            reportFailure("gourd", error);
        }
    }

    /** Submit in animated vanilla model space, just like the gourd. */
    public static void submitBackMount(AvatarRenderState state, PoseStack poses,
                                       SubmitNodeCollector collector, PlayerModel model, CameraRenderState camera) {
        if (state.isInvisible || !Boolean.TRUE.equals(state.getGeckolibData(NiunaiXRenderData.ACTIVE))) {
            return;
        }
        CloneNiunaiRenderer renderer = CloneNiunaiRenderer.get();
        if (renderer == null) {
            return;
        }
        renderer.performRenderPass(state,
                xiaoshi2022.corpseorigin.client.renderer.player.NiunaiXRenderer.bodyPose(poses, model), collector, camera);
        diagnoseSubmit(state, "背挂");
    }
}
