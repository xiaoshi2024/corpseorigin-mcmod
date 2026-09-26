package xiaoshi2022.corpseorigin.skill.longyou;

import net.fabricmc.fabric.api.attachment.v1.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.codec.ByteBufCodecs;
import xiaoshi2022.corpseorigin.CorpseOrigin;

public final class BloodReserve {
    public static final int MAX = 600, RESTORE_COST = 200;

    /** 「尸兄肉块」一次补的气血。 */
    public static final int FLESH_BLOOD = 20;
    /**
     * 「象棋尸兄」整具躯体一次补的气血。
     * <p>
     * 按既有的"生物血量 → 血肉"公式折算（{@link xiaoshi2022.corpseorigin.skill.chapter.GourdBalance#flesh}）：
     * 象棋尸兄 40 点血 → 60。这里直接引它的血量常量算，改了血量不会走样。
     */
    public static final int CHESS_ZBRS_BLOOD = xiaoshi2022.corpseorigin.skill.chapter.GourdBalance
            .flesh(xiaoshi2022.corpseorigin.block.entity.CNChessZbrsBlockEntity.DEFAULT_MAX_HEALTH);

    private static final java.util.Map<java.util.UUID,xiaoshi2022.corpseorigin.growth.BalanceRules.Window> COMBAT=new java.util.HashMap<>();
    public static void addCombat(ServerPlayer p,float damage){
        if(!p.isAlive()||p.isSpectator()||!Float.isFinite(damage)||damage<=0)return;
        int limit=Math.clamp(xiaoshi2022.corpseorigin.config.CorpseConfig.get().growth.combatBloodPerSecond,0,600);
        int amount=COMBAT.computeIfAbsent(p.getUUID(),k->new xiaoshi2022.corpseorigin.growth.BalanceRules.Window())
                .take(p.level().getGameTime(),(int)Math.min(600,Math.ceil(damage*.25)),limit);
        if(amount>0)add(p,amount);
    }
    public static final AttachmentType<Integer> VALUE = AttachmentRegistry.create(CorpseOrigin.id("blood_reserve"),
            b -> b.initializer(() -> 0).persistent(com.mojang.serialization.Codec.INT)
                    .syncWith(ByteBufCodecs.VAR_INT, AttachmentSyncPredicate.all()));
    public static void init() {
        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT.register((h,s)->COMBAT.remove(h.player.getUUID()));
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(s->COMBAT.clear());
        net.fabricmc.fabric.api.event.player.UseItemCallback.EVENT.register((player, level, hand) -> {
            var stack = player.getItemInHand(hand);
            if (!player.isShiftKeyDown()) return net.minecraft.world.InteractionResult.PASS;
            // ① 尸兄肉块：够格的尸族都能吸；② 象棋尸兄是"整具尸兄躯体"：只有尸兄身体能吸
            boolean flesh = stack.is(xiaoshi2022.corpseorigin.registry.ModItems.ZBR_FLESH);
            boolean chess = !flesh && stack.is(xiaoshi2022.corpseorigin.registry.ModItems.CN_CHESS_ZBRS);
            if (!flesh && !chess) return net.minecraft.world.InteractionResult.PASS;
            if (!(player instanceof ServerPlayer serverPlayer)) return net.minecraft.world.InteractionResult.PASS;
            if (chess ? !xiaoshi2022.corpseorigin.growth.WeaponEligibility.corpseBody(serverPlayer)
                    : !isEligible(serverPlayer))
                return net.minecraft.world.InteractionResult.PASS;
            if (serverPlayer.getAttachedOrCreate(VALUE) >= MAX
                    && !xiaoshi2022.corpseorigin.growth.FreeGrowth.isFree(serverPlayer))
                return net.minecraft.world.InteractionResult.FAIL;
            int amount = chess ? CHESS_ZBRS_BLOOD : FLESH_BLOOD;
            stack.shrink(1);
            int gained = Math.min(amount, MAX - get(serverPlayer));
            add(serverPlayer, amount);
            xiaoshi2022.corpseorigin.growth.SurvivalGrowth.fleshConsumed(serverPlayer);
            serverPlayer.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("message.corpseorigin.blood_reserve.text_01", gained));
            return net.minecraft.world.InteractionResult.SUCCESS;
        });
    }
    public static boolean isEligible(ServerPlayer player) {
        if (xiaoshi2022.corpseorigin.character.PlayerCharacterData.get(player).getLearnedSkills(player.getUUID())
                .stream().anyMatch(path -> xiaoshi2022.corpseorigin.skill.SkillResourceRules.cost(path, 0).blood() > 0)
                || xiaoshi2022.corpseorigin.growth.WeaponEligibility.vampire(player)) return true;
        String role = xiaoshi2022.corpseorigin.character.CharacterManager.getInstance().getPlayerCharacterId(player);
        int level = xiaoshi2022.corpseorigin.skill.EvolutionManager.getLevel(
                xiaoshi2022.corpseorigin.character.PlayerCharacterData.get(player).getEarnedPoints(player.getUUID()));
        return BloodReserveRules.eligible(role,
                xiaoshi2022.corpseorigin.component.PlayerCorpseComponent.isCorpse(player), level)
                || xiaoshi2022.corpseorigin.component.PlayerCorpseComponent.isCorpse(player)
                && (xiaoshi2022.corpseorigin.growth.SurvivalGrowth.has(player,"wings")||xiaoshi2022.corpseorigin.growth.SurvivalGrowth.has(player,"gills"));
    }
    public static void add(ServerPlayer p, int amount) {
        p.setAttached(VALUE, (int)Math.clamp((long)p.getAttachedOrCreate(VALUE) + amount, 0, MAX));
    }
    public static boolean spend(ServerPlayer p) {
        if (p.getAttachedOrCreate(VALUE) < RESTORE_COST) {
            p.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("message.corpseorigin.blood_reserve.text_02"));
            return false;
        }
        add(p, -RESTORE_COST);
        return true;
    }

    /** 当前气血余量 */
    public static int get(ServerPlayer p) {
        return p.getAttachedOrCreate(VALUE);
    }

    /**
     * 通用气血消耗（左护法「唤龙」复活青龙等用）。
     * 余量不足返回 false（调用方自己提示），不做任何改动。
     */
    public static boolean spend(ServerPlayer p, int amount) {
        if(amount<0)return false;
        if (p.getAttachedOrCreate(VALUE) < amount) {
            return false;
        }
        add(p, -amount);
        return true;
    }
}
