package xiaoshi2022.corpseorigin.skill.longyou;

import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.food.FoodData;
import xiaoshi2022.corpseorigin.character.LongYou;
import xiaoshi2022.corpseorigin.character.PlayerCharacterData;
import xiaoshi2022.corpseorigin.entity.CloneAvatarEntity;
import xiaoshi2022.corpseorigin.network.CorpseNetwork;
import xiaoshi2022.corpseorigin.registry.ModEntities;
import xiaoshi2022.corpseorigin.shell.CharacterShellStateComponent;
import xiaoshi2022.corpseorigin.shell.PlayerBodySnapshot;
import xiaoshi2022.corpseorigin.shell.ServerShell;
import xiaoshi2022.corpseorigin.shell.ShellState;

import java.util.List;

/**
 * 尸王的"换身体"手段：金蝉脱壳 / 血肉重塑。
 * <p>
 * 两个技能共用同一套动作，区别只在"换进去的新身体长什么样、代价是什么"：
 * <ol>
 *   <li>把<b>当前这具身体</b>蜕成一个分身实体，站在玩家原地 —— 背包、装备、角色全留在它身上；</li>
 *   <li>再把玩家意识写进一具新身体（见 {@link #buildOwnBody}），走的是和克隆转移同一条
 *       {@link ServerShell#apply} 路径。</li>
 * </ol>
 * 所以"换回来"完全不用新机制：分身就在身体列表里，选它就能换回去，行囊也跟着回来。
 * <p>
 * ⚠️ 原版 {@code SCALE} 属性会被 {@code LivingEntity#sanitizeScale} 夹在 [1/16, 16] 之间，
 * 所以"拇指大小"最小也就是 1/16 左右，再小就不是夹不夹的问题、而是视角贴地没法玩了。
 */
public final class BodyTransplantHandler {

    private BodyTransplantHandler() {
    }

    /** 血肉重塑要求的最低饱食度（再饿就"重塑不出来"） */
    private static final int MIN_FOOD_TO_RESHAPE = 6;

    /**
     * 右键"穿回旧身体"。
     * <p>
     * 只在<b>处于金蝉脱壳状态</b>（人正缩在拇指原体里）时才接管 —— 平时右键分身不该把人瞬移过去，
     * 那种跨身体的转移本来就该走克隆仓面板。判定直接用"体型被缩小过"，
     * 不需要额外记一份状态（也就不会因为掉线/换身留下脏标记）。
     */
    public static void register() {
        UseEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
            if (level.isClientSide() || hand != InteractionHand.MAIN_HAND) {
                return InteractionResult.PASS;
            }
            if (!(player instanceof ServerPlayer caster) || !(entity instanceof CloneAvatarEntity body)) {
                return InteractionResult.PASS;
            }
            if (body.isAbandonedBody()) {
                return BodyPossession.possess(caster, body) ? InteractionResult.SUCCESS : InteractionResult.PASS;
            }
            if (!isInOriginalBody(caster) || !caster.getUUID().equals(body.getOwnerUuid())) {
                return InteractionResult.PASS;
            }
            return returnToBody(caster, body) ? InteractionResult.SUCCESS : InteractionResult.PASS;
        });
    }

    /** 这位玩家是不是正缩在「原体」里（即处于金蝉脱壳状态） */
    public static boolean isInOriginalBody(ServerPlayer player) {
        return player.getAttributeValue(Attributes.SCALE) < 1.0F;
    }

    /**
     * 穿回指定的旧身体：原体重新缩回躯壳之内，所以那具缩小的身体不留在世界上
     * （想再脱壳，技能可以随时再造一颗）。
     *
     * @return true = 换身成功
     */
    public static boolean returnToBody(ServerPlayer player, CloneAvatarEntity body) {
        ShellState state = body.getBodyState();
        if (state == null) {
            return false;
        }
        // ★ 真过场：镜头播完（见 CorpseNetwork#playTransferCutscene）才真正穿回去，
        //   终点是那具旧身体所在的位置/维度，所以镜头会朝它甩出去
        CorpseNetwork.playTransferCutscene(player,
                body.blockPosition(),
                player.getDirection(),
                body.level().dimension().identifier(),
                p -> doReturnToBody(p, body));
        return true;
    }

    /** 过场结束后的实际动作：取下快照 → 移除分身（remove() 会顺手清掉身体索引）→ 写回玩家 */
    private static void doReturnToBody(ServerPlayer player, CloneAvatarEntity body) {
        ShellState state = body.getBodyState();
        if (state == null || !body.isAlive() || body.isRemoved()
                || !player.getUUID().equals(body.getOwnerUuid()) || !isInOriginalBody(player)) {
            return;   // 过场这几秒里身体被取走了
        }
        // The shell can move during the cutscene; its saved snapshot is not a live location.
        var destination = body.position();
        float yaw = body.getYRot();
        float pitch = body.getXRot();
        state.setPos(body.blockPosition());
        state.setWorld(body.level().dimension().identifier());
        body.discard();
        ServerShell.of(player).apply(state);
        // ShellState stores block coordinates; preserve the body's exact position and facing.
        player.teleportTo(destination.x, destination.y, destination.z);
        player.setYRot(yaw);
        player.setXRot(pitch);
    }

    /**
     * 金蝉脱壳：把当前身体蜕成分身，意识直接转移进藏在体内的「原体」——
     * 原著里那颗拇指大小、黑发黑身的真身。
     *
     * @return true = 换身已排队（镜头播完才真正执行）
     */
    public static boolean shedIntoOriginalBody(ServerPlayer player) {
        if (UndeadBodyState.sealed(player) || isInOriginalBody(player)) {
            if (isInOriginalBody(player))
                player.sendOverlayMessage(Component.translatable("skill.corpseorigin.golden_cicada_shell.already_original"));
            return false;
        }
        if (!(player.level() instanceof ServerLevel)) {
            return false;
        }
        // 原地换身：镜头终点就是原地，水平段会沿身体朝向的反方向把意识抽出来
        CorpseNetwork.playTransferCutscene(player, player.blockPosition(), player.getDirection(),
                player.level().dimension().identifier(),
                BodyTransplantHandler::doShedIntoOriginalBody);
        return true;
    }

    private static void doShedIntoOriginalBody(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level) || isInOriginalBody(player)
                || UndeadBodyState.sealed(player)) {
            return;
        }
        ShellState original = buildOwnBody(player, LongYou.ORIGINAL_BODY_SCALE, false);
        shedOldBodyAsAvatar(player, level);
        ServerShell.of(player).apply(original);
        // ★ 保底：缩进原体后「血肉重塑」必须能用（哪怕从没在技能树里点过它），
        //   否则人被困在拇指身体里出不来。
        grantReshapeFallback(player);
        player.sendOverlayMessage(Component.translatable(
                "skill.corpseorigin." + GoldenCicadaShellSkill.PATH + ".done"));
    }

    /** 把「血肉重塑」直接学会并同步给客户端 —— 原体状态的脱身保底 */
    private static void grantReshapeFallback(ServerPlayer player) {
        PlayerCharacterData data = PlayerCharacterData.get(player);
        if (!data.hasLearned(player.getUUID(), FleshReshapeSkill.PATH)) {
            data.learnSkill(player.getUUID(), FleshReshapeSkill.PATH);
            CorpseNetwork.sendEvolutionSync(player);
        }
    }

    /**
     * 血肉重塑：消耗一半饱食度，血肉重构成一具崭新的正常身体。
     * <p>
     * 和「金蝉脱壳」不同，这里<b>不留旧身体</b>：模拟的是"原体重新长出一具新躯体"，
     * 意识是搬进去的，不是把旧壳蜕在原地。所以随身物品要跟着一起进新身体，
     * 否则东西会连同被丢掉的那具身体一起消失。
     *
     * @return true = 换身已排队（镜头播完才真正换；太饿则直接 false）
     */
    public static boolean reshapeBody(ServerPlayer player) {
        if (UndeadBodyState.sealed(player)) return false;
        if (!(player.level() instanceof ServerLevel)) {
            return false;
        }

        FoodData food = player.getFoodData();
        if (LongYou.ID.equals(PlayerCharacterData.get(player).getCharacterId(player.getUUID()))) {
            if (!BloodReserve.spend(player)) return false;
            CorpseNetwork.playTransferCutscene(player, player.blockPosition(), player.getDirection(),
                    player.level().dimension().identifier(), BodyTransplantHandler::doReshapeBody);
            return true;
        }
        // 脱身保底：人正缩在原体里的话，饿着也得放得出来（代价自然只剩"没什么可扣的"），
        // 不然被饿死/困在拇指身体里就真出不来了
        boolean fallback = isInOriginalBody(player);
        if (!fallback && food.getFoodLevel() < MIN_FOOD_TO_RESHAPE) {
            player.sendOverlayMessage(
                    Component.translatable("skill.corpseorigin.flesh_reshape.hungry"));
            return false;
        }
        // 代价：一半饱食度（饥饿值与饱和一起砍半）
        // ★ 先扣：过场只是表现，判定和消耗不该跟着镜头一起延后
        food.setFoodLevel(food.getFoodLevel() / 2);
        food.setSaturation(food.getSaturationLevel() / 2.0F);

        // carryInventory = true：背包与装备原样带进新身体（不留壳，所以不能丢东西）
        CorpseNetwork.playTransferCutscene(player, player.blockPosition(), player.getDirection(),
                player.level().dimension().identifier(),
                BodyTransplantHandler::doReshapeBody);
        return true;
    }

    /** 过场结束后的实际动作：不留旧身体，行囊跟着意识一起进新身体 */
    private static void doReshapeBody(ServerPlayer player) {
        ServerShell.of(player).apply(buildOwnBody(player, 1.0F, true));
        player.sendOverlayMessage(Component.translatable(
                "skill.corpseorigin." + FleshReshapeSkill.PATH + ".done"));
    }

    /** An unowned, empty shell; inventory stays with the living original body. */
    public static void abandonBody(ServerPlayer player) {
        if (UndeadBodyState.sealed(player)) return;
        if (!LongYou.ID.equals(PlayerCharacterData.get(player).getCharacterId(player.getUUID()))) return;
        if (isInOriginalBody(player)) {
            player.sendOverlayMessage(Component.translatable("skill.corpseorigin.flesh_abandon.original"));
            return;
        }
        CorpseNetwork.playTransferCutscene(player, player.blockPosition(), player.getDirection(),
                player.level().dimension().identifier(), p -> {
                    if (!p.isAlive() || isInOriginalBody(p)
                            || !LongYou.ID.equals(PlayerCharacterData.get(p).getCharacterId(p.getUUID()))) return;
                    ServerLevel level = (ServerLevel)p.level();
                    ShellState empty = buildOwnBody(p, 1.0F, false);
                    CloneAvatarEntity body = new CloneAvatarEntity(ModEntities.CLONE_AVATAR, level);
                    body.setBodyState(empty);
                    body.setOwnerUuid(null);
                    body.setAbandonedSkin(p.getUUID());
                    body.setAbandonedBody(true);
                    body.setNoAi(true);
                    body.setPersistenceRequired();
                    body.setActive(true);
                    body.setProgress(1.0F);
                    body.setCustomName(Component.translatable("entity.corpseorigin.abandoned_body"));
                    body.setCustomNameVisible(true);
                    body.setPos(p.getX(), p.getY(), p.getZ());
                    body.setYRot(p.getYRot());
                    if (!level.addFreshEntity(body)) return;
                    ServerShell.of(p).apply(buildOwnBody(p, LongYou.ORIGINAL_BODY_SCALE, true));
                    grantReshapeFallback(p);
                    p.sendOverlayMessage(Component.translatable("skill.corpseorigin.flesh_abandon.done"));
                });
    }

    // ==================== 技能不随换身丢失 ====================

    /*
     * 换身不该让技能消失。
     * <p>
     * 真正的保证在 {@code ServerPlayerShellMixin#apply} —— 那是所有换身入口（克隆仓面板 /
     * 金蝉脱壳 / 血肉重塑 / 右键回旧身体 / 死亡夺舍）的唯一汇合点：<b>尸王（龙右）</b>换身时
     * 会在写入身体数据前后把"角色 id + 已学技能 + 进化点"原样带回。
     * 另外「已学技能」的读写本身也必须用 {@code StringTag#value()} 取字符串
     * （26.2 的 {@code asString()} 返回 {@code Optional}，早期写法会把技能存成 {@code Optional[xxx]}）。
     * 本类只负责把"现场这份角色数据"填进要换的身体快照里
     * （见 {@link #carryLiveCharacterData}），让蜕下来的壳 / 分身也带着正确的身份。
     */

    /**
     * 造一具"自己的身体"。
     * <p>
     * 角色 / 尸兄体质 / 内力 / 进化点都照抄当前状态。
     *
     * @param scale          新身体的大小倍率
     * @param carryInventory true = 随身物品跟着进新身体（不留旧身体时用，否则东西会丢）；
     *                       false = 新身体是干净的、行囊留给蜕下来的旧身体
     *                       （金蝉脱壳走这条：背包与装备留在壳上，右键回去才拿得回来，
     *                       也避免"换身就复制一份物品"）
     */
    private static ShellState buildOwnBody(ServerPlayer player, float scale, boolean carryInventory) {
        ShellState state = ShellState.of(player, player.blockPosition());
        if (!carryInventory) {
            state.setBody(PlayerBodySnapshot.blank(player));
            state.setEquipment(List.of());
        }
        state.setScale(scale);
        carryLiveCharacterData(player, state);
        return state;
    }

    /** 把当前这具身体蜕成分身实体，留在玩家原地（背包/装备/角色都记在它身上） */
    private static void shedOldBodyAsAvatar(ServerPlayer player, ServerLevel level) {
        ShellState oldBody = ShellState.of(player, player.blockPosition());
        carryLiveCharacterData(player, oldBody);

        CloneAvatarEntity avatar = new CloneAvatarEntity(ModEntities.CLONE_AVATAR, level);
        avatar.setOwnerUuid(player.getUUID());
        avatar.setOwnerName(player.getScoreboardName());
        avatar.setBodyState(oldBody);
        avatar.setActive(true);
        avatar.setProgress(1.0F);
        avatar.setPos(player.getX(), player.getY(), player.getZ());
        avatar.setYRot(player.getYRot());
        level.addFreshEntity(avatar);

        // 刷新一下身体列表，客户端马上就能看到这具"蜕下来的壳"
        CorpseNetwork.refreshShellStates(player);
    }

    /**
     * 用<b>活着的玩家</b>这份角色数据覆盖身体快照里的那一份。
     * <p>
     * 换身体不该让技能消失，但 {@code CharacterShellStateComponent#applyTo} 是拿
     * <b>身体快照里的角色数据</b>反写玩家数据的（已学技能、进化点都在里面）——
     * 快照那份只要偏旧或为空（培育出来的身体被清过角色身份、快照抓取时机不对……），
     * 一换身玩家的技能就被抹平了。这里统一以现场数据为准，保证脱壳 / 重塑来回换不掉技能。
     */
    private static void carryLiveCharacterData(ServerPlayer player, ShellState state) {
        CharacterShellStateComponent live = new CharacterShellStateComponent(player);
        CharacterShellStateComponent target =
                state.getComponent().as(CharacterShellStateComponent.class);
        if (target != null) {
            target.clone(live);
        }
    }
}
